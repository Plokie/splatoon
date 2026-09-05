package com.plokie.mixin;

import com.mojang.serialization.Codec;
import com.plokie.Splatoon;
import com.plokie.interfaces.IPlayerMixin;
import com.plokie.interfaces.IPlayerStatsMixin;
import com.plokie.management.GameFlowManager;
import com.plokie.management.PlayerStats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(ServerPlayer.class)
public class PlayerStatsMixin implements IPlayerStatsMixin {
    ServerPlayer self = null;

    @Unique
    Map<PlayerStats, Integer> stats = new HashMap<>();

    @Unique
    Map<PlayerStats, Integer> matchStats = new HashMap<>();

    @Override
    public void add(PlayerStats stat, int delta)
    {
        if(((IPlayerMixin)self).getSplatoonClass() == null) return;
        if(self.getTags().contains("Skirmish")) return;

        forceAdd(stat, delta);
    }

    @Override
    public void forceAdd(PlayerStats stat, int delta)
    {
        if(stats.containsKey(stat)) {
            stats.put(stat, stats.get(stat) + delta);
        }
        else
        {
            stats.put(stat, delta);
        }

        PlayerStats.updateScoreboardReflection(self, stat);

        if(matchStats.containsKey(stat))
        {
            matchStats.put(stat, matchStats.get(stat) + delta);
        }
        else
        {
            matchStats.put(stat, delta);
        }
    }

    @Override
    public void forceAddNoMatch(PlayerStats stat, int delta)
    {
        if(stats.containsKey(stat)) {
            stats.put(stat, stats.get(stat) + delta);
        }
        else
        {
            stats.put(stat, delta);
        }

        PlayerStats.updateScoreboardReflection(self, stat);
    }

    @Override
    public void addOnlyMatchStat(PlayerStats stat, int delta)
    {
        if(((IPlayerMixin)self).getSplatoonClass() == null) return;
        if(self.getTags().contains("Skirmish")) return;

        forceAddOnlyMatchStat(stat, delta);
    }

    @Override
    public void forceAddOnlyMatchStat(PlayerStats stat, int delta)
    {
        if(matchStats.containsKey(stat))
        {
            matchStats.put(stat, matchStats.get(stat) + delta);
        }
        else
        {
            matchStats.put(stat, delta);
        }
    }

    @Override
    public int get(PlayerStats stat)
    {
        return stats.getOrDefault(stat, 0);
    }

    @Override
    public int getMatchStat(PlayerStats stat)
    {
        return matchStats.getOrDefault(stat, 0);
    }

    @Override
    public void resetMatchStats()
    {
        matchStats.clear();
    }

    @Inject(method="<init>", at=@At("TAIL"))
    void init(CallbackInfo ci)
    {
        self=(ServerPlayer) (Object)this;
    }

    @Inject(method="tick", at=@At("TAIL"))
    void tick(CallbackInfo ci)
    {
        if(self.tickCount % 20 == 1) {
            PlayerStats.get(self).forceAdd(PlayerStats.TOTAL_SECONDS_ONLINE, 1);
        }
    }

    @Inject(method="addAdditionalSaveData", at=@At("TAIL"))
    void onSaveData(ValueOutput valueOutput, CallbackInfo ci)
    {
        valueOutput.store("splatoon_player_stats", PlayerStats.MAP_CODEC, stats);
        valueOutput.store("splatoon_player_match_stats", PlayerStats.MAP_CODEC, matchStats);

    }

    @Inject(method="readAdditionalSaveData", at=@At("TAIL"))
    void onReadData(ValueInput valueInput, CallbackInfo ci)
    {
        valueInput.read("splatoon_player_stats", PlayerStats.MAP_CODEC).ifPresent(map->{
            stats.clear();
            stats.putAll(map);
        });

        if(self != null) {
            PlayerStats.portAnyOldScores(self);
        }
        else {
            Splatoon.LOGGER.error("readAdditionalSaveData is called before <init>");
        }

        for(PlayerStats stat : PlayerStats.values())
        {
            PlayerStats.updateScoreboardReflection(self, stat);
        }

        //self.getScoreboard().getObjective()

        // only reload match stats if they load in during a game (mid-game disconnect)
        if(Splatoon.gameFlowManager == null) return;
        if(Splatoon.gameFlowManager.getCurrentGamemode() == null) return;
        if(Splatoon.gameFlowManager.getCurrentGameState() == null) return;
        if(Splatoon.gameFlowManager.getCurrentGameState() == GameFlowManager.GameState.NONE) return;

        valueInput.read("splatoon_player_match_stats", PlayerStats.MAP_CODEC).ifPresent(map->{
            matchStats.clear();
            matchStats.putAll(map);
        });
    }
}
