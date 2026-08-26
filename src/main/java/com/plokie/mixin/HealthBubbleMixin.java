package com.plokie.mixin;

import com.mojang.math.Transformation;
import com.plokie.Splatoon;
import com.plokie.helpers.Fill;
import com.plokie.helpers.Helpers;
import com.plokie.helpers.Teams;
import com.plokie.interfaces.IPlayerTeamMixin;
import com.plokie.interfaces.IProjectile;
import net.minecraft.commands.arguments.selector.SelectorPattern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.UUID;

@Mixin(Shulker.class)
public class HealthBubbleMixin implements IProjectile {
    @Unique UUID playerUUID = null;

    @Override
    public void setPlayerOwner(Player player) {
        this.playerUUID = player.getUUID();
    }

    @Unique Display.TextDisplay textDisplay = null;
    @Unique Shulker self = null;

    @Unique int dustCol = -1;
    @Unique Block groundBlock = Blocks.AIR;
    @Unique IPlayerTeamMixin playerTeam = null;

    @Unique boolean hasSetup = false;

    @Inject(method="<init>", at=@At("TAIL"))
    void onInit(EntityType entityType, Level level, CallbackInfo ci)
    {
        this.self = (Shulker)(Object)this;
    }

    void setup()
    {
        Display.TextDisplay textDisplay = EntityType.TEXT_DISPLAY.create(self.level(), EntitySpawnReason.SPAWN_ITEM_USE);
        if(textDisplay == null) return;

        float health = self.getHealth();
        textDisplay.setText(Component.literal(String.valueOf(health)));

        textDisplay.setBillboardConstraints(Display.BillboardConstraints.VERTICAL);

        textDisplay.setTransformation(new Transformation(
                new Vector3f(0.0f, 3.0f, 0.0f),
                null,
                new Vector3f(5.0f, 5.0f, 5.0f),
                null
        ));

        //

        textDisplay.setUUID(UUID.randomUUID());

        textDisplay.addTag("HealthBubbleText");

        self.level().addFreshEntity(textDisplay);

        textDisplay.setPos(self.getEyePosition());

        this.textDisplay = textDisplay;

        hasSetup = true;
    }

    @Inject(method="tick", at=@At("TAIL"))
    void onTick(CallbackInfo ci)
    {
        if(!self.getTags().contains("HealthBubble")) return;

        ServerLevel level = (ServerLevel)self.level();

        if(level.isClientSide()) return;

        if(!hasSetup)
        {
            setup();
        }

        if(this.textDisplay == null) return;

        if(dustCol == -1 || groundBlock == Blocks.AIR) {
            if(playerUUID != null) {
                Player player = level.getPlayerByUUID(playerUUID);
                if(player != null) {
                    playerTeam = Teams.getTeamMixinFromPlayer(player);
                    if(playerTeam != null) {
                        dustCol = playerTeam.getTeamColourInt();
                        groundBlock = playerTeam.getGroundBlock();
                    }
                }
            }
        }

        DustParticleOptions dustParticleOptions = new DustParticleOptions(dustCol, 1);

        Vec3 forward = textDisplay.getForward();
        Vec3 forwardRadius = new Vec3(forward.x * 3.0, forward.y * 3.0, forward.z * 3.0);

        Vec3 position = textDisplay.getEyePosition();


        for(int i=0; i<3; i++) {
            Vec3 pos = new Vec3(position.x + forwardRadius.x, position.y + forwardRadius.y, position.z + forwardRadius.z);

            level.sendParticles(
                    dustParticleOptions,
                    pos.x, pos.y + i, pos.z,
                    1, // count
                    0.0, 0.0, 0.0, // delta
                    0.0 // speed
            );

            pos = new Vec3(position.x - forwardRadius.x, position.y - forwardRadius.y, position.z - forwardRadius.z);

            level.sendParticles(
                    dustParticleOptions,
                    pos.x, pos.y + i, pos.z,
                    1, // count
                    0.0, 0.0, 0.0, // delta
                    0.0 // speed
            );
        }

        textDisplay.setYRot(textDisplay.getYRot() + 7.5f);

        if(groundBlock != Blocks.AIR)
        {
            Fill.replace(
                    level,
                    self.getOnPos(),
                    new BlockPos(2,2,2),
                    new BlockPos(-2,-2,-2),
                    groundBlock,
                    Splatoon.Tags.GROUND_BLOCKS
            );
        }

        if(playerTeam != null && self.tickCount % 10 == 0)
        {
            List<Player> playersInRange = Helpers.getEntitiesInRadius(level, Player.class, self.getEyePosition(), 3.0f);

            for(Player healPlayer : playersInRange)
            {
                IPlayerTeamMixin checkTeam = Teams.getTeamMixinFromPlayer(healPlayer);
                if(checkTeam != null && checkTeam == playerTeam)
                {
                    if(healPlayer.getHealth() < healPlayer.getMaxHealth())
                    {
                        healPlayer.heal(2.0f);
                        self.heal(-2.0f);
                    }
                }
            }
        }

        if(self.tickCount % 60 == 0) {
            self.heal(-2.0f);
        }

        float health = self.getHealth();
        textDisplay.setText(Component.literal(String.valueOf((int)health)));

        if(self.deathTime == 1) {
            textDisplay.discard();
        }
    }
}
