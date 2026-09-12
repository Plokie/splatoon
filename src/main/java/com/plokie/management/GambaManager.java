package com.plokie.management;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.plokie.Splatoon;
import com.plokie.helpers.CommandBuilder;
import com.plokie.helpers.Helpers;
import com.plokie.helpers.Teams;
import com.plokie.interfaces.IPlayerTeamMixin;
import com.plokie.management.hats.HatDatabase;
import com.plokie.management.tournaments.Tournament;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.CommonColors;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.apache.logging.log4j.core.tools.picocli.CommandLine;

import java.security.SecureRandom;
import java.util.*;

public class GambaManager {
    UUID playerSpinning = null;

    int spinningForTicks = 0;

    BlockPos gambaRoomPos = new BlockPos(-151, 101, -114);

    static final int numGambaSlots = 11;
    Queue<HatDatabase.Hat> gambaSlots = new ArrayDeque<>();

    float currentSlot = 0;
    int prevSlot = 0;

    List<Tuple<UUID, HatDatabase.Hat>> wonQueue = new ArrayList<>();

    public GambaManager()
    {
        ServerTickEvents.START_SERVER_TICK.register(server->{
            update(server);
        });

        CommandBuilder.command("gamba").subcommand("spin").argumentPlayer("target").executes(ctx->{
            try {
                ServerPlayer player = ctx.getArgumentPlayer("target");

                if(playerSpinning != null)
                {
                    player.sendSystemMessage(Component.literal("Can't spin while someone else is spinning"));
                    return "! Can't spin while someone else is spinning";
                }

                if(PlayerStats.get(player).get(PlayerStats.MONEY) < 100)
                {
                    player.sendSystemMessage(Component.literal("Insufficient funds!"));
                    return "! Insufficient funds";
                }

                if(TournamentManager.Instance.isAnyOngoing())
                {
                    com.plokie.interfaces.IPlayerTeamMixin teamMixin = Teams.getTeamMixinFromPlayer(player);
                    if(teamMixin != null)
                    {
                        PlayerTeam team = teamMixin.getPlayerTeam();

                        for(Tournament tournament : TournamentManager.Instance.getOngoingTournaments())
                        {
                            if(tournament.getCurrentMatchup().getTeam0() == team || tournament.getCurrentMatchup().getTeam1() == team) {
                                player.sendSystemMessage(Component.literal("You cannot gamba while you're needed for a tournament match!"));
                                return "! You cannot gamba while you're needed for a tournament match!";
                            }
                        }
                    }
                }

                PlayerStats.get(player).forceAddNoMatch(PlayerStats.MONEY, -100);

                player.sendSystemMessage(Component.literal("Rolling... Good luck!"));
                startSpin(player);

                return "Starting spin for " + player.getName().getString();
            }
            catch(CommandSyntaxException e)
            {
                return "! Invalid target";
            }
        }).register();
    }

    HatDatabase.Hat pickRandomHat()
    {
        float weightTotal = 0.0f;
        for(HatDatabase.Rarity rarity : HatDatabase.Rarity.values())
        {
            if(rarity.weight < 0) continue;

            weightTotal += rarity.weight;
        }

        Random random = new SecureRandom();
        float r = random.nextFloat() * weightTotal;
        HatDatabase.Rarity chosenRarity = HatDatabase.Rarity.COMMON;
        for(HatDatabase.Rarity rarity : HatDatabase.Rarity.values())
        {
            if(rarity.weight < 0) continue;

            chosenRarity = rarity;
            r -= rarity.weight;
            if (r <= 0.0f) break;
        }

        List<HatDatabase.Hat> possibleHats = HatDatabase.getHatsOfRarity(chosenRarity);

        int hatIdx = random.nextInt(possibleHats.size());

        return possibleHats.get(hatIdx);
    }

    float friction = 0.75f;
    float initialSpeed = 1.0f;

