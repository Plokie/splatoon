package com.plokie.customitems;

import com.plokie.classes.abilities.Ability;
import com.plokie.interfaces.IPlayerMixin;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

public class ICustomItem {
    protected int useDuration = 1;
    protected int usageRate = 1;

    public int getUseDuration() { return useDuration; }
    public int getUsageRate() { return usageRate; }
    public void onUseItem(Player player) {
        IPlayerMixin playerMixin = (IPlayerMixin)player;

        int idx = 0;
        for(Ability ability : playerMixin.getAbilities()) {
            if(ability.getItem().is(player.getItemInHand(player.getUsedItemHand())))
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
            if(ability.getItem().is(player.getItemInHand(player.getUsedItemHand())))
            {
                ability.onUseBlock(player, player.getUsedItemHand(), hit, idx);
            }
            idx++;
        }
    }

    public void onUserFunc(Player player, Object object) {}

}
