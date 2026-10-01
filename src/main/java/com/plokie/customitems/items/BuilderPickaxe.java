package com.plokie.customitems.items;

import com.plokie.customitems.ICustomItem;
import com.plokie.helpers.Teams;
import com.plokie.interfaces.IPlayerTeamMixin;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AdventureModePredicate;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class BuilderPickaxe extends ICustomItem {
    public static void dataCallback(Player player, ItemStack item)
    {
        BlockPredicate blockPredicate = BlockPredicate.Builder.block().build();
        IPlayerTeamMixin playerTeam = Teams.getTeamMixinFromPlayer(player);
        if(playerTeam != null)
        {
            blockPredicate = BlockPredicate.Builder.block().of(BuiltInRegistries.BLOCK, playerTeam.getAuxBlock()).build();
        }

        AdventureModePredicate canBreak = new AdventureModePredicate(List.of(blockPredicate));
        item.set(
                DataComponents.CAN_BREAK,
                canBreak
        );
    }
}
