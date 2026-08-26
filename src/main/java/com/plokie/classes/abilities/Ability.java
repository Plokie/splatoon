package com.plokie.classes.abilities;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.Function;
import java.util.function.Supplier;

public class Ability {
    int rechargeTime = -1;
    int maxCount = -1;

    int rechargeTimer = -1;
    int count = 1;

    Function<Player, ItemStack> createItemFunc;
    ItemStack item;

    public void onGranted(Player player, int abilityIndex)
    {
        count = maxCount;

        item = createItemFunc.apply(player);
    }

    public void onRevoked(Player player, int abilityIndex)
    {
        if(abilityIndex >= 0) {
            int slot = abilityIndex + 2;

            ItemStack currentItem = player.getInventory().getItem(slot);
            if(!currentItem.is(Items.AIR)) {
                ItemStack customItem = new ItemStack(Items.AIR);
                player.getInventory().setItem(slot, customItem);
            }

        }
    }

    public void onUse()
    {
        count--;
        rechargeTimer = 0;
    }

    public void onUseBlock(Player player, InteractionHand hand, BlockHitResult hitResult, int abilityIndex)
    {

    }

    public void onUseItem(Player player, InteractionHand hand, int abilityIndex)
    {

    }



    public void tick(Player player, int abilityIndex)
    {
        int slot = abilityIndex + 2;

        ItemStack currentItem = player.getInventory().getItem(slot);

        if(count == 0)
        {
            int secondsLeft = (int)Math.ceil((rechargeTime - rechargeTimer) / 20.0);

            if(!currentItem.is(Items.BARRIER) || currentItem.getCount() != secondsLeft) {
                ItemStack customItem = new ItemStack(Items.BARRIER);
                customItem.setCount(secondsLeft);
                if(item != null) {
                    customItem.set(
                            DataComponents.ITEM_NAME,
                            item.getItemName()
                    );
                }
                player.getInventory().setItem(slot, customItem);
            }
        }
        else if(!currentItem.is(item.getItem()) || currentItem.getCount() != count)
        {
            ItemStack customItem = createItemFunc.apply(player);
            customItem.setCount(count);
            player.getInventory().setItem(slot, customItem);
            player.containerMenu.broadcastChanges();
        }

        if(rechargeTime >= 0) { // if the ability recharges over time

            if(rechargeTimer >= 0) { // if the ability requires recharging
                rechargeTimer += 1; // increment timer

                // if time is met
                if(rechargeTimer >= rechargeTime) {
                    count += 1; // increase count

                    rechargeTimer %= rechargeTime; // loop recharge timer

                    // check if we need to even continue recharging
                    // now we're at max count
                    if(count >= maxCount) {
                        rechargeTimer = -1;
                    }
                }
            }

        }



    }
}
