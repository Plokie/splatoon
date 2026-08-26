package com.plokie.classes.abilities;

import com.plokie.Splatoon;
import com.plokie.helpers.ScheduleEvent;
import com.plokie.interfaces.IPlayerMixin;
import com.plokie.interfaces.IProjectile;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.TickTask;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.ticks.ScheduledTick;

import java.util.Objects;

public class CleansingGrenade extends Ability {
    static boolean isStaticInitialised = false;

    public CleansingGrenade()
    {
        this.rechargeTime = 30 * 20;
        this.maxCount = 1;

        this.createItemFunc = player -> {
            ItemStack item = new ItemStack(Items.EXPERIENCE_BOTTLE);

            item.set(
                    DataComponents.ITEM_NAME,
                    Component.literal("Cleansing Grenade")
            );

            return item;
        };
    }

    void staticInitialise()
    {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack itemInHand = player.getItemInHand(hand);

            //Splatoon.LOGGER.info("Evaluate used {} {} equals {}", itemInHand, itemInHand.getItemName(), this.item.getItemName());

            if(!world.isClientSide() && itemInHand.is(Items.EXPERIENCE_BOTTLE))
            {
                //Splatoon.LOGGER.info("Cleansing grenade used");
                ((IPlayerMixin)player).onUseAbilityItem("CleansingGrenade", player, hand);
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

        //Splatoon.LOGGER.info("Scheduling cleansing... {}", level.getServer().getTickCount());

        ScheduleEvent.schedule(
            1, server->{
                //Splatoon.LOGGER.info("Executing cleansing tick task {}", level.getServer().getTickCount());

                AABB area = new AABB(player.getOnPos()).inflate(4.0);

                level.getEntitiesOfClass(ThrownExperienceBottle.class, area).forEach(bottle -> {
                    //Splatoon.LOGGER.info("found {}", bottle);
                    ((IProjectile)bottle).setPlayerOwner(player);
                });
            }
        );
    }

    @Override
    public void tick(Player player, int abilityIndex)
    {
        if(!isStaticInitialised)
        {
            staticInitialise();
        }

        super.tick(player, abilityIndex);
    }

}
