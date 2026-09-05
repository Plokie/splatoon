package com.plokie.management;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.plokie.Splatoon;
import com.plokie.helpers.CommandBuilder;
import com.plokie.helpers.Teams;
import com.plokie.interfaces.IPlayerStatsMixin;
import com.plokie.interfaces.IPlayerTeamMixin;
import net.fabricmc.fabric.api.command.v2.EntitySelectorOptionRegistry;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiFunction;

public enum PlayerStats implements StringRepresentable {
    MONEY,
    PLAYER_KILLS, MOB_KILLS, DAMAGE_DEALT,
    BLOCKS_INKED, PAYLOAD_INKED,
    AMOUNT_HEALED,
    TURF_WAR_WINS, PAYLOAD_WINS,
    TOTAL_ZOMBIES_POINTS, TOTAL_ZOMBIES_ROUNDS,
    TOTAL_SECONDS_ONLINE
    ;

    public String getScoreboardReflectionName()
    {
        return "playerstat." + getSerializedName();
    }

    static Integer defaultPortFunction(Player player, Integer oldScore) {
        return oldScore;
    }

    static Map<String, Tuple<PlayerStats, BiFunction<Player, Integer, Integer>>> portOldScoreboards = new HashMap<>();

    public static void portAnyOldScores(ServerPlayer player) {
        for(var entry : portOldScoreboards.entrySet())
        {
            Objective objective = player.getScoreboard().getObjective(entry.getKey());
            if(objective == null) {
                Splatoon.LOGGER.warn("Expected to port unrecognised scoreboard '{}'", entry.getKey());
                continue;
            }
            ReadOnlyScoreInfo scoreInfo = player.getScoreboard().getPlayerScoreInfo(player, objective);
            if(scoreInfo == null) {
                Splatoon.LOGGER.info("Player did not have any info on old scoreboard '{}' to port", entry.getKey());
                continue;
            }

            int oldValue = scoreInfo.value();

            int newValue = entry.getValue().getB().apply(player, oldValue);

            PlayerStats stat = entry.getValue().getA();
            Splatoon.LOGGER.info("Porting old player scoreboard '{}' to new stat {} (Adding {}, was {})", entry.getKey(), stat.toString(), newValue, oldValue);

            PlayerStats.get(player).forceAddNoMatch(stat, newValue);

            player.getScoreboard().resetSinglePlayerScore(player, objective);
        }
    }

