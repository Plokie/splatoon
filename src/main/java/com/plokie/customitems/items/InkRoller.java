package com.plokie.customitems.items;

import com.plokie.Splatoon;
import com.plokie.customitems.ICustomItem;
import net.minecraft.world.entity.player.Player;

public class InkRoller extends ICustomItem {

    public InkRoller()
    {
        this.useDuration = 5;
        this.usageRate = 2;
    }

    @Override
    public void onUse(Player player)
    {
        Splatoon.LOGGER.info("Roller splat");
    }

    @Override
    public void whileHeld(Player player)
    {

    }
}
