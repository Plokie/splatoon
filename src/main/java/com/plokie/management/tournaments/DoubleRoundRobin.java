package com.plokie.management.tournaments;

import com.plokie.Splatoon;
import net.minecraft.world.scores.PlayerTeam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class DoubleRoundRobin extends Tournament {
    public DoubleRoundRobin(String name)
    {
        super(name, Type.DoubleRoundRobin);
    }

    @Override
    public void start() {
        List<PlayerTeam> teams = getTeams();

        List<Matchup> matchups = new ArrayList<>();

        for(PlayerTeam team0 : teams) {
            for(PlayerTeam team1 : teams) {
                if(team0 == team1) continue;
                //Splatoon.LOGGER.info("adding team matchup {} vs {}", team0.getName(), team1.getName());
                matchups.add(new Matchup(team0, team1));
            }
        }

        Collections.shuffle(matchups);

        matchupQueue.addAll(matchups);
//
//        for(Matchup matchup : matchups) {
//            matchupQueue.add(matchup);
//        }
    }
}
