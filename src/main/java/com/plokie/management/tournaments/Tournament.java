package com.plokie.management.tournaments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.plokie.Splatoon;
import com.plokie.management.GameFlowManager;
import com.plokie.management.gamemodes.Gamemode;
import com.plokie.management.gamemodes.Gamemodes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.scores.PlayerTeam;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;

public abstract class Tournament {
    public static class Matchup {
        public Matchup(PlayerTeam team0, PlayerTeam team1) {
            this.team0 = team0;
            this.team1 = team1;
        }
        public final PlayerTeam team0;
        public final PlayerTeam team1;

        Gamemodes specifiedGamemode = null;
        PlayerTeam winner = null;

        public static final Codec<Matchup> CODEC = RecordCodecBuilder.create(instance->
                instance.group(
                        Tournament.PLAYERTEAM_CODEC.fieldOf("team0").forGetter(Matchup::getTeam0),
                        Tournament.PLAYERTEAM_CODEC.fieldOf("team1").forGetter(Matchup::getTeam1),
                        Codec.STRING.optionalFieldOf("winner", "").forGetter((inst)-> inst.getWinner()==null?"":inst.getWinner().getName() ),
                        Codec.STRING.optionalFieldOf("gamemode", "").forGetter(inst-> inst.getSpecifiedGamemode()==null?"":inst.getSpecifiedGamemode().getName())
                ).apply(instance, (team0, team1, winner, gamemode)->{
                    Matchup matchup = new Matchup(team0, team1);
                    if(!winner.isEmpty()) {
                        matchup.setWinner(Splatoon.SERVER.getScoreboard().getPlayerTeam(winner));
                    }
                    if(!gamemode.isEmpty()) {
                        try {
                            matchup.setSpecifiedGamemode(Gamemodes.valueOf(gamemode));
                        }
                        catch(IllegalArgumentException ignored) {}
//                        matchup.setSpecifiedGamemode(gamemode);
                    }
                    return matchup;
                })
        );
        public static final Codec<List<Matchup>> LIST_CODEC = Codec.list(Matchup.CODEC);

        public PlayerTeam getWinner() { return winner; }
        public Gamemodes getSpecifiedGamemode() { return specifiedGamemode; }

        public PlayerTeam getTeam0() { return team0; }
        public PlayerTeam getTeam1() { return team1; }

        public void setSpecifiedGamemode(Gamemodes gamemode) { specifiedGamemode = gamemode; }
        public void setWinner(PlayerTeam winner) { this.winner = winner; }
    }

    public enum Type implements StringRepresentable {
        RoundRobin,
        DoubleRoundRobin;

        @Override
        public @NotNull String getSerializedName() {
            return this.toString().toLowerCase();
        }
    }

    public Tournament(String name, Tournament.Type type) {
        this.name = name;
        this.type = type;
    }

    final String name;
    final Tournament.Type type;
    List<PlayerTeam> teams = new ArrayList<>();
    Map<PlayerTeam, Integer> scores = new HashMap<>();
    boolean isOngoing = false;
    boolean isComplete = false;

    List<Matchup> previousMatchups = new ArrayList<>();
    protected Queue<Matchup> matchupQueue = new ArrayDeque<>();

    public static final Codec<Tournament.Type> TYPE_CODEC = StringRepresentable.fromEnum(Tournament.Type::values);

    public static final Codec<PlayerTeam> PLAYERTEAM_CODEC = Codec.stringResolver(
            team->team==null?"":team.getName(),
            teamName->(teamName.equals("")?null:Splatoon.SERVER.getScoreboard().getPlayerTeam(teamName))
    );
    public static final Codec<Map<PlayerTeam, Integer>> SCORES_CODEC = Codec.unboundedMap(PLAYERTEAM_CODEC, Codec.INT);



