package com.plokie.helpers;

import com.plokie.Splatoon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public class Affects {
    public static int hurtLivingEntitiesInRange(Level level, BlockPos pos, double radius, float amount)
    {
        return hurtLivingEntitiesInRange(level, new Vector3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5), radius, amount, null, DamageTypes.GENERIC);
    }

    public static int hurtLivingEntitiesInRange(Level level, BlockPos pos, double radius, float amount, Entity damageBy, ResourceKey<DamageType> damageType)
    {
        return hurtLivingEntitiesInRange(level, new Vector3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5), radius, amount, damageBy, damageType);
    }

    public static int hurtLivingEntitiesInRange(Level level, Vector3d pos, double radius, float amount)
    {
        return hurtLivingEntitiesInRange(level, pos, radius, amount, null, DamageTypes.GENERIC);
    }

    public static int hurtLivingEntitiesInRange(Level level, Vector3d pos, double radius, float amount, Entity damageBy, ResourceKey<DamageType> damageType)
    {
        int numHurtEntities = 0;

        BlockPos blockPos = new BlockPos(
                (int)Math.floor(pos.x),
                (int)Math.floor(pos.y),
                (int)Math.floor(pos.z)
        );

        AABB aabb = new AABB(blockPos).inflate(radius);

        Holder<DamageType> damageTypeHolder = Splatoon.SERVER.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(damageType);

        DamageSource source;

        if(damageBy == null)
        {
            source = new DamageSource(damageTypeHolder);
        }
        else {
            source = new DamageSource(damageTypeHolder, damageBy);
        }

        for(LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, aabb))
        {
            if(entity.distanceToSqr(new Vec3(pos.x, pos.y, pos.z)) < radius)
            {
                entity.hurtServer((ServerLevel)level, source, amount);
                numHurtEntities++;
            }
        }

        return numHurtEntities;
    }
}
