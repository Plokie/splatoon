package com.plokie.management.tournaments;

import net.minecraft.world.scores.PlayerTeam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RoundRobin extends Tournament {
    public RoundRobin(String name)
    {
        super(name, Type.RoundRobin);
    }

    @Override
    public void start() {
        List<PlayerTeam> teams = getTeams();

        List<Matchup> matchups = new ArrayList<>();

        for(PlayerTeam team0 : teams) {
            for (PlayerTeam team1 : teams) {
                if (team0 == team1) continue;
                if (matchupListContainsCommutativeMatchup(matchupQueue.stream().toList(), team0, team1)) continue;

                matchups.add(new Matchup(team0, team1));
            }
        }

        Collections.shuffle(matchups);

        matchupQueue.addAll(matchups);
    }
}