    public static final Codec<Tournament> CODEC = RecordCodecBuilder.create(instance->
            instance.group(
                    Codec.STRING.fieldOf("name").forGetter(Tournament::getName),
                    TYPE_CODEC.fieldOf("type").forGetter(Tournament::getType),
                    Codec.BOOL.fieldOf("isOngoing").forGetter(Tournament::isOngoing),
                    Codec.BOOL.fieldOf("isComplete").forGetter(Tournament::isComplete),
                    SCORES_CODEC.fieldOf("scores").forGetter(Tournament::getScores),
                    Matchup.LIST_CODEC.fieldOf("previousMatchups").forGetter(Tournament::getPreviousMatchups),
                    Matchup.LIST_CODEC.fieldOf("matchupQueue").forGetter(tournament->tournament.getMatchupQueue().stream().toList())
            ).apply(instance, (name, type, isOngoing, isComplete, scores, previousMatchups, matchupQueue)->{
                Tournament tournament;

                switch(type) {
                    case RoundRobin -> { tournament = new RoundRobin(name); }
                    case DoubleRoundRobin -> { tournament = new DoubleRoundRobin(name); }
                    default -> { throw new IllegalArgumentException("Loading invalid tournament type: " + type); }
                }


                tournament.isOngoing = isOngoing;
                tournament.isComplete = isComplete;
                tournament.scores.putAll(scores);

                Splatoon.LOGGER.info("\t{} isOngoing", isOngoing);
                Splatoon.LOGGER.info("\t{} isCompelte", isComplete);
                Splatoon.LOGGER.info("\t{} scores", scores.size());

                for(var entry : tournament.scores.entrySet()) {
                    tournament.teams.add(entry.getKey());
                }

                Splatoon.LOGGER.info("\t{} previous matchups", previousMatchups.size());
                Splatoon.LOGGER.info("\t{} queued matchups", matchupQueue.size());

                tournament.previousMatchups.addAll(previousMatchups);
                tournament.matchupQueue.addAll(matchupQueue);

                return tournament;
            })
    );

    public static boolean matchupListContainsCommutativeMatchup(List<Matchup> matchups, PlayerTeam team0, PlayerTeam team1)
    {
        for(Matchup matchup : matchups)
        {
            if( (matchup.getTeam0() == team0 || matchup.getTeam0() == team1) && (matchup.getTeam1() == team0 || matchup.getTeam1() == team1)   )
            {
                return true;
            }
        }

        return false;
    }

    public String getName() {
        return name;
    }

    public Tournament.Type getType() {
        return type;
    }

    public List<PlayerTeam> getTeams() {
        return teams;
    }

    public boolean isOngoing() {
        return isOngoing;
    }

    public void setOngoing(boolean value) {
        isOngoing = value;
    }

    public void setComplete(boolean value) {
        isComplete = value;
    }

    public boolean isComplete() { return isComplete; }

    Map<PlayerTeam, Integer> getScores() {
        return scores;
    }

    public List<PlayerTeam> getWinningTeams()
    {
        int greatestScore = 0;
        List<PlayerTeam> greatestTeams = new ArrayList<>();

        for(var entry : scores.entrySet()) {
            int score = entry.getValue();
            PlayerTeam team = entry.getKey();
            if(score > greatestScore) {
                greatestScore = score;
                greatestTeams.clear();
                greatestTeams.add(team);
            }
            else if(score == greatestScore){
                greatestTeams.add(team);
            }
        }

        return greatestTeams;
    }

    public boolean addTeam(String teamId) {
        PlayerTeam team = Splatoon.SERVER.getScoreboard().getPlayerTeam(teamId);
        if(team == null) {
            Splatoon.LOGGER.warn("Tried to add team {}, but this is not a valid team id", teamId);
            return false;
        }

        addTeam(team);

        return true;
    }

    public void addTeam(PlayerTeam team)
    {
        teams.add(team);
        scores.put(team, 0);
    }

    public void removeTeam(PlayerTeam team)
    {
        teams.remove(team);
        scores.remove(team);
    }

    public int getTeamScore(PlayerTeam team) {
        if(!scores.containsKey(team)) return -1;

        return scores.get(team);
    }

    public boolean setTeamScore(PlayerTeam team, int score)
    {
        if(!teams.contains(team)) return false;

        scores.put(team, score);
        return true;
    }

    public Matchup getCurrentMatchup()
    {
        return matchupQueue.peek();
    }

    public Queue<Matchup> getMatchupQueue() {
        Splatoon.LOGGER.info("get size {} matchup queue", matchupQueue.size());
        return matchupQueue;
    }

    public List<Matchup> getPreviousMatchups()
    {
        Splatoon.LOGGER.info("get {} previous matchups", previousMatchups.size());
        return previousMatchups;
    }

    public List<Matchup> getAllMatchups() {
        List<Matchup> ret = new ArrayList<>();
        ret.addAll(previousMatchups);
        ret.addAll(matchupQueue);
        return ret;
    }

