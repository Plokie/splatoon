package com.plokie.customitems.items;

import com.plokie.customitems.ICustomItem;
import com.plokie.helpers.Effects;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

public class FocusApple extends ICustomItem {
    @Override
    public void onUseItem(Player player)
    {
        super.onUseItem(player);

        Effects.givePotionEffect(player, MobEffects.REGENERATION, 14, 1, true);
    }
}
