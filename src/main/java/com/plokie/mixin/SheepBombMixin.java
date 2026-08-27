package com.plokie.mixin;

import com.mojang.math.Transformation;
import com.plokie.Splatoon;
import com.plokie.helpers.Affects;
import com.plokie.helpers.Effects;
import com.plokie.helpers.Fill;
import com.plokie.helpers.Teams;
import com.plokie.interfaces.IPlayerTeamMixin;
import com.plokie.interfaces.IProjectile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.sheep.Sheep;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mixin(Sheep.class)
public class SheepBombMixin implements IProjectile {
    @Unique
    Sheep sheep;

    @Unique
    UUID playerOwnerUUID = null;

    @Unique
    int fuseTime = 0;

    @Unique boolean initialisedTeam = false;

    @Override
    public void setPlayerOwner(Player player)
    {
        playerOwnerUUID = player.getUUID();
    }

    @Inject(method="addAdditionalSaveData", at = @At("TAIL"))
    private void onSaveData(ValueOutput valueOutput, CallbackInfo ci)
    {
        if(playerOwnerUUID!=null)
        {
            valueOutput.putString("playerOwnerUUID", playerOwnerUUID.toString());
        }
    }

    @Inject(method="readAdditionalSaveData", at = @At("TAIL"))
    private void onReadData(ValueInput valueInput, CallbackInfo ci)
    {
        if(playerOwnerUUID!=null)
        {
            Optional<String> uuidString = valueInput.getString("playerOwnerUUID");
            if(uuidString.isPresent())
            {
                playerOwnerUUID = UUID.fromString(uuidString.get());
            }
        }
    }

    @Inject(method = "finalizeSpawn", at = @At("TAIL"))
    void onSpawned(ServerLevelAccessor serverLevelAccessor, DifficultyInstance difficultyInstance, EntitySpawnReason entitySpawnReason, SpawnGroupData spawnGroupData, CallbackInfoReturnable<SpawnGroupData> cir)
    {
        if(sheep == null) return;
        //if(!sheep.getTags().contains("InkBomb")) return;

        Level level = sheep.level();

        sheep.setInvisible(true);
        sheep.setSilent(true);

        {
            Display.BlockDisplay tntBlock = EntityType.BLOCK_DISPLAY.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
            if(tntBlock == null) return;

            tntBlock.setPos(sheep.getEyePosition());

            Splatoon.LOGGER.info("{} tnt disp pos", tntBlock.getEyePosition());

            BlockState blockState = Blocks.TNT.defaultBlockState();
            tntBlock.setBlockState(blockState);

            Transformation transform = new Transformation(
                    new Vector3f(-0.5f, -1.25f, -0.5f),
                    null, null, null
            );
            tntBlock.setTransformation(transform);

            tntBlock.setUUID(UUID.randomUUID());

            level.addFreshEntity(tntBlock);

            tntBlock.startRiding(sheep, true);
        }
    }


    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        sheep = (Sheep)(Object)this;
        if(sheep == null) return;
    }

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        if(sheep.level().isClientSide()) return;

        if(!sheep.getTags().contains("InkBomb")) return;

        if(!initialisedTeam)
        {
            IPlayerTeamMixin playerTeam = Teams.getTeamMixinFromPlayerUUID(playerOwnerUUID);
            if(playerTeam!=null) {

                sheep.level().playSound(
                        null, // everyone
                        sheep.getX(), sheep.getY(), sheep.getZ(),
                        SoundEvents.TNT_PRIMED,
                        SoundSource.HOSTILE,
                        4.0f, // volume
                        1.0f // pitch
                );

                Display.BlockDisplay woolBlock = EntityType.BLOCK_DISPLAY.create(sheep.level(), EntitySpawnReason.SPAWN_ITEM_USE);
                if(woolBlock == null) return;

                woolBlock.setPos(sheep.getEyePosition());

                BlockState blockState = playerTeam.getWallBlock().defaultBlockState();
                woolBlock.setBlockState(blockState);

                Transformation transform = new Transformation(
                        new Vector3f(-0.53125f,-1.0f,-0.53125f),
                        null, new Vector3f(1.05f, 0.5f, 1.05f), null
                );
                woolBlock.setTransformation(transform);

                woolBlock.setUUID(UUID.randomUUID());

                sheep.level().addFreshEntity(woolBlock);

                woolBlock.startRiding(sheep, true);

                initialisedTeam = true;
            }
        }

        if(sheep.onGround()) {
            fuseTime += 3;
        }
        else {
            fuseTime += 1;
        }

        if(fuseTime >= 120 && sheep.deathTime == 0) {
            if(sheep.level() instanceof ServerLevel serverLevel)
            {
                Effects.explosionEffect(serverLevel, sheep.getOnPos());

                Player player = serverLevel.getPlayerByUUID(playerOwnerUUID);
                if(player != null)
                {
                    int numHurtEntities = Affects.hurtLivingEntitiesInRange(
                            serverLevel, sheep.getOnPos(), 4.0f, 28.0f, player, DamageTypes.EXPLOSION
                    );

                    IPlayerTeamMixin playerTeam = Teams.getTeamMixinFromPlayer(player);
                    if(playerTeam!=null)
                    {
                        int numReplaced = Fill.replace(
                                serverLevel,
                                sheep.getOnPos(),
                                new BlockPos(3,3,3),
                                new BlockPos(-3,-3,-3),
                                playerTeam.getGroundBlock(),
                                Splatoon.Tags.GROUND_BLOCKS
                        );

                        numReplaced += Fill.replace(
                                serverLevel,
                                sheep.getOnPos(),
                                new BlockPos(3,3,3),
                                new BlockPos(-3,-3,-3),
                                playerTeam.getWallBlock(),
                                Splatoon.Tags.WALL_BLOCKS
                        );
                    }
                }


                sheep.kill(serverLevel);
            }

        }

        if(sheep.deathTime == 1) {
            onDeath();
        }
    }

    @Unique
    void onDeath()
    {
        List<Entity> passengers = sheep.getPassengers();

        for(Entity entity : passengers)
        {
            entity.discard();
        }
    }
}
