package com.plokie.customitems.items;

import com.nimbusds.oauth2.sdk.id.IdentifierWithOptionalURIRepresentation;
import com.plokie.Splatoon;
import com.plokie.classes.abilities.Ability;
import com.plokie.classes.abilities.AbilityManager;
import com.plokie.customitems.CustomItem;
import com.plokie.customitems.ICustomItem;
import com.plokie.helpers.Helpers;
import com.plokie.interfaces.IPlayerMixin;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.CommonColors;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AdventureModePredicate;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class BuildingRails extends ICustomItem {
    public static void dataCallback(Player player, ItemStack item)
    {
        Block wallBlock = Blocks.RAIL;
        ResourceLocation model = BuiltInRegistries.BLOCK.getKey(wallBlock);
        item.set(DataComponents.ITEM_MODEL, model);

        BlockPredicate blockPredicate = BlockPredicate.Builder.block().build();
        AdventureModePredicate canPlaceOn = new AdventureModePredicate(List.of(blockPredicate));
        item.set(
                DataComponents.CAN_PLACE_ON,
                canPlaceOn
        );
    }

    public static class RailNode {
        public RailNode(BlockPos position) {
            this.position = position;
        }

        public void removedCallback(Player brokenByPlayer, RailNode nextNodeOpt)
        {
            int numRailsToRefund = 0;

            if(nextNodeOpt != null) {
                Vec3 a = position.getCenter();
                Vec3 b = nextNodeOpt.position.getCenter();

                float length = (float)a.distanceTo(b);
                int intLength = (int)Math.ceil(length);
                numRailsToRefund += intLength;


                BlockGetter.traverseBlocks(a, b, null, (context, bpUnsafe)->{
                    BlockPos bp = new BlockPos(bpUnsafe); // looks like it reuses bpUnsafe. getting weird behaviour otherwise

                    if(brokenByPlayer.level().getBlockState(bp).is(Blocks.RAIL)) {
                        brokenByPlayer.level().setBlockAndUpdate(bp, Blocks.AIR.defaultBlockState());

                        ChunkPos chunkPos = new ChunkPos(bp);
                        if(s_cachedFetchedRails.containsKey(chunkPos))
                        {
                            var blocksInChunk = s_cachedFetchedRails.get(chunkPos);
                            if(blocksInChunk != null) {
                                Splatoon.LOGGER.info("Removed cached rail block Position={} chunk={}", bp, chunkPos);
                                blocksInChunk.remove(bp);
                            }
                        }
//                        else
//                        {
//                            Splatoon.LOGGER.info("Rail block Position={} chunk={} could not be found cached", bp, chunkPos);
//                        }
                    }


                    return null;
                }, (ctx)->{return null;});
            }
            else
            {
                if(brokenByPlayer.level().getBlockState(position).is(Blocks.RAIL)) {
                    brokenByPlayer.level().setBlockAndUpdate(position, Blocks.AIR.defaultBlockState());
                }
            }

            Splatoon.LOGGER.info("Refunding {} rails...", numRailsToRefund);

            Ability ability = ((IPlayerMixin)brokenByPlayer).getAbility(AbilityManager.AbilityEnum.BuildingRails);
            if(ability != null) {
                ability.setCount(Math.clamp(ability.getCount() + numRailsToRefund, 0, ability.getMaxCount()));
            }
        }

        public void changeHealth(Level level, float delta, RailNode nextNodeOpt)
        {
            health += delta;
            health = Math.clamp(health, 0.0f, 1.0f);

            if(health <= 0.0f) isBroken = true;
            if(health >= 1.0f) isBroken = false;

            if(nextNodeOpt != null) {
                int damageStage = Math.round((1.0f - health) * 9);
                if(damageStage == 0) damageStage = 15;

                if(damageStage != cachedPrevDamageStage) {
                    cachedPrevDamageStage = damageStage;

                    int destrStage = damageStage;

                    Vec3 a = position.getCenter();
                    Vec3 b = nextNodeOpt.position.getCenter();
                    ServerLevel serverLevel = (ServerLevel)level;

                    BlockGetter.traverseBlocks(a, b, null, (context, bpUnsafe)->{
                        BlockPos bp = new BlockPos(bpUnsafe);

                        if(level.getBlockState(bp).is(Blocks.RAIL)) {
                            int hashCode = bp.hashCode();
//                            Splatoon.LOGGER.info("Send destruction packet for rail {}, hash={}", bp, hashCode);
                            ClientboundBlockDestructionPacket packet = new ClientboundBlockDestructionPacket(hashCode, bp, destrStage);

                            for (ServerPlayer player : serverLevel.getChunkSource().chunkMap.getPlayers(new ChunkPos(bp), false)) {
                                player.connection.send(packet);
                            }
                        }

                        return null;
                    }, (ctx)->{return null;});
                }

            }
        }

        public BlockPos position;
        float health = 1.0f;
        public boolean isBroken = false;

        int cachedPrevDamageStage = -1;
    }

    public static class PlacedRails {
        public PlacedRails(UUID playerUUID)
        {
            this.playerUUID = playerUUID;
            for(int i=0; i<numChannels; i++) {
                channels.add(new ArrayList<>());
                channels.add(new ArrayList<>());
                channels.add(new ArrayList<>());
            }
        }

        public UUID playerUUID;
        public int selectedChannel = 0;
        public List<List<RailNode>> channels = new ArrayList<>();
    }

    public record FetchedRail(PlacedRails placedRails, int channelIndex, RailNode node) {
        public int calculateNodeIndex()
        {
            return placedRails.channels.get(channelIndex).indexOf(node);
        }
    }

    public static FetchedRail getRailNodeForPosition(BlockPos position)
    {
        ChunkPos chunkPos = new ChunkPos(position);

        //Splatoon.LOGGER.info("Querying rails at position {}, chunkPos = {}", position, chunkPos);

        if(!s_cachedFetchedRails.containsKey(chunkPos)) {
            //Splatoon.LOGGER.info("\tDoes not contain chunkPos.");
            return null;
        }


        var blocksInChunk = s_cachedFetchedRails.get(chunkPos);

        if(blocksInChunk == null) {
            //Splatoon.LOGGER.info("\tblocksInChunk was null");
            return null;
        }

//        for(var entry : blocksInChunk.entrySet())
//        {
//            Splatoon.LOGGER.info("\t=={}=={}", chunkPos, entry.getKey());
//        }

        if(!blocksInChunk.containsKey(position)) {
            //Splatoon.LOGGER.info("\tDoes not contain block position in chunk.");
            return null;
        }

//        FetchedRail fetchedRail = blocksInChunk.get(position);

        //Splatoon.LOGGER.info("\tFound FetchedRail channel={} node={}", fetchedRail.channelIndex, fetchedRail.nodeIndex);

        return blocksInChunk.get(position);

//        for(var entry : s_placedRails.entrySet())
//        {
//            FetchedRail fetchedRail = getRailNodeForPosition(position, entry.getKey());
//            if(fetchedRail != null) return fetchedRail;
//        }
    }
    public static FetchedRail getRailNodeForPosition(BlockPos position, UUID forPlayerUUID)
    {
        FetchedRail railHere = getRailNodeForPosition(position);
        if(railHere == null) return null;

        if(!railHere.placedRails.playerUUID.equals(forPlayerUUID)) return null;

        return railHere;

//        if(!s_placedRails.containsKey(forPlayerUUID)) return null;
//        PlacedRails placedRails = s_placedRails.get(forPlayerUUID);
//
//        Splatoon.LOGGER.info("Querying rails at position {} for {}", position, forPlayerUUID);
//
//        int channelIdx=0;
//        for(List<RailNode> channel : placedRails.channels)
//        {
//            Splatoon.LOGGER.info("\tCheck channel {}", channelIdx);
//
//            for(int i=0; i<channel.size()-1; i++) {
//                Splatoon.LOGGER.info("\t\tCheck node {}", i);
//
//                RailNode nodeA = channel.get(i);
//                RailNode nodeB = channel.get(i + 1);
//
//                Vec3 a = nodeA.position.getCenter();
//                Vec3 b = nodeB.position.getCenter();
//
//                int channelIdx0 = channelIdx;
//                int nodeIndex = i;
//                AtomicReference<FetchedRail> returnValue = new AtomicReference<>();
//
//                BlockGetter.traverseBlocks(a, b, null, (context, bp)->{
//                    Splatoon.LOGGER.info("\t\tCheck rail at {}", bp);
//                    if(position.getX() == bp.getX() && position.getY() == bp.getY() && position.getZ() == bp.getZ()) {
//                        Splatoon.LOGGER.info("\t\tFound rail at {}", bp);
//                        returnValue.set(new FetchedRail(placedRails, channelIdx0, nodeIndex));
//                        return null;
//                    }
//
//                    return null;
//                }, (ctx)->{return null;});
//
//                if(returnValue.get() != null) {
//                    Splatoon.LOGGER.info("\tReturning rail...");
//                    return returnValue.get();
//                }
//            }
//
//            channelIdx++;
//        }


//        return null;
    }

    static HashMap<UUID, PlacedRails> s_placedRails = new HashMap<>();

    // Map of ChunkPos to HashMap...: for each chunk in the world that contains a single rail block/position
    // Map of BlockPos to FetchedRail: individual block position of a rail and the fetched rail it related to
    static HashMap<ChunkPos, HashMap<BlockPos, FetchedRail>> s_cachedFetchedRails = new HashMap<>();
    static boolean hasSetupTick = false;
    static int numChannels = 3;
//    static HashMap<ChunkPo>

    public static PlacedRails getRailsPlacedBy(Player player) {
        if(!s_placedRails.containsKey(player.getUUID())) return null;

        return s_placedRails.get(player.getUUID());
    }

    public static void deleteAllRails(Level level)
    {
        for(var entry : s_placedRails.entrySet()) {
            deleteAllRailsPlacedByPlayer(level, entry.getKey());
        }
    }

    public static void deleteAllRailsPlacedByPlayer(Level level, UUID playerUuid) {
        if(!s_placedRails.containsKey(playerUuid)) return;

        PlacedRails placedRails = s_placedRails.get(playerUuid);

        for(List<RailNode> channel : placedRails.channels)
        {
            for(int i=0; i<channel.size()-1; i++)
            {
                RailNode node = channel.get(i);
                RailNode nextNode = channel.get(i+1);

                Vec3 a = node.position.getCenter();
                Vec3 b = nextNode.position.getCenter();

                BlockGetter.traverseBlocks(a, b, null, (context, bpUnsafe)->{
                    BlockPos bp = new BlockPos(bpUnsafe);

                    if(level.getBlockState(bp).is(Blocks.RAIL)) {
                        level.setBlockAndUpdate(bp, Blocks.AIR.defaultBlockState());

                        ChunkPos chunkPos = new ChunkPos(bp);
                        if(s_cachedFetchedRails.containsKey(chunkPos))
                        {
                            var blocksInChunk = s_cachedFetchedRails.get(chunkPos);
                            if(blocksInChunk != null) {
                                Splatoon.LOGGER.info("AllRemoved cached rail block Position={} chunk={}", bp, chunkPos);
                                blocksInChunk.remove(bp);
                            }
                        }
                    }

                    return null;
                }, (ctx)->{return null;});
            }
        }

        s_placedRails.remove(playerUuid);

    }

    public void tick(MinecraftServer server)
    {
        for(var entry : s_minecarts.entrySet())
        {
            MinecartInstance minecartInstance = entry.getValue();
            minecartInstance.tick();
        }

        for(var entry : s_placedRails.entrySet())
        {
            UUID playerUUID = entry.getKey();

            Player player = server.getPlayerList().getPlayer(playerUUID);
            if(player == null) continue;

            PlacedRails rails = entry.getValue();

            DustParticleOptions yellowDust = new DustParticleOptions(CommonColors.YELLOW, 1);

            for(List<RailNode> channelNodes : rails.channels )
            {
                for(int i=0; i<channelNodes.size()-1; i++)
                {
                    RailNode nodeA = channelNodes.get(i);
                    RailNode nodeB = channelNodes.get(i+1);

                    Vec3 a = nodeA.position.getCenter();
                    Vec3 b = nodeB.position.getCenter();

                    int col = CommonColors.WHITE;
                    if(nodeA.isBroken) {
                        col = CommonColors.RED;
                    }

                    DustParticleOptions dustParticleOptions = new DustParticleOptions(col, 1);

                    ((ServerLevel)player.level()).sendParticles(
                            yellowDust,
                            a.x, a.y, a.z,
                            1, // count
                            0.0, 0.0, 0.0, // delta
                            0.0 // speed
                    );


                    //lineFunc.accept(a, b);
                    float length = (float)a.distanceTo(b);
                    Vec3 dir = b.subtract(a).normalize();


                    for(float j=0; j < length; j += 1.0f) {
                        Vec3 p = a.add(dir.scale(j));

                        if(nodeA.isBroken) {
                            ((ServerLevel)player.level()).sendParticles(
                                    dustParticleOptions,
                                    p.x, p.y, p.z,
                                    1, // count
                                    0.0, 0.0, 0.0, // delta
                                    0.0 // speed
                            );
                        }
                    }
                }
            }


        }
    }

    public void placeRails(Player placedBy , BlockPos bpa, BlockPos bpb)
    {
        Vec3 a = bpa.getCenter();
        Vec3 b = bpb.getCenter();

        BlockGetter.traverseBlocks(a, b, null, (context, bp)->{
            BlockPos bpDown = new BlockPos(bp.getX(), bp.getY() - 1, bp.getZ());

            if(!placedBy.level().getBlockState(bp).is(Blocks.AIR)) {
                return null;
            }
            if(placedBy.level().getBlockState(bpDown).is(Blocks.AIR)) {
                return null;
            }

            BlockPos bpCopy = new BlockPos(bp);

            placedBy.level().setBlockAndUpdate(bpCopy, Blocks.RAIL.defaultBlockState());

            ChunkPos chunkPos = new ChunkPos(bpCopy);
            if(!s_cachedFetchedRails.containsKey(chunkPos))
            {
                s_cachedFetchedRails.put(chunkPos, new HashMap<>());
//                var blocksInChunk = s_cachedFetchedRails.get(chunkPos);
//                if(blocksInChunk == null) {
//                    s_c
//                }
            }


            PlacedRails playerPlacedRails = s_placedRails.get(placedBy.getUUID());
            List<RailNode> currentChannel = playerPlacedRails.channels.get(playerPlacedRails.selectedChannel);

            int nodeIdx = currentChannel.size() - 1;

            FetchedRail fetchedRail = new FetchedRail(playerPlacedRails, playerPlacedRails.selectedChannel, currentChannel.getLast());

            Splatoon.LOGGER.info("Added cached rail block Position={} chunk={}, channel={} size/node={}", bpCopy, chunkPos, playerPlacedRails.selectedChannel, nodeIdx);

            s_cachedFetchedRails.get(chunkPos).put(bpCopy, fetchedRail);


            return null;
        }, (ctx)->{return null;});

        // do a second pass to update the rails to fix most erroneous gaps
        BlockGetter.traverseBlocks(a, b, null, (context, bp)-> {
            BlockState blockHere = placedBy.level().getBlockState(bp);
            if(blockHere.is(Blocks.RAIL)) {

                placedBy.level().neighborChanged(bp, blockHere.getBlock(), null);

//                placedBy.level().sendBlockUpdated(bp, blockHere, blockHere, 3);
//
//                int k = 3 & -34; //?
//                int j = 512;
//                blockHere.updateIndirectNeighbourShapes(placedBy.level(), bp, k, j - 1);
//                blockHere.updateNeighbourShapes(placedBy.level(), bp, k, j - 1);

//                placedBy.level().setBlockAndUpdate()
//                placedBy.level().setBlockAndUpdate(bp, Blocks.RAIL.defaultBlockState());
            }

            return null;
        }, (ctx)->{return null;});
    }

    public static int calculateNumStoredRails(Player player)
    {
        ItemStack itemInHand = player.getItemBySlot(EquipmentSlot.MAINHAND);
        if(itemInHand.is(Items.BARRIER)) {
            return 0;
        }

        int stackCount = itemInHand.getCount();
        return stackCount;
    }

    public boolean plotRailLineFunc(Player player, BlockPos bpa, BlockPos bpb)
    {
        int numStoredRails = calculateNumStoredRails(player);
//        int numStoredRails = 32;

        Vec3 a = bpa.getCenter();
        Vec3 b = bpb.getCenter();

        float length = (float)a.distanceTo(b);
        Vec3 dir = b.subtract(a).normalize();
        boolean isInvalid = false;
        int dustCol = CommonColors.GREEN;

        // minimum length
//        if(length < 2.0) {
//            isInvalid = true;
//            dustCol = CommonColors.RED;
//        }

        ((ServerPlayer)player).connection.send(new ClientboundSetActionBarTextPacket(Component.literal("Plotting Length: ").append(String.valueOf(Math.ceil(length))).append("m")));

        for(float i=0; i < length; i += 0.5f)
        {
            Vec3 p = a.add(dir.scale(i));
            BlockPos bp = Helpers.toBlockPos(p);
            BlockPos bpDown = Helpers.toBlockPos(p.subtract(0, 1, 0));

            if(Math.ceil(i) > numStoredRails) {
                isInvalid = true;
                dustCol = CommonColors.RED;
            }

            if(i > 1) {
                if(!player.level().getBlockState(bp).is(Blocks.AIR)) {
                    isInvalid = true;
                    dustCol = CommonColors.RED;
                }
                if(player.level().getBlockState(bpDown).is(Blocks.AIR)) {
                    isInvalid = true;
                    dustCol = CommonColors.RED;
                }
                if(player.level().getBlockState(bpDown).is(Splatoon.Tags.AUX_BLOCKS)) {
                    isInvalid = true;
                    dustCol = CommonColors.RED;
                }
            }

            DustParticleOptions dustParticleOptions = new DustParticleOptions(dustCol, 1);

            ((ServerLevel)player.level()).sendParticles(
                    dustParticleOptions,
                    p.x, p.y, p.z,
                    1, // count
                    0.0, 0.0, 0.0, // delta
                    10.0 // speed
            );
        }

        return !isInvalid;
    }

    public void onStartHeld(Player player)
    {
        if(!hasSetupTick) {
            Splatoon.LOGGER.info("BuildingBlock first tick setup");
            ServerTickEvents.START_SERVER_TICK.register(this::tick);
            hasSetupTick = true;
        }

        ((ServerPlayer)player).connection.send(new ClientboundSetTitleTextPacket(Component.literal("")));
    }

    public void onEndHeld(Player player)
    {
        ((ServerPlayer)player).connection.send(new ClientboundClearTitlesPacket(true));
    }

    public void whileHeld(Player player)
    {
        UUID playerUUID = player.getUUID();

        if(!s_placedRails.containsKey(playerUUID))
        {
            s_placedRails.put(playerUUID, new PlacedRails(playerUUID));
        }

        if(s_placedRails.containsKey(playerUUID)) {
            PlacedRails rails = s_placedRails.get(playerUUID);

            IPlayerMixin playerMixin = (IPlayerMixin) player;
            if (playerMixin.getItemDroppedThisTick() != null)
            {
                rails.selectedChannel += 1;
                rails.selectedChannel %= numChannels;
                ((ServerPlayer)player).connection.send(new ClientboundSetTitleTextPacket(Component.literal("")));
            }

            MutableComponent channelsSubtitle = Component.literal("");
            for(int i=0; i<numChannels; i++) {
                char status = ' ';
                String selected = "  ";
                ChatFormatting col = ChatFormatting.AQUA;

                if(rails.selectedChannel == i) {
                    selected = "[]";
                    col = ChatFormatting.GREEN;
                }

                List<RailNode> nodes = rails.channels.get(i);
                if(nodes.isEmpty()) col = ChatFormatting.GRAY;
                for(RailNode node : nodes) {
                    if(node.isBroken) {
                        col = ChatFormatting.RED;
                        status = '!';
                        break;
                    }
                }

                MutableComponent channelPart = Component.literal("").withStyle(col);
                channelPart = channelPart.append(" ");
                channelPart = channelPart.append(""+selected.charAt(0));
                channelPart = channelPart.append(""+status);
                channelPart = channelPart.append(String.valueOf(i+1));
                channelPart = channelPart.append(""+status);
                channelPart = channelPart.append(""+selected.charAt(1));
                channelPart = channelPart.append(" ");

                channelsSubtitle = channelsSubtitle.append(channelPart);
            }
            ((ServerPlayer)player).connection.send(new ClientboundSetSubtitleTextPacket(channelsSubtitle));

            if(player.tickCount % Math.round(20*4.5) == 0) {
                ((ServerPlayer)player).connection.send(new ClientboundSetTitleTextPacket(Component.literal("")));
            }

            List<RailNode> nodes = rails.channels.get(rails.selectedChannel);

//            for(int i=0; i<nodes.size()-1; i++)
//            {
//                RailNode nodeA = nodes.get(i);
//                RailNode nodeB = nodes.get(i+1);
//
//                Vec3 a = nodeA.position.getCenter().add(0, 0.2, 0);
//                Vec3 b = nodeB.position.getCenter().add(0, 0.2, 0);
//
//                //lineFunc.accept(a, b);
//                Helpers.particleLine(player.level(), a, b, ParticleTypes.HAPPY_VILLAGER, 0.5f, player);
//            }

            BlockHitResult raycast = (BlockHitResult) player.pick(5.0f, 0.0f, false);

            if(raycast.getType() == HitResult.Type.BLOCK)
            {
                if(!nodes.isEmpty()) {
                    BlockPos lastNode = nodes.getLast().position;
                    BlockPos hitBlock = Helpers.calculateDesiredPlacement(raycast);

                    boolean isValid = plotRailLineFunc(player, lastNode, hitBlock);
                }
            }
        }
    }

    public static boolean railBlockBroken(Player brokenBy, BlockPos blockPos)
    {
        Splatoon.LOGGER.info("Rail block broken");

        FetchedRail fetchedRail = getRailNodeForPosition(blockPos, brokenBy.getUUID());
        if(fetchedRail != null) {
            Splatoon.LOGGER.info("\tIs owned by this player");

            PlacedRails placedRails = fetchedRail.placedRails;
            List<RailNode> channel = placedRails.channels.get(fetchedRail.channelIndex);

            Splatoon.LOGGER.info("\tBroke node index {} in channel {}", fetchedRail.calculateNodeIndex(), fetchedRail.channelIndex);

            int totalNumNodes = channel.size();
            int midPoint = totalNumNodes / 2;
//            RailNode node = channel.get(fetchedRail.channelIndex);

            Splatoon.LOGGER.info("\tTotal num nodes: {}, Midpoint: {}", totalNumNodes, midPoint);

            if(fetchedRail.calculateNodeIndex() < midPoint )
            {
                Splatoon.LOGGER.info("\tRemoving {} nodes from the front...", fetchedRail.calculateNodeIndex() + 1);

                // Remove all from front leading up to this node
                for(int i=0; i <= fetchedRail.calculateNodeIndex(); i++) {
                    if(!channel.isEmpty())
                    {
                        RailNode next = channel.get(1);


                        channel.getFirst().removedCallback(brokenBy, next);
//                        blockBaseLifetime
                        channel.removeFirst();
                    }
                }
            }
            else
            {
                int numToRemove = (totalNumNodes - fetchedRail.calculateNodeIndex()) - 1;
                Splatoon.LOGGER.info("\tRemoving {} nodes from the back...", numToRemove);

                // Remove all from back leading up to this node
                for(int i=0; i < numToRemove; i++) {
                    if(channel.size() == 1)
                    {
                        channel.getLast().removedCallback(brokenBy, null);
                        channel.removeLast();
                    }
                    else if(channel.size() > 1)
                    {
                        RailNode lastNode = channel.getLast();
                        RailNode secondToLastNode = channel.get(channel.size() - 2);
                        secondToLastNode.removedCallback(brokenBy, lastNode);
                        channel.removeLast();
                    }
                }


            }

            if(channel.size() == 1) {
                channel.getLast().removedCallback(brokenBy, null);
                channel.removeLast();
            }


            return true;
        }
        else
        {
            Splatoon.LOGGER.info("\tWasnt owned by this player");
            // cant break rails that arent owned by the player
            brokenBy.level().setBlockAndUpdate(blockPos, Blocks.RAIL.defaultBlockState());
            return false;
        }
    }

    public void onUseBlock(Player player, BlockHitResult hit)
    {
        int numStoredRails = calculateNumStoredRails(player);

        UUID playerUUID = player.getUUID();



        if(player.level().getBlockState(hit.getBlockPos()).is(Blocks.RAIL)) {
            FetchedRail existingRail = getRailNodeForPosition(hit.getBlockPos(), playerUUID);
            if(existingRail != null) {
                List<RailNode> channel = existingRail.placedRails.channels.get(existingRail.channelIndex);
                RailNode node = channel.get(existingRail.calculateNodeIndex());
                RailNode nextNode = channel.get(existingRail.calculateNodeIndex() + 1);

                float length = 1.0f;
                if(nextNode != null) {
                    length = (float)node.position.getCenter().distanceTo(nextNode.position.getCenter());
                }

                if(node.health < 1.0f) {

                    node.changeHealth(player.level(), ((12.0f * 0.015f) / length), nextNode);


                    Vec3 pos = hit.getBlockPos().getCenter();

                    if(node.health >= 1.0f) {
                        player.level().playSound(
                                null,
                                pos.x, pos.y, pos.z,
                                SoundEvents.ANVIL_USE,
                                SoundSource.HOSTILE,
                                3.0f,
                                1.0f
                        );
                    }
                    else
                    {
                        player.level().playSound(
                                null,
                                pos.x, pos.y, pos.z,
                                SoundEvents.ZOMBIE_ATTACK_IRON_DOOR,
                                SoundSource.HOSTILE,
                                1.0f,
                                1.0f
                        );
                    }


                }
            }
        }
        else
        {
            BlockPos placePos = Helpers.calculateDesiredPlacement(hit);

            if(!s_placedRails.containsKey(playerUUID))
            {
                s_placedRails.put(playerUUID, new PlacedRails(playerUUID));
            }

            RailNode lastNode = null;

            PlacedRails placedRails = s_placedRails.get(playerUUID);
            List<RailNode> currentChannel = placedRails.channels.get(placedRails.selectedChannel);

            boolean isValidPlacement = currentChannel.isEmpty();
            if(!isValidPlacement) {
                lastNode = currentChannel.getLast();
                isValidPlacement = plotRailLineFunc(player, lastNode.position, placePos);
            }

            if(player.level().getBlockState(hit.getBlockPos()).is(Splatoon.Tags.AUX_BLOCKS))
            {
                isValidPlacement = false;
            }

            if(isValidPlacement) {
                if(currentChannel.isEmpty())
                {
                    player.level().setBlockAndUpdate(placePos, Blocks.RAIL.defaultBlockState());
                }
                else {
                    placeRails(player, lastNode.position, placePos);

                    float length = (float)lastNode.position.getCenter().distanceTo(placePos.getCenter());
                    int intLength = (int)Math.ceil(length);

                    int newCount = numStoredRails - intLength;
                    player.getItemBySlot(EquipmentSlot.MAINHAND).setCount(Math.clamp(newCount, 1, 99));

                    Ability ability = ((IPlayerMixin)player).getAbility(AbilityManager.AbilityEnum.BuildingRails);
                    if(ability != null) {
                        ability.setCount(Math.clamp(ability.getCount() - intLength, 0, 9999));
                    }
                }

                currentChannel.add(new RailNode(placePos));
            }
        }
    }

    public static class MinecartInstance {
        public MinecartInstance(Player passenger, BlockPos spawnPos, Vec3 dir, List<RailNode> channel, RailNode mountAtNode) {
            this.ridingChannel = channel;
            this.passengerUUID = passenger.getUUID();



            Vec3 nodePos = mountAtNode.position.getCenter();
            Vec3 pos = spawnPos.getCenter();

            Vec3 dirToNode = nodePos.subtract(pos).normalize();

            float dot = (float)dirToNode.dot(dir);

            // if the node we're mounting at is behind us, we're moving forwards
            if(dot < 0.0) {
                this.prevNode = mountAtNode;
                movingDir = 1;
            }
            else // otherwise, we're moving backwards
            {
                int indexOfNode = channel.indexOf(mountAtNode);
                if((indexOfNode + 1) < channel.size()) {
                    this.prevNode = channel.get(indexOfNode + 1);
                }
                else {
                    this.prevNode = mountAtNode;
                }
                movingDir = -1;
            }

            vehicle = EntityType.BLOCK_DISPLAY.create(passenger.level(), EntitySpawnReason.SPAWN_ITEM_USE);
            if(vehicle == null) {
                Splatoon.LOGGER.warn("Failed to create minecart for rails");
                return;
            }

            vehicle.setPos(pos);

            vehicle.lookAt(EntityAnchorArgument.Anchor.EYES, vehicle.position().add(dir));

            passenger.level().addFreshEntity(vehicle);
        }

        public void tick()
        {
            if(vehicle == null) return;

            Entity passenger = vehicle.level().getEntity(passengerUUID);
            if(passenger != null)
            {
                if(passenger instanceof Player player) {
                    Input input = ((IPlayerMixin)player).getInput();
                    if(input.shift()) {
                        Splatoon.LOGGER.info("Minecart ejected by sneak");
                        onEject();
                        return;
                    }
                    if(input.jump()) {
                        Splatoon.LOGGER.info("Minecart ejected by jump");

                        Vec3 launchDir = passenger.getForward();
                        Vec3 launch = launchDir.scale(1.1f);
                        launch = new Vec3(launch.x, Math.clamp(launch.y, 0.0, 0.2), launch.z);

                        player.setDeltaMovement(launch);

                        ((ServerPlayer)player).connection.send(new ClientboundSetEntityMotionPacket(player));

                        onEject();
                        return;
                    }
                }

                if(!passenger.isPassenger()) {
                    passenger.startRiding(vehicle, true);
                }

                if(passenger.tickCount % 20*4 == 0) {
                    Vec3 ppos = passenger.position();
                    passenger.level().playSound(
                            null,
                            ppos.x, ppos.y, ppos.z,
                            SoundEvents.MINECART_RIDING,
                            SoundSource.HOSTILE,
                            1.0f, 1.0f
                    );
                }
            }

            int indexOfPrevNode = ridingChannel.indexOf(prevNode);
            if(indexOfPrevNode == -1) {
                Splatoon.LOGGER.info("Minecart node was removed");
                onEject();
                return;
            }
            int indexOfNextNode = indexOfPrevNode + movingDir;
            if(indexOfNextNode < 0 || indexOfNextNode > (ridingChannel.size()-1)) {
                Splatoon.LOGGER.info("Minecart reached end of route via index");
                onEject();
                return;
            }

            RailNode nextNode = ridingChannel.get(indexOfNextNode);
            if(nextNode == null) {
                Splatoon.LOGGER.info("Minecart reached end of route via null");
                onEject();
                return;
            }

            boolean isBroken = movingDir==1&&prevNode.isBroken;
            isBroken |= movingDir==-1&&nextNode.isBroken;

            if(isBroken) {
                Splatoon.LOGGER.info("Minecart ejected because rail node was broken");
                onEject();
                return;
            }

            vehicle.lookAt(EntityAnchorArgument.Anchor.EYES, nextNode.position.getCenter());

            //vehicle.setDeltaMovement(vehicle.getForward().scale(0.5f));
            vehicle.setPos(vehicle.position().add(vehicle.getForward().scale(0.6)));

            Vec3 pos = vehicle.position();

            if(pos.distanceTo(nextNode.position.getCenter()) < 1.0) {
                vehicle.setPos(nextNode.position.getCenter());
                prevNode = nextNode;
            }


        }

        public void onEject()
        {
            if(vehicle != null) {
                Entity passenger = vehicle.level().getEntity(passengerUUID);
                if(passenger != null) {
                    if (passenger instanceof Player player) {
                        var soundKey = BuiltInRegistries.SOUND_EVENT.getKey(SoundEvents.MINECART_RIDING);
                        ((ServerPlayer)player).connection.send(new ClientboundStopSoundPacket(soundKey, SoundSource.HOSTILE));
                    }
                }

                vehicle.discard();
            }
            vehicle = null;
        }

        UUID passengerUUID;
        Entity vehicle = null;
        List<RailNode> ridingChannel;
        RailNode prevNode = null;
        int movingDir = 0;
    }

    static HashMap<UUID, MinecartInstance> s_minecarts = new HashMap<>();

    public static void playerUseRail(Player player, BlockPos pos) {
        if(player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.CARROT_ON_A_STICK)) return;
        if(CustomItem.BuildingRails.is(player.getItemInHand(InteractionHand.MAIN_HAND))) return;

        FetchedRail fetchedRail = getRailNodeForPosition(pos);
        if(fetchedRail != null) {
            Vec3 fwd = player.getForward();

            if(s_minecarts.containsKey(player.getUUID())) {
                s_minecarts.get(player.getUUID()).onEject();
                s_minecarts.remove(player.getUUID());
            }

            PlacedRails allRails = fetchedRail.placedRails;
            List<RailNode> channel = allRails.channels.get(fetchedRail.channelIndex);

            if(!fetchedRail.node.isBroken)
            {
                Splatoon.LOGGER.info("Creating new minecart instance at {}", pos);

                player.level().playSound(
                        null,
                        pos.getX(), pos.getY(), pos.getZ(),
                        SoundEvents.MINECART_RIDING,
                        SoundSource.HOSTILE,
                        1.0f, 1.0f
                );

                s_minecarts.put(player.getUUID(), new MinecartInstance(player, pos, fwd, channel, fetchedRail.node));
            }



            // go thru all nodes
            // find closest one thats infront
            //

        }
    }
}
