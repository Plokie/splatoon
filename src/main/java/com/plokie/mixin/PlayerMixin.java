package com.plokie.mixin;

import com.mojang.serialization.Codec;
import com.plokie.Splatoon;
import com.plokie.classes.abilities.Ability;
import com.plokie.classes.abilities.AbilityManager;
import com.plokie.interfaces.IPlayerMixin;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Mixin(Player.class)
public class PlayerMixin implements IPlayerMixin {
    @Unique private Player player;
    @Unique private List<Ability> abilities = new ArrayList<Ability>();

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        player = (Player)(Object)this;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {

        if(player.level().isClientSide()) return;

        int idx = 0;
        for(Ability ability : abilities) {
            ability.tick(player, idx);
            idx++;
        }
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
    }

    @Inject(method="readAdditionalSaveData", at=@At("TAIL"))
    void onReData(ValueInput valueInput, CallbackInfo ci)
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
    }
}
