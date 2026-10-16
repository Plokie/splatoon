package com.plokie.management.tournaments;

import com.plokie.Splatoon;
import net.minecraft.world.scores.PlayerTeam;

import java.util.*;

public class RoundRobin extends Tournament {
    public RoundRobin(String name)
    {
        super(name, Type.RoundRobin);
    }

    @Override
    public void start() {
        List<PlayerTeam> teams = getTeams();

        // always have an even number of teams. null means a bye
        if(teams.size() % 2 == 1) {
            teams.add(null);
        }

        Deque<PlayerTeam> roundTable = new ArrayDeque<>(teams);

        PlayerTeam pivot = roundTable.peekFirst();
        roundTable.removeFirst();

        Splatoon.LOGGER.info("pivot={}", pivot.getName());

        for(int i=0; i < teams.size() - 1; i++) {
            List<PlayerTeam> sectionList = new ArrayList<>();
            sectionList.add(pivot);
            sectionList.addAll(roundTable);

            String debugString = "==";
            for(PlayerTeam playerTeam : sectionList) {
                debugString += (playerTeam!=null ? playerTeam.getName() : "null");
                debugString += ", ";
            }
            Splatoon.LOGGER.info(debugString);


            for(int teamAidx=0; teamAidx < teams.size() / 2; teamAidx++) {
                int teamBidx = (teams.size() - teamAidx) - 1;

                PlayerTeam teamA = sectionList.get(teamAidx);
                PlayerTeam teamB = sectionList.get(teamBidx);

                Splatoon.LOGGER.info("{} vs {}", (teamA!=null ? teamA.getName() : "null"), (teamB!=null ? teamB.getName() : "null") );

                if(teamA == null || teamB == null) continue;

                matchupQueue.add( new Matchup(teamA, teamB) );
            }


            // take the back and add it to the front
            PlayerTeam back = roundTable.getLast();
            roundTable.removeLast();
            roundTable.addFirst(back);
        }


//        List<Matchup> matchups = new ArrayList<>();

//        for(PlayerTeam team0 : teams) {
//            for (PlayerTeam team1 : teams) {
//                if (team0 == team1) continue;
//                if (matchupListContainsCommutativeMatchup(matchupQueue.stream().toList(), team0, team1)) continue;
//
//                matchups.add(new Matchup(team0, team1));
//            }
//        }

//        Collections.shuffle(matchups);

//        matchupQueue.addAll(matchups);
    }
}
