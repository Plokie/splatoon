package com.plokie.customitems;

import com.plokie.customitems.items.InkRoller;
import com.plokie.customitems.items.guns.*;
import com.plokie.helpers.items.Enchantments;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public enum CustomItem {
    InkRoller(
            Builder
                    .item(Items.IRON_AXE)
                    .name("Ink Roller")
                    .enchant("sharpness", 1)
                    .enchant("smite", 3)
                    .behaviour(new InkRoller())
            .build()
    ),
    Splattershot(
            Builder
                    .item(Items.CARROT_ON_A_STICK)
                    .name("Splattershot")
                    .model(ResourceLocation.fromNamespaceAndPath("splatoon", "splattershot"))
                    .behaviour(new Splattershot())
            .build()
    ),
    Shotgun(
            Builder
                    .item(Items.CARROT_ON_A_STICK)
                    .name("Shotgun")
                    .model(ResourceLocation.fromNamespaceAndPath("splatoon", "shotgun"))
                    .behaviour(new Shotgun())
            .build()
    ),
    Dualies(
            Builder
                    .item(Items.CARROT_ON_A_STICK)
                    .name("Dualies")
                    .model(ResourceLocation.fromNamespaceAndPath("splatoon", "dualies"))
                    .behaviour(new Dualies())
            .build()
    ),
    Burstshot(
            Builder
                    .item(Items.CARROT_ON_A_STICK)
                    .name("Burstshot")
                    .model(ResourceLocation.fromNamespaceAndPath("splatoon", "burst_rifle"))
                    .behaviour(new Burstshot())
            .build()
    ),
    Revolver(
            Builder
                    .item(Items.CARROT_ON_A_STICK)
                    .name("Revolver")
                    .model(ResourceLocation.fromNamespaceAndPath("splatoon", "pistol"))
                    .behaviour(new Revolver())
            .build()
    );

    final CustomItemDefinition itemInstance;

    CustomItem(CustomItemDefinition item)
    {
        this.itemInstance = item;
    }

    public ItemStack getItem()
    {
        return this.itemInstance.baseItem;
    }
    public CustomItemDefinition getItemDefinition() { return this.itemInstance; }

    public static class Builder
    {
        ItemStack baseItem;
        ICustomItem itemInterface = null;

        public static CustomItem.Builder item(Item item)
        {
            Builder builder = new Builder();
            builder.baseItem = new ItemStack(item);
            return builder;
        }

        public Builder name(Component name)
        {
            baseItem.set(DataComponents.ITEM_NAME, name);
            return this;
        }

        public Builder name(String name)
        {
            baseItem.set(DataComponents.ITEM_NAME, Component.literal(name));
            return this;
        }

        public Builder model(ResourceLocation modelPath) {
            baseItem.set(DataComponents.ITEM_MODEL, modelPath);
            return this;
        }

        public Builder enchant(String enchantment, int level)
        {
            Enchantments.AddEnchantmentToItem(baseItem, enchantment, level);
            return this;
        }

        public Builder behaviour(ICustomItem itemInterface)
        {
            this.itemInterface = itemInterface;
            return this;
        }

        public CustomItemDefinition build()
        {
            return new CustomItemDefinition(baseItem, itemInterface);
        }

    }
}
