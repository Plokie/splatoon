package com.plokie.customitems;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;

public class ICustomItem {
    protected int useDuration = 1;
    protected int usageRate = 1;

    public int getUseDuration() { return useDuration; }
    public int getUsageRate() { return usageRate; }
    public void onUse(Player player) {}
    public void whileHeld(Player player) {}
    public void onStartHeld(Player player) {}
    public void onEndHeld(Player player) {}
    public void onAttackHit(Player player, Entity hitEntity) {}

}
