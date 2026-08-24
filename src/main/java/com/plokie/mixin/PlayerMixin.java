package com.plokie.mixin;

import com.plokie.classes.abilities.Ability;
import com.plokie.interfaces.IPlayerMixin;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
            ability.Tick(player, idx);
            idx++;
        }
    }

    @Override
    public void grantAbility(Ability ability)
    {
        ability.setCountMax();
        abilities.add(ability);
    }

    @Override
    public void revokeAbility(String abilityId)
    {
        abilities.removeIf(ability -> ability.toString() == abilityId);
    }
}
