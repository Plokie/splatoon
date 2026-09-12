package com.plokie.helpers;

import com.mojang.serialization.Codec;
import com.plokie.Splatoon;
import com.plokie.interfaces.IInkablePayloadBlock;
import com.plokie.interfaces.IPlayerTeamMixin;
import com.plokie.management.PlayerStats;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Helpers {
    //public static final Codec<UUID> UUID_CODEC = Codec.stringResolver(UUID::toString, UUID::fromString);

    public static Vec3 toVec3(BlockPos blockPos)
    {
        return new Vec3(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5);
    }

    public static BlockPos toBlockPos(Vec3 vec3) {
        return new BlockPos(
                (int)Math.floor(vec3.x()),
                (int)Math.floor(vec3.y()),
                (int)Math.floor(vec3.z())
        );
    }

    public static BlockPos toBlockPos(Vector3f vec3f)
    {
        return new BlockPos(
                (int)Math.floor(vec3f.x),
                (int)Math.floor(vec3f.y),
                (int)Math.floor(vec3f.z)
        );
    }

    public static BlockPos toBlockPos(Vector3d vec3d)
    {
        return new BlockPos(
                (int)Math.floor(vec3d.x),
                (int)Math.floor(vec3d.y),
                (int)Math.floor(vec3d.z)
        );
    }

    public static <T extends Entity> List<T> getEntitiesInRadius(Level level, Class<T> ofClass, Vec3 position, float radius) {
        return getEntitiesInRadius(level, ofClass, position.toVector3f(), radius);
    }

    public static <T extends Entity> List<T> getEntitiesInRadius(Level level, Class<T> ofClass, Vector3f position, float radius) {
        List<T> ret = new ArrayList<>();

        AABB aabb = new AABB(toBlockPos(position)).inflate(radius * 1.2);

        for(T entity : level.getEntitiesOfClass(ofClass, aabb))
        {
            if(entity.distanceToSqr(new Vec3(position.x, position.y, position.z)) < radius*radius)
            {
                ret.add(entity);
            }
        }

        return  ret;
    }

    public static void mergeNbtIntoEntity(Entity entity, CompoundTag nbt) {
        HolderLookup.Provider registries = entity.level().registryAccess();

        TagValueOutput vo = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        entity.saveWithoutId(vo);

        CompoundTag newNbt = vo.buildResult();
        newNbt.merge(nbt);

        TagValueInput vi = (TagValueInput) TagValueInput.create(ProblemReporter.DISCARDING, registries, newNbt);

        entity.load(vi);
    }

    public static Vec3 localToWorld(Vec3 observerPos, double yawDeg, double pitchDeg, Vec3 localVector)
    {
        double yaw = Math.toRadians(yawDeg);
        double pitch = Math.toRadians(pitchDeg);

        double cosYaw = Math.cos(yaw);
        double sinYaw = Math.sin(yaw);
        double cosPitch = Math.cos(pitch);
        double sinPitch = Math.sin(pitch);

        double yy = localVector.y * cosPitch - localVector.z * sinPitch;
        double zz = localVector.z * cosPitch + localVector.y * sinPitch;

        Vec3 relative = new Vec3(
                localVector.x * cosYaw - zz * sinYaw,
                yy,
                localVector.x * sinYaw + zz * cosYaw
        );

        return observerPos.add(relative);
    }

    public static Vec3 localToWorld(Entity entity, Vec3 localVector) {
        return localToWorld(entity.getPosition(0.0f), entity.getYRot(), entity.getXRot(), localVector);
    }

    public static void inkSlimesInRadius(BlockPos worldPos, float radius, Player inkedBy)
    {
        inkSlimesInRadius(new Vec3(worldPos.getX(), worldPos.getY(), worldPos.getZ()), radius, inkedBy);
    }

    public static void inkSlimesInRadius(Vec3 worldPos, float radius, Player inkedBy) {
        IPlayerTeamMixin playerTeam = Teams.getTeamMixinFromPlayer(inkedBy);
        if(playerTeam == null) return;

        for (Slime slime : getEntitiesInRadius(inkedBy.level(), Slime.class, worldPos, radius)) {
            IInkablePayloadBlock payloadBlock = (IInkablePayloadBlock)slime;

            if(payloadBlock.getTeam() != playerTeam) {
                payloadBlock.setTeam(playerTeam);
                PlayerStats.get(inkedBy).add(PlayerStats.PAYLOAD_INKED, 1);
            }

        }
    }

    public static void cleanSlimesInRadius(BlockPos worldPos, float radius, Player cleanedBy)
    {
        cleanSlimesInRadius(new Vec3(worldPos.getX(), worldPos.getY(), worldPos.getZ()), radius, cleanedBy);
    }

    public static void cleanSlimesInRadius(Vec3 worldPos, float radius, Player cleanedBy) {

        for (Slime slime : getEntitiesInRadius(cleanedBy.level(), Slime.class, worldPos, radius)) {
            IInkablePayloadBlock payloadBlock = (IInkablePayloadBlock)slime;

            if(payloadBlock.getTeam() != null) {
                payloadBlock.setTeam(null);
                PlayerStats.get(cleanedBy).add(PlayerStats.PAYLOAD_INKED, 1);
            }
        }
    }

    public static void summonBasicFirework(Level level, Vec3 position, int intCol, int duration)
    {
        ItemStack rocketItem = new ItemStack(Items.FIREWORK_ROCKET);
        IntArrayList explosionColors = new IntArrayList(new int[]{intCol});
        IntArrayList fadeColors = new IntArrayList(new int[]{});

        FireworkExplosion explosion = new FireworkExplosion(
                FireworkExplosion.Shape.SMALL_BALL,
                explosionColors,
                fadeColors,
                false,// hasTrail
                false //hasTwinkle
        );

        Fireworks fireworksComponent = new Fireworks(duration, List.of(explosion));

        rocketItem.set(DataComponents.FIREWORKS, fireworksComponent);

        FireworkRocketEntity firework = new FireworkRocketEntity(Splatoon.SERVER.overworld(), position.x, position.y, position.z, rocketItem);
        level.addFreshEntity(firework);
    }
}
