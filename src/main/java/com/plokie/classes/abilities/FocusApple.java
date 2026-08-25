package com.plokie.classes.abilities;

import com.plokie.Splatoon;
import com.plokie.helpers.Effects;
import com.plokie.interfaces.IPlayerMixin;
import com.plokie.interfaces.IProjectile;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.TickTask;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownLingeringPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Objects;

public class FocusApple extends Ability {
    static boolean isStaticInitialised = false;

    public FocusApple()
    {
        this.rechargeTime = 30 * 20;
        this.maxCount = 1;

        this.createItemFunc = player -> {
            ItemStack item = new ItemStack(Items.GOLDEN_APPLE);

            item.set(
                    DataComponents.ITEM_NAME,
                    Component.literal("Apple of Focus")
            );

            //List<ConsumeEffect> consumeEffectList = List.of();

            //ConsumeEffect.Type.APPLY_EFFECTS

            //Consumable consumable = new Consumable(0.0f, ItemUseAnimation.EAT, SoundEvents.GENERIC_EAT, true, consumeEffectList);

            Consumable consumable = Consumable.builder().consumeSeconds(0.0f).build();

            item.set(
                    DataComponents.CONSUMABLE,
                    consumable
            );

            return item;
        };
    }

    void staticInitialise()
    {
        UseItemCallback.EVENT.register((player, world, hand)->{
            if(!world.isClientSide() && player.getItemInHand(hand).getItem() == Items.GOLDEN_APPLE)
            {
                ((IPlayerMixin)player).onUseAbilityItem("FocusApple", player, hand);
            }

            return InteractionResult.PASS;
        });

        isStaticInitialised = true;
    }

    @Override
    public void onUseItem(Player player, InteractionHand hand, int abilityIndex)
    {
        super.onUse();

        //Effects.givePotionEffect(player, MobEffects.SLOWNESS, 14, 1, true);
        Effects.givePotionEffect(player, MobEffects.STRENGTH, 14, 3, true);
        Effects.givePotionEffect(player, MobEffects.REGENERATION, 14, 1, true);
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
