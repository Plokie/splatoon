package com.plokie.classes.abilities.rework;

import com.plokie.customitems.CustomItem;

public class SideAbility {
    public SideAbility(String name, float inkCost, CustomItem item) {
        this.name = name;
        this.inkCost = inkCost;
        this.item = item;
    }

    final String name;
    final float inkCost;
    final CustomItem item;


}
