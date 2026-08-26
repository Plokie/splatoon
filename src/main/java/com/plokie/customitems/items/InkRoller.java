package com.plokie.customitems.items;

import com.plokie.Splatoon;
import com.plokie.customitems.ICustomItem;
import net.minecraft.world.entity.player.Player;

public class InkRoller extends ICustomItem {
    @Override
    public void onUse(Player player)
    {

    }

    @Override
    public void whileHeld(Player player)
    {
        Splatoon.LOGGER.info("Held roller");
    }
}
