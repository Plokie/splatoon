package com.plokie.interfaces;

import com.plokie.classes.SplatoonClasses;
import com.plokie.classes.abilities.Ability;
import com.plokie.classes.abilities.AbilityManager;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

public interface IPlayerMixin {
    List<Ability> getAbilities();
    Ability getAbility(AbilityManager.AbilityEnum abilityEnum);
    void revokeAllAbilities();
    void grantAbility(Ability ability);
    void revokeAbility(String abilityId);

    void setClass(SplatoonClasses.SplatoonClass klass);
    SplatoonClasses.SplatoonClass getSplatoonClass();

    void setInputPacket(Input input);
    Input getInput();
    Input getPreviousInput();

    void setPunched();
    boolean punchedThisTick();

    int getTimeNotInInk();
    float getInk();
    void setInk(float value);
    void changeInk(float delta);
    boolean isInInk();

    boolean isInInkOnWall();
}
