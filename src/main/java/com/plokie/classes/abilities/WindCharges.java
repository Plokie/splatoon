package com.plokie.classes.abilities;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.UseCooldown;

public class WindCharges extends Ability {
    public WindCharges()
    {
        this.rechargeTime = 5 * 20;
        this.maxCount = 5;

        this.createItemFunc = player -> {
            ItemStack item = new ItemStack(Items.WIND_CHARGE);

            item.set(
                    DataComponents.ITEM_NAME,
                    Component.literal("Wind Charge")
            );

//            item.set(
//                DataComponents.USE_COOLDOWN,
//                new UseCooldown(5.0f)
//            );

            return item;
        };
    }

    @Override
    public void onUseItem(Player player, InteractionHand hand, int abilityIndex) {
        super.onUse();
    }

    @Override
    public void tick(Player player, int abilityIndex) {
        super.tick(player, abilityIndex);
    }
}
