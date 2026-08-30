package com.plokie.customitems;

import com.plokie.customitems.items.InkRoller;
import com.plokie.customitems.items.JumperMace;
import com.plokie.customitems.items.guns.*;
import com.plokie.helpers.Teams;
import com.plokie.helpers.items.Enchantments;
import com.plokie.interfaces.IPlayerTeamMixin;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.scores.PlayerTeam;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public enum CustomItem {
    StandardSword(
            Builder
                    .item(Items.DIAMOND_SWORD)
                    .name("Diamond Sword")
                    .enchant("sharpness", 3)
            .build()
    ),
    InkRoller(
            Builder
                    .item(Items.IRON_AXE)
                    .name("Ink Roller")
                    .enchant("sharpness", 1)
                    .enchant("smite", 3)
                    .behaviour(new InkRoller())
            .build()
    ),
    ScoutSword(
            Builder
                    .item(Items.NETHERITE_SWORD)
                    .name("Netherite Sword")
                    .enchant("sharpness", 6)
            .build()
    ),
    SupportAxe(
            Builder
                    .item(Items.IRON_AXE)
                    .name("Iron Axe")
                    .enchant("sharpness", 4)
            .build()
    ),
    JumperMace(
            Builder
                    .item(Items.MACE)
                    .name("Mace")
                    .enchant("wind_burst", 3)
                    .enchant("density", 3)
                    .behaviour(new JumperMace())
            .build()
    ),
    SniperSword(
            Builder
                    .item(Items.DIAMOND_SWORD)
                    .name("Diamond Sword")
                    .enchant("sharpness", 4)
                    .enchant("knockback", 2)
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
    ),
    SniperGun(
            Builder
                    .item(Items.CARROT_ON_A_STICK)
                    .name("Sniper")
                    .model(ResourceLocation.fromNamespaceAndPath("splatoon", "sniper"))
                    .behaviour(new SniperGun())
            .build()
    ),
    KnockbackBrush(
            Builder
                    .item(Items.BRUSH)
                    .name("Knockback Brush")
                    .enchant("knockback", 5)
            .build()
    ),
    Shield(
            Builder
                    .item(Items.SHIELD)
                    .name("Shield")
                    .dataCallback((player, item)->{
                        IPlayerTeamMixin team = Teams.getTeamMixinFromPlayer(player);
                        if(team == null) return;
                        PlayerTeam playerTeam = (PlayerTeam)team;
                        String name = playerTeam.getName().toUpperCase();
                        try {
                            DyeColor col = DyeColor.valueOf(name);
                            item.set(DataComponents.BASE_COLOR, col);
                        }
                        catch(Exception e)
                        {

                        }

                    })
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
    public List<BiConsumer<Player, ItemStack>> getDataCallbacks() { return this.itemInstance.dataCallbacks; }

    public static class Builder
    {
        ItemStack baseItem;
        ICustomItem itemInterface = null;
        List<BiConsumer<Player, ItemStack>> dataCallbacks = new ArrayList<>();

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

        public Builder dataCallback(BiConsumer<Player, ItemStack> callback)
        {
            dataCallbacks.add(callback);
            return this;
        }

        public CustomItemDefinition build()
        {
            return new CustomItemDefinition(baseItem, itemInterface, this.dataCallbacks);
        }

    }
}
