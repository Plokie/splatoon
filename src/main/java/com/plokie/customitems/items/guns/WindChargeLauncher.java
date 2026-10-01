package com.plokie.customitems.items.guns;

import com.plokie.customitems.ICustomItem;
import com.plokie.helpers.Teams;
import com.plokie.interfaces.IPlayerMixin;
import com.plokie.interfaces.IPlayerTeamMixin;
import com.plokie.interfaces.IProjectile;
import com.plokie.interfaces.IWindChargeMixin;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.windcharge.WindCharge;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class WindChargeLauncher extends ICustomItem {

    public WindChargeLauncher() {
        this.useDuration = 5;
        this.usageRate = 15;
    }

    @Override
    public void onUseItem(Player player) {
        IPlayerMixin playerMixin = (IPlayerMixin) player;

        if(playerMixin.getTimeNotInInk() < 2) {
            return;
        }

        if(playerMixin.getInk() < 0.15f) {
            return;
        }

        playerMixin.changeInk(-0.15f);

        WindCharge windCharge = EntityType.WIND_CHARGE.create(player.level(), EntitySpawnReason.SPAWN_ITEM_USE);
        if(windCharge == null) return;
//
//        IPlayerTeamMixin playerTeam = Teams.getTeamMixinFromPlayer(player);
//        if(playerTeam != null) {
//
//        }

        Level level = player.level();

        player.level().addFreshEntity(windCharge);

        Vec3 playerPos = player.getEyePosition();
        Vec3 forward = player.getForward();

        level.playSound(
                null, // everyone
                playerPos.x, playerPos.y, playerPos.z,
                SoundEvents.ENDER_DRAGON_SHOOT,
                SoundSource.HOSTILE,
                4.0f, // volume
                1.0f // pitch
        );

        level.playSound(
                null, // everyone
                playerPos.x, playerPos.y, playerPos.z,
                SoundEvents.ENDER_DRAGON_FLAP,
                SoundSource.HOSTILE,
                4.0f, // volume
                1.0f // pitch
        );

        float forwardMult = 0.5f;

        Vec3 spawnPos = new Vec3(playerPos.x + (forward.x * forwardMult), (playerPos.y - 0.05f) + (forward.y * forwardMult), playerPos.z + (forward.z * forwardMult));

        windCharge.setPos(spawnPos);
        windCharge.setXRot(player.getXRot());
        windCharge.setYRot(player.getYRot());

        Vec3 playerVel = player.getDeltaMovement();
        playerVel = new Vec3(playerVel.x * 1.5, 0.0, playerVel.z * 1.5);

        float shootForceFl = 2.0f;

        Vec3 shootForce = new Vec3((forward.x * shootForceFl) + playerVel.x, ((forward.y * shootForceFl) + playerVel.y), (forward.z * shootForceFl) + playerVel.z);
        windCharge.setDeltaMovement(shootForce);

        windCharge.setOwner(player);

        ((IWindChargeMixin)windCharge).setShotByLauncher(true);
        //((IProjectile)windCharge).setPlayerOwner(player);
    }
}
