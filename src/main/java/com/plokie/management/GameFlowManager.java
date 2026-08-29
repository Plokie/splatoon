package com.plokie.management;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.plokie.Splatoon;
import com.plokie.classes.SplatoonClasses;
import com.plokie.helpers.CommandBuilder;
import com.plokie.helpers.Fill;
import com.plokie.helpers.Helpers;
import com.plokie.helpers.ScheduleEvent;
import com.plokie.interfaces.IPlayerMixin;
import com.plokie.management.gamemodes.Gamemode;
import com.plokie.management.gamemodes.Gamemodes;
import com.plokie.management.maps.GamemodeMap;
import com.plokie.management.maps.GamemodeMaps;
import com.plokie.management.maps.UrchinUnderpass;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.bossevents.CustomBossEvent;
import net.minecraft.server.commands.ForceLoadCommand;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class GameFlowManager {
    public enum GameState {
        NONE, INTRO, CLASS_SELECT, GAME_TIME, RESULTS;

        public static GameState next(GameState current) {
            return switch (current) {
                case NONE -> INTRO;
                case INTRO -> CLASS_SELECT;
                case CLASS_SELECT -> GAME_TIME;
                case GAME_TIME -> RESULTS;
                case RESULTS -> NONE;
                default -> NONE;
            };
        }

        public static int getDuration(GameState state, Gamemodes gamemode)
        {
            return switch (state) {
                case NONE -> -1;
                case INTRO -> (gamemode.getGamemode().getIntroText().size() + 2) * 100;
                case CLASS_SELECT -> 900;
                case GAME_TIME -> 7200;
                case RESULTS -> 600;
                default -> -1;
            };
        }
    }

    List<TeamSelector> teamSelectors = new ArrayList<>();

    public TeamSelector getTeamSelectorAt(BlockPos pos) {
        for(TeamSelector teamSelector : teamSelectors)
        {
            if(teamSelector.blockPos.equals(pos)) {
                return teamSelector;
            }
        }
        return null;
    }

    Gamemode currentGamemode = null;
    GameState currentGameState = GameState.NONE;
    GamemodeMap currentMap = null;
    int timer = -1;

    Map<Integer, List<UUID>> players = new HashMap<>();
    List<UUID> spectators = new ArrayList<>();
    Map<Integer, Boolean> readyState = new HashMap<>();

    public GameState getCurrentGameState() { return currentGameState; }
    public Gamemode getCurrentGamemode() { return currentGamemode; }
    public GamemodeMap getCurrentMap() { return currentMap; }
    public int getTimer() { return timer; }

    Entity introGuide = null;

    void setPlayerTeam(Player player, int teamIndex) {
        removePlayerTeam(player);
        players.get(teamIndex).add(player.getUUID());

        for(TeamSelector teamSelector : teamSelectors) {
            if(teamSelector.selectedTeam == null) continue;
            if(teamSelector.teamIndex == teamIndex)
            {
                player.getScoreboard().addPlayerToTeam(player.getScoreboardName(), teamSelector.selectedTeam);
            }
        }
    }

    void removePlayerTeam(Player player) {
        for(Map.Entry<Integer, List<UUID>> entry : players.entrySet())
        {
            entry.getValue().remove(player.getUUID());
        }
    }

    List<Player> getTeamPlayers() {
        List<Player> ret = new ArrayList<>();
        if(currentGamemode != null) {
            for(int i=0; i<currentGamemode.getNumTeams(); i++) {
                List<UUID> teamPlayers = players.get(i);
                if(teamPlayers != null) {
                    for(UUID playerUUID : teamPlayers) {
                        Player player = Splatoon.SERVER.getPlayerList().getPlayer(playerUUID);
                        if(player != null) {
                            ret.add(player);
                        }
                    }
                }
            }
        }
        return ret;
    }

    public List<Player> getTeamPlayers(int teamIndex) {
        List<Player> ret = new ArrayList<>();
        List<UUID> teamPlayers = players.get(teamIndex);
        if(teamPlayers != null) {
            for(UUID playerUUID : teamPlayers) {
                Player player = Splatoon.SERVER.getPlayerList().getPlayer(playerUUID);
                if(player != null) {
                    ret.add(player);
                }
            }
        }
        return ret;
    }

    public List<Player> getSpectators()
    {
        List<Player> ret = new ArrayList<>();
        for(UUID playerUUID : spectators) {
            Player player = Splatoon.SERVER.getPlayerList().getPlayer(playerUUID);
            if(player != null) {
                ret.add(player);
            }
        }
        return ret;
    }

    public List<Player> getGamersIncludingSpectators() {
        List<Player> ret = getTeamPlayers();
        ret.addAll(getSpectators());
        return ret;
    }

    void clearActivePlayers()
    {
        for(Player player : getTeamPlayers())
        {
            IPlayerMixin playerMixin = (IPlayerMixin)player;
            playerMixin.setClass(null);
            playerMixin.revokeAllAbilities();
        }

        for(Map.Entry<Integer, List<UUID>> entry : players.entrySet())
        {
            entry.getValue().clear();
        }

//        for(UUID spectatorUUID : spectators) {
//
//        }
        spectators.clear();

        for(Map.Entry<Integer, Boolean> entry : readyState.entrySet())
        {
            readyState.put(entry.getKey(), false);
        }
    }

    void PlaySong(String soundPath)
    {
        ScheduleEvent.schedule(1, server->{
            for(Player player : getGamersIncludingSpectators())
            {
                ServerPlayer serverPlayer = (ServerPlayer)player;

                serverPlayer.connection.send(new ClientboundStopSoundPacket(null, SoundSource.MUSIC));

                if(soundPath != "")
                {
                    serverPlayer.connection.send(new ClientboundSoundPacket(
                            Holder.direct(SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("splatoon", soundPath))),
                            SoundSource.MUSIC,
                            0, 0, 0,
                            1.0f,
                            1.0f,
                            player.getRandom().nextLong()
                    ));
                }
            }
        });

    }

    Vec3 hubSpawn = new Vec3(-131, 103, -146);
    Map<Integer, Vec3> classSelectSpawns = new HashMap<>();

