package com.plokie.classes.abilities;

import com.plokie.Splatoon;
import com.plokie.helpers.items.Enchantments;
import com.plokie.interfaces.IPlayerMixin;
import com.plokie.interfaces.IProjectile;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.TickTask;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.UseCooldown;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.Objects;

public class Hook extends Ability {

    public Hook()
    {
        this.rechargeTime = 15 * 20;
        this.maxCount = 1;

        this.createItemFunc = player -> {
            ItemStack item = new ItemStack(Items.TRIDENT);

            item.set(
                    DataComponents.ITEM_NAME,
                    Component.literal("Hook")
            );

            item.set(
                    DataComponents.USE_COOLDOWN,
                    new UseCooldown(15.0f)
            );

            Enchantments.AddEnchantmentToItem(item, "loyalty", 3);

            return item;
        };
    }

    @Override
    public void onUseItem(Player player, InteractionHand hand, int abilityIndex) {
        super.onUse();

        Splatoon.LOGGER.info("Used trident");

        Level level = player.level();

        Objects.requireNonNull(level.getServer()).schedule(
                new TickTask(level.getServer().getTickCount() + 1, ()->{
                    AABB area = new AABB(player.getOnPos()).inflate(2.0);

                    level.getEntitiesOfClass(ThrownTrident.class, area).forEach(trident -> {
                        ((IProjectile)trident).setPlayerOwner(player);
                    });
                })
        );
    }

    @Override
    public void tick(Player player, int abilityIndex) {
        super.tick(player, abilityIndex);
    }
}
