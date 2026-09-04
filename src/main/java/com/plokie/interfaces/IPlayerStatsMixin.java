package com.plokie.interfaces;

import com.plokie.management.PlayerStats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public interface IPlayerStatsMixin {
    void add(PlayerStats stat, int delta);
    void forceAdd(PlayerStats stat, int delta);
    void addOnlyMatchStat(PlayerStats stat, int delta);
    void forceAddOnlyMatchStat(PlayerStats stat, int delta);
    int get(PlayerStats stat);
    int getMatchStat(PlayerStats stat);
    void resetMatchStats();
}
