package com.plokie.management;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.plokie.Splatoon;
import com.plokie.commands.Command;
import com.plokie.helpers.CommandBuilder;
import com.plokie.helpers.ScheduleEvent;
import com.plokie.management.tournaments.DoubleRoundRobin;
import com.plokie.management.tournaments.RoundRobin;
import com.plokie.management.tournaments.Tournament;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.scores.PlayerTeam;

import java.util.*;

public class TournamentManager {
    public static class SaveData extends SavedData {
        public SaveData()
        {

        }
        public SaveData(List<Tournament> tournaments)
        {
            this.tournaments.addAll(tournaments);
        }

        public static final Codec<SaveData> CODEC = RecordCodecBuilder.create(instance->
            instance.group(
                    Codec.list(Tournament.CODEC).fieldOf("tournaments").forGetter(SaveData::getTournaments)
            ).apply(instance, (tournaments)->{
                return new SaveData(tournaments);
            })
        );

        public static final SavedDataType<SaveData> TYPE = new SavedDataType<>(
                "splatoon_tournament_data",
                SaveData::new,
                CODEC,
                DataFixTypes.SAVED_DATA_SCOREBOARD
        );

        public List<Tournament> tournaments = new ArrayList<>();

        public List<Tournament> getTournaments() { return tournaments; }
    }

    public static TournamentManager Instance;

    SaveData data = null;

    public List<Tournament> getAllTournaments()
    {
        return data.getTournaments();
    }

    public Tournament getTournamentByName(String tournamentName)
    {
        for(Tournament checkTournament : getAllTournaments()) {
            if(checkTournament.getName().equals(tournamentName)) {
                return checkTournament;
            }
        }
        return null;
    }

    List<String> getAllTournamentNames()
    {
        List<String> ret = new ArrayList<>();
        for(Tournament tournament : getAllTournaments()) {
            ret.add(tournament.getName());
        }
        return ret;
    }
    List<String> getOngoingTournamentNames()
    {
        List<String> ret = new ArrayList<>();
        for(Tournament tournament : getAllTournaments()) {
            if(tournament.isOngoing())
            {
                ret.add(tournament.getName());
            }
        }
        return ret;
    }
    public List<Tournament> getOngoingTournaments()
    {
        List<Tournament> ret = new ArrayList<>();
        for(Tournament tournament : getAllTournaments()) {
            if(tournament.isOngoing())
            {
                ret.add(tournament);
            }
        }
        return ret;
    }
    List<String> getTeamNames() {
        List<String> ret = new ArrayList<>();
        for(PlayerTeam team : Splatoon.SERVER.getScoreboard().getPlayerTeams()) {
            ret.add(team.getName());
        }
        return ret;
    }

    public void save()
    {
        if(data == null) return;

        Splatoon.SERVER.overworld().getDataStorage().set(SaveData.TYPE, data);
    }

    public void declareWinnerOfMatchup(PlayerTeam team)
    {
        for(Tournament tournament : getOngoingTournaments()) {
            tournament.declareWinner(team);
        }
    }

    public boolean isAnyOngoing() {
        return !getOngoingTournaments().isEmpty();
    }

