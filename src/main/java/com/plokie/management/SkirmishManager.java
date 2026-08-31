package com.plokie.management;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.plokie.Splatoon;
import com.plokie.classes.SplatoonClasses;
import com.plokie.helpers.CommandBuilder;
import com.plokie.helpers.Helpers;
import com.plokie.helpers.Teams;
import com.plokie.interfaces.IPlayerMixin;
import com.plokie.interfaces.IPlayerTeamMixin;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;

public class SkirmishManager {

    public SkirmishManager()
    {
//        CommandBuilder.command("vanish").argumentPlayer("target").executes(
//                ctx->{
//                    try {
//                        ServerPlayer player = ctx.getArgumentPlayer("target");
//                        if(player.isInvisible()) {
//                            player.setInvisible(false);
//                            return "Unvanished " + player.getName();
//                        }
//                        else {
//                            player.setInvisible(true);
//                            return "Vanished " + player.getName();
//                        }
//                    }
//                    catch(CommandSyntaxException e) {
//                        return "! Unrecognised target";
//                    }
//                }
//        ).register();

        CommandBuilder.command("skirmish").subcommand("enter").argumentPlayer("target").argumentString("class").executes(
                ctx->{
                    try {
                        SplatoonClasses.SplatoonClass klass = ctx.getArgumentEnum("class", SplatoonClasses.SplatoonClass.class);

                        try {
                            ServerPlayer player = ctx.getArgumentPlayer("target");
                            IPlayerMixin playerMixin = (IPlayerMixin)player;
                            playerMixin.setClass(klass);

                            IPlayerTeamMixin teamMixin = Teams.getTeamMixinFromPlayer(player);
                            if(teamMixin == null) {
                                PlayerTeam defaultTeam = player.level().getScoreboard().getPlayerTeam("lime");
                                if(defaultTeam != null) {
                                    player.level().getScoreboard().addPlayerToTeam(player.getScoreboardName(), defaultTeam);
                                }
                                else {
                                    Splatoon.LOGGER.error("Failed to get default team to put player into");
                                }
                            }

                            Vec3 skirmishPos = new Vec3(1331, 110, 353);

                            player.teleportTo(skirmishPos.x, skirmishPos.y, skirmishPos.z);

                            ServerPlayer.RespawnConfig respawnConfig = new ServerPlayer.RespawnConfig(ServerLevel.OVERWORLD, Helpers.toBlockPos(skirmishPos), 0.0f, true);
                            player.setRespawnPosition(respawnConfig, false);

                            player.addTag("Skirmish");

                            Splatoon.LOGGER.info("Enter skirmish {}", player.getName().getString());

                            return "Entered skirmish";
                        }
                        catch(CommandSyntaxException e) {
                            return "! Unrecognised target";
                        }


                    }
                    catch(IllegalArgumentException e) {
                        return "! Unrecognised class";
                    }
                }
        ).register();

        CommandBuilder.command("skirmish").subcommand("exit").argumentPlayer("target").executes(
                ctx->{
                    try {
                        ServerPlayer player = ctx.getArgumentPlayer("target");
                        IPlayerMixin playerMixin = (IPlayerMixin)player;
                        playerMixin.setClass(null);

                        Vec3 hubSpawn = new Vec3(-121.5, 87, -119.5);

                        player.teleportTo(hubSpawn.x, hubSpawn.y, hubSpawn.z);

                        ServerPlayer.RespawnConfig respawnConfig = new ServerPlayer.RespawnConfig(ServerLevel.OVERWORLD, Helpers.toBlockPos(hubSpawn), 0.0f, true);
                        player.setRespawnPosition(respawnConfig, false);

                        player.removeTag("Skirmish");

                        playerMixin.setClass(null);

                        Splatoon.LOGGER.info("Exit skirmish {}", player.getName().getString());

                        return "Left skirmish";
                    }
                    catch(CommandSyntaxException e) {
                        return "! Unrecognised target";
                    }
                }
        ).register();
    }
}
