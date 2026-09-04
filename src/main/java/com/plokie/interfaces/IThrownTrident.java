package com.plokie.interfaces;

import net.minecraft.world.entity.LivingEntity;

public interface IThrownTrident extends IProjectile {
    void setRopedTarget(LivingEntity ropedTarget);
}
