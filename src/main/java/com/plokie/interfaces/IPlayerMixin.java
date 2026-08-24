package com.plokie.interfaces;

import com.plokie.classes.abilities.Ability;

public interface IPlayerMixin {
    void grantAbility(Ability ability);
    void revokeAbility(String abilityId);
}
