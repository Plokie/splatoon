package com.plokie.management.gameflow;

import com.plokie.Splatoon;
import com.plokie.helpers.Affects;
import com.plokie.helpers.Effects;
import com.plokie.interfaces.IPlayerMixin;
import com.plokie.management.GameFlowManager;
import com.plokie.management.gamemodes.Gamemode;
import com.plokie.management.maps.GamemodeMap;
import net.minecraft.server.bossevents.CustomBossEvent;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class Results implements IGameState {
    @Override
    public void onStateEnter(Gamemode currentGamemode, GamemodeMap currentMap) {

        for(Player player : Splatoon.gameFlowManager.getTeamPlayers())
        {
            ((IPlayerMixin)player).setClass(null);
        }

        //PlaySong("");

        Vec3 resultsPos = currentMap.resultsPosition;
        Vec2 resultsRot = currentMap.resultsRotation;
        for(Player player : Splatoon.gameFlowManager.getGamersIncludingSpectators()) {
            player.teleportTo(resultsPos.x, resultsPos.y, resultsPos.z);
            player.forceSetRotation(resultsRot.x, resultsRot.y);
        }

        CustomBossEvent timerBossbar = Splatoon.gameFlowManager.getTimerBossbar();
        timerBossbar.setVisible(false);
        timerBossbar.removeAllPlayers();





    }

    @Override
    public GameFlowManager.GameState onStateTick(int timer, Gamemode currentGamemode, GamemodeMap currentMap) {
        Vec3 resultsPos = currentMap.resultsPosition;
        Vec2 resultsRot = currentMap.resultsRotation;

        for(Player player : Splatoon.gameFlowManager.getGamersIncludingSpectators())
        {
            Effects.givePotionEffect(player, MobEffects.INVISIBILITY, 1, 1, true);
            Effects.givePotionEffect(player, MobEffects.WEAKNESS, 1, 100, true);

            if(!player.getItemBySlot(EquipmentSlot.HEAD).is(Items.AIR)) {
                player.setItemSlot(EquipmentSlot.FEET, player.getItemBySlot(EquipmentSlot.HEAD));
                player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.AIR));
            }

            player.setNoGravity(true);

            player.snapTo(resultsPos.x, resultsPos.y, resultsPos.z, resultsRot.x, resultsRot.y);
            player.teleportTo(resultsPos.x, resultsPos.y, resultsPos.z);
        }

        return null;
    }

    @Override
    public void onStateExit(Gamemode currentGamemode, GamemodeMap currentMap) {

    }

    @Override
    public GameFlowManager.GameState getDefaultNextState() {
        return GameFlowManager.GameState.CELEBRATION;
    }

    @Override
    public int calculateDuration(Gamemode currentGamemode, GamemodeMap currentMap) {
        return 300;
    }

    @Override
    public String getStateMusic() { return "music.ending.win"; }
}
