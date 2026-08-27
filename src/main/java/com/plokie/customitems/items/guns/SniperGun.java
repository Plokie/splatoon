package com.plokie.customitems.items.guns;

import com.plokie.Splatoon;
import com.plokie.customitems.CustomItem;
import com.plokie.customitems.ICustomItem;
import com.plokie.helpers.Affects;
import com.plokie.helpers.Fill;
import com.plokie.helpers.Helpers;
import com.plokie.helpers.Teams;
import com.plokie.interfaces.IPlayerMixin;
import com.plokie.interfaces.IPlayerTeamMixin;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class SniperGun extends ICustomItem {

    public static int getTempInt(Player player)
    {
        return player.getScore();
    }

    public static void setTempInt(Player player, int value)
    {
        player.setScore(value);
    }

    public static void changeTempInt(Player player, int delta)
    {
        setTempInt(player, getTempInt(player) + delta);
    }

    @Override
    public void onUse(Player player) {
        if(getTempInt(player)>0)
        {
            ServerPlayer serverPlayer = (ServerPlayer)player;
            serverPlayer.connection.send(new ClientboundClearTitlesPacket(true));
        }
        setTempInt(player, getTempInt(player) == 0 ? 1 : 0);
        scope(player);
    }

    void inputMove(Player player, boolean inputBool, float rotationOffset)
    {
        if(!inputBool) return;

        float movementForce = 0.05f;

        Vec2 rotation = player.getRotationVector();
        rotation = new Vec2(0.0f, rotation.y + rotationOffset);

        Vec3 direction = Vec3.directionFromRotation(rotation);
        direction = new Vec3(direction.x * movementForce, direction.y * movementForce, direction.z * movementForce);

        Vec3 currentVelocity = player.getDeltaMovement();
        player.setDeltaMovement(new Vec3(direction.x, direction.y + currentVelocity.y, direction.z));
        ((ServerPlayer)player).connection.send(new ClientboundSetEntityMotionPacket(player));
    }

    @Override
    public void whileHeld(Player player) {
        boolean isScoping = getTempInt(player)>0;
        IPlayerMixin playerMixin = ((IPlayerMixin)player);
        Input input = playerMixin.getInput();
        ServerPlayer serverPlayer = (ServerPlayer)player;


        if(isScoping)
        {
            ServerLevel level = (ServerLevel)player.level();

            if(!player.getItemBySlot(EquipmentSlot.HEAD).is(Items.CARVED_PUMPKIN))
            {
                ItemStack item = new ItemStack(Items.CARVED_PUMPKIN);
                player.setItemSlot(EquipmentSlot.HEAD, item);
            }

            if(input.jump()) setTempInt(player, 1);

            changeTempInt(player, 1);
            if(player.hasEffect(MobEffects.REGENERATION))
            {
                changeTempInt(player, 1);
            }

            int timeScoping = getTempInt(player);
            int sniperCharge = timeScoping / 10;
            int chargeIndex = timeScoping % 10;

            float reach = 30.0f;
            if(sniperCharge >= 3)
            {
                reach = 80.0f;
            }

            if(chargeIndex == 0 || timeScoping == 2 || timeScoping == 3)
            {
                Affects.setAttributeModifier(player, "entity_interaction_range", "scoping", reach - 5.0f, AttributeModifier.Operation.ADD_VALUE);
            }


            if(chargeIndex == 0 || chargeIndex == 1 || timeScoping == 2 || timeScoping == 3)
            {
                MutableComponent titleComponent = Component.literal("[").withStyle(ChatFormatting.WHITE);

                ChatFormatting col = ChatFormatting.RED;
                if(sniperCharge == 3) col = ChatFormatting.GOLD;
                if(sniperCharge == 4) col = ChatFormatting.YELLOW;
                if(sniperCharge == 5) col = ChatFormatting.DARK_GREEN;

                for(int i=1; i<=5; i++)
                {
                    if(i <= sniperCharge) {
                        titleComponent.append(Component.literal("=").withStyle(col));
                    }
                    else
                    {
                        titleComponent.append(Component.literal("-").withStyle(ChatFormatting.GRAY));
                    }
                }
                titleComponent.append(Component.literal("]").withStyle(ChatFormatting.WHITE));

                serverPlayer.connection.send(new ClientboundSetSubtitleTextPacket(titleComponent));
            }

            if(timeScoping == 2 || timeScoping == 3)
            {
                serverPlayer.connection.send(new ClientboundSetTitleTextPacket(Component.literal("")));
            }

            if(sniperCharge > 5)
            {
                serverPlayer.connection.send(new ClientboundClearTitlesPacket(true));
            }

            inputMove(player, input.left(), -90.0f);
            inputMove(player, input.right(), 90.0f);
            inputMove(player, input.forward(), 0.0f);
            inputMove(player, input.backward(), 180.0f);

            if(playerMixin.punchedThisTick() && sniperCharge > 0)
            {
                setTempInt(player, 1);
                Vec3 eyePos = player.getEyePosition();

                //Splatoon.LOGGER.info("Punched");

                level.playSound(
                        null, // everyone
                        eyePos.x, eyePos.y, eyePos.z,
                        SoundEvents.FIREWORK_ROCKET_LARGE_BLAST_FAR,
                        SoundSource.HOSTILE,
                        3.0f, // volume
                        1.0f // pitch
                );

                Vec3 forward = player.getForward();

                IPlayerTeamMixin playerTeam = Teams.getTeamMixinFromPlayer(player);
                if(playerTeam != null) {
                    int dustCol = playerTeam.getTeamColourInt();
                    //Block groundBlock = playerTeam.getGroundBlock();
                    //Block wallBlock = playerTeam.getWallBlock();
                    DustParticleOptions dustParticleOptions = new DustParticleOptions(dustCol, 1);

                    for(float i=0; i < reach; i+=0.5f)
                    {
                        Vec3 pos = new Vec3(
                                eyePos.x + (forward.x * i),
                                eyePos.y + (forward.y * i),
                                eyePos.z + (forward.z * i)
                        );

                        level.sendParticles(
                                dustParticleOptions,
                                pos.x, pos.y, pos.z,
                                1, // count
                                0.0, 0.0, 0.0, // delta
                                10.0 // speed
                        );

//                        Fill.replace()
                        int numReplaced = Fill.replace(
                                level, Helpers.toBlockPos(pos),
                                new BlockPos(0,1,0),
                                new BlockPos(0,-10,0),
                                playerTeam.getGroundBlock(),
                                Splatoon.Tags.GROUND_BLOCKS
                        );

                        numReplaced = Fill.replace(
                                level, Helpers.toBlockPos(pos),
                                new BlockPos(0,1,0),
                                new BlockPos(0,-10,0),
                                playerTeam.getWallBlock(),
                                Splatoon.Tags.WALL_BLOCKS
                        );
                    }
                }

            }
        }
    }

    @Override
    public void onAttackHit(Player player, Entity hitEntity)
    {
        int timeScoping = getTempInt(player);
        boolean isScoping = timeScoping>0;

        int sniperCharge = timeScoping / 10;
        if(sniperCharge > 5) sniperCharge = 5;

        setTempInt(player, 1);

        if(isScoping && sniperCharge >= 1)
        {
            if(hitEntity instanceof LivingEntity entity)
            {
                ServerLevel level = (ServerLevel) hitEntity.level();

                IPlayerTeamMixin playerTeam = Teams.getTeamMixinFromPlayer(player);

                if(playerTeam == null) return;



                float damage = switch (sniperCharge) {
                    case 1 -> 1;
                    case 2 -> 2;
                    case 3 -> 6;
                    case 4 -> 14;
                    case 5 -> 18;
                    default -> 0;
                };

                Affects.hurtEntity(entity, damage, player, DamageTypes.ARROW);

                level.playSound(
                        player,
                        player.getEyePosition().x, player.getEyePosition().y, player.getEyePosition().z,
                        SoundEvents.EXPERIENCE_ORB_PICKUP,
                        SoundSource.HOSTILE,
                        0.6f, // volume
                        1.0f // pitch
                );

                int numReplaced = Fill.replace(
                        level, entity.getOnPos(),
                        new BlockPos(2,2,2),
                        new BlockPos(-2,-2,-2),
                        playerTeam.getGroundBlock(),
                        Splatoon.Tags.GROUND_BLOCKS
                );

                numReplaced = Fill.replace(
                        level, entity.getOnPos(),
                        new BlockPos(2,2,2),
                        new BlockPos(-2,-2,-2),
                        playerTeam.getWallBlock(),
                        Splatoon.Tags.WALL_BLOCKS
                );
            }
        }
    }

    @Override
    public void onStartHeld(Player player) {
        //Splatoon.LOGGER.info("Start held");
        setTempInt(player, 0);
    }

    @Override
    public void onEndHeld(Player player) {
        if(getTempInt(player)>0)
        {
            ServerPlayer serverPlayer = (ServerPlayer)player;
            serverPlayer.connection.send(new ClientboundClearTitlesPacket(true));
        }
        setTempInt(player, 0);
        scope(player);

        //Splatoon.LOGGER.info("End held");
    }

    void scope(Player player)
    {
        boolean isScoping = getTempInt(player)==1;
        //Splatoon.LOGGER.info("is scoping {}", isScoping);

        if(isScoping)
        {
            ItemStack item = new ItemStack(Items.CARVED_PUMPKIN);
            player.setItemSlot(EquipmentSlot.HEAD, item);
            Affects.setAttributeModifier(player, "movement_speed", "scoping", -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            //Affects.setAttributeModifier(player, "entity_interaction_range", "scoping", 85.0, AttributeModifier.Operation.ADD_VALUE);

            ItemStack sniper = CustomItem.SniperGun.getItemDefinition().baseItem;
            ItemStack invisSniper = sniper.copy();
            invisSniper.set(DataComponents.ITEM_MODEL, ResourceLocation.withDefaultNamespace("air"));

            Tool toolCom = new Tool(new ArrayList<>(), 1.0f, 2, false);
            invisSniper.set(DataComponents.TOOL, toolCom);

            for(int i=0; i<9; i++)
            {
                if(player.getInventory().getItem(i).getItemName().equals(sniper.getItemName()))
                {

                    player.getInventory().setItem(i, invisSniper);
                }
            }

//            ItemStack sniper = CustomItem.SniperGun.getItemDefinition().baseItem.copy();
//            sniper.set(DataComponents.ITEM_MODEL, ResourceLocation.withDefaultNamespace("air"));
//            player.setItemInHand(InteractionHand.MAIN_HAND, sniper);
            player.containerMenu.broadcastChanges();
        }
        else
        {
            ItemStack item = new ItemStack(Items.AIR);
            player.setItemSlot(EquipmentSlot.HEAD, item);
            Affects.removeAttributeModifier(player, "movement_speed", "scoping");
            Affects.removeAttributeModifier(player, "entity_interaction_range", "scoping");

            ItemStack sniper = CustomItem.SniperGun.getItemDefinition().baseItem;
            for(int i=0; i<9; i++)
            {
                if(player.getInventory().getItem(i).getItemName().equals(sniper.getItemName()))
                {
                    player.getInventory().setItem(i, sniper.copy());
//                    ItemStack invisSniper = sniper.copy();
//                    invisSniper.set(DataComponents.ITEM_MODEL, ResourceLocation.withDefaultNamespace("air"));
                }
            }
            player.containerMenu.broadcastChanges();
//            ItemStack sniper = CustomItem.SniperGun.getItemDefinition().baseItem.copy();
//            //sniper.set(DataComponents.ITEM_MODEL, ResourceLocation.withDefaultNamespace("air"));
//            player.setItemInHand(InteractionHand.MAIN_HAND, sniper);
//            player.containerMenu.broadcastChanges();
        }
    }
}
