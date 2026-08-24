package com.plokie.mixin;

import com.plokie.Splatoon;
import com.plokie.helpers.Teams;
import com.plokie.interfaces.IPlayerTeamMixin;
import com.plokie.interfaces.IProjectile;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractThrownPotion;
import net.minecraft.world.entity.projectile.ThrownLingeringPotion;
import net.minecraft.world.entity.projectile.ThrownSplashPotion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mixin(AbstractThrownPotion.class)
public class LingeringPotionMixin implements IProjectile {
    @Unique
    UUID playerOwnerUUID = null;

    @Override
    public void setPlayerOwner(Player player)
    {
        playerOwnerUUID = player.getUUID();
    }

    @Inject(method="onHit", at=@At("TAIL"))
    void onHit(HitResult hitResult, CallbackInfo ci)
    {
        AbstractThrownPotion thrownPotion = (AbstractThrownPotion)(Object)this;

        if(thrownPotion instanceof ThrownLingeringPotion self)
        {
            if(playerOwnerUUID != null)
            {
                Level level = self.level();
                Player player = level.getPlayerByUUID(playerOwnerUUID);

                if(player != null)
                {
                    IPlayerTeamMixin playerTeam = Teams.getTeamMixinFromPlayer(player);
                    if (playerTeam != null) {
                        // create smoke cloud
                        AreaEffectCloud smokeCloud = EntityType.AREA_EFFECT_CLOUD.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
                        if(smokeCloud == null) return;

                        PotionContents potionContents = new PotionContents(Optional.of(Potions.AWKWARD), Optional.of(playerTeam.getTeamColourInt()), List.of(), Optional.empty());

                        smokeCloud.setPotionContents(potionContents);

                        smokeCloud.setOwner(player);

                        smokeCloud.setPos(self.getEyePosition());

                        smokeCloud.addTag("SmokeCloud");

                        smokeCloud.setUUID(UUID.randomUUID());

                        level.addFreshEntity(smokeCloud);

                    }
                }
            }

        }

    }
}
