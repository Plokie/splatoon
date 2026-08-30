package com.plokie.customitems;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class CustomItemDefinition {
    public ItemStack baseItem;
    ICustomItem itemInterface = null;
    List<BiConsumer<Player, ItemStack>> dataCallbacks = new ArrayList<>();

    public ICustomItem getItemInterface()
    {
        return itemInterface;
    }

    public CustomItemDefinition(ItemStack baseItem, ICustomItem itemInterface, List<BiConsumer<Player, ItemStack>> dataCallbacks)
    {
        this.baseItem = baseItem;

        if(itemInterface == null)
        {
            itemInterface = new ICustomItem();
        }

        this.itemInterface = itemInterface;
        this.dataCallbacks = dataCallbacks;
    }
}
