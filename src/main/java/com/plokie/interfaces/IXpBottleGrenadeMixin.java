package com.plokie.interfaces;

import net.minecraft.world.phys.Vec3;

public interface IXpBottleGrenadeMixin extends IProjectile {
    void explode(Vec3 position);
}
