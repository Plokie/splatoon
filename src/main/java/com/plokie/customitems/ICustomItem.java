package com.plokie.customitems;

import net.minecraft.world.entity.player.Player;

public class ICustomItem {
    protected int useDuration = 0;
    protected int usageRate = 1;

    public int getUseDuration() { return useDuration; }
    public int getUsageRate() { return usageRate; }
    public void onUse(Player player) {}
    public void whileHeld(Player player) {}

}
