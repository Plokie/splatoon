package com.plokie.customitems.items;

import com.plokie.customitems.ICustomItem;
import com.plokie.helpers.Teams;
import com.plokie.interfaces.IPlayerTeamMixin;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.scores.PlayerTeam;

public class SupportAxe extends ICustomItem {
    @Override
    public void onStartHeld(Player player) {



    }

    void giveShield(Player player)
    {
        IPlayerTeamMixin team = Teams.getTeamMixinFromPlayer(player);
        if(team == null) return;

        ItemStack shield = new ItemStack(Items.SHIELD);

        PlayerTeam playerTeam = (PlayerTeam)team;
        String name = playerTeam.getName().toUpperCase();
        try {
            DyeColor col = DyeColor.valueOf(name);
            shield.set(DataComponents.BASE_COLOR, col);
        }
        catch(Exception ignored) {}

        player.setItemSlot(EquipmentSlot.OFFHAND, shield);
    }

    @Override
    public void whileHeld(Player player) {
        if(!player.getItemBySlot(EquipmentSlot.OFFHAND).is(Items.SHIELD))
        {
            giveShield(player);
        }
    }

    @Override
    public void onEndHeld(Player player) {
        player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
    }
}
