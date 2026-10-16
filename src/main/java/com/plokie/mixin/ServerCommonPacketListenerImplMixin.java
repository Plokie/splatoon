package com.plokie.mixin;

import com.plokie.Splatoon;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerCommonPacketListenerImpl.class)
public class ServerCommonPacketListenerImplMixin {

    @Inject(method = "handleCustomClickAction", at=@At("HEAD"))
    void handleClickAction(ServerboundCustomClickActionPacket packet, CallbackInfo ci) {
        ResourceLocation actionId = packet.id();




        if ((Object) this instanceof net.minecraft.server.network.ServerGamePacketListenerImpl gameListener)
        {
            ServerPlayer player = gameListener.player;

            Splatoon.LOGGER.info("click action {} for {}", actionId.toString(), player.getName().getString());
        }
        else
        {
            Splatoon.LOGGER.info("click action {} for unknown", actionId.toString());
        }

    }
}
