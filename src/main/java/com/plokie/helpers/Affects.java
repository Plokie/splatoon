package com.plokie.helpers;

import com.plokie.Splatoon;
import com.plokie.management.PlayerStats;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import java.util.Optional;
import java.util.jar.Attributes;

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
            if(entity instanceof Sheep) continue;

            if(entity.distanceToSqr(new Vec3(pos.x, pos.y, pos.z)) < radius*radius)
            {
                entity.hurtServer((ServerLevel)level, source, amount);
                numHurtEntities++;
            }
        }

        if(damageBy instanceof Player player)
        {
            PlayerStats.get(player).add(PlayerStats.DAMAGE_DEALT, (int)(numHurtEntities * amount));
        }

        return numHurtEntities;
    }

    public static void hurtEntity(LivingEntity entity, float amount)
    {
        hurtEntity(entity, amount, null, DamageTypes.GENERIC);
    }

    public static void hurtEntity(LivingEntity entity, float amount, Entity damagedBy, ResourceKey<DamageType> damageType)
    {
        Holder<DamageType> damageTypeHolder = Splatoon.SERVER.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(damageType);

        DamageSource source;

        if(damagedBy == null)
        {
            source = new DamageSource(damageTypeHolder);
        }
        else {
            source = new DamageSource(damageTypeHolder, damagedBy);
        }

        entity.hurtServer((ServerLevel)entity.level(), source, amount);
    }

    public static void setAttributeModifier(LivingEntity entity, String attribute, AttributeModifier modifier)
    {
        Optional<Holder.Reference<Attribute>> optAttrib = BuiltInRegistries.ATTRIBUTE.get(ResourceLocation.withDefaultNamespace(attribute));

        if(optAttrib.isEmpty()) return;

        Holder.Reference<Attribute> attrib = optAttrib.get();

        AttributeInstance attributeInstance = entity.getAttribute(attrib);

        if(attributeInstance == null) return;

        attributeInstance.addOrReplacePermanentModifier(modifier);
    }

    public static void setAttributeModifier(LivingEntity entity, String attribute, String modifierName, double value, AttributeModifier.Operation operation)
    {
        AttributeModifier modifier = new AttributeModifier(
                ResourceLocation.withDefaultNamespace(modifierName),
                value,
                operation
        );

        Optional<Holder.Reference<Attribute>> optAttrib = BuiltInRegistries.ATTRIBUTE.get(ResourceLocation.withDefaultNamespace(attribute));

        if(optAttrib.isEmpty()) return;

        Holder.Reference<Attribute> attrib = optAttrib.get();

        AttributeInstance attributeInstance = entity.getAttribute(attrib);

        if(attributeInstance == null) return;

        attributeInstance.addOrReplacePermanentModifier(modifier);
    }

    public static void removeAttributeModifier(LivingEntity entity, String attribute, String modifierName)
    {
        Optional<Holder.Reference<Attribute>> optAttrib = BuiltInRegistries.ATTRIBUTE.get(ResourceLocation.withDefaultNamespace(attribute));

        if(optAttrib.isEmpty()) return;

        Holder.Reference<Attribute> attrib = optAttrib.get();

        AttributeInstance attributeInstance = entity.getAttribute(attrib);

        if(attributeInstance == null) return;

        attributeInstance.removeModifier(ResourceLocation.withDefaultNamespace(modifierName));
    }

    public static void removeAttributeModifier(LivingEntity entity, String attribute, AttributeModifier modifier)
    {
        Optional<Holder.Reference<Attribute>> optAttrib = BuiltInRegistries.ATTRIBUTE.get(ResourceLocation.withDefaultNamespace(attribute));

        if(optAttrib.isEmpty()) return;

        Holder.Reference<Attribute> attrib = optAttrib.get();

        AttributeInstance attributeInstance = entity.getAttribute(attrib);

        if(attributeInstance == null) return;

        attributeInstance.removeModifier(modifier);
    }
}
