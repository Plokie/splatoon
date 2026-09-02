package com.plokie.classes.abilities;

import com.plokie.customitems.CustomItem;
import com.plokie.interfaces.IPlayerMixin;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.Function;
import java.util.function.Supplier;

public class Ability {
    public enum UsageTypeFlags {
        Item(1), Block(2);
        final int value;
        UsageTypeFlags(int value) {
            this.value = value;
        }
    }

    final AbilityManager.AbilityEnum enumVal;

    int rechargeTime = -1;
    int maxCount = -1;

    int rechargeTimer = -1;
    int count = 1;

    int usageTypeFlags = 0;
    boolean hideWhileInInk = true;

    CustomItem item = CustomItem.Invalid;

    public CustomItem getItem()
    {
        return item;
    }

    public AbilityManager.AbilityEnum getEnumValue() {
        return enumVal;
    }

    Ability(AbilityManager.AbilityEnum enumVal){
        this.enumVal = enumVal;
    }
    Ability(AbilityManager.AbilityEnum enumVal, CustomItem customItem, int usageTypeFlags, float rechargeTimeSeconds, int maxCount)
    {
        this.enumVal = enumVal;
        this.item = customItem;
        this.rechargeTime = (int)Math.floor(rechargeTimeSeconds * 20);
        this.maxCount = maxCount;
        this.usageTypeFlags = usageTypeFlags;
    }

    public void onGranted(Player player, int abilityIndex)
    {
        count = maxCount;
    }

    public void onRevoked(Player player, int abilityIndex)
    {
        if(abilityIndex >= 0) {
            int slot = abilityIndex + 2;

            ItemStack currentItem = player.getInventory().getItem(slot);
            if(!currentItem.is(Items.AIR)) {
                ItemStack customItem = new ItemStack(Items.AIR);
                player.getInventory().setItem(slot, customItem);

                //((ServerPlayer)player).containerMenu.broadcastChanges();
            }

        }
    }

    public void onUse()
    {
        count--;
        if(rechargeTimer <= -1) {
            rechargeTimer = 0;
        }
    }

    public void onUseBlock(Player player, InteractionHand hand, BlockHitResult hitResult, int abilityIndex)
    {
        if((this.usageTypeFlags & UsageTypeFlags.Block.value) != 0) {
            onUse();
        }
    }

    public void onUseItem(Player player, InteractionHand hand, int abilityIndex)
    {
        if((this.usageTypeFlags & UsageTypeFlags.Item.value) != 0) {
            onUse();
        }
    }



    public void tick(Player player, int abilityIndex)
    {
        int slot = abilityIndex + 2;

        ItemStack currentItem = player.getInventory().getItem(slot);

        IPlayerMixin playerMixin = (IPlayerMixin) player;

        boolean doGive = true;
        if(hideWhileInInk && playerMixin.isInInk()) doGive = false;
        if(count == 0) doGive = true;

        if(doGive)
        {
            if(count == 0)
            {
                int secondsLeft = (int)Math.ceil((rechargeTime - rechargeTimer) / 20.0);

                if(!currentItem.is(Items.BARRIER) || currentItem.getCount() != secondsLeft) {
                    ItemStack customItem = new ItemStack(Items.BARRIER);

                    if(item != null) {
                        customItem.set(
                                DataComponents.ITEM_NAME,
                                item.getItem().getItemName()
                        );
                    }

                    if(item != null) {
                        ItemEnchantments enchants =  item.getItem().get(DataComponents.ENCHANTMENTS);
                        customItem.set(DataComponents.ENCHANTMENTS, enchants);
                    }

                    customItem.setCount(secondsLeft);
                    player.getInventory().setItem(slot, customItem);
                    //player.containerMenu.broadcastChanges();
                }
            }
            else if(!item.is(currentItem) || currentItem.getCount() != count)
            {
//                ItemStack customItem = createItemFunc.apply(player);
                ItemStack customItem = item.construct(player);
                customItem.setCount(count);
                player.getInventory().setItem(slot, customItem);
                //player.containerMenu.broadcastChanges();
            }
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
