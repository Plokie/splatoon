package com.plokie.customitems;

import com.plokie.Splatoon;
import com.plokie.customitems.items.*;
import com.plokie.customitems.items.guns.*;
import com.plokie.helpers.Teams;
import com.plokie.helpers.items.Enchantments;
import com.plokie.interfaces.IPlayerTeamMixin;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.CustomData;
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
    Invalid(
            Builder
                    .item(Items.EMERALD)
                    .name("Invalid item")
                    .model(ResourceLocation.fromNamespaceAndPath("splatoon", "splattershot"))
                    .build()
    ),
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
    ),
    SpectateItem(
            Builder
                    .item(Items.WARPED_FUNGUS_ON_A_STICK)
                    .name("Toggle spectating")
                    .model(ResourceLocation.fromNamespaceAndPath("splatoon", "spectate"))
                    .behaviour(new SpectateItem())
            .build()
    ),
    InkPuck(
            Builder
                    .item(Items.FIREWORK_STAR)
                    .name("Ink Puck")
                    .behaviour(new InkPuck())
                    //.model()
            .build()
    ),
    EnderPearl(Builder.item(Items.ENDER_PEARL).name("Ender Pearl").build()),
    WindCharge(Builder.item(Items.WIND_CHARGE).name("Wind Charge").build()),
    CleansingGrenade(Builder.item(Items.EXPERIENCE_BOTTLE).name("Cleansing Grenade").behaviour(new CleansingGrenade()).build()),
    Hook(Builder
            .item(Items.TRIDENT)
            .enchant("loyalty", 3)
            .build()
    ),
    HealthPotion(Builder
            .item(Items.SPLASH_POTION)
            .name("Health Potion")
            .dataCallback((player, item)->{
                item.set(
                        DataComponents.POTION_CONTENTS,
                        new PotionContents(Potions.STRONG_HEALING)
                );
            })
            .build()
    ),
    HealthBubble(Builder
            .item(Items.SHULKER_SPAWN_EGG)
            .name("Health Bubble")
            .behaviour(new HealthBubble())
            .dataCallback(com.plokie.customitems.items.HealthBubble::dataCallback)
            .build()
    ),
    SmokeGrenade(Builder
            .item(Items.LINGERING_POTION)
            .name("Smoke Grenade")
            .behaviour(new SmokeGrenade())
            .dataCallback(com.plokie.customitems.items.SmokeGrenade::dataCallback)
            .build()
    ),
    InkBomb(Builder
            .item(Items.SHEEP_SPAWN_EGG)
            .name("Ink Bomb")
            .enchant("knockback", 5)
            .model(ResourceLocation.fromNamespaceAndPath("minecraft", "tnt"))
            .behaviour(new InkBomb())
            .dataCallback(com.plokie.customitems.items.InkBomb::dataCallback)
            .build()
    ),
    FocusApple(Builder
            .item(Items.GOLDEN_APPLE)
            .name("Apple of Focus")
            .behaviour(new FocusApple())
            .build()
    ),
    SuperJumpIndicator(Builder.item(Items.FEATHER).name("Super Jump").build())
    ;


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

    public boolean is(ItemStack item)
    {
        if(item.getItem()!=this.itemInstance.baseItem.getItem()) return false;

        if(!item.has(DataComponents.CUSTOM_DATA)) return false;

        CustomData checkCustomData = item.get(DataComponents.CUSTOM_DATA);
        CustomData thisCustomData = this.itemInstance.baseItem.get(DataComponents.CUSTOM_DATA);

        if(checkCustomData == null || thisCustomData==null) return false;

        CompoundTag checkNbt = checkCustomData.copyTag();
        CompoundTag thisNbt = thisCustomData.copyTag();

        if(!checkNbt.contains("customItemId") || !thisNbt.contains("customItemId")) return false;

        String checkItemId = checkNbt.getStringOr("customItemId", "");
        String thisItemId = thisNbt.getStringOr("customItemId", "");

        if(!thisItemId.equals(checkItemId)) return false;


        //Splatoon.LOGGER.info("\t\t\t{}=={}", this.itemInstance.baseItem.getDisplayName(), item.getDisplayName());
        //if(!this.itemInstance.baseItem.getDisplayName().equals(item.getDisplayName())) return false;
        //this.itemInstance.baseItem.getHoverName().getString();

        return true;

//        Function<String, String> stripName = value->{
//            if(value.endsWith(".empty")) value = value.replace(".empty", "");
//            if(value.endsWith(".awkward")) value = value.replace(".awkward", "");
//            return value;
//        };
//
//        String checkItemName = stripName.apply(item.getDisplayName());
//        String thisItemName = stripName.apply(this.itemInstance.baseItem.getItemName().getString());
//        Splatoon.LOGGER.info("\t\t\t{}=={}", checkItemName, thisItemName);
//
//        return thisItemName.equals(checkItemName);
        //return item.getItemName().equals(this.itemInstance.baseItem.getItemName()) && (item.getItem()==this.itemInstance.baseItem.getItem());
    }

    public ItemStack construct(Player player)
    {
        ItemStack ret = getItem().copy();

        for(var callback : getDataCallbacks())
        {
            callback.accept(player, ret);
        }

        return ret;
    }

    public static class Builder
    {
        String name = "";
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
            this.name = name.getString();
            baseItem.set(DataComponents.ITEM_NAME, name);
            return this;
        }

        public Builder name(String name)
        {
            this.name = name;
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
            return new CustomItemDefinition(this.name, baseItem, itemInterface, this.dataCallbacks);
        }

    }
}
