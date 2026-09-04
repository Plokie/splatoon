package com.plokie.customitems.items;

import com.plokie.Splatoon;
import com.plokie.classes.abilities.Ability;
import com.plokie.classes.abilities.AbilityManager;
import com.plokie.customitems.ICustomItem;
import com.plokie.helpers.ScheduleEvent;
import com.plokie.interfaces.IPlayerMixin;
import com.plokie.interfaces.IThrownTrident;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;

import java.util.HashMap;
import java.util.Map;

public class Hook extends ICustomItem {
    @Override
        public void onUserFunc(Player player, Object object)
    {
        AbstractArrow abstractArrow = (AbstractArrow)object;
        IThrownTrident thrownTrident = (IThrownTrident) abstractArrow;


        if(!player.getPassengers().isEmpty()) {
            for(Entity passenger : player.getPassengers())
            {
                if(passenger instanceof LivingEntity livingEntity)
                {
                    thrownTrident.setRopedTarget(livingEntity);
                    //passenger.startRiding(abstractArrow, true);
//                    for(ServerPlayer serverPlayer : PlayerLookup.tracking(abstractArrow))
//                    {
//                        serverPlayer.connection.send(new ClientboundSetPassengersPacket(abstractArrow));
//                    }
                }
            }
        }

        player.ejectPassengers();
    }

    @Override
    public void onAttackHit(Player player, Entity hitEntity)
    {
        if(player.getCooldowns().isOnCooldown(player.getItemBySlot(EquipmentSlot.MAINHAND))) return;

        if(hitEntity instanceof Shulker) return;

        if(hitEntity.isPassenger()) {
            player.getCooldowns().addCooldown(ResourceLocation.withDefaultNamespace("trident"), 60);
            player.ejectPassengers();
        }
        else
        {
            if(hitEntity instanceof LivingEntity livingEntity)
            {
                if(!player.getPassengers().isEmpty()) {
                    player.ejectPassengers();
                }
                livingEntity.startRiding(player);
                // livingEntity.hurtTime = 0;

            }

        }

    }

    @Override
    public void whileHeld(Player player)
    {
        if(player.isCrouching()) {
            player.ejectPassengers();
            player.getCooldowns().addCooldown(ResourceLocation.withDefaultNamespace("trident"), 60);
        }

        for(Entity passenger : player.getPassengers())
        {
            if(passenger instanceof LivingEntity livingEntity)
            {
                if(livingEntity.hurtTime != 0) {
                    var dmgSource = livingEntity.getLastDamageSource();

                    if(dmgSource != null && dmgSource.getEntity() != player)
                    {
                        player.ejectPassengers();
                        player.getCooldowns().addCooldown(ResourceLocation.withDefaultNamespace("trident"), 60);
                        break;
                    }

                }
            }
        }
    }

    @Override
    public void onEndHeld(Player player)
    {
        if(!player.getPassengers().isEmpty()) {
            player.ejectPassengers();
        }
    }
}
