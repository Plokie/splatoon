package com.plokie.interfaces;

import com.plokie.classes.abilities.Ability;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

public interface IPlayerMixin {
    void grantAbility(Ability ability);
    void revokeAbility(String abilityId);
    void onUseAbilityBlock(String abilityId, Player player, InteractionHand hand, BlockHitResult hitResult);
    void onUseAbilityItem(String abilityId, Player player, InteractionHand hand);
}
