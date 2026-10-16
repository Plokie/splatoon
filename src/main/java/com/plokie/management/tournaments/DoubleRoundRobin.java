package com.plokie.management.tournaments;

import com.plokie.Splatoon;
import com.plokie.management.gamemodes.Gamemode;
import com.plokie.management.gamemodes.Gamemodes;
import net.minecraft.world.scores.PlayerTeam;

import java.util.*;

public class DoubleRoundRobin extends Tournament {
    public DoubleRoundRobin(String name)
    {
        super(name, Type.DoubleRoundRobin);
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

        for(int i=0; i < teams.size() - 1; i++) {
            List<PlayerTeam> sectionList = new ArrayList<>();
            sectionList.add(pivot);
            sectionList.addAll(roundTable);

            for(int teamAidx=0; teamAidx < teams.size() / 2; teamAidx++) {
                int teamBidx = (teams.size() - teamAidx) - 1;

                PlayerTeam teamA = sectionList.get(teamAidx);
                PlayerTeam teamB = sectionList.get(teamBidx);

                if(teamA == null || teamB == null) continue;

                Matchup match1 = new Matchup(teamA, teamB);
                Matchup match2 = new Matchup(teamA, teamB);

                match1.setSpecifiedGamemode(Gamemodes.TurfWar);
                match2.setSpecifiedGamemode(Gamemodes.Payload);

                matchupQueue.add( match1 );
                matchupQueue.add( match2 );
            }


            // take the back and add it to the front
            PlayerTeam back = roundTable.getLast();
            roundTable.removeLast();
            roundTable.addFirst(back);
        }


//        List<PlayerTeam> teams = getTeams();
//
//        List<Matchup> matchups = new ArrayList<>();
//        Deque<PlayerTeam> roundTable = new ArrayDeque<>(teams);
//
//        for(int i=0; i<teams.size(); i++) {
//
//
//
//
//            PlayerTeam swapTeam = roundTable.peekFirst();
//            roundTable.removeFirst();
//            roundTable.addLast(swapTeam);
//        }
//
//        matchupQueue.addAll(matchups);

//        List<Matchup> matchups = new ArrayList<>();
//
//        for(PlayerTeam team0 : teams) {
//            for(PlayerTeam team1 : teams) {
//                if(team0 == team1) continue;
//
//                Matchup matchup = new Matchup(team0, team1);
//
//                boolean alreadyFought = matchupListContainsCommutativeMatchup(matchups, team0, team1);
//                if(alreadyFought) {
//                    matchup.setSpecifiedGamemode(Gamemodes.Payload);
//                }
//                else
//                {
//                    matchup.setSpecifiedGamemode(Gamemodes.TurfWar);
//                }
//
//                //Splatoon.LOGGER.info("adding team matchup {} vs {}", team0.getName(), team1.getName());
//                matchups.add(matchup);
//            }
//        }
//
//        Collections.shuffle(matchups);
//
//        matchupQueue.addAll(matchups);
//
//        for(Matchup matchup : matchups) {
//            matchupQueue.add(matchup);
//        }
    }
}
