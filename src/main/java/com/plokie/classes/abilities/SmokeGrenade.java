package com.plokie.classes.abilities;

import com.plokie.Splatoon;
import com.plokie.interfaces.IPlayerMixin;
import com.plokie.interfaces.IProjectile;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.TickTask;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownLingeringPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.apache.logging.log4j.core.tools.picocli.CommandLine;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SmokeGrenade extends Ability {
    static boolean isStaticInitialised = false;

    public SmokeGrenade()
    {
        this.rechargeTime = (int)22.5 * 20;
        this.maxCount = 1;

        this.createItemFunc = player -> {
            ItemStack customItem = new ItemStack(Items.LINGERING_POTION);

            PotionContents potionContents = new PotionContents(Optional.of(Potions.AWKWARD), Optional.of(6473482), List.of(), Optional.empty());

            customItem.set(
                    DataComponents.POTION_CONTENTS,
                    potionContents
            );

            customItem.set(
                    DataComponents.CUSTOM_NAME,
                    Component.literal("Smoke Grenade").setStyle(Style.EMPTY.withItalic(false))
            );

            return customItem;
        };
    }

    void staticInitialise()
    {
        UseItemCallback.EVENT.register((player, world, hand)->{
            if(!world.isClientSide() && player.getItemInHand(hand).getItem() == Items.LINGERING_POTION)
            {
                ((IPlayerMixin)player).onUseAbilityItem("SmokeGrenade", player, hand);
            }

            return InteractionResult.PASS;
        });

        isStaticInitialised = true;
    }

    @Override
    public void onUseItem(Player player, InteractionHand hand, int abilityIndex)
    {
        super.onUse();

        Level level = player.level();

        Objects.requireNonNull(level.getServer()).schedule(
                new TickTask(level.getServer().getTickCount() + 1, ()->{
                    Splatoon.LOGGER.info("\tTick task {}", player.getName());
                    AABB area = new AABB(player.getOnPos()).inflate(2.0);

                    level.getEntitiesOfClass(ThrownLingeringPotion.class, area).forEach(potion -> {
                        ((IProjectile)potion).setPlayerOwner(player);
                    });
                }
                )
        );
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