    public boolean declareWinner(PlayerTeam winningTeam)
    {
        Matchup currentMatchup = getCurrentMatchup();
        if(currentMatchup == null) {
            Splatoon.LOGGER.warn("Attempted to declare winner of tournmanent, but there is no current matchup (tournament might have not started yet, or has ended)");
            return false;
        }

        if(currentMatchup.team1 != winningTeam && currentMatchup.team0 != winningTeam) {
            Splatoon.LOGGER.warn("Attempted to declare winner of tournmanent, but the current matchup does not contain the declared winning team");
            return false;
        }

        Matchup matchup = matchupQueue.peek();
        if(matchup != null) {
            matchup.setWinner(winningTeam);

            previousMatchups.add(matchup);
            matchupQueue.remove();
        }

        scores.put(winningTeam, scores.getOrDefault(winningTeam, 0) + 1);

        MutableComponent message = Component.literal("Tournament matchup results:\n");
        message = message.append(winningTeam.getFormattedDisplayName());
        message = message.append(" wins!\n\nStandings:\n");

        message = message.append(getScoresMessage());
        message = message.append("\n\n");

        Matchup nextMatchup = matchupQueue.peek();
        if(nextMatchup != null) {
            message = message.append("Up next:\n");
            message = message.append(nextMatchup.team0.getFormattedDisplayName());
            message = message.append(" vs ");
            message = message.append(nextMatchup.team1.getFormattedDisplayName());
        }

        Splatoon.LOGGER.info(message.getString());
        for(ServerPlayer player : Splatoon.SERVER.getPlayerList().getPlayers())
        {
            player.sendSystemMessage(message);
        }

        if(matchupQueue.isEmpty()) {
            onComplete();
        }

        return true;
    }

    public Component getScoresMessage()
    {
        MutableComponent message = Component.literal("");

        for(var entry : scores.entrySet()) {
            String score = String.valueOf(entry.getValue());
            message = message.append(entry.getKey().getFormattedDisplayName());
            message = message.append(": " + score + "\n");
        }

        return message;
    }

    public Component getMatchupsMessage()
    {
        MutableComponent message = Component.literal("");

        Function<Matchup, MutableComponent> addMatchup = matchup -> {
            PlayerTeam team0 = matchup.team0;
            PlayerTeam team1 = matchup.team1;

            MutableComponent thisMessage = Component.literal("");
            thisMessage = thisMessage.append(team0.getFormattedDisplayName());
            thisMessage = thisMessage.append(" vs ");
            thisMessage = thisMessage.append(team1.getFormattedDisplayName());
            return thisMessage;
        };

        for(Matchup matchup : previousMatchups) {
            message = message.append(addMatchup.apply(matchup));

            if(matchup.getWinner() != null) {
                message = message.append(" : ");
                message = message.append(matchup.getWinner().getFormattedDisplayName());
                message = message.append(" wins!");
            }

            message = message.append("\n");
        }

        for(Matchup matchup : matchupQueue) {
            message = message.append(addMatchup.apply(matchup));

            if(matchupQueue.peek() == matchup) {
                if(Splatoon.gameFlowManager.getCurrentGameState() == GameFlowManager.GameState.NONE) {
                    message = message.append(" <- Upcoming");
                }
                else
                {
                    message = message.append(" <- Current");
                }
            }

            message = message.append("\n");
        }

        return message;
    }

    public abstract void start();

    void onComplete()
    {
        List<PlayerTeam> winningTeams = getWinningTeams();
        if(winningTeams.size() > 1) {
            MutableComponent message = Component.literal("Tiebreakers!\n");
            message = message.append(getScoresMessage());
            message = message.append("Multiple teams are tied! Adding more matchups to the queue...\n\n");

            for(PlayerTeam team0 : winningTeams) {
                for(PlayerTeam team1 : winningTeams) {
                    if(team0 == team1) continue;
                    if(matchupListContainsCommutativeMatchup(matchupQueue.stream().toList(), team0, team1)) continue;

                    Matchup matchup = new Matchup(team0, team1);
                    matchupQueue.add(matchup);

                    message = message.append(team0.getFormattedDisplayName());
                    message = message.append(" vs ");
                    message = message.append(team1.getFormattedDisplayName());
                    message = message.append("\n");
                }
            }

            Splatoon.LOGGER.info(message.getString());
            for(ServerPlayer player : Splatoon.SERVER.getPlayerList().getPlayers())
            {
                player.sendSystemMessage(message);
            }

        }
        else if(winningTeams.size() == 1){
            MutableComponent message = Component.literal("The tournament is now over!\n");
            message = message.append(getScoresMessage());
            message = message.append("\n\nWhich means:\n\n");
            message = message.append(winningTeams.getFirst().getFormattedDisplayName());
            message = message.append(" has won the tournament!");

            Splatoon.LOGGER.info(message.getString());
            for(ServerPlayer player : Splatoon.SERVER.getPlayerList().getPlayers())
            {
                player.sendSystemMessage(message);
            }

            setComplete(true);
            setOngoing(false);
        }

    }

}