    public static void initialise()
    {
        portOldScoreboards.put("Splatoon.PlayerStat.m_money", new Tuple<>(PlayerStats.MONEY, PlayerStats::defaultPortFunction));
        portOldScoreboards.put("Splatoon.PlayerStat.m_blocksPainted", new Tuple<>(PlayerStats.BLOCKS_INKED, PlayerStats::defaultPortFunction));
        portOldScoreboards.put("Splatoon.PlayerStat.m_kills", new Tuple<>(PlayerStats.PLAYER_KILLS, PlayerStats::defaultPortFunction));
        portOldScoreboards.put("Splatoon.PlayerStat.m_mobKills", new Tuple<>(PlayerStats.MOB_KILLS, PlayerStats::defaultPortFunction));
        portOldScoreboards.put("Splatoon.PlayerStat.m_payloadPainted", new Tuple<>(PlayerStats.PAYLOAD_INKED, PlayerStats::defaultPortFunction));
        portOldScoreboards.put("Splatoon.PlayerStat.m_damageDealt", new Tuple<>(PlayerStats.DAMAGE_DEALT, ((player, oldValue) -> oldValue/10)));
        portOldScoreboards.put("Splatoon.PlayerStat.m_amountHealed", new Tuple<>(PlayerStats.AMOUNT_HEALED, PlayerStats::defaultPortFunction));
        portOldScoreboards.put("Splatoon.PlayerStat.m_turfWarWins", new Tuple<>(PlayerStats.TURF_WAR_WINS, PlayerStats::defaultPortFunction));
        portOldScoreboards.put("Splatoon.PlayerStat.m_payloadWins", new Tuple<>(PlayerStats.PAYLOAD_WINS, PlayerStats::defaultPortFunction));
        portOldScoreboards.put("Splatoon.PlayerStat.m_totalZombiesPoints", new Tuple<>(PlayerStats.TOTAL_ZOMBIES_POINTS, PlayerStats::defaultPortFunction));
        portOldScoreboards.put("Splatoon.PlayerStat.m_totalTimeOnline", new Tuple<>(PlayerStats.TOTAL_SECONDS_ONLINE, PlayerStats::defaultPortFunction));

//        EntitySelectorOptionRegistry.registerNonRepeatable(
//                ResourceLocation.fromNamespaceAndPath("splatoon", "money"),
//                Component.literal("Filter by money"),
//                reader->{
//                    //int cursorBefore = reader.getReader().getCursor();
//                    MinMaxBounds.Ints moneyBounds = MinMaxBounds.Ints.fromReader(reader.getReader());
//
//                    reader.addPredicate(entity -> {
//                        if (entity instanceof Player player) {
//                            int money = PlayerStats.get(player).get(MONEY);
//                            return moneyBounds.matches(money);
//                        }
//
//                        return false;
//                    });
//                }
//
//        );

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if(!(damageSource.getEntity() instanceof Player playerSource)) return;

            IPlayerTeamMixin sourceTeam = Teams.getTeamMixinFromPlayer(playerSource);
            if(sourceTeam == null) return;

            if(entity instanceof Player targetPlayer) {
                IPlayerTeamMixin targetTeam = Teams.getTeamMixinFromPlayer(targetPlayer);
                if(targetTeam == null) return;

                if(targetTeam != sourceTeam) {
                    PlayerStats.get(playerSource).add(PLAYER_KILLS, 1);
                }
            }
            else
            {
                PlayerStats.get(playerSource).add(MOB_KILLS, 1);
            }
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, damageSource, amount)->{
            if(!(damageSource.getEntity() instanceof Player playerSource)) return true;

            IPlayerTeamMixin sourceTeam = Teams.getTeamMixinFromPlayer(playerSource);
            if(sourceTeam == null) return true;

            if(entity instanceof Player targetPlayer) {
                IPlayerTeamMixin targetTeam = Teams.getTeamMixinFromPlayer(targetPlayer);
                if(targetTeam == null) return true;

                if(targetTeam != sourceTeam) {
                    Splatoon.LOGGER.info("Registered dealt damage {} to {} by {}", amount, targetPlayer.getName().getString(), playerSource.getName().getString());
                    PlayerStats.get(playerSource).add(DAMAGE_DEALT, (int)amount);
                }
            }
            return true;
        });

        var lifetimeOrMatch = List.of("lifetime", "match");
        CommandBuilder.command("playerstat").subcommand("query").argumentString("type", lifetimeOrMatch).argumentPlayer("player").argumentEnum("stat", PlayerStats::values).executes(ctx->{
            try {
                ServerPlayer player = ctx.getArgumentPlayer("player");
                PlayerStats stat = ctx.getArgumentEnum("stat", PlayerStats.class);
                String lifetime = ctx.getArgumentString("type");

                if(lifetime.equals("match")) {
                    int statValue = PlayerStats.get(player).getMatchStat(stat);
                    return player.getName().getString() + " has match stat " + stat.toString() + " = " + statValue;
                }
                else
                {
                    int statValue = PlayerStats.get(player).get(stat);
                    return player.getName().getString() + " has lifetime stat " + stat.toString() + " = " + statValue;
                }
            }
            catch(CommandSyntaxException e) {
                return "! Unrecognised target";
            }
            catch(IllegalArgumentException e) {
                return "! Unrecognised stat";
            }
        }).permission((stack)->true).register();

        CommandBuilder.command("playerstat").subcommand("add").argumentString("type", lifetimeOrMatch).argumentPlayer("player").argumentEnum("stat", PlayerStats::values).argumentInteger("delta").executes(ctx->{
            try {
                ServerPlayer player = ctx.getArgumentPlayer("player");
                PlayerStats stat = ctx.getArgumentEnum("stat", PlayerStats.class);
                int delta = ctx.getArgumentInteger("delta");
                String lifetime = ctx.getArgumentString("type");

                if(lifetime.equals("match")) {
                    PlayerStats.get(player).forceAddOnlyMatchStat(stat, delta);
                    int newValue = PlayerStats.get(player).getMatchStat(stat);
                    return "Changed player match stat " + stat.toString() + " by " + delta +". Now " + newValue;
                }
                else
                {
                    PlayerStats.get(player).forceAdd(stat, delta);
                    int newValue = PlayerStats.get(player).get(stat);
                    return "Changed player lifetime stat " + stat.toString() + " by " + delta +". Now " + newValue;
                }
            }
            catch(CommandSyntaxException e) {
                return "! Unrecognised target";
            }
            catch(IllegalArgumentException e) {
                return "! Unrecognised stat";
            }

        }).register();

        CommandBuilder.command("playerstat").subcommand("leaderboard").argumentEnum("stat", PlayerStats::values).executes(ctx->{
            try {
                PlayerStats stat = ctx.getArgumentEnum("stat", PlayerStats.class);

                String objectiveName = stat.getScoreboardReflectionName();
                Objective objective = Splatoon.SERVER.getScoreboard().getObjective(objectiveName);
                if(objective == null) return "! No scoreboard reflection for this stat";

                TreeMap<Integer, String> scores = new TreeMap<>();

                for(var scoreHolder : Splatoon.SERVER.getScoreboard().getTrackedPlayers())
                {
                    var scoreInfo = Splatoon.SERVER.getScoreboard().getPlayerScoreInfo(scoreHolder, objective);
                    if(scoreInfo == null) continue;

                    scores.put(scoreInfo.value(), scoreHolder.getScoreboardName());
                }

                String returnMessage = "Top 10 leaderboard of stat " + stat.getSerializedName() + "\n";
                returnMessage += "----------------\n";

                var sorted = scores.entrySet();


                int idx = 0;
                for(var entry : sorted.stream().toList().reversed())
                {
                    if(idx >= 10) break;

                    returnMessage += entry.getValue() + " = " + entry.getKey() + "\n";

                    idx++;
                }

                return returnMessage;
            }
            catch(IllegalArgumentException e) {
                return "! Unrecognised stat";
            }

        }).permission(stack->true).register();

        CommandBuilder.command("playerstat").subcommand("set").argumentString("type", lifetimeOrMatch).argumentPlayer("player").argumentEnum("stat", PlayerStats::values).argumentInteger("value").executes(ctx->{
            try {
                ServerPlayer player = ctx.getArgumentPlayer("player");
                PlayerStats stat = ctx.getArgumentEnum("stat", PlayerStats.class);
                int value = ctx.getArgumentInteger("value");
                String lifetime = ctx.getArgumentString("type");

                if(lifetime.equals("match")) {
                    int currentValue = PlayerStats.get(player).getMatchStat(stat);
                    int difference = value - currentValue;
                    PlayerStats.get(player).forceAddOnlyMatchStat(stat, difference);
                    int newValue = PlayerStats.get(player).getMatchStat(stat);
                    return "Changed player match stat " + stat.toString() + " by " + difference +". Now " + newValue;
                }
                else
                {
                    int currentValue = PlayerStats.get(player).get(stat);
                    int difference = value - currentValue;
                    PlayerStats.get(player).forceAdd(stat, difference);
                    int newValue = PlayerStats.get(player).get(stat);
                    return "Changed player lifetime stat " + stat.toString() + " by " + difference +". Now " + newValue;
                }
            }
            catch(CommandSyntaxException e) {
                return "! Unrecognised target";
            }
            catch(IllegalArgumentException e) {
                return "! Unrecognised stat";
            }

        }).register();
    }

    public static IPlayerStatsMixin get(Player player) {
        return get((ServerPlayer) player);
    }

    public static IPlayerStatsMixin get(ServerPlayer player) {
        return (IPlayerStatsMixin) player;
    }

    public static PlayerStats vaulueOfIgnoreCase(String enumString) {
        for(PlayerStats stat : PlayerStats.values()) {
            if(stat.toString().equalsIgnoreCase(enumString))
            {
                return stat;
            }
        }
        throw new IllegalArgumentException("Invalid enum string to convert to PlayerStats enum '" + enumString + "'");
    }

    public static void updateScoreboardReflection(Player player, PlayerStats stat)
    {
        String scoreboardName = stat.getScoreboardReflectionName();
        Objective objective = player.getScoreboard().getObjective(scoreboardName);
        if(objective == null) {
            objective = player.getScoreboard().addObjective(scoreboardName, ObjectiveCriteria.DUMMY, Component.literal(scoreboardName), ObjectiveCriteria.RenderType.INTEGER, true, null);
        }


        ScoreAccess scoreAccess = player.getScoreboard().getOrCreatePlayerScore(player, objective);
        scoreAccess.set(PlayerStats.get(player).get(stat));
    }

    public static Tuple<Player, Integer> getMatchPlayerStatGreatestOf(PlayerStats stat, List<Player> players)
    {
        TreeMap<Integer, Player> statMap = new TreeMap<>();

        for(Player player : players) {
            int value = PlayerStats.get(player).getMatchStat(stat);
            statMap.put(value, player);
        }

        for(var entry : statMap.entrySet()  .stream().toList().reversed()) {
            return new Tuple<>(entry.getValue(), entry.getKey());
        }

        return new Tuple<>(null, 0);
    }

    public static void resetMatchStats(Player player)
    {
        for(PlayerStats stat : PlayerStats.values()) {
            PlayerStats.get(player).forceAddOnlyMatchStat(stat, -PlayerStats.get(player).getMatchStat(stat));
        }
    }

    public static final Codec<PlayerStats> CODEC = StringRepresentable.fromEnum(PlayerStats::values);
    public static final Codec<Map<PlayerStats, Integer>> MAP_CODEC = Codec.unboundedMap(PlayerStats.CODEC, Codec.INT);

    @Override
    public @NotNull String getSerializedName() {
        return this.toString().toLowerCase();
    }
}