//    private final ServerBossEvent timerBossEvent = new ServerBossEvent(
//            Component.literal("Timer"),
//            BossEvent.BossBarColor.PURPLE,
//            BossEvent.BossBarOverlay.PROGRESS
//    );
    CustomBossEvent getTimerBossbar() {
        ResourceLocation barId = ResourceLocation.fromNamespaceAndPath("minecraft", "timer");
        CustomBossEvent bar = Splatoon.SERVER.getCustomBossEvents().get(barId);
        if (bar == null) {
            bar = Splatoon.SERVER.getCustomBossEvents().create(barId, Component.literal("Timer"));
        }

        return bar;
    }

    public final BlockPos readyUpZone = new BlockPos(-136, 103, -160);
    public final BlockPos readyUpZoneSize = new BlockPos(10, 5, 5);

    boolean setGamemode(Gamemodes gamemode)
    {
        return setGamemode(gamemode.getGamemode());
    }

    boolean setGamemode(Gamemode gamemode)
    {
        if(this.currentGameState == GameState.NONE)
        {
            this.currentGamemode = gamemode;

            teamSelectors.clear();
            teamSelectors.add(new TeamSelector(new BlockPos(-122, 88, -127), TeamSelector.Type.OwnTeam, -1));

            AABB mapAabb = new AABB(readyUpZone).inflate(25.0);

            Splatoon.SERVER.overworld().getEntitiesOfClass(Display.TextDisplay.class, mapAabb).forEach(textDisplay -> {
                if(textDisplay.getTags().contains("MapName")) {
                    textDisplay.setText(Component.literal("Random"));
                }
            });

            for(int i=0; i<this.currentGamemode.getMaps().size(); i++)
            {
                GamemodeMaps map = this.currentGamemode.getMaps().get(i);
                for(Display.TextDisplay textDisplay : Splatoon.SERVER.overworld().getEntitiesOfClass(Display.TextDisplay.class, mapAabb))
                {
                    if(textDisplay.getTags().contains("MapName" + String.valueOf(i)))
                    {
                        textDisplay.setText(Component.literal(map.getName().replace(" ", "\n")));
                    }
                }
            }

            Fill.replace(Splatoon.SERVER.overworld(), readyUpZone, readyUpZone.offset(readyUpZoneSize).offset(0, 0, 1), Blocks.AIR);
            Fill.replace(
                    Splatoon.SERVER.overworld(),
                    readyUpZone.offset(0, 0, -1),
                    readyUpZone.offset(readyUpZoneSize.getX(), readyUpZoneSize.getY(), -1),
                    Blocks.BLACK_CONCRETE
            );

            int zoneSegmentWidth = (int)Math.ceil(readyUpZoneSize.getX() / (float)currentGamemode.getNumTeams());
            BlockPos segmentSize = new BlockPos(zoneSegmentWidth, readyUpZoneSize.getY(), readyUpZoneSize.getZ());

            BlockState button = Blocks.POLISHED_BLACKSTONE_BUTTON.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);

            for(int i=0; i < currentGamemode.getNumTeams(); i++) {

                BlockPos segmentPos = new BlockPos(readyUpZone.getX() + (zoneSegmentWidth * i), readyUpZone.getY(), readyUpZone.getZ());
                BlockPos halfSegmentSize = new BlockPos((int) Math.floor(segmentSize.getX() * 0.5f), (int) Math.floor(segmentSize.getY() * 0.5f), (int) Math.floor(segmentSize.getZ() * 0.5f));

                Fill.replace(
                        Splatoon.SERVER.overworld(),
                        new BlockPos(
                                readyUpZone.getX() + ((i+1)*segmentSize.getX()) + i,
                                readyUpZone.getY(),
                                readyUpZone.getZ()
                        ),
                        new BlockPos(
                                readyUpZone.getX() + ((i+1)*segmentSize.getX()) + i,
                                readyUpZone.getY() + readyUpZoneSize.getY(),
                                readyUpZone.getZ() + readyUpZoneSize.getZ() + 1
                        ),
                        Blocks.BLACK_CONCRETE
                );

                BlockPos barrelPos = new BlockPos(
                        readyUpZone.getX() + (i*segmentSize.getX()) + (halfSegmentSize.getX()) + i,
                        readyUpZone.getY() + (1),
                        readyUpZone.getZ() + (-1)
                );



                Fill.replace(Splatoon.SERVER.overworld(), barrelPos, barrelPos, Blocks.BARREL);
                //Fill.replace(Splatoon.SERVER.overworld(), barrelPos.offset(0,1,0), barrelPos.offset(0, 1, 0), Blocks.WAXED_COPPER_BULB);
                Splatoon.SERVER.overworld().setBlockAndUpdate(barrelPos.offset(0,1,1), button);

                Splatoon.SERVER.overworld().setBlockAndUpdate(barrelPos.offset(0,1,0), Blocks.WAXED_COPPER_BULB.defaultBlockState());

                teamSelectors.add(new TeamSelector(barrelPos, TeamSelector.Type.TeamSlot, i));

            }

            return true;
        }
        return false;
    }

    boolean setMap(GamemodeMaps map) {
        if(this.currentGameState == GameState.NONE)
        {
            this.currentMap = map.getMap();
            return true;
        }
        return false;
    }


    public GameFlowManager()
    {
//        this.currentGamemode = Gamemodes.TurfWar.getGamemode();
        ServerTickEvents.START_SERVER_TICK.register(this::tick);

        classSelectSpawns.put(0, new Vec3(-135.5,94,-157.5));
        classSelectSpawns.put(1, new Vec3(-126.5,94,-157.5));

        players.put(0, new ArrayList<>());
        players.put(1, new ArrayList<>());

        readyState.put(0, false);
        readyState.put(1, false);

        CommandBuilder.command("gameflow").subcommand("ready").argumentInteger("team_index").executes(ctx -> {
            readyState.put(ctx.getArgumentInteger("team_index"), true);
            return "Readied team";
        }).register();

        CommandBuilder.command("gameflow").subcommand("unready").argumentInteger("team_index").executes(ctx -> {
            readyState.put(ctx.getArgumentInteger("team_index"), false);
            return "Unreadied team";
        }).register();

        CommandBuilder.command("gameflow").subcommand("skip").executes(ctx->{
            setGameState(GameState.next(currentGameState));
            return "Skipping to next gamestate " + currentGameState.toString();
        }).register();

        CommandBuilder.command("gameflow").subcommand("set").subcommand("gamestate").argumentEnum("gamestate", GameState.class).executes(
                ctx->{
                    try {
                        GameState gamestateEnum = ctx.getArgumentEnum("gamestate", GameState.class);
                        setGameState(gamestateEnum);
                        return "Set gamestate to " + gamestateEnum.toString();
                    }
                    catch(IllegalArgumentException ignored) { return "! Unrecognised gamestate";}
                }
        ).register();

        CommandBuilder.command("gameflow").subcommand("set").subcommand("team").argumentPlayer("target").argumentInteger("team_index").executes(
                ctx->{
                    try {
                        ServerPlayer player = ctx.getArgumentPlayer("target");
                        int teamIdx = ctx.getArgumentInteger("team_index");
                        setPlayerTeam(player, teamIdx);

                        return "Set " + player.getName().getString() + " team side to " + teamIdx;

                    } catch(CommandSyntaxException ignored) { return "! Unrecognised target";}
                }
        ).register();

        CommandBuilder.command("gameflow").subcommand("set").subcommand("team").argumentPlayer("target").subcommand("none").executes(
                ctx->{
                    try {
                        ServerPlayer player = ctx.getArgumentPlayer("target");
                        removePlayerTeam(player);

                        return "Removed " + player.getName().getString() + " from any team";

                    } catch(CommandSyntaxException ignored) { return "! Unrecognised target";}
                }
        ).register();

        CommandBuilder.command("gameflow").subcommand("set").subcommand("map").subcommand("enum").argumentEnum("map", GamemodeMaps.class).executes(
                ctx->{
                    try {
                        GamemodeMaps mapEnum = ctx.getArgumentEnum("map", GamemodeMaps.class);
                        if(setMap(mapEnum)) {
                            return "Set map to " + mapEnum.getName();
                        }
                        else {
                            return "! Cannot change map while a game is in progress";
                        }
                    }
                    catch(IllegalArgumentException ignored) { return "! Unrecognised map";}
                }
        ).register();

        CommandBuilder.command("gameflow").subcommand("set").subcommand("map").subcommand("index").argumentInteger("map_index").executes(
                ctx->{
                    if(currentGamemode == null) return "! No gamemode selected";
                    int index = ctx.getArgumentInteger("map_index");
                    if(index < currentGamemode.getMaps().size())
                    {
                        if(setMap(currentGamemode.getMaps().get(index)))
                        {
                            return "Set map to "+ currentGamemode.getMaps().get(index).getName();
                        }
                        else
                        {
                            return "! Cannot change map while a game is in progress";
                        }
                    }
                    else {
                        index = (int)(Math.random() * (double)currentGamemode.getMaps().size());
                        GamemodeMaps map = currentGamemode.getMaps().get(index);
                        if(setMap(map))
                        {
                            return "Invalid map index, setting to a random map: " + map.getName();
                        }
                        else {
                            return "! Cannot change map while a game is in progress";
                        }
                    }
                }
        ).register();

        CommandBuilder.command("gameflow").subcommand("set").subcommand("map").subcommand("random").executes(
                ctx->{
                    if(currentGamemode == null) return "! No gamemode selected";
                    if(currentGamemode.getMaps().isEmpty()) return "! No maps in this gamemode";
                    int index = (int)(Math.random() * (double)currentGamemode.getMaps().size());
                    GamemodeMaps map = currentGamemode.getMaps().get(index);
                    if(setMap(map))
                    {
                        return "Chose random map " + map.getName();
                    }
                    else {
                        return "! Cannot change map while a game is in progress";
                    }
                }
        ).register();

        CommandBuilder.command("gameflow").subcommand("set").subcommand("gamemode").argumentEnum("gamemode", Gamemodes.class).executes(
            ctx->{
                try {
                    Gamemodes gamemodeEnum = ctx.getArgumentEnum("gamemode", Gamemodes.class);
                    if(setGamemode(gamemodeEnum)) {
                        return "Set gamemode to " + gamemodeEnum.getName();
                    }
                    else {
                        return "! Cannot change gamemode while a game is in progress";
                    }
                }
                catch(IllegalArgumentException ignored) { return "! Unrecognised gamemode";}
            }
        ).register();

        CommandBuilder.command("gameflow").subcommand("spectate").argumentPlayer("target").executes(
                ctx->{
                    try {
                        ServerPlayer player = ctx.getArgumentPlayer("target");
                        if(spectators.contains(player.getUUID()))
                        {
                            spectators.remove(player.getUUID());
                            player.teleportTo(hubSpawn.x, hubSpawn.y, hubSpawn.z);

                            ServerPlayer.RespawnConfig respawnConfig = new ServerPlayer.RespawnConfig(ServerLevel.OVERWORLD, Helpers.toBlockPos(hubSpawn), 0.0f, true);
                            player.setRespawnPosition(respawnConfig, false);

                            return "Removing " + player.getName() + " as a spectator...";
                        }
                        else {
                            Vec3 spectatorZone = currentMap.spectatorZone;
                            player.teleportTo(spectatorZone.x, spectatorZone.y, spectatorZone.z);

                            spectators.add(player.getUUID());
                            return "Making " + player.getName() + " a spectator...";
                        }
                    } catch(CommandSyntaxException ignored) { return "! Unrecognised target";}
                }
        ).register();

        CommandBuilder.command("gameflow").subcommand("get").subcommand("team_players").argumentInteger("team_index").subcommand("team").executes(
                ctx->{
                    String returnMessage = "";
                    for(Player player : getTeamPlayers(ctx.getArgumentInteger("team_index"))) {
                        returnMessage += player.getName().getString() + ",";
                    }
                    return returnMessage;
                }
        ).register();

        CommandBuilder.command("gameflow").subcommand("get").subcommand("all_players").executes(
                ctx->{
                    String returnMessage = "";
                    for(Player player : getTeamPlayers()) {
                        returnMessage += player.getName().getString() + ",";
                    }
                    return returnMessage;
                }
        ).register();

        CommandBuilder.command("gameflow").subcommand("get").subcommand("spectators").executes(
                ctx->{
                    String returnMessage = "";
                    for(Player player : getSpectators()) {
                        returnMessage += player.getName().getString() + ",";
                    }
                    return returnMessage;
                }
        ).register();

        CommandBuilder.command("gameflow").subcommand("get").subcommand("all_players_and_spectators").executes(
                ctx->{
                    String returnMessage = "";
                    for(Player player : getGamersIncludingSpectators()) {
                        returnMessage += player.getName().getString() + ",";
                    }
                    return returnMessage;
                }
        ).register();

        CommandBuilder.command("gameflow").subcommand("get").subcommand("current_map").executes(
                ctx->{
                    String returnMessage = "No map selected";
                    if(currentMap != null) returnMessage = currentMap.getClass().getSimpleName();
                    return returnMessage;
                }
        ).register();

        CommandBuilder.command("gameflow").subcommand("get").subcommand("current_gamemode").executes(
                ctx->{
                    String returnMessage = "No map selected";
                    if(currentGamemode != null) returnMessage = currentGamemode.getClass().getSimpleName();
                    return returnMessage;
                }
        ).register();

        CommandBuilder.command("gameflow").subcommand("get").subcommand("current_gamestate").executes(
                ctx->{
                    String returnMessage = "! Uhh no game state?";
                    if(currentGameState != null) returnMessage = "Current game state is "+currentGameState.toString();
                    return returnMessage;
                }
        ).register();

        CommandBuilder.command("gameflow").subcommand("is").subcommand("current_gamemode").argumentEnum("gamemode", Gamemodes.class).executes(
                ctx->{
                    try {
                        Gamemodes gamemodeEnum = ctx.getArgumentEnum("gamemode", Gamemodes.class);
                        String returnMessage = "! Current gamemode is not " + gamemodeEnum.toString();
                        if(currentGamemode != null && currentGamemode.toEnum() == gamemodeEnum) returnMessage = "Current gamemode is "+gamemodeEnum.toString();
                        return returnMessage;

                    }
                    catch(IllegalArgumentException ignored) { return "! Unrecognised gamemode";}
                }
        ).register();

        CommandBuilder.command("gameflow").subcommand("is").subcommand("current_map").subcommand("enum").argumentEnum("map", GamemodeMaps.class).executes(
                ctx->{
                    try {
                        GamemodeMaps mapEnum = ctx.getArgumentEnum("map", GamemodeMaps.class);
                        String returnMessage = "! Current map is not " + mapEnum.toString();
                        if(currentMap != null && currentMap.toEnum() == mapEnum) returnMessage = "Current map is "+mapEnum.toString();
                        return returnMessage;

                    }
                    catch(IllegalArgumentException ignored) {
                        String arg = ctx.getArgumentString("map");
                        return "! Unrecognised map " + arg;
                    }
                }
        ).register();

        CommandBuilder.command("gameflow").subcommand("is").subcommand("current_map").subcommand("index").argumentInteger("gamemode_map_index").executes(
                ctx->{
                    if(currentGamemode == null) return "! No gamemode selected";
                    if(currentMap == null) return "! No map selected";
                    int mapIndex = ctx.getArgumentInteger("gamemode_map_index");
                    if(mapIndex < currentGamemode.getMaps().size())
                    {
                        if(currentGamemode.getMaps().get(mapIndex) == currentMap.toEnum())
                        {
                            return "Gamemode map index " + mapIndex + " ("+currentMap.toEnum()+") is currently active";
                        }
                    }
                    return "! Gamemode map index " + mapIndex + " is not currently active";
                }
        ).register();
    }

    public void setGameState(GameState gameState)
    {
        if(this.currentGameState != gameState && currentGamemode != null)
        {
            currentGamemode.onGameStateChange(this, gameState);
        }
        this.currentGameState = gameState;

        timer = GameState.getDuration(currentGameState, currentGamemode.toEnum());

        if(this.currentMap!= null)
        {
            if(this.currentGameState == GameState.INTRO)
            {
//                CommandSourceStack dummySource = new CommandSourceStack(
//                        CommandSource.NULL,
//                        Vec3.ZERO,
//                        Vec2.ZERO,
//                        Splatoon.SERVER.overworld(),
//                        4,
//                        "DummySource",
//                        Component.literal("Dummy"),
//                        Splatoon.SERVER,
//                        null
//                );
//                try {
//                    ColumnPos a = new ColumnPos((int)this.currentMap.mapCorner.x, (int)this.currentMap.mapCorner.y);
//                    ColumnPos b = new ColumnPos((int)(this.currentMap.mapCorner.x + this.currentMap.mapSize.x), (int)(this.currentMap.mapCorner.y + this.currentMap.mapSize.y));
//
//                    Method method = ForceLoadCommand.class.getDeclaredMethod("changeForceLoad", CommandSourceStack.class, ColumnPos.class, ColumnPos.class, boolean.class);
//                    method.setAccessible(true);
//                    try {
//                        int result = (int)method.invoke(dummySource, a, b, true);
//                    } catch (Exception e) {
//                        Splatoon.LOGGER.error("Forceload invokation error occured: {}", e.getMessage());
//                    }
//                }
//                catch(NoSuchMethodException e) {
//                    Splatoon.LOGGER.error("Could not get force load method");
//                }

                PlaySong("music.opening.match_start");

                int zoneSegmentWidth = (int)Math.ceil(readyUpZoneSize.getX() / (float)currentGamemode.getNumTeams());
                for(int i=0; i < currentGamemode.getNumTeams(); i++) {

                    BlockPos segmentPos = new BlockPos(readyUpZone.getX() + (zoneSegmentWidth * i), readyUpZone.getY(), readyUpZone.getZ());
                    BlockPos segmentSize = new BlockPos(zoneSegmentWidth, readyUpZoneSize.getY(), readyUpZoneSize.getZ());
                    BlockPos halfSegmentSize = new BlockPos((int)(segmentSize.getX() * 0.5f), (int)(segmentSize.getY() * 0.5f), (int)(segmentSize.getZ() * 0.5f));

                    AABB aabb = new AABB(new BlockPos(segmentPos.getX() + halfSegmentSize.getX(), segmentPos.getY() + halfSegmentSize.getY(),  segmentPos.getZ() + halfSegmentSize.getZ()));
                    aabb = aabb.inflate(halfSegmentSize.getX(), halfSegmentSize.getY(), halfSegmentSize.getZ());

                    Splatoon.LOGGER.info("Setup team members {}, min: {} max: {}", i, aabb.getMinPosition(), aabb.getMaxPosition());

                    for(Player player : Splatoon.SERVER.overworld().getEntitiesOfClass(Player.class, aabb))
                    {
                        Splatoon.LOGGER.info("\t Player {}", player.getName().toString());
                        setPlayerTeam(player, i);
                    };
                }


                ServerLevel level = Splatoon.SERVER.overworld();
                introGuide = EntityType.BLOCK_DISPLAY.create(level, EntitySpawnReason.COMMAND);
                assert introGuide != null;

                introGuide.setPos(currentMap.introStartPosition);
                introGuide.forceSetRotation(currentMap.introStartRotation.x, getCurrentMap().introStartRotation.y);

                level.addFreshEntity(introGuide);
            }
            else {
                if(introGuide != null) {
                    introGuide.discard();
                    introGuide = null;
                }
            }
            if(this.currentGameState == GameState.CLASS_SELECT)
            {
                PlaySong("music.lobby.main");

                getTimerBossbar().setVisible(true);
                getTimerBossbar().setName(Component.literal("Class select"));

                for(int i=0; i < currentGamemode.getNumTeams(); i++) {
                    Vec3 classSelect = classSelectSpawns.get(i);
                    for(Player player : getTeamPlayers(i)) {
                        player.teleportTo(classSelect.x, classSelect.y, classSelect.z);

                        ServerPlayer serverPlayer = ((ServerPlayer)player);

                        ServerPlayer.RespawnConfig respawnConfig = new ServerPlayer.RespawnConfig(ServerLevel.OVERWORLD, Helpers.toBlockPos(classSelect), 0.0f, true);
                        serverPlayer.setRespawnPosition(respawnConfig, false);

//                        CommandSourceStack source = serverPlayer.getServer().createCommandSourceStack().withSuppressedOutput();
//                        serverPlayer.getServer().getCommands().performPrefixedCommand(source, "bossbar set minecraft:timer visible true");

                        getTimerBossbar().addPlayer(serverPlayer);
                    }
                }

                Vec3 spectatorZone = currentMap.spectatorZone;
                for(Player player : getSpectators()) {
                    player.teleportTo(spectatorZone.x, spectatorZone.y, spectatorZone.z);

                    ServerPlayer serverPlayer = ((ServerPlayer)player);

                    ServerPlayer.RespawnConfig respawnConfig = new ServerPlayer.RespawnConfig(ServerLevel.OVERWORLD, Helpers.toBlockPos(spectatorZone), 0.0f, true);
                    serverPlayer.setRespawnPosition(respawnConfig, false);

                    getTimerBossbar().addPlayer(serverPlayer);
                }
            }
            if(this.currentGameState == GameState.GAME_TIME)
            {
                getTimerBossbar().setName(Component.literal("Game time"));

                PlaySong("music.battle.splattack");

                for(int i=0; i < currentGamemode.getNumTeams(); i++) {
                    Vec3 teamSpawn = currentMap.teamSpawns.get(i);
                    for(Player player : getTeamPlayers(i)) {
                        List<String> removeTags = new ArrayList<>();
                        for(String tag : player.getTags())
                        {
                            if(tag.endsWith("Pick")) {
//                                String klass = tag.substring(0, tag.length() - 4);
                                String klass = tag.replace("Pick", "");
                                Splatoon.LOGGER.info("XPICK {}", klass);

                                try {
                                    SplatoonClasses.SplatoonClass pickClass = SplatoonClasses.SplatoonClass.valueOf(klass);
                                    ((IPlayerMixin)player).setClass(pickClass);
                                }
                                catch(Exception e)
                                {
                                    Splatoon.LOGGER.warn("Unknown pick tag {}", tag);
                                }

                                removeTags.add(tag);
                            }
                        }
                        // avoids concurrentmodificationexception
                        for(String tag : removeTags) {
                            player.removeTag(tag);
                        }

                        player.teleportTo(teamSpawn.x, teamSpawn.y, teamSpawn.z);

                        ServerPlayer.RespawnConfig respawnConfig = new ServerPlayer.RespawnConfig(ServerLevel.OVERWORLD, Helpers.toBlockPos(teamSpawn), 0.0f, true);
                        ((ServerPlayer)player).setRespawnPosition(respawnConfig, false);
                    }
                }
            }
            if(this.currentGameState == GameState.RESULTS)
            {
                for(Player player : getTeamPlayers())
                {
                    ((IPlayerMixin)player).setClass(null);
                }

                PlaySong("");

                Vec3 resultsPos = this.currentMap.resultsPosition;
                Vec2 resultsRot = this.currentMap.resultsRotation;
                for(Player player : getGamersIncludingSpectators()) {
                    player.teleportTo(resultsPos.x, resultsPos.y, resultsPos.z);
                    player.forceSetRotation(resultsRot.x, resultsRot.y);
                }

                getTimerBossbar().setVisible(false);
                getTimerBossbar().removeAllPlayers();
            }
            if(this.currentGameState == GameState.NONE)
            {
                for(Player player : getGamersIncludingSpectators()) {
                    player.teleportTo(hubSpawn.x, hubSpawn.y, hubSpawn.z);

                    ServerPlayer.RespawnConfig respawnConfig = new ServerPlayer.RespawnConfig(ServerLevel.OVERWORLD, Helpers.toBlockPos(hubSpawn), 0.0f, true);
                    ((ServerPlayer)player).setRespawnPosition(respawnConfig, false);
                }
                clearActivePlayers();

                setGamemode(getCurrentGamemode().toEnum());
            }
        }

    }

    void tick(MinecraftServer server)
    {
        if(server.getTickCount() == 10) {
            setGamemode(Gamemodes.TurfWar);
            setMap(GamemodeMaps.UrchinUnderpass);
        }

        if(currentGamemode != null && currentMap != null)
        {
            getTimerBossbar().setProgress(timer / (float)GameState.getDuration(currentGameState, currentGamemode.toEnum()));

            if(currentGameState == GameState.NONE)
            {
                if(areAllTeamsReady())
                {
                    setGameState(GameState.INTRO);
                }
            }
            else
            {
                if(this.currentGameState == GameState.INTRO)
                {
                    if(introGuide != null) {
                        for(Player player : getGamersIncludingSpectators()) {
                            if(!player.isPassenger()) {
                                player.startRiding(introGuide, true);
                            }
                        }

                        Vec3 forward = introGuide.getForward();
                        Vec3 pos = introGuide.getEyePosition();
                        forward = new Vec3(pos.x + (forward.x * 0.25f), pos.y + (forward.y * 0.25), pos.z + (forward.z * 0.25));

                        introGuide.setPos(forward);
                    }

                    int duration = GameState.getDuration(currentGameState, currentGamemode.toEnum());
                    if(timer == duration - 1)
                    {
                        for(Player player : getGamersIncludingSpectators())
                        {
                            ServerPlayer serverPlayer = (ServerPlayer)player;

                            serverPlayer.connection.send(new ClientboundSetTitleTextPacket(Component.literal(this.currentGamemode.getName())));
                        }


                    }

                    for(int i=0; i<this.currentGamemode.getIntroText().size(); i++)
                    {
                        if(timer == duration - ((i+1) * 100))
                        {
                            for(Player player : getGamersIncludingSpectators())
                            {
                                ServerPlayer serverPlayer = (ServerPlayer)player;

                                player.level().playSound(
                                        player,
                                        player.getEyePosition().x, player.getEyePosition().y, player.getEyePosition().z,
                                        SoundEvents.EXPERIENCE_ORB_PICKUP,
                                        SoundSource.MASTER,
                                        0.5f,
                                        1.0f
                                );

                                String text = this.currentGamemode.getIntroText().get(i);
                                serverPlayer.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(text)));
                                serverPlayer.connection.send(new ClientboundSetTitleTextPacket(Component.literal("")));
                            }
                        }
                    }
                }
                else if(this.currentGameState == GameState.RESULTS)
                {
                    Vec3 resultsPos = currentMap.resultsPosition;
                    Vec2 resultsRot = currentMap.resultsRotation;

                    for(Player player : getGamersIncludingSpectators())
                    {
                        player.snapTo(resultsPos.x, resultsPos.y, resultsPos.z, resultsRot.x, resultsRot.y);
                        player.teleportTo(resultsPos.x, resultsPos.y, resultsPos.z);
                    }
                }
                else if(this.currentGameState == GameState.GAME_TIME)
                {
                    if(timer == 1200) {
                        PlaySong("music.battle.last_minute");

                        for(Player player : getGamersIncludingSpectators())
                        {
                            ServerPlayer serverPlayer = (ServerPlayer)player;
                            serverPlayer.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("One minute left!")));
                            serverPlayer.connection.send(new ClientboundSetTitleTextPacket(Component.literal("")));
                        }
                    }
                }

                currentGamemode.tick(this, timer);
                timer--;

                if(timer <= 0)
                {
                    setGameState(GameState.next(currentGameState));
                }
            }
        }
    }

    boolean areAllTeamsReady()
    {
        if(currentGamemode == null) return false;

        for(int i=0; i < currentGamemode.getNumTeams(); i++)
        {
            Boolean ready = readyState.get(i);
            if(ready == null) return false;
            if(!ready) return false;
        }

        for(TeamSelector teamSelector : teamSelectors) {
            if(teamSelector.type != TeamSelector.Type.TeamSlot) continue;

            if(teamSelector.selectedTeam == null) return false;
        }

        return true;
    }
}
