package com.plokie.mixin;

import com.plokie.Splatoon;
import com.plokie.dialog.DialogBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(ServerCommonPacketListenerImpl.class)
public class ServerCommonPacketListenerImplMixin {
    @Unique
    private static Map<ResourceLocation, Integer> actionLastCalledTick = new HashMap<>();

    @Inject(method = "handleCustomClickAction", at=@At("HEAD"))
    void handleClickAction(ServerboundCustomClickActionPacket packet, CallbackInfo ci) {
        ResourceLocation actionId = packet.id();


        if ((Object) this instanceof net.minecraft.server.network.ServerGamePacketListenerImpl gameListener)
        {
            ServerPlayer player = gameListener.player;

            int currentTick = Splatoon.SERVER.getTickCount();

            if(actionLastCalledTick.containsKey(actionId))
            {
                int ticksSinceLastCall = currentTick - actionLastCalledTick.get(actionId);
                if(ticksSinceLastCall < 20) {
                    return;
                }
            }


            Splatoon.LOGGER.info("click action {} for {}", actionId.toString(), player.getName().getString());


            if(DialogBuilder.customActionCallbacks.containsKey(actionId))
            {
                Tag tag = packet.payload().orElse(new CompoundTag());

                actionLastCalledTick.put(actionId, Splatoon.SERVER.getTickCount());
                DialogBuilder.customActionCallbacks.get(actionId).accept(player, tag);
            }
        }
        else
        {
            Splatoon.LOGGER.warn("click action {} for unknown", actionId.toString());
        }

    }
}