    public void startSpin(ServerPlayer player)
    {
        playerSpinning = player.getUUID();
//        speed = maxSpeed;
        currentSlot = 0;
        prevSlot = 0;
        spinningForTicks= 0;

        Splatoon.SERVER.overworld().playSound(null, gambaRoomPos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.MASTER);

        Random random = new SecureRandom();
        friction = random.nextFloat(0.30f, 0.5f);
        initialSpeed = random.nextFloat(0.8f, 1.5f);

        if(gambaSlots.size() < numGambaSlots)
        {
            gambaSlots.clear();
            for(int i=0; i<numGambaSlots; i++) {
                gambaSlots.add(pickRandomHat());
            }
            updateItemFrames();
        }
    }

    void updateItemFrames()
    {
        AABB gambaRoom = new AABB(gambaRoomPos).inflate(20.0);

        for(int i=0; i<numGambaSlots; i++) {
            int idx = numGambaSlots - (i+1);

            for (ItemFrame itemFrame : Splatoon.SERVER.overworld().getEntitiesOfClass(ItemFrame.class, gambaRoom)) {
                if(itemFrame.getTags().contains("GambaSlot"+idx)) {
                    HatDatabase.Hat hat = (HatDatabase.Hat) gambaSlots.toArray()[i];

                    itemFrame.setItem(hat.createItemStack());

                    BlockPos pos = itemFrame.getOnPos();
                    pos = new BlockPos(pos.getX(), pos.getY(), pos.getZ() - 1);

                    itemFrame.level().setBlockAndUpdate(pos, hat.getRarity().block.defaultBlockState());

                    break;
                }
            }
        }
    }

    HatDatabase.Hat getMiddleSlot()
    {
        int count = gambaSlots.size();
        int mid = (int)Math.floor(count / 2.0f);

        return (HatDatabase.Hat) gambaSlots.toArray()[mid];
    }

    void update(MinecraftServer server)
    {
        for(Tuple<UUID, HatDatabase.Hat> award : wonQueue)
        {
            ServerPlayer player = Splatoon.SERVER.getPlayerList().getPlayer(award.getA());
            if(player != null) {
                MutableComponent message = Component.literal("\n"+player.getName().getString());
                message = message.append(Component.literal(" won a ").withStyle(ChatFormatting.GOLD));
                message = message.append(award.getB().getRarity().formattedName());
                message = message.append(Component.literal(" " + award.getB().getName()).withStyle(award.getB().getRarity().formatCol()));
                message = message.append(Component.literal(" from GAMBA!!!!!\n").withStyle(ChatFormatting.GOLD));

                Splatoon.SERVER.sendSystemMessage(message);
                for(ServerPlayer player1 : Splatoon.SERVER.getPlayerList().getPlayers())
                {
                    player1.sendSystemMessage(message);
                }

                HatDatabase.giveHat(player, award.getB().getId(), 1);
                wonQueue.remove(award);
                break;
            }
        }

        if(playerSpinning == null) return;

        float spinTime = spinningForTicks * (1.0f / 20.0f);


        float speed = initialSpeed * ((float)Math.exp(-friction * spinTime));

        currentSlot += speed;

        int slotDiff = (int)Math.floor(currentSlot) - prevSlot;

        for(int i=0; i<slotDiff; i++) {
            gambaSlots.remove();
            gambaSlots.add(pickRandomHat());
        }
        if(slotDiff > 0) {
            Splatoon.SERVER.overworld().playSound(null, gambaRoomPos, SoundEvents.BAMBOO_WOOD_PRESSURE_PLATE_CLICK_ON, SoundSource.MASTER);
            updateItemFrames();
        }

//        Splatoon.LOGGER.info("Spin step {}: speed{}. {}",currentSlot, speed, spinningForTicks);

        if(speed <= 0.01f)
        {
            Splatoon.LOGGER.info("Finished rolling");

            Splatoon.SERVER.overworld().playSound(null, gambaRoomPos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.MASTER);
            Helpers.summonBasicFirework(Splatoon.SERVER.overworld(), Helpers.toVec3(gambaRoomPos).add(0, 0, 1), CommonColors.RED, 0);

            HatDatabase.Hat wonHat = getMiddleSlot();
            Splatoon.LOGGER.info("Won {}", wonHat.getName());

            wonQueue.add(new Tuple<>(playerSpinning, wonHat));

            playerSpinning = null;
        }
        else
        {
            prevSlot = (int)Math.floor(currentSlot);
            spinningForTicks++;
        }

    }

}
