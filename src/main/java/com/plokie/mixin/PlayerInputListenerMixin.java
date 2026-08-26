package com.plokie.mixin;

import com.plokie.interfaces.IPlayerMixin;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
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
    void handlePlayerInput(ServerboundPlayerInputPacket serverboundPlayerInputPacket, CallbackInfo ci)
    {
        Input input = serverboundPlayerInputPacket.input();

        ((IPlayerMixin)(Player)player).setInputPacket(input);
    }
}
