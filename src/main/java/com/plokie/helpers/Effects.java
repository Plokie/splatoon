package com.plokie.helpers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;

public class Effects {
    public static void explosionEffect(Level level, BlockPos pos)
    {
        explosionEffect((ServerLevel)level, pos);
    }
    public static void explosionEffect(ServerLevel level, BlockPos pos)
    {
        explosionEffect(level, new Vector3d(pos.getX() + 0.5, pos.getY() + 0.0, pos.getZ() + 0.5));
    }
    public static void explosionEffect(Level level, Vector3d pos)
    {
        explosionEffect((ServerLevel)level, pos);
    }
    public static void explosionEffect(ServerLevel level, Vector3d pos)
    {
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                pos.x, pos.y, pos.z,
                1, // count
                0.0, 0.0, 0.0, // delta
                0.0 // speed
        );

        level.playSound(
                null, // everyone
                pos.x, pos.y, pos.z,
                SoundEvents.GENERIC_EXPLODE,
                SoundSource.HOSTILE,
                4.0f, // volume
                1.0f // pitch
        );

        level.playSound(
                null, // everyone
                pos.x, pos.y, pos.z,
                SoundEvents.SQUID_DEATH,
                SoundSource.HOSTILE,
                4.0f, // volume
                1.0f // pitch
        );
    }

    public static void givePotionEffect(LivingEntity entity, Holder<MobEffect> effect, float timeSeconds, int strength, boolean hidden)
    {
        MobEffectInstance effectInstance = new MobEffectInstance(
                effect,
                (int)timeSeconds * 20,
                strength,
                false, // ambient
                !hidden, // visible particles
                !hidden // show icon
        );

        entity.addEffect(effectInstance);
    }
}
