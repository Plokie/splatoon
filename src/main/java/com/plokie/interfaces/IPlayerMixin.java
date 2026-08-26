package com.plokie.interfaces;

import com.plokie.classes.SplatoonClasses;
import com.plokie.classes.abilities.Ability;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

public interface IPlayerMixin {
    List<Ability> getAbilities();
    void revokeAllAbilities();
    void grantAbility(Ability ability);
    void revokeAbility(String abilityId);
    void onUseAbilityBlock(String abilityId, Player player, InteractionHand hand, BlockHitResult hitResult);
    void onUseAbilityItem(String abilityId, Player player, InteractionHand hand);

    void setClass(SplatoonClasses.SplatoonClass klass);

    void setInputPacket(Input input);
    Input getInput();
    Input getPreviousInput();

    float getInk();
    void changeInk(float delta);
    boolean isInInk();
}
