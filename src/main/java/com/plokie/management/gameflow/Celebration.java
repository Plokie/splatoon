package com.plokie.management.gameflow;

import com.mojang.datafixers.types.templates.Tag;
import com.mojang.math.Axis;
import com.mojang.math.Transformation;
import com.plokie.Splatoon;
import com.plokie.helpers.Effects;
import com.plokie.helpers.Fill;
import com.plokie.helpers.Teams;
import com.plokie.interfaces.IPlayerMixin;
import com.plokie.interfaces.IPlayerTeamMixin;
import com.plokie.management.GameFlowManager;
import com.plokie.management.PlayerStats;
import com.plokie.management.gamemodes.Gamemode;
import com.plokie.management.maps.GamemodeMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class Celebration implements IGameState {
    List<Display.TextDisplay> textDisplays = new ArrayList<>();

    static int idxToPodiumIdx(int idx)
    {
        idx %= 5;
        switch(idx) {
            case 0: return 2;
            case 1: return 1;
            case 2: return 3;
            case 3: return 0;
            case 4: return 4;
        }
        return 0;
    }

    @Override
    public void onStateEnter(Gamemode currentGamemode, GamemodeMap currentMap) {
        Vec3 podiumViewerPos = currentMap.podiumViewerPosition;
        Vec2 podiumViewerRot = currentMap.podiumViewerRotation;
        int winningTeamIdx = Splatoon.gameFlowManager.getWinningTeam();

        List<Player> winningTeamPlayers = Splatoon.gameFlowManager.getTeamPlayers(winningTeamIdx);
        if(winningTeamPlayers.isEmpty()) return;

        IPlayerTeamMixin team = Teams.getTeamMixinFromPlayer(winningTeamPlayers.get(0));
        if (team != null) {
            //String winningTeamName = "Error";
            MutableComponent winningTeamTitle = ((PlayerTeam)team).getFormattedDisplayName();

            for(Player titlePlayer : Splatoon.gameFlowManager.getGamersIncludingSpectators())
            {
                ServerPlayer serverPlayer = (ServerPlayer)titlePlayer;

                serverPlayer.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("Wins!")));
                serverPlayer.connection.send(new ClientboundSetTitleTextPacket(winningTeamTitle));
            }
        }


        Tuple<Player, Integer> mostKills = PlayerStats.getMatchPlayerStatGreatestOf(PlayerStats.PLAYER_KILLS, winningTeamPlayers);
        Tuple<Player, Integer> mostBlocksInked = PlayerStats.getMatchPlayerStatGreatestOf(PlayerStats.BLOCKS_INKED, winningTeamPlayers);
        Tuple<Player, Integer> mostHealed = PlayerStats.getMatchPlayerStatGreatestOf(PlayerStats.AMOUNT_HEALED, winningTeamPlayers);
        Tuple<Player, Integer> mostDamageDealt = PlayerStats.getMatchPlayerStatGreatestOf(PlayerStats.DAMAGE_DEALT, winningTeamPlayers);

        int idx = 0;
        for(Player player : winningTeamPlayers)
        {
            //todo: read from equipped taunts
            for(int i=0; i<9; i++) {
                if(i==0) {
                    if(player.getInventory().getItem(i).is(Items.AIR)) {
                        ItemStack taunt = new ItemStack(Items.GOAT_HORN);
                        player.getInventory().setItem(i, taunt);
                    }
                }
                else
                {

                }
            }



            int podiumIdx = idxToPodiumIdx(idx);
            BlockPos podiumPos = currentMap.podiums.get(podiumIdx);
            Vec3 podiumPosv3 = new Vec3(podiumPos.getX() + 0.5, podiumPos.getY(), podiumPos.getZ() + 0.5);

            Fill.replace(player.level(), podiumPos, new BlockPos(1,0,1), new BlockPos(-1,0,-1), Blocks.GOLD_BLOCK, Blocks.AIR);

            player.setNoGravity(false);
            player.setDeltaMovement(Vec3.ZERO);
            player.lookAt(EntityAnchorArgument.Anchor.EYES, podiumViewerPos.add(0, 1.5, 0));
            player.teleportTo(podiumPosv3.x, podiumPosv3.y + 1, podiumPosv3.z);


            // get axis pointing towards viewers
            Vec3 diffToViewer = podiumViewerPos.subtract(podiumPosv3);
            int greatestAxis = 0;
            double greatest = 0;
            for(int i=0; i<3; i++) {
                double value = 0;
                switch(i) {
                    case 0: value = diffToViewer.x; break;
                    case 1: value = diffToViewer.y; break;
                    case 2: value = diffToViewer.z; break;
                }

                if(Math.abs(value) > greatest) {
                    greatestAxis = i;
                    greatest = Math.abs(value);
                }
            }

            double x = 0.0;
            double y = 0.0;
            double z = 0.0;

            switch(greatestAxis) {
                case 0: x = diffToViewer.x > 0 ? 1 : -1; break;
                case 1: y = diffToViewer.y > 0 ? 1 : -1; break;
                case 2: z = diffToViewer.z > 0 ? 1 : -1; break;
            }

            Vec3 orthogonalVector = new Vec3(x,y,z);

            String podiumText = "";
            if(mostKills.getA() == player) podiumText += "Most kills : " + mostKills.getB() + "\n";
            if(mostBlocksInked.getA() == player) podiumText += "Most blocks inked : " + mostBlocksInked.getB() + "\n";
            if(mostHealed.getA() == player) podiumText += "Most healed : " + mostHealed.getB() + "\n";
            if(mostDamageDealt.getA() == player) podiumText += "Most damage dealt : " + mostDamageDealt.getB() + "\n";
            podiumText += "\n" + player.getName().getString();

            Display.TextDisplay outwardText = EntityType.TEXT_DISPLAY.create(player.level(), EntitySpawnReason.COMMAND);
            if(outwardText != null) {
                Vec3 pos = podiumPosv3.subtract(orthogonalVector.scale(1.51)).add(0,3.5,0);
                outwardText.setPos(pos);
                outwardText.lookAt(EntityAnchorArgument.Anchor.EYES, pos.add(orthogonalVector));
                outwardText.setText(Component.literal(podiumText));
                outwardText.setTransformation(new Transformation(
                    new Vector3f(0.0f, 0.0f, 0.0f),
                    null,
                    new Vector3f(2.0f, 2.0f, 2.0f),
                    null
                ));
                player.level().addFreshEntity(outwardText);
                textDisplays.add(outwardText);
            }

            Display.TextDisplay inwardText = EntityType.TEXT_DISPLAY.create(player.level(), EntitySpawnReason.COMMAND);
            if(inwardText != null) {
                Vec3 pos = podiumPosv3.add(orthogonalVector.scale(2.5)).add(0,1.5,0);
                inwardText.setPos(pos);
                inwardText.lookAt(EntityAnchorArgument.Anchor.EYES, pos.subtract(orthogonalVector));
                inwardText.setText(Component.literal(podiumText));
                inwardText.setTransformation(new Transformation(
                        new Vector3f(0.0f, 0.0f, 0.0f),
                        null,
                        new Vector3f(1.0f, 1.0f, 1.0f),
                        null
                ));
                player.level().addFreshEntity(inwardText);
                textDisplays.add(inwardText);
            }

            idx++;
        }

        for(Player player : Splatoon.gameFlowManager.getGamersIncludingSpectators()) {
            if(winningTeamPlayers.contains(player)) continue;

            player.setDeltaMovement(Vec3.ZERO);
            player.forceSetRotation(podiumViewerRot.x, podiumViewerRot.y);
            player.teleportTo(podiumViewerPos.x, podiumViewerPos.y, podiumViewerPos.z);
        }
    }

    void createFireworks(GamemodeMap currentMap, IPlayerTeamMixin team)
    {
        ItemStack rocketItem = new ItemStack(Items.FIREWORK_ROCKET);
        IntArrayList explosionColors = new IntArrayList(new int[]{team.getTeamColourInt()});
        IntArrayList fadeColors = new IntArrayList(new int[]{});

        FireworkExplosion explosion = new FireworkExplosion(
                FireworkExplosion.Shape.SMALL_BALL,
                explosionColors,
                fadeColors,
                false,// hasTrail
                false //hasTwinkle
        );

        int flightDuration = 0;
        Fireworks fireworksComponent = new Fireworks(flightDuration, List.of(explosion));

        rocketItem.set(DataComponents.FIREWORKS, fireworksComponent);

        for(int i=0; i<5; i++) {
            BlockPos podiumPos = currentMap.podiums.get(idxToPodiumIdx(i));
            Vec3 podiumPosv3 = new Vec3(podiumPos.getX() + 0.5, podiumPos.getY() + 2, podiumPos.getZ() + 0.5);

            FireworkRocketEntity firework = new FireworkRocketEntity(Splatoon.SERVER.overworld(), podiumPosv3.x, podiumPosv3.y, podiumPosv3.z, rocketItem);
            Splatoon.SERVER.overworld().addFreshEntity(firework);

        }
    }

    @Override
    public GameFlowManager.GameState onStateTick(int timer, Gamemode currentGamemode, GamemodeMap currentMap) {
        int winningTeam = Splatoon.gameFlowManager.getWinningTeam();

        if(timer % 40 == 0) {
            List<Player> teamPlayers = Splatoon.gameFlowManager.getTeamPlayers(winningTeam);
            if(!teamPlayers.isEmpty())
            {
                Player player = teamPlayers.get(0);
                IPlayerTeamMixin team = Teams.getTeamMixinFromPlayer(player);
                if(team != null) {
                    createFireworks(currentMap, team);
                }
            }
        }

        Vec3 resultsPos = currentMap.podiumViewerPosition;
        Vec2 resultsRot = currentMap.podiumViewerRotation;

        for(Player player : Splatoon.gameFlowManager.getGamersIncludingSpectators()) {
            if(Splatoon.gameFlowManager.getTeamPlayers(winningTeam).contains(player))
            {

            }
            else
            {
                Effects.givePotionEffect(player, MobEffects.INVISIBILITY, 1, 1, true);
                Effects.givePotionEffect(player, MobEffects.WEAKNESS, 1, 100, true);

                if(!player.getItemBySlot(EquipmentSlot.HEAD).is(Items.AIR)) {
                    player.setItemSlot(EquipmentSlot.FEET, player.getItemBySlot(EquipmentSlot.HEAD));
                    player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.AIR));
                }

                player.setNoGravity(true);
                player.setDeltaMovement(Vec3.ZERO);
                player.forceSetRotation(resultsRot.x, resultsRot.y);
                player.teleportTo(resultsPos.x, resultsPos.y, resultsPos.z);
            }

        }
        return null;
    }

    @Override
    public void onStateExit(Gamemode currentGamemode, GamemodeMap currentMap) {
        for(Display.TextDisplay textDisplay : textDisplays) {
            textDisplay.discard();
        }

        for(int i=0; i<5; i++) {
            BlockPos podiumPos = currentMap.podiums.get(i);
            Fill.replace(Splatoon.SERVER.overworld(), podiumPos, new BlockPos(1,0,1), new BlockPos(-1,0,-1), Blocks.AIR, Blocks.GOLD_BLOCK);
        }

        for(Player player : Splatoon.gameFlowManager.getGamersIncludingSpectators())
        {
            player.setNoGravity(false);

            player.getInventory().clearOrCountMatchingItems(
                    itemStack -> itemStack.is(Items.GOAT_HORN),
                    64, // ?
                    player.getInventory()
            );

            Splatoon.gameFlowManager.playSong("", player);
        }
    }

    @Override
    public GameFlowManager.GameState getDefaultNextState() {
        return GameFlowManager.GameState.NONE;
    }

    @Override
    public int calculateDuration(Gamemode currentGamemode, GamemodeMap currentMap) {
        return 500;
    }

    @Override
    public String getStateMusic() { return "music.ending.win_results"; }
}
