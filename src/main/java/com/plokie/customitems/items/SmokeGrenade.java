package com.plokie.customitems.items;

import com.plokie.customitems.ICustomItem;
import com.plokie.helpers.ScheduleEvent;
import com.plokie.interfaces.IProjectile;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownLingeringPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Optional;

public class SmokeGrenade extends ICustomItem {
    public static void dataCallback(Player player, ItemStack item)
    {
        PotionContents potionContents = new PotionContents(Optional.of(Potions.AWKWARD), Optional.of(6473482), List.of(), Optional.empty());

        item.set(
                DataComponents.POTION_CONTENTS,
                potionContents
        );

        item.set(
                DataComponents.CUSTOM_NAME,
                Component.literal("Smoke Grenade").setStyle(Style.EMPTY.withItalic(false))
        );
    }

    @Override
    public void onUseItem(Player player) {
        super.onUseItem(player);

        ScheduleEvent.schedule(
                1, server->{
                    //Splatoon.LOGGER.info("\tTick task {}", player.getName());
                    AABB area = new AABB(player.getOnPos()).inflate(4.0);

                    player.level().getEntitiesOfClass(ThrownLingeringPotion.class, area).forEach(potion -> {
                        ((IProjectile)potion).setPlayerOwner(player);
                    });
                }

        );
    }
}
