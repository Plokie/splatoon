package com.plokie.classes.abilities;

import com.plokie.Splatoon;
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
            if(!world.isClientSide() && player.getItemInHand(hand).getItem() == Items.EXPERIENCE_BOTTLE)
            {
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

        Objects.requireNonNull(level.getServer()).schedule(
                new TickTask(level.getServer().getTickCount() + 1, ()->{
                    AABB area = new AABB(player.getOnPos()).inflate(2.0);

                    level.getEntitiesOfClass(ThrownExperienceBottle.class, area).forEach(bottle -> {
                        ((IProjectile)bottle).setPlayerOwner(player);
                    });
                })
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
