package com.plokie.customitems;

import com.plokie.classes.abilities.Ability;
import com.plokie.interfaces.IPlayerMixin;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

public class ICustomItem {
    protected int useDuration = 1;
    protected int usageRate = 5;

    public int getUseDuration() { return useDuration; }
    public int getUsageRate() { return usageRate; }
    public void onUseItem(Player player) {
        IPlayerMixin playerMixin = (IPlayerMixin)player;

        int idx = 0;
        for(Ability ability : playerMixin.getAbilities()) {
            if(ability.getItem().is(player.getItemBySlot(EquipmentSlot.MAINHAND)))
            {
                ability.onUseItem(player, player.getUsedItemHand(), idx);
            }
            idx++;
        }
    }
    public void whileHeld(Player player) {}
    public void onStartHeld(Player player) {}
    public void onEndHeld(Player player) {}
    public void onAttackHit(Player player, Entity hitEntity) {}
    public void onUseBlock(Player player, BlockHitResult hit) {
        IPlayerMixin playerMixin = (IPlayerMixin)player;

        int idx = 0;
        for(Ability ability : playerMixin.getAbilities()) {
            if(ability.getItem().is(player.getItemBySlot(EquipmentSlot.MAINHAND)))
            {
                ability.onUseBlock(player, InteractionHand.MAIN_HAND, hit, idx);
            }
            idx++;
        }
    }

    public void onUserFunc(Player player, Object object) {}

}
