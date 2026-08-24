package com.plokie.classes.abilities;

import com.plokie.Splatoon;
import com.plokie.interfaces.IPlayerMixin;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.UseCooldown;

public class EnderPearl extends Ability {
    static boolean isStaticInitialised = false;

    public EnderPearl()
    {
        this.rechargeTime = 25*20;
        this.maxCount = 1;

        this.createItemFunc = player -> {
            ItemStack item = new ItemStack(Items.ENDER_PEARL);

            item.set(
                    DataComponents.ITEM_NAME,
                    Component.literal("Ender Pearl")
            );

            item.set(
              DataComponents.USE_COOLDOWN,
              new UseCooldown(25.f)
            );

            return item;
        };
    }

    void staticInitialise()
    {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if(!world.isClientSide() && player.getItemInHand(hand).getItem() == Items.ENDER_PEARL)
            {
                ((IPlayerMixin)player).onUseAbilityItem("EnderPearl", player, hand);
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
