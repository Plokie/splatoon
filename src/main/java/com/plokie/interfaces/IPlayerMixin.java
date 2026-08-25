package com.plokie.interfaces;

import com.plokie.classes.abilities.Ability;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

public interface IPlayerMixin {
    List<Ability> getAbilities();
    void grantAbility(Ability ability);
    void revokeAbility(String abilityId);
    void onUseAbilityBlock(String abilityId, Player player, InteractionHand hand, BlockHitResult hitResult);
    void onUseAbilityItem(String abilityId, Player player, InteractionHand hand);
}
