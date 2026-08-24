package com.plokie.helpers.items;

import com.plokie.Splatoon;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public class Enchantments {
    public static void AddEnchantmentToItem(ItemStack item, String enchantmentId, int level)
    {
        Registry<Enchantment> enchantmentRegistry = Splatoon.SERVER.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

        ResourceKey<Enchantment> enchantmentKey = ResourceKey.create(
                Registries.ENCHANTMENT,
                ResourceLocation.withDefaultNamespace(enchantmentId)
        );

        Holder.Reference<Enchantment> enchantmentHolder = enchantmentRegistry.getOrThrow(enchantmentKey);

        ItemEnchantments existingEnchantments = item.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

        ItemEnchantments.Mutable builder = new ItemEnchantments.Mutable(existingEnchantments);
        builder.set(enchantmentHolder, level);

        item.set(
                DataComponents.ENCHANTMENTS,
                builder.toImmutable()
        );
    }
}
