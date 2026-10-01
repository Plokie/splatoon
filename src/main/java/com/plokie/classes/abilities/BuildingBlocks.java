package com.plokie.classes.abilities;

import com.plokie.customitems.CustomItem;
import net.minecraft.world.entity.player.Player;

public class BuildingBlocks extends Ability {
//    BuildingBlocks(AbilityManager.AbilityEnum enumVal, CustomItem customItem, int usageTypeFlags, float rechargeTimeSeconds, int maxCount, int initialCount) {
//        super(enumVal, customItem, usageTypeFlags, rechargeTimeSeconds, maxCount, initialCount);
//    }

    BuildingBlocks(AbilityManager.AbilityEnum enumVal) {
        super(enumVal);

        this.usageTypeFlags = UsageTypeFlags.Block.value;

        this.rechargeTime = -1;
        this.maxCount = 16;
        this.initialCount = 16;

        this.item = CustomItem.BuildingBlock;
    }

    @Override
    public void tick(Player player, int abilityIndex) {


        super.tick(player, abilityIndex);
    }
}
