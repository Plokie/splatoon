package com.plokie.classes.abilities.rework;

import com.plokie.customitems.CustomItem;

import java.util.function.Function;
import java.util.function.Supplier;

public enum SideAbilities {
    InkBomb("Ink Bomb", name->new SideAbility(name, 0.333f, CustomItem.InkBomb));

    SideAbilities(String name, Function<String, SideAbility> constructor)
    {
        this.name = name;
        this.constructor = constructor;
    }

    public SideAbility create() {
         return constructor.apply(name);
    }

    final String name;
    final Function<String, SideAbility> constructor;
}
