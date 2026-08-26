package com.plokie.classes.abilities;

import com.plokie.interfaces.IPlayerMixin;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.UseCooldown;

public class WindCharges extends Ability {
    static boolean isStaticInitialised = false;

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

            item.set(
                DataComponents.USE_COOLDOWN,
                new UseCooldown(0.1f)
            );

            return item;
        };
    }

    void staticInitialise()
    {
        UseItemCallback.EVENT.register((player, world, hand)->{
            if(!world.isClientSide() && player.getItemInHand(hand).getItem() == Items.WIND_CHARGE)
            {
                ((IPlayerMixin)player).onUseAbilityItem("WindCharges", player, hand);
            }

            return InteractionResult.PASS;
        });

        isStaticInitialised = true;
    }

    @Override
    public void onUseItem(Player player, InteractionHand hand, int abilityIndex) {
        super.onUse();
    }

    @Override
    public void tick(Player player, int abilityIndex) {
        if(!isStaticInitialised)
        {
            staticInitialise();
        }

        super.tick(player, abilityIndex);
    }
}
