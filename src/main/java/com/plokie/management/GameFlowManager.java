package com.plokie.management;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.plokie.Splatoon;
import com.plokie.helpers.CommandBuilder;
import com.plokie.helpers.Helpers;
import com.plokie.interfaces.IPlayerMixin;
import com.plokie.management.gamemodes.Gamemode;
import com.plokie.management.gamemodes.Gamemodes;
import com.plokie.management.maps.GamemodeMap;
import com.plokie.management.maps.GamemodeMaps;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.bossevents.CustomBossEvent;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

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

    BlockPos readyUpZone = new BlockPos(-136, 103, -160);
    BlockPos readyUpZoneSize = new BlockPos(10, 10, 5);


    public GameFlowManager()
    {
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

        CommandBuilder.command("gameflow").subcommand("set").subcommand("map").argumentEnum("map", GamemodeMaps.class).executes(
                ctx->{
                    try {
                        GamemodeMaps mapEnum = ctx.getArgumentEnum("map", GamemodeMaps.class);
                        this.currentMap = mapEnum.getMap();
                        return "Set map to " + mapEnum;
                    }
                    catch(IllegalArgumentException ignored) { return "! Unrecognised map";}
                }
        ).register();

        CommandBuilder.command("gameflow").subcommand("set").subcommand("gamemode").argumentEnum("gamemode", Gamemodes.class).executes(
            ctx->{
                try {
                    Gamemodes gamemodeEnum = ctx.getArgumentEnum("gamemode", Gamemodes.class);
                    currentGamemode = gamemodeEnum.getGamemode();
                    return "Set gamemode to " + gamemodeEnum.toString();
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
                PlaySong("music.opening.match_start");

                int zoneSegmentWidth = (int)Math.ceil(readyUpZoneSize.getX() / (float)currentGamemode.getNumTeams());
                for(int i=0; i < currentGamemode.getNumTeams(); i++) {
                    BlockPos segmentPos = new BlockPos(readyUpZone.getX() + (zoneSegmentWidth * i), readyUpZone.getY(), readyUpZone.getZ());
                    BlockPos segmentSize = new BlockPos(zoneSegmentWidth, readyUpZoneSize.getY(), readyUpZoneSize.getZ());

                    AABB aabb = new AABB(segmentPos);
                    aabb.setMinX(segmentPos.getX());
                    aabb.setMinY(segmentPos.getY());
                    aabb.setMinZ(segmentPos.getZ());
                    aabb.setMaxX(segmentPos.getX() + segmentSize.getX());
                    aabb.setMaxY(segmentPos.getY() + segmentSize.getY());
                    aabb.setMaxZ(segmentPos.getZ() + segmentSize.getZ());

                    for(Player player : Splatoon.SERVER.overworld().getEntitiesOfClass(Player.class, aabb))
                    {
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
                        player.teleportTo(teamSpawn.x, teamSpawn.y, teamSpawn.z);

                        ServerPlayer.RespawnConfig respawnConfig = new ServerPlayer.RespawnConfig(ServerLevel.OVERWORLD, Helpers.toBlockPos(teamSpawn), 0.0f, true);
                        ((ServerPlayer)player).setRespawnPosition(respawnConfig, false);
                    }
                }
            }
            if(this.currentGameState == GameState.RESULTS)
            {
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
            }
        }

    }

    void tick(MinecraftServer server)
    {
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

        return true;
    }
}