    public TournamentManager()
    {
        Instance = this;

        ServerLifecycleEvents.SERVER_STARTED.register(server->{
            SaveData saveData = server.overworld().getDataStorage().get(SaveData.TYPE);
            if(saveData == null) {
//                saveData = server.overworld().getDataStorage().computeIfAbsent(SaveData.TYPE);
                this.data = new SaveData();
            }
            else{
                this.data = saveData;
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server->{
            save();
        });

        ServerLifecycleEvents.AFTER_SAVE.register((server, a, b)->{
            save();
        });

        CommandBuilder.command("tournament").subcommand("create").argumentString("tournament_name").argumentEnum("type", Tournament.Type::values).executes(ctx->{
            try {
                String tournamentName = ctx.getArgumentString("tournament_name");
                Tournament.Type type = ctx.getArgumentEnum("type", Tournament.Type.class);

                Tournament tournament;

                switch(type) {
                    case Tournament.Type.RoundRobin -> tournament = new RoundRobin(tournamentName);
                    case Tournament.Type.DoubleRoundRobin -> tournament = new DoubleRoundRobin(tournamentName);
                    default -> {
                        return "! Unhandled tournament type";
                    }
                }

                data.tournaments.add(tournament);

//                allTournaments.add(tournament);

                return "Created tournament " + tournamentName + ", use '/tournament modify "+tournamentName+" teams add' to add teams, then '/tournament start' to start";
            }
            catch (IllegalArgumentException e) {
                return "! Unrecognised tournament type";
            }


        }).register();

        CommandBuilder.command("tournament").subcommand("start").argumentString("tournament_name", this::getAllTournamentNames).executes(ctx->{
            String tournamentName = ctx.getArgumentString("tournament_name");
            Tournament tournament = getTournamentByName(tournamentName);
            if(tournament == null) {
                return "! Could not find tournament of name: " + tournamentName;
            }

            tournament.getPreviousMatchups().clear();
            tournament.getMatchupQueue().clear();

            try {
                tournament.start();
            }
            catch(Exception e) {
                Splatoon.LOGGER.info(e.getMessage());
                Arrays.stream(e.getStackTrace()).map(StackTraceElement::toString).forEach(Splatoon.LOGGER::info);
                throw e;
            }

            tournament.setOngoing(true);
            //ongoingTournaments.add(tournament);

            return "Started tournament " + tournamentName;
        }).register();

        CommandBuilder.command("tournament")
                .subcommand("modify")
                .argumentString("tournament_name", this::getAllTournamentNames)
                .subcommand("resume").executes(ctx->{
            String tournamentName = ctx.getArgumentString("tournament_name");
            Tournament tournament = getTournamentByName(tournamentName);
            if(tournament == null) {
                return "! Could not find tournament of name: " + tournamentName;
            }

            tournament.setOngoing(true);

            return "Started tournament " + tournamentName;
        }).register();

        CommandBuilder.command("tournament")
                .subcommand("modify")
                .argumentString("tournament_name", this::getAllTournamentNames)
                .subcommand("shuffle_matchups").executes(ctx->{
                    String tournamentName = ctx.getArgumentString("tournament_name");
                    Tournament tournament = getTournamentByName(tournamentName);
                    if(tournament == null) {
                        return "! Could not find tournament of name: " + tournamentName;
                    }

                    List<Tournament.Matchup> matchups = new ArrayList<>(tournament.getMatchupQueue().stream().toList());
                    Collections.shuffle(matchups);
                    tournament.getMatchupQueue().clear();
                    tournament.getMatchupQueue().addAll(matchups);


                    return "Started tournament " + tournamentName;
                }).register();

        CommandBuilder.command("tournament").subcommand("view").subcommand("scores").executes(ctx->{
            List<Tournament> ongoingTournaments = getOngoingTournaments();
            if(ongoingTournaments.isEmpty()) return "! There are no ongoing tournaments";

            Tournament tournament = ongoingTournaments.getFirst();

            ScheduleEvent.schedule(1, server->{
                ctx.getStack().getSource().sendSystemMessage(tournament.getScoresMessage());
            });


            return "Returning scores...";
        }).permission(stack->true).register();

        CommandBuilder.command("tournament").subcommand("view").subcommand("matchups").executes(ctx->{
            List<Tournament> ongoingTournaments = getOngoingTournaments();
            if(ongoingTournaments.isEmpty()) return "! There are no ongoing tournaments";

            Tournament tournament = ongoingTournaments.getFirst();

            ScheduleEvent.schedule(1, server->{
                ctx.getStack().getSource().sendSystemMessage(tournament.getMatchupsMessage());
            });

            return "Returning matchups...";
        }).permission(stack->true).register();

        CommandBuilder.command("tournament")
                .subcommand("modify")
                .argumentString("tournament_name", this::getOngoingTournamentNames)
                .subcommand("pause").executes(ctx->{
            List<Tournament> ongoingTournaments = getOngoingTournaments();
            if(ongoingTournaments.isEmpty()) return "! There are no ongoing tournaments";

            String tournamentName = ctx.getArgumentString("tournament_name");
            Tournament tournament = getTournamentByName(tournamentName);
            if(tournament == null) {
                return "! Invalid tournament name";
            }

            if(!ongoingTournaments.contains(tournament)) {
                return "! Tournament is not an ongoing tournament";
            }

            tournament.setOngoing(false);

            return "Removed " + tournamentName + " as an ongoing tournament";
        }).register();

        CommandBuilder.command("tournament")
                .subcommand("modify")
                .argumentString("tournament_name", this::getOngoingTournamentNames)
                .subcommand("complete").executes(ctx->{
            List<Tournament> ongoingTournaments = getOngoingTournaments();
            if(ongoingTournaments.isEmpty()) return "! There are no ongoing tournaments";

            String tournamentName = ctx.getArgumentString("tournament_name");
            Tournament tournament = getTournamentByName(tournamentName);
            if(tournament == null) {
                return "! Invalid tournament name";
            }

            if(!ongoingTournaments.contains(tournament)) {
                return "! Tournament is not an ongoing tournament";
            }

            tournament.setOngoing(false);
            tournament.setComplete(true);

            return "Removed " + tournamentName + " as an ongoing tournament";
        }).register();

        CommandBuilder.command("tournament")
                .subcommand("modify")
                .argumentString("tournament_name", this::getAllTournamentNames)
                .subcommand("teams")
                .subcommand("add")
                .argumentString("team_id", this::getTeamNames)
                .executes(
        ctx->{
            String tournamentName = ctx.getArgumentString("tournament_name");
            Tournament tournament = getTournamentByName(tournamentName);
            if(tournament == null) {
                return "! Could not find tournament of name: " + tournamentName;
            }


            String teamId = ctx.getArgumentString("team_id");

            PlayerTeam team = Splatoon.SERVER.getScoreboard().getPlayerTeam(teamId);
            if(team == null) {
                return "! team_id is not a valid team id. It must be an existing team (of /team)";
            }

            tournament.addTeam(team);

            return "Added team " + teamId + " to tournament " + tournamentName;
        }).register();

        CommandBuilder.command("tournament")
                .subcommand("modify")
                .argumentString("tournament_name", this::getAllTournamentNames)
                .subcommand("teams")
                .subcommand("remove")
                .argumentString("team_id", this::getTeamNames)
                .executes(
    ctx->{
            String tournamentName = ctx.getArgumentString("tournament_name");
            Tournament tournament = getTournamentByName(tournamentName);
            if(tournament == null) {
                return "! Could not find tournament of name: " + tournamentName;
            }


            String teamId = ctx.getArgumentString("team_id");

            PlayerTeam team = Splatoon.SERVER.getScoreboard().getPlayerTeam(teamId);
            if(team == null) {
                return "! team_id is not a valid team id. It must be an existing team (of /team)";
            }

            if(tournament.getTeams().contains(team))
            {
                tournament.removeTeam(team);
                return "Removed team " + teamId + " from tournament " + tournamentName;
            }
            else
            {
                return "Nothing changed, as this tournament did not contain this team";
            }
        }).register();

        CommandBuilder.command("tournament")
                .subcommand("modify")
                .argumentString("tournament_name", this::getAllTournamentNames)
                .subcommand("scores")
                .argumentString("team_id", this::getTeamNames)
                .subcommand("add")
                .argumentInteger("delta")
                .executes(ctx->{
            String tournamentName = ctx.getArgumentString("tournament_name");
            Tournament tournament = getTournamentByName(tournamentName);
            if(tournament == null) {
                return "! Could not find tournament of name: " + tournamentName;
            }

            String teamId = ctx.getArgumentString("team_id");

            PlayerTeam team = Splatoon.SERVER.getScoreboard().getPlayerTeam(teamId);
            if(team == null) {
                return "! team_id is not a valid team id. It must be an existing team (of /team)";
            }

            int currentScore = tournament.getTeamScore(team);
            int delta = ctx.getArgumentInteger("delta");
            tournament.setTeamScore(team, currentScore + delta);

            return "Changed team " + teamId +" score by " + delta + ". Now " + tournament.getTeamScore(team);
        }).register();

        CommandBuilder.command("tournament")
                .subcommand("modify")
                .argumentString("tournament_name", this::getAllTournamentNames)
                .subcommand("scores")
                .argumentString("team_id", this::getTeamNames)
                .subcommand("set")
                .argumentInteger("value")
                .executes(ctx->{
            String tournamentName = ctx.getArgumentString("tournament_name");
            Tournament tournament = getTournamentByName(tournamentName);
            if(tournament == null) {
                return "! Could not find tournament of name: " + tournamentName;
            }

            String teamId = ctx.getArgumentString("team_id");

            PlayerTeam team = Splatoon.SERVER.getScoreboard().getPlayerTeam(teamId);
            if(team == null) {
                return "! team_id is not a valid team id. It must be an existing team (of /team)";
            }

            int value = ctx.getArgumentInteger("value");
            tournament.setTeamScore(team, value);

            return "Set team " + teamId +" score to " + value;
        }).register();

        CommandBuilder.command("tournament")
                .subcommand("modify")
                .argumentString("tournament_name", this::getAllTournamentNames)
                .subcommand("result")
                .argumentString("team0", this::getTeamNames)
                .argumentString("team1", this::getTeamNames)
                .argumentString("winning_team", ()->{var ret = getTeamNames(); ret.add("none"); return ret;})
                .argumentInteger("matchup_index")
                .executes(ctx->{
            String tournamentName = ctx.getArgumentString("tournament_name");
            Tournament tournament = getTournamentByName(tournamentName);
            if(tournament == null) {
                return "! Could not find tournament of name: " + tournamentName;
            }

            String team0Id = ctx.getArgumentString("team0");
            PlayerTeam team0 = Splatoon.SERVER.getScoreboard().getPlayerTeam(team0Id);
            if(team0 == null) {
                return "! team0 is not a valid team id. It must be an existing team (of /team)";
            }

            String team1Id = ctx.getArgumentString("team1");
            PlayerTeam team1 = Splatoon.SERVER.getScoreboard().getPlayerTeam(team1Id);
            if(team1 == null) {
                return "! team1 is not a valid team id. It must be an existing team (of /team)";
            }

            List<Tournament.Matchup> matchups = new ArrayList<>();
            for(var checkMatchup : tournament.getAllMatchups())
            {
                if(checkMatchup.team0 == team0 && checkMatchup.team1 == team1) {
                    matchups.add(checkMatchup);
                }
            }
            if(matchups.isEmpty()) {
                return "! Could not find a matchup of this combination of team0 and team1";
            }

            int matchupIndex = ctx.getArgumentInteger("matchup_index");
            if(matchupIndex >= matchups.size()) {
                return "! Invalid matchup_index. There are " + matchups.size() + " matchups matching this description. matchup_index is the index of the matchup between the two teams to modify (e.g. index 1 is the 2nd time these two teams fought (as team0 team1 respecively)";
            }

            Tournament.Matchup matchup = matchups.get(matchupIndex);

            PlayerTeam oldWinningTeam = matchup.getWinner();
            if(oldWinningTeam != null) {
                tournament.setTeamScore(oldWinningTeam, tournament.getTeamScore(oldWinningTeam) - 1);
            }

            String winningTeamId = ctx.getArgumentString("winning_team");
            if(!winningTeamId.equals("none"))
            {
                PlayerTeam winningTeam = Splatoon.SERVER.getScoreboard().getPlayerTeam(team1Id);
                if(winningTeam == null) {
                    return "! winning_team is not a valid team id. It must be an existing team (of /team)";
                }

                if(winningTeam != team0 && winningTeam != team1) {
                    return "! winning_team must be either team0 or team1";
                }

                matchup.setWinner(winningTeam);

                tournament.setTeamScore(winningTeam, tournament.getTeamScore(winningTeam) + 1);

                return "Set winning team of matchup " + matchupIndex + " of " + team0Id + " vs " + team1Id + " to " + winningTeamId;
            }
            else
            {
                matchups.get(matchupIndex).setWinner(null);
                return "Removed a winning team from matchup " + matchupIndex + " of " + team0Id + " vs " + team1Id;
            }

        }).register();

        CommandBuilder.command("tournament").subcommand("modify").subcommand("force_delcare_matchup_winner").argumentString("team_id", this::getTeamNames).executes(ctx->{
            List<Tournament> ongoingTournaments = getOngoingTournaments();
            if(ongoingTournaments.isEmpty()) return "! There are no ongoing tournaments";

            String teamId = ctx.getArgumentString("team_id");
            PlayerTeam team = Splatoon.SERVER.getScoreboard().getPlayerTeam(teamId);
            if(team == null) {
                return "! team_id is not a valid team id. It must be an existing team (of /team)";
            }

            Tournament tournament = ongoingTournaments.getFirst();
            Tournament.Matchup currentMatchup = tournament.getCurrentMatchup();
            if(currentMatchup.getTeam0() != team && currentMatchup.getTeam1() != team)
            {
                return "! team_id is not a team in the current matchup";
            }

            tournament.declareWinner(team);

            return "Force declaring matchup winner...";

        }).register();

        CommandBuilder.command("tournament").subcommand("history").executes(ctx->{
            MutableComponent message = Component.literal("Tournament history -------------------\n");
            for(Tournament tournament : getAllTournaments().reversed())
            {
                if(!tournament.isOngoing() && tournament.isComplete()) {
                    message = message.append(tournament.getName());
                    message = message.append(" - ");
                    for(PlayerTeam team : tournament.getWinningTeams())
                    {
                        message = message.append(team.getFormattedDisplayName());
                    }
                    message = message.append("\n");
                }
            }

            Component messageCopy = message;

            ScheduleEvent.schedule(1, server->{
                ctx.getStack().getSource().sendSystemMessage(messageCopy);
            });

            return "Getting tournament history...";
        }).permission(stack->true).register();

        CommandBuilder.command("tournament").subcommand("delete").argumentString("tournament_name", this::getAllTournamentNames).executes(ctx->{
            String tournamentName = ctx.getArgumentString("tournament_name");
            Tournament tournament = getTournamentByName(tournamentName);
            if(tournament == null) {
                return "! Invalid tournament name";
            }

            data.tournaments.remove(tournament);

            return "Deleted tournament " + tournamentName;
        }).register();
    }


}
