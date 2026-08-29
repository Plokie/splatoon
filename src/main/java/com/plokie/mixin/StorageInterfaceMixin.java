package com.plokie.mixin;

import com.plokie.Splatoon;
import com.plokie.management.TeamSelector;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.scores.PlayerTeam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class StorageInterfaceMixin {

    @Shadow
    public abstract Slot getSlot(int i);

    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    void clicked(int slotId, int button, ClickType clickType, Player _player, CallbackInfo ci)
    {
        if(!(_player instanceof ServerPlayer player)) return;
        AbstractContainerMenu _menu = (AbstractContainerMenu)(Object)this;
        if(!(_menu instanceof ChestMenu menu)) return;

        //if(slotId < 0 || slotId >= menu.getContainer().getContainerSize()) return;

        if(!(menu.getContainer() instanceof BarrelBlockEntity barrel)) return;

        Slot clickedSlot = this.getSlot(slotId);
        ItemStack clickedItem = clickedSlot.getItem();

        TeamSelector teamSelector = Splatoon.gameFlowManager.getTeamSelectorAt(barrel.getBlockPos());

        if(teamSelector != null) {
            teamSelector.callback(player, clickedItem);
            ci.cancel();
        }

        //player.getScoreboard().
        //if(clickedItem.getItemName().getString().equals())





    }
}
