package com.plokie.mixin;

import com.mojang.serialization.Codec;
import com.plokie.Splatoon;
import com.plokie.classes.SplatoonClasses;
import com.plokie.classes.abilities.Ability;
import com.plokie.classes.abilities.AbilityManager;
import com.plokie.customitems.CustomItem;
import com.plokie.helpers.Affects;
import com.plokie.helpers.Effects;
import com.plokie.helpers.Teams;
import com.plokie.interfaces.IPlayerMixin;
import com.plokie.interfaces.IPlayerTeamMixin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.stream.Stream;

@Mixin(Player.class)
public class PlayerMixin implements IPlayerMixin {
    @Shadow
    public float experienceProgress;
    @Shadow
    public int experienceLevel;
    @Shadow
    @Final
    private Inventory inventory;
    @Unique private Player player;

    @Unique private Input input = new Input(false, false, false, false, false, false, false);
    @Unique private Input oldInput = new Input(false, false, false, false, false, false, false);
    @Override
    public void setInputPacket(Input input)
    {
        this.input = input;
    }
    @Override
    public Input getInput() { return this.input; }
    @Override
    public Input getPreviousInput() { return this.oldInput; }

    boolean punchedThisTick = false;

    @Override
    public void setPunched() {
        punchedThisTick = true;
    }

    @Override public boolean punchedThisTick()
    {
        return punchedThisTick;
    }

    @Unique
    SplatoonClasses.SplatoonClass splatoonClass = null;

    @Override
    public void setClass(SplatoonClasses.SplatoonClass klass) {

        if(splatoonClass != null)
        {
            ItemStack airItem = new ItemStack(Items.AIR);

            int idx=0;
            for(CustomItem customItem : splatoonClass.definition.customItems)
            {
                int slotIdx = idx;
                if(slotIdx >= 2) {
                    slotIdx += abilities.size();
                }

                player.getInventory().setItem(slotIdx, airItem);

                idx++;
            }

            for(Map.Entry<String, AttributeModifier> entry : splatoonClass.definition.attributes.entrySet())
            {
//                Affects.removeAttributeModifier(player, entry.getKey(), entry.getValue().id().getPath());
                Affects.removeAttributeModifier(player, entry.getKey(), entry.getValue());
            }

            for(Map.Entry<Holder<MobEffect>, Integer> entry : splatoonClass.definition.effects.entrySet())
            {
                Effects.clearPotionEffect(player, entry.getKey());
            }
        }

        splatoonClass = klass;

        revokeAllAbilities();

        if(splatoonClass != null)
        {
            splatoonClass.definition.abilities.forEach(abilityEnum ->
            {
                grantAbility(abilityEnum.Construct());
            });

            for(Map.Entry<String, AttributeModifier> entry : splatoonClass.definition.attributes.entrySet())
            {
                Affects.setAttributeModifier(player, entry.getKey(), entry.getValue());
            }

            for(Map.Entry<Holder<MobEffect>, Integer> entry : splatoonClass.definition.effects.entrySet())
            {
                //Effects.givePotionEffect(player, entry.getKey(), 9999, entry.getValue(), true);
                // this crashes the server if the player isnt fully connected, so lets queue it for application
                classEffectQueue.add(entry);
            }
        }

    }

    @Unique List<Map.Entry<Holder<MobEffect>, Integer>> classEffectQueue = new ArrayList<>();

    @Unique private List<Ability> abilities = new ArrayList<Ability>();

    @Unique IPlayerTeamMixin playerTeam = null;
    @Unique Block groundBlock = null;
    @Unique Block wallBlock = null;

    @Unique boolean inInk = false;
    @Unique boolean onWall = false;
    @Unique int timeNotInInk = 0;

    @Override public boolean isInInk() { return inInk; }
    @Override public boolean isInInkOnWall() { return onWall; }

    @Unique float ink = 0.6f;
    @Override
    public void changeInk(float delta) {
        ink += delta;
        if(ink > 1.0f) ink = 1.0f;
        else if(ink < 0.0f) ink = 0.0f;

        player.experienceProgress = ink;
        ((ServerPlayer)player).connection.send(new ClientboundSetExperiencePacket(
           player.experienceProgress,
           player.totalExperience,
           player.experienceLevel
        ));
    }
    @Override public float getInk() { return ink; }


