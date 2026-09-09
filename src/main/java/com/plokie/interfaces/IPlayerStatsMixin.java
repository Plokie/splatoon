package com.plokie.interfaces;

import com.plokie.management.PlayerStats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Map;

public interface IPlayerStatsMixin {
    void add(PlayerStats stat, int delta);
    void forceAdd(PlayerStats stat, int delta);
    void forceAddNoMatch(PlayerStats stat, int delta);
    void addOnlyMatchStat(PlayerStats stat, int delta);
    void forceAddOnlyMatchStat(PlayerStats stat, int delta);
    int get(PlayerStats stat);
    int getMatchStat(PlayerStats stat);
    void resetMatchStats();

    void copyFrom(Player oldPlayer);
    Map<PlayerStats, Integer> getAllStats();
    Map<PlayerStats, Integer> getAllMatchStats();
}
