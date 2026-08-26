package com.plokie.customitems;

import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public class CustomItemDefinition {
    public ItemStack baseItem;
    ICustomItem itemInterface = null;

    public ICustomItem getItemInterface()
    {
        return itemInterface;
    }

    public CustomItemDefinition(ItemStack baseItem, ICustomItem itemInterface)
    {
        this.baseItem = baseItem;
        this.itemInterface = itemInterface;
    }
}
