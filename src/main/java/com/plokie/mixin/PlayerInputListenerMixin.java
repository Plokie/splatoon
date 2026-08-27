package com.plokie.mixin;

import com.plokie.interfaces.IPlayerMixin;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class PlayerInputListenerMixin {

    @Shadow public ServerPlayer player;


    @Inject(method="handlePlayerInput", at = @At("HEAD"))
    void handlePlayerInput(ServerboundPlayerInputPacket packet, CallbackInfo ci)
    {
//        ((ServerGamePacketListenerImpl)(Object)this);

        Input input = packet.input();

        ((IPlayerMixin)(Player)player).setInputPacket(input);
    }

    @Inject(method="handleAnimate", at = @At("HEAD"))
    void handleAnimate(ServerboundSwingPacket packet, CallbackInfo ci)
    {
        if(packet.getHand() == InteractionHand.MAIN_HAND)
        {
            //Splatoon.LOGGER.info("{} animate punch", player.getName());
            ((IPlayerMixin)(Player)player).setPunched();
        }
    }

//    @Inject(method="handlePlayerAction", at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/server/level/ServerPlayer;drop(Z)Z"
//    ))
//    void handlePlayerAction(ServerboundPlayerActionPacket packet, CallbackInfo ci)
//    {
//        ServerboundPlayerActionPacket.Action action = packet.getAction();
//
//        if(action == ServerboundPlayerActionPacket.Action.DROP_ITEM || action == ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS)
//        {
//
//
//            if(player != null)
//            {
//                player.containerMenu.sendAllDataToRemote();
//            }
//        }
//    }
}