    @Override
    public List<Ability> getAbilities()
    {
        return abilities;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        player = (Player)(Object)this;
    }


    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {

        ServerLevel level = (ServerLevel)player.level();

        if(level.isClientSide()) return;

        if(!classEffectQueue.isEmpty())
        {
            Effects.givePotionEffect(player, MobEffects.INSTANT_HEALTH, 1, 200, true);

            for(Map.Entry<Holder<MobEffect>, Integer> entry : classEffectQueue)
            {
                Effects.givePotionEffect(player, entry.getKey(), 99999, entry.getValue(), true);
            }
            classEffectQueue.clear();
        }

        ItemStack itemInHand = player.getItemInHand(player.getUsedItemHand());
        Arrays.stream(CustomItem.values()).forEach(item -> {
            if(itemInHand.getItemName().equals(item.getItem().getItemName()))
            {
                item.getItemDefinition().getItemInterface().whileHeld(player);
            }
        });

        IPlayerTeamMixin playerTeam = Teams.getTeamMixinFromPlayer(player);
        if(playerTeam != this.playerTeam) // if team has changes
        {
            // re-cache blocks
            groundBlock = null;
            wallBlock = null;
            this.playerTeam = playerTeam;

            if(this.playerTeam != null)
            {
                groundBlock = playerTeam.getGroundBlock();
                wallBlock = playerTeam.getWallBlock();
            }
        }


        boolean wasInInk = inInk;

        if(groundBlock != null && wallBlock != null && splatoonClass != null)
        {
            inInk = false;
            onWall = false;

            if(player.isCrouching())
            {
                BlockPos playerPos = player.getBlockPosBelowThatAffectsMyMovement();

                for(int y=-2; y<=-1; y++)
                {
                    BlockPos groundCheckPos = new BlockPos(playerPos.getX(), playerPos.getY() + y + 1, playerPos.getZ());
                    BlockState checkGroundBlock = level.getBlockState(groundCheckPos);
                    if(checkGroundBlock.getBlock() == groundBlock) {
                        inInk = true;
                    }
                }

                if(!player.onGround())
                {
                    for(int x=-1; x<=1; x++)
                    {
                        for(int z=-1; z<=1; z++)
                        {
                            BlockPos wallCheckPos = new BlockPos(playerPos.getX() + x, playerPos.getY(), playerPos.getZ() + z);

                            BlockState checkWallBlock = level.getBlockState(wallCheckPos);

                            boolean nextToBlock = false;
                            nextToBlock |= checkWallBlock.getBlock() == groundBlock;
                            nextToBlock |= checkWallBlock.getBlock() == wallBlock;

                            if(nextToBlock)
                            {
                                inInk = true;
                                onWall = true;
                            }
                        }
                    }
                }

            }
        }





        if(inInk)
        {
            changeInk(0.022f);

            //Splatoon.LOGGER.info("xpprog{} xplvl{}", experienceProgress, experienceLevel);

//            experienceProgress = ink;


            timeNotInInk=0;

            if(player.tickCount % 7 == 0)
            {
                level.playSound(null, player.getOnPos(), SoundEvents.SQUID_AMBIENT, SoundSource.HOSTILE);
            }

            Effects.givePotionEffect(player, MobEffects.WEAKNESS, 1, 200, true);
            Effects.givePotionEffect(player, MobEffects.INVISIBILITY, 1, 1, true);

            if(onWall)
            {
                if(input.jump())
                {
                    Effects.givePotionEffect(player, MobEffects.LEVITATION, 10, 5, true);
                }
                else {
                    Effects.clearPotionEffect(player, MobEffects.LEVITATION);
                    Effects.givePotionEffect(player, MobEffects.SLOW_FALLING, 10, 5, true);
                }
            }
            else {
                Effects.clearPotionEffect(player, MobEffects.SLOW_FALLING);
                //Effects.clearPotionEffect(player, MobEffects.LEVITATION);
            }
        }
        else {
            timeNotInInk++;
            Effects.clearPotionEffect(player, MobEffects.SLOW_FALLING);
            Effects.clearPotionEffect(player, MobEffects.LEVITATION);

            if(splatoonClass != null)
            {
                ItemStack airItem = new ItemStack(Items.AIR);
                for(int i=0;i<9;i++)
                {
                    boolean doSetAir = true;

                    if(i < splatoonClass.definition.customItems.size() )
                    {
                        doSetAir = false;
                    }

                    if(i - 2 < splatoonClass.definition.abilities.size() )
                    {
                        doSetAir = false;
                    }

                    if(i - (splatoonClass.definition.abilities.size() - 2) < splatoonClass.definition.customItems.size() + 2)
                    {
                        doSetAir = false;
                    }

                    if(doSetAir)
                    {
                        if(
                                !player.getInventory().getItem(i).is(Items.AIR)
                                &&
                                !player.getInventory().getItem(i).is(Items.WARPED_FUNGUS_ON_A_STICK)
                        )
                        {
                            player.getInventory().setItem(i, airItem);
                        }
                    }
                }
            }
        }

        if(inInk && !wasInInk)
        {
            level.playSound(null, player.getOnPos(), SoundEvents.SLIME_BLOCK_BREAK, SoundSource.HOSTILE);

            Affects.setAttributeModifier(player, "scale", "inkscale", -0.75, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            Affects.setAttributeModifier(player, "sneaking_speed", "inkspeed", 0.7, AttributeModifier.Operation.ADD_VALUE);
            Affects.setAttributeModifier(player, "movement_speed", "inkspeed", 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

            ItemStack airItem = new ItemStack(Items.AIR);
            for(int i = 0; i<9; i++)
            {
                player.getInventory().setItem(i, airItem);
            }
            //player.containerMenu.broadcastChanges();
        }
        else if(!inInk && wasInInk)
        {
            level.playSound(null, player.getOnPos(), SoundEvents.SLIME_BLOCK_BREAK, SoundSource.HOSTILE);

            Affects.removeAttributeModifier(player, "scale", "inkscale");
            Affects.removeAttributeModifier(player, "sneaking_speed", "inkspeed");
            Affects.removeAttributeModifier(player, "movement_speed", "inkspeed");
            Effects.clearPotionEffect(player, MobEffects.INVISIBILITY);

            player.setInvisible(false);
        }

        int idx = 0;
        for(Ability ability : abilities) {
            ability.tick(player, idx);
            idx++;
        }

        if(!inInk)
        {
            if(splatoonClass != null)
            {
                idx=0;
                for(CustomItem customItem : splatoonClass.definition.customItems)
                {
                    ItemStack baseItem = customItem.getItem();
                    int slotIdx = idx;
                    if(slotIdx >= 2) {
                        slotIdx += abilities.size();
                    }

                    if(!player.getInventory().getItem(slotIdx).getItemName().equals(baseItem.getItemName()))
                    {
                        player.getInventory().setItem(slotIdx, baseItem.copy());
                        player.containerMenu.broadcastChanges();
                    }

                    idx++;
                }
            }
        }

        this.oldInput = this.input;
//        if(this.punchedThisTick)
//        {
//            Splatoon.LOGGER.info("{} Reset punched flag", level.getServer().getTickCount());
//        }
        this.punchedThisTick = false;
    }

    @Override
    public void grantAbility(Ability ability)
    {
        ability.onGranted(player, abilities.size());
        abilities.add(ability);
    }

    @Override
    public void revokeAbility(String abilityId)
    {
        int idx = 0;
        for(Ability ability : abilities.stream().toList()) {
            //Splatoon.LOGGER.info("-{}|{}", ability.getClass().getSimpleName(), abilityId);
            if(ability.getClass().getSimpleName().equals(abilityId))
            {
                ability.onRevoked(player, idx);
                abilities.remove(idx);
            }

            idx++;
        }
        //abilities.removeIf(ability -> ability.getClass().getSimpleName().equals(abilityId));
    }

    @Override
    public void revokeAllAbilities()
    {
        ItemStack air = new ItemStack(Items.AIR);

        int idx = 0;
        for(Ability ability : new ArrayList<>(abilities)) {
            ability.onRevoked(player, -1);
            abilities.remove(ability);

            player.getInventory().setItem(idx + 2, air);

            idx++;
        }
        //player.containerMenu.broadcastChanges();

    }

    @Override
    public void onUseAbilityBlock(String abilityId, Player player, InteractionHand hand, BlockHitResult hitResult) {
        int idx = 0;
        for(Ability ability : abilities.stream().toList()) {
            //Splatoon.LOGGER.info("-{}|{}", ability.getClass().getSimpleName(), abilityId);
            if(ability.getClass().getSimpleName().equals(abilityId))
            {
                ability.onUseBlock(player, hand, hitResult, idx);
            }

            idx++;
        }
    }

    @Override
    public void onUseAbilityItem(String abilityId, Player player, InteractionHand hand) {
        int idx = 0;
        for(Ability ability : abilities.stream().toList()) {
            //Splatoon.LOGGER.info("-{}|{}", ability.getClass().getSimpleName(), abilityId);
            if(ability.getClass().getSimpleName().equals(abilityId))
            {
                ability.onUseItem(player, hand, idx);
            }

            idx++;
        }
    }

    @Inject(method="addAdditionalSaveData", at=@At("TAIL"))
    void onSaveData(ValueOutput valueOutput, CallbackInfo ci)
    {
        Stream<String> abilitiesList = abilities.stream().map(ability -> ability.getClass().getSimpleName());

        valueOutput.store("abilities", Codec.list(Codec.STRING), abilitiesList.toList());

        String classString = "none";
        if(splatoonClass != null) classString = splatoonClass.getID();
        valueOutput.putString("class", classString);
    }

    @Inject(method="readAdditionalSaveData", at=@At("TAIL"))
    void onReadData(ValueInput valueInput, CallbackInfo ci)
    {
        List<String> abilitiesList = valueInput.read("abilities", Codec.list(Codec.STRING)).orElse(Collections.emptyList());

        for(String abilityId : abilitiesList) {
            try {
                AbilityManager.AbilityEnum abilityEnum = AbilityManager.AbilityEnum.valueOf(abilityId);
                grantAbility(abilityEnum.Construct());
                Splatoon.LOGGER.info("{} loaded ability '{}'", player.getName(), abilityId);
            }
            catch(IllegalArgumentException e) {
                Splatoon.LOGGER.warn("{} Tried to load unknown ability '{}'", player.getName(), abilityId);
            }
        }

        String classString = valueInput.getStringOr("class", "none");
        if(!classString.equals("") && !classString.equals("none"))
        {
            try {
                SplatoonClasses.SplatoonClass klass = SplatoonClasses.SplatoonClass.valueOf(classString);

                setClass(klass);
            }
            catch(IllegalArgumentException e)
            {
                Splatoon.LOGGER.warn("{} Attempted to load unknown class {}", player.getName(), classString);
            }
        }
    }
}
