package com.plokie.classes.abilities;

import com.plokie.interfaces.IPlayerMixin;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

public class HealthPotions extends Ability {
    static boolean isStaticInitialised = false;

    public HealthPotions()
    {
        this.rechargeTime = 5 * 20;
        this.maxCount = 16;

        this.createItemFunc = player -> {
            ItemStack item = new ItemStack(Items.SPLASH_POTION);

            PotionContents potionContents = new PotionContents(Potions.STRONG_HEALING);

            item.set(
                    DataComponents.POTION_CONTENTS,
                    potionContents
            );

            return item;
        };
    }

    void staticInitialise()
    {
        UseItemCallback.EVENT.register((player, world, hand)->{
            if(!world.isClientSide() && player.getItemInHand(hand).getItem() == Items.SPLASH_POTION)
            {
                ((IPlayerMixin)player).onUseAbilityItem("HealthPotions", player, hand);
            }

            return InteractionResult.PASS;
        });

        isStaticInitialised = true;
    }

    @Override
    public void onUseItem(Player player, InteractionHand hand, int abilityIndex)
    {
        super.onUse();
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
