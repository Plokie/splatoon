package com.plokie.helpers;

import com.plokie.Splatoon;
import com.plokie.interfaces.IPlayerTeamMixin;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;

import java.util.UUID;

public class Teams {
    public static IPlayerTeamMixin getTeamMixinFromPlayerUUID(UUID playerUUID)
    {
        if(playerUUID == null) return null;

        for(ServerLevel level : Splatoon.SERVER.getAllLevels())
        {
            Player player = level.getPlayerByUUID(playerUUID);
            if(player != null)
            {
                return getTeamMixinFromPlayer(player);
            }
        }

        return null;
    }

    public static IPlayerTeamMixin getTeamMixinFromPlayer(Player player)
    {
        PlayerTeam team = player.level().getScoreboard().getPlayersTeam(player.getScoreboardName());
        if(team != null)
        {
            return (IPlayerTeamMixin)team;
        }

        return null;
    }
}
