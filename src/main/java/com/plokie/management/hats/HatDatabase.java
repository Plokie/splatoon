package com.plokie.management.hats;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.plokie.Splatoon;
import com.plokie.helpers.CommandBuilder;
import com.plokie.helpers.Helpers;
import com.plokie.helpers.ScheduleEvent;
import com.plokie.management.PlayerStats;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.CommandStorage;
import org.apache.commons.codec.binary.Hex;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Function;

public class HatDatabase extends SavedData {
    public enum Rarity implements StringRepresentable {
        EXCLUSIVE("Exclusive", Blocks.NETHERITE_BLOCK, -1, -1.0f),
        COMMON("Common", Blocks.COAL_BLOCK, 25, 50.0f),
        UNCOMMON("Uncommon", Blocks.WAXED_COPPER_BLOCK, 50, 21.0f),
        RARE("Rare", Blocks.IRON_BLOCK, 75, 18.0f),
        EPIC("Epic", Blocks.GOLD_BLOCK, 150, 9.0f),
        LEGENDARY("Legendary", Blocks.DIAMOND_BLOCK, 300, 2.0f)
        ;

        Rarity(String name, Block block, int value, float weight) {
            this.name = name;
            this.block = block;
            this.value = value;
            this.weight = weight;
        }

        public final String name;
        public final Block block;
        public final int value;
        public final float weight;

        public static Rarity fromOldInt(int rarityIndex) {
            switch(rarityIndex) {
                case -1: return Rarity.EXCLUSIVE;
                case 0: return Rarity.COMMON;
                case 1: return Rarity.UNCOMMON;
                case 2: return Rarity.RARE;
                case 3: return Rarity.EPIC;
                case 4: return Rarity.LEGENDARY;
                default: return Rarity.COMMON;
            }
        }

        public ChatFormatting formatCol() {
            switch(this) {
                case EXCLUSIVE: return ChatFormatting.LIGHT_PURPLE;
                case COMMON: return ChatFormatting.WHITE;
                case UNCOMMON: return ChatFormatting.GREEN;
                case RARE: return ChatFormatting.BLUE;
                case EPIC: return ChatFormatting.GOLD;
                case LEGENDARY: return ChatFormatting.AQUA;
            }

            return ChatFormatting.WHITE;
        }

        public Component formattedName() {
            ChatFormatting col = formatCol();

            return Component.literal(this.name).withStyle(col);
        }

        @Override
        public @NotNull String getSerializedName() {
            return this.toString().toLowerCase();
        }
    }

    public static class Hat {
        public Hat(String name, String id, Rarity rarity)
        {
            this.name = name;
            this.id = id;
            this.rarity = rarity;
        }

        String name;
        String id;
        Rarity rarity;
        List<String> lore = new ArrayList<>();

        public String getName() { return name; }
        public String getId() { return id; }
        public Rarity getRarity() { return rarity; }
        public List<String> getLore() { return lore; }

        public static final Codec<Hat> CODEC = RecordCodecBuilder.create(instance->
                instance.group(
                        Codec.STRING.fieldOf("name").forGetter(Hat::getName),
                        Codec.STRING.fieldOf("id").forGetter(Hat::getId),
                        StringRepresentable.fromEnum(Rarity::values).fieldOf("rarity").forGetter(Hat::getRarity)
                ).apply(instance, Hat::new)
        );

        public static Item hatItem = Items.EMERALD;

        public ItemStack createItemStack()
        {
            ItemStack item = new ItemStack(hatItem);

            item.set(DataComponents.ITEM_MODEL, ResourceLocation.fromNamespaceAndPath(id.split(":")[0], id.split(":")[1]));

            item.set(DataComponents.ITEM_NAME, Component.literal(name).withStyle(rarity.formatCol()));

            List<Component> loreList = new ArrayList<>();
            loreList.add(rarity.formattedName());
            for(String lore : lore)
            {
                loreList.add(Component.literal(lore));
            }

            item.set(DataComponents.LORE, new ItemLore(loreList));

            item.set(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD).build());

            return item;
        }
    }

    public static class PlayerHat {
        public PlayerHat(String id, int count)
        {
            this.id = id;
            this.count = count;
        }

        public final String id;
        public int count = 0;

        public static Codec<PlayerHat> CODEC = RecordCodecBuilder.create(instance->
                instance.group(
                        Codec.STRING.fieldOf("id").forGetter(inst->inst.id),
                        Codec.INT.fieldOf("count").forGetter(inst->inst.count)

                ).apply(instance, PlayerHat::new)
        );
    }

    public HatDatabase()
    {

    }
    public HatDatabase(List<Hat> hats, Map<UUID, List<PlayerHat>> playerHats)
    {
        this.hats.addAll(hats);

        for(var entry : playerHats.entrySet())
        {
            this.playerHats.put(entry.getKey(), new ArrayList<>());
            this.playerHats.get(entry.getKey()).addAll(entry.getValue());
        }

        //this.playerHats.putAll(playerHats);
    }

    List<Hat> hats = new ArrayList<>();
    Map<UUID, List<PlayerHat>> playerHats = new HashMap<>();

    public static final Codec<UUID> UUID_CODEC = Codec.stringResolver(UUID::toString, UUID::fromString);
    public static final Codec<Map<UUID, List<PlayerHat>>> PLAYER_HATS_MAP_CODEC = Codec.unboundedMap(UUID_CODEC, Codec.list(PlayerHat.CODEC));

    public static final Codec<HatDatabase> CODEC = RecordCodecBuilder.create(instance->
            instance.group(
                    Codec.list(Hat.CODEC).fieldOf("hats").forGetter(inst->inst.hats),
                    PLAYER_HATS_MAP_CODEC.optionalFieldOf("player_hats", new HashMap<>()).forGetter(inst->inst.playerHats)
            ).apply(instance, (hats, playerHats)->{
                return new HatDatabase(hats, playerHats);
            })
    );

    public static final SavedDataType<HatDatabase> TYPE = new SavedDataType<>(
            "splatoon_hats",
            HatDatabase::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    public static HatDatabase Instance = null;

    static void save()
    {
        if(Instance == null) return;

        Splatoon.SERVER.overworld().getDataStorage().set(TYPE, Instance);
    }

    static void load()
    {
        Instance = Splatoon.SERVER.overworld().getDataStorage().get(TYPE);
        if(Instance == null) {
            Splatoon.LOGGER.info("Creating new hat database...");
            Instance = new HatDatabase();
        }
    }

    public static List<String> getAllHatIds()
    {
        List<String> ret = new ArrayList<>();
        for(Hat hat : Instance.hats)
        {
            ret.add(hat.id);
        }
        return ret;
    }

    public static Hat getHatById(String hatId)
    {
        for(Hat hat : Instance.hats)
        {
            if(hat.id.equals(hatId)) {
                return hat;
            }
        }
        return null;
    }

    public static void reloadPlayerHats(ServerPlayer player)
    {
        if(!Instance.playerHats.containsKey(player.getUUID())) return;

        player.getInventory().clearOrCountMatchingItems(i->i.is(Hat.hatItem), 64, player.getInventory());

        int maxInvSize = player.getInventory().getContainerSize();

        List<PlayerHat> hats = Instance.playerHats.get(player.getUUID());
        int idx=0;
        for(PlayerHat playerHat : hats)
        {
            Hat hat = getHatById(playerHat.id);
            if(hat != null) {
                ItemStack item = hat.createItemStack();
                item.setCount(playerHat.count);

//                player.getInventory().setItem(maxInvSize - idx, item);
                player.getInventory().setItem(idx + 9, item);


                idx++;
            }
        }
    }

    public static PlayerHat getPlayersHatById(Player player, String hatId)
    {
        if(!Instance.playerHats.containsKey(player.getUUID())) return null;

        for(PlayerHat playerHat : Instance.playerHats.get(player.getUUID()))
        {
            if(playerHat.id.equals(hatId)) return playerHat;
        }

        return null;
    }

    public static boolean giveHat(Player player, String hatId, int count)
    {
        if(!Instance.playerHats.containsKey(player.getUUID())) Instance.playerHats.put(player.getUUID(), new ArrayList<>());

        Hat hat = getHatById(hatId);
        if(hat == null) {
            Splatoon.LOGGER.warn("Attempted to give hat to {}, but {} isnt a valid hat id", player.getName().getString(), hatId);
            return false;
        }

        PlayerHat existingHat = getPlayersHatById(player, hatId);
        if(existingHat != null) {
            existingHat.count += count;

            if(existingHat.count <= 0) {
                Instance.playerHats.get(player.getUUID()).removeIf(ph->ph.id.equals(hatId));
            }
        }
        else
        {
            Instance.playerHats.get(player.getUUID()).add(new PlayerHat(hatId, count));
        }

        reloadPlayerHats((ServerPlayer)player);

        //Instance.playerHats.get(player.getUUID()).add();

        return true;
    }

    public static boolean revokeHat(Player player, String hatId, int count) {
        if(!Instance.playerHats.containsKey(player.getUUID())) return false;

        Hat hat = getHatById(hatId);
        if(hat == null) {
            Splatoon.LOGGER.warn("Attempted to revoke hat from {}, but {} isnt a valid hat id", player.getName().getString(), hatId);
            return false;
        }

        PlayerHat existingHat = getPlayersHatById(player, hatId);
        if(existingHat != null) {
            if(count < 0) {
                existingHat.count = 0;
            }
            else {
                existingHat.count -= count;
            }

            if(existingHat.count <= 0) {
                Instance.playerHats.get(player.getUUID()).removeIf(ph->ph.id.equals(hatId));
                Splatoon.LOGGER.warn("{} has ran out of hat {} after their last ones were revoked", player.getName().getString(), hatId);
            }

            reloadPlayerHats((ServerPlayer) player);

            return true;
        }
        else return false;
    }

    public static List<Hat> getHatsOfRarity(Rarity rarity)
    {
        List<Hat> ret = new ArrayList<>();

        for(Hat hat : Instance.hats)
        {
            if(hat.rarity == rarity) ret.add(hat);
        }
        return ret;
    }

    public static List<Hat> getAllHats()
    {
        return Instance.hats;
    }

    public static void setup()
    {
        CommandBuilder.command("hats").subcommand("admin").subcommand("database").subcommand("reload").executes(ctx->{
            load();
            return "Reloading hat database";
        }).register();

        CommandBuilder.command("hats").subcommand("admin").subcommand("database").subcommand("migrate_hats").executes(ctx->{
            CommandStorage commandStorage = Splatoon.SERVER.getCommandStorage();
            ResourceLocation oldStorageId = ResourceLocation.fromNamespaceAndPath("splatoon", "hats");
            CompoundTag oldStorage = commandStorage.get(oldStorageId);

            ListTag oldHats = oldStorage.getListOrEmpty("hats");

            Instance.hats.clear();

            Splatoon.LOGGER.info("Found {} old hats", oldHats.size());

            for(int hatIdx=0; hatIdx < oldHats.size(); hatIdx++)
            {
                Optional<CompoundTag> oldHat = oldHats.getCompound(hatIdx);
                if(oldHat.isPresent())
                {
                    Optional<String> name = oldHat.get().getString("name");
                    Optional<String> id = oldHat.get().getString("id");
                    Optional<Integer> rarity = oldHat.get().getInt("rarity");

                    if(name.isPresent() && id.isPresent() && rarity.isPresent())
                    {
                        Splatoon.LOGGER.info("Found hat {} {} {}", name.get(), id.get(), rarity.get());

                        Hat hat = new Hat(name.get(), id.get(), Rarity.fromOldInt(rarity.get()));
                        Instance.hats.add(hat);
                    }
                    else
                    {
                        if(name.isPresent()) Splatoon.LOGGER.info("\t! Missing name");
                        if(id.isPresent()) Splatoon.LOGGER.info("\t! Missing id");
                        if(rarity.isPresent()) Splatoon.LOGGER.info("\t! Missing rarity");
                    }
                }
            }

            return "";
        }).register();

        CommandBuilder.command("hats").subcommand("admin").subcommand("database").subcommand("migrate_players").executes(ctx->{
            CommandStorage commandStorage = Splatoon.SERVER.getCommandStorage();
            ResourceLocation oldStorageId = ResourceLocation.fromNamespaceAndPath("emerald", "vars");
            CompoundTag oldStorage = commandStorage.get(oldStorageId);

            ListTag players = oldStorage.getListOrEmpty("players");

            Instance.playerHats.clear();

            Splatoon.LOGGER.info("Found {} old players", players.size());

            for(int i=0; i<players.size(); i++)
            {
                Optional<CompoundTag> playerData = players.getCompound(i);
                if(playerData.isPresent()) {
                    Optional<int[]> uuidInts = playerData.get().getIntArray("UUID");

                    if(uuidInts.isPresent())
                    {
                        if(uuidInts.get().length != 4) {
                            Splatoon.LOGGER.warn("UUID ints didnt have 4 integers");
                            continue;
                        }

                        byte[] bytes = ByteBuffer.allocate(4*4)
                                .putInt(uuidInts.get()[0])
                                .putInt(uuidInts.get()[1])
                                .putInt(uuidInts.get()[2])
                                .putInt(uuidInts.get()[3])
                                .array();
                        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);

                        long high = byteBuffer.getLong();
                        long low = byteBuffer.getLong();

                        UUID uuid = new UUID(high, low);
                        Splatoon.LOGGER.info("player uuid {}", uuid.toString());

                        Player isOnline = Splatoon.SERVER.getPlayerList().getPlayer(uuid);
                        if(isOnline != null)
                        {
                            Splatoon.LOGGER.info("\tis online player {}", isOnline.getName().getString());
                        }

                        ListTag hats = playerData.get().getListOrEmpty("m_hats");
                        Splatoon.LOGGER.info("\thas {} hats", hats.size());

                        for(int hatIdx=0; hatIdx<hats.size(); hatIdx++)
                        {
                            Optional<CompoundTag> hat = hats.getCompound(hatIdx);
                            if(hat.isPresent())
                            {
                                Optional<Integer> count = hat.get().getInt("count");
                                Optional<String> id = hat.get().getString("id");

                                if(count.isPresent() && id.isPresent())
                                {
                                    Splatoon.LOGGER.info("\tFound hat {} {}", id.get(), count.get());

                                    PlayerHat playerHat = new PlayerHat(id.get(), count.get());
                                    if(!Instance.playerHats.containsKey(uuid))
                                    {
                                        Instance.playerHats.put(uuid, new ArrayList<>());
                                    }

                                    Instance.playerHats.get(uuid).add(playerHat);

                                    //this.playerHats.put(uuid, )
                                }
                                else
                                {
                                    if(count.isPresent()) Splatoon.LOGGER.info("\t\t! Missing count");
                                    if(id.isPresent()) Splatoon.LOGGER.info("\t\t! Missing id");
                                }
                            }
                        }
                    }
                }
            }

            return "";
        }).register();

        Function<UUID, MutableComponent> constructHatsMessage = (uuid)->{
            if(uuid == null) return Component.literal("Invalid UUID");

            if(Instance.playerHats.containsKey(uuid))
            {
                List<PlayerHat> hats = new ArrayList<>(Instance.playerHats.get(uuid));
                if(hats.isEmpty()) {
                    return Component.literal("No hats!");
                }
                else
                {
                    hats.sort((a, b)->{
                        Hat hatA = getHatById(a.id);
                        Hat hatB = getHatById(b.id);
                        if(hatA == null || hatB == null) return 0;
                        if(hatB.rarity == Rarity.EXCLUSIVE) return 1;
                        if(hatA.rarity == Rarity.EXCLUSIVE) return -1;
                        return hatB.rarity.value - hatA.rarity.value;
                    });

                    MutableComponent message = Component.literal("Hats:\n");
                    for(PlayerHat playerHat : hats)
                    {
                        Hat hat = getHatById(playerHat.id);
                        if(hat == null) {
                            message = message.append("? Unknown hat: ");
                            message = message.append(playerHat.id);
                        }
                        else
                        {
                            message = message.append(Component.literal(hat.getName()).withStyle(hat.getRarity().formatCol() )  );
                            message = message.append(Component.literal(" (x").withStyle(hat.getRarity().formatCol() ) );
                            message = message.append(Component.literal(String.valueOf(playerHat.count)).withStyle(hat.getRarity().formatCol() ) );
                            message = message.append(Component.literal(") - ").withStyle(hat.getRarity().formatCol() ) );
                            message = message.append(hat.getRarity().formattedName());
                        }

                        message = message.append("\n");
                    }

                    return message;
                }
            }
            else
            {
                return Component.literal("No hats!");
            }
        };

        CommandBuilder.command("hats").subcommand("admin").subcommand("database").subcommand("list").executes(ctx->{
            ScheduleEvent.schedule(1, server->{
                MutableComponent message = Component.literal("Hats:\n");
                for(Hat hat : Instance.hats)
                {
                    message = message.append(hat.id).withStyle(hat.rarity.formatCol());
                    message = message.append(", ");
                    message = message.append(hat.name);
                    message = message.append(", ");
                    message = message.append(hat.rarity.formattedName());
                    message = message.append("\n");
                }

                ctx.getStack().getSource().sendSystemMessage(message);
            });

            return "...";
        }).register();


        CommandBuilder.command("hats").subcommand("list").argumentPlayer("target").executes(ctx->{
            var ref = new Object() {
                ServerPlayer target;
            };

            try {
                ref.target = ctx.getArgumentPlayer("target");

            } catch (Exception e) {
                try {
                    ref.target = ctx.getStack().getSource().getPlayerOrException();
                } catch (CommandSyntaxException ex) {
                    return "! Command must be executed by a player to view own hats";
                }
            }

            ScheduleEvent.schedule(1, server->{
                MutableComponent message = constructHatsMessage.apply(ref.target.getUUID());
                ctx.getStack().getSource().sendSystemMessage(message);
            });


            return "Getting hats...";
        }).permission(stack->true).register();

        CommandBuilder.command("hats").subcommand("sort").subcommand("rarity_descending").executes(ctx->{
            ServerPlayer caller = ctx.getStack().getSource().getPlayer();
            if(caller == null) return "! This command must be called by a player";

            if(!Instance.playerHats.containsKey(caller.getUUID())) return "! You dont have any hats";

            List<PlayerHat> hats = Instance.playerHats.get(caller.getUUID());

            hats.sort((a, b)->{
                Hat hatA = getHatById(a.id);
                Hat hatB = getHatById(b.id);
                if(hatA == null || hatB == null) return 0;
                if(hatB.rarity == Rarity.EXCLUSIVE) return 1;
                if(hatA.rarity == Rarity.EXCLUSIVE) return -1;
                return hatB.rarity.value - hatA.rarity.value;
            });

            reloadPlayerHats(caller);

            return "Sorted hats...";
        }).permission(stack->true).register();

        CommandBuilder.command("hats").subcommand("sort").subcommand("rarity_ascending").executes(ctx->{
            ServerPlayer caller = ctx.getStack().getSource().getPlayer();
            if(caller == null) return "! This command must be called by a player";

            if(!Instance.playerHats.containsKey(caller.getUUID())) return "! You dont have any hats";

            List<PlayerHat> hats = Instance.playerHats.get(caller.getUUID());

            hats.sort((a, b)->{
                Hat hatA = getHatById(a.id);
                Hat hatB = getHatById(b.id);
                if(hatA == null || hatB == null) return 0;
                if(hatB.rarity == Rarity.EXCLUSIVE) return -1;
                if(hatA.rarity == Rarity.EXCLUSIVE) return 1;
                return hatA.rarity.value - hatB.rarity.value;
            });

            reloadPlayerHats(caller);

            return "Sorted hats...";
        }).permission(stack->true).register();

        CommandBuilder.command("hats").subcommand("sort").subcommand("alphabetically").executes(ctx->{
            ServerPlayer caller = ctx.getStack().getSource().getPlayer();
            if(caller == null) return "! This command must be called by a player";

            if(!Instance.playerHats.containsKey(caller.getUUID())) return "! You dont have any hats";

            List<PlayerHat> hats = Instance.playerHats.get(caller.getUUID());

            hats.sort((a, b)->{
                Hat hatA = getHatById(a.id);
                Hat hatB = getHatById(b.id);
                if(hatA == null || hatB == null) return 0;
                return hatA.getName().compareTo(hatB.getName());
            });

            reloadPlayerHats(caller);

            return "Sorted hats...";
        }).permission(stack->true).register();

        CommandBuilder.command("hats").subcommand("reload").executes(ctx->{
            ServerPlayer caller = ctx.getStack().getSource().getPlayer();
            if(caller == null) return "! This command must be called by a player";

            reloadPlayerHats(caller);

            return "Reloading hats...";

        }).permission(stack->true).register();

        CommandBuilder.command("hats").subcommand("admin").subcommand("give").argumentPlayer("target").argumentResourceLocation("hat_id", HatDatabase::getAllHatIds).argumentInteger("count").executes(ctx->{
            int count = 1;
            try {
                count = ctx.getArgumentInteger("count");
            } catch (Exception ignored) {}

            String hatId = ctx.getArgumentResourceLocation("hat_id").toString();
            Hat hat = getHatById(hatId);
            if(hat==null) {
                return "! Invalid hat ID";
            }

            try {
                ServerPlayer target = ctx.getArgumentPlayer("target");

                giveHat(target, hatId, count);

                return "Gave " + target.getName().getString() + " " + count + " " + hatId + " hat(s)";
            } catch (CommandSyntaxException e) {
                return "! Invalid player target";
            }
        }).register();

        CommandBuilder.command("hats").subcommand("admin").subcommand("revoke").argumentPlayer("target").argumentResourceLocation("hat_id", HatDatabase::getAllHatIds).argumentInteger("count").executes(ctx->{
            int count = -1;
            try {
                count = ctx.getArgumentInteger("count");
            } catch (Exception ignored) {}

            String hatId = ctx.getArgumentResourceLocation("hat_id").toString();
            Hat hat = getHatById(hatId);
            if(hat==null) {
                return "! Invalid hat ID";
            }

            try {
                ServerPlayer target = ctx.getArgumentPlayer("target");

                revokeHat(target, hatId, count);

                return "Revoked " + count + " " + hatId + " hat(s) from " + target.getName().getString();
            } catch (CommandSyntaxException e) {
                return "! Invalid player target";
            }
        }).register();

        CommandBuilder.command("hats").subcommand("quicksell_hand").executes(ctx->{
            ServerPlayer caller = ctx.getStack().getSource().getPlayer();
            if(caller == null) return "! This command must be called by a player";

            ItemStack itemInHand = caller.getItemInHand(InteractionHand.MAIN_HAND);
            ResourceLocation model = itemInHand.get(DataComponents.ITEM_MODEL);
            if(model == null) {
                return "! Not holding a hat";
            }

            Hat hat = getHatById(model.toString());
            if(hat == null) {
                return "! Not holding a hat";
            }

            if(hat.rarity == Rarity.EXCLUSIVE) {
                return "! Cannot sell exclusive hats";
            }

            int value = hat.rarity.value;

            if(revokeHat(caller, hat.id, 1)) {
                PlayerStats.get(caller).forceAddNoMatch(PlayerStats.MONEY, value);
            }

            return "Sold 1x " + hat.name;
        }).permission(stack->true).register();

        CommandBuilder.command("hats")
                .subcommand("admin")
                .subcommand("database")
                .subcommand("create_new_hat")
                .argumentResourceLocation("hat_id")
                .argumentString("name")
                .argumentEnum("rarity", Rarity::values)
                .executes(ctx->
        {
            try {
                String hatId = ctx.getArgumentResourceLocation("hat_id").toString();
                String name = ctx.getArgumentString("name");
                Rarity rarity = ctx.getArgumentEnum("rarity", Rarity.class);

                Splatoon.LOGGER.info("Create hat {} {} {}", hatId, name, rarity);

                Hat newHat = new Hat(name, hatId, rarity);
                Instance.hats.add(newHat);
                save();

                return "Added new hat! Saving...";
            }
            catch(IllegalArgumentException e) {
                return "! Invalid rarity";
            }
        }).register();

        CommandBuilder.command("hats")
                .subcommand("admin")
                .subcommand("database")
                .subcommand("delete_hat")
                .argumentResourceLocation("hat_id", HatDatabase::getAllHatIds)
                .executes(ctx->
        {
            String hatId = ctx.getArgumentResourceLocation("hat_id").toString();

            Splatoon.LOGGER.info("Delete hat {}", hatId);

            Instance.hats.removeIf(hat->hat.id.equals(hatId));

            return "Deleted hat " + hatId;
        }).register();

        CommandBuilder.command("hats")
                .subcommand("admin")
                .subcommand("database")
                .subcommand("modify_hat")
                .argumentResourceLocation("hat_id", HatDatabase::getAllHatIds)
                .subcommand("name")
                .argumentString("name")
                .executes(ctx->
        {
            String hatId = ctx.getArgumentResourceLocation("hat_id").toString();
            String newName = ctx.getArgumentString("name");

            Splatoon.LOGGER.info("Rename hat {} to {}", hatId, newName);

            Instance.hats.forEach(hat->{
                if(hat.id.equals(hatId)) {
                    hat.name = newName;
                }
            });

            //Instance.hats.removeIf(hat->hat.id.equals(hatId));

            return "Renamed hat " + hatId + " to " + newName;
        }).register();

        CommandBuilder.command("hats")
                .subcommand("admin")
                .subcommand("database")
                .subcommand("modify_hat")
                .argumentResourceLocation("hat_id", HatDatabase::getAllHatIds)
                .subcommand("rarity")
                .argumentEnum("rarity", Rarity::values)
                .executes(ctx->
        {
            try {
                String hatId = ctx.getArgumentResourceLocation("hat_id").toString();
                Rarity rarity = ctx.getArgumentEnum("rarity", Rarity.class);

                Splatoon.LOGGER.info("Change hat rarity {} to {}", hatId, rarity);

                Instance.hats.forEach(hat->{
                    if(hat.id.equals(hatId)) {
                        hat.rarity = rarity;
                    }
                });

                return "Changed hat " + hatId + " rarity to " + rarity;
            }
            catch(IllegalArgumentException e)
            {
                return "! Invalid rarity";
            }
        }).register();

        CommandBuilder.command("hats")
                .subcommand("admin")
                .subcommand("database")
                .subcommand("modify_hat")
                .argumentResourceLocation("hat_id", HatDatabase::getAllHatIds)
                .subcommand("lore")
                .subcommand("add")
                .argumentString("lore")
                .executes(ctx->
        {

            String hatId = ctx.getArgumentResourceLocation("hat_id").toString();
            String lore = ctx.getArgumentString("lore");

            Splatoon.LOGGER.info("Add hat lore {}: {}", hatId, lore);

            Instance.hats.forEach(hat->{
                if(hat.id.equals(hatId)) {
                    hat.lore.add(lore);
                }
            });

            return "Added hat " + hatId + " lore " + lore;
        }).register();

        CommandBuilder.command("hats")
                .subcommand("admin")
                .subcommand("database")
                .subcommand("modify_hat")
                .argumentResourceLocation("hat_id", HatDatabase::getAllHatIds)
                .subcommand("lore")
                .subcommand("set")
                .argumentInteger("lore_index")
                .argumentString("lore")
                .executes(ctx->
        {

            String hatId = ctx.getArgumentResourceLocation("hat_id").toString();
            String lore = ctx.getArgumentString("lore");
            int loreIndex = ctx.getArgumentInteger("lore_index");

            Splatoon.LOGGER.info("Change hat lore {} {}: {}", loreIndex, hatId, lore);

            Instance.hats.forEach(hat->{
                if(hat.id.equals(hatId)) {
                    if(loreIndex < hat.lore.size()) {
                        hat.lore.set(loreIndex, lore);
                    }
                }
            });

            return "Set hat " + hatId + " lore index " + loreIndex +" to " + lore;
        }).register();

        CommandBuilder.command("hats")
                .subcommand("admin")
                .subcommand("database")
                .subcommand("modify_hat")
                .argumentResourceLocation("hat_id", HatDatabase::getAllHatIds)
                .subcommand("lore")
                .subcommand("remove")
                .argumentInteger("lore_index")
                .executes(ctx->
        {

            String hatId = ctx.getArgumentResourceLocation("hat_id").toString();
            int loreIndex = ctx.getArgumentInteger("lore_index");

            Splatoon.LOGGER.info("Remove hat lore {} {}", loreIndex, hatId);

            Instance.hats.forEach(hat->{
                if(hat.id.equals(hatId)) {
                    if(loreIndex == -1) {
                        if(!hat.lore.isEmpty())
                        {
                            hat.lore.removeLast();
                        }
                    }
                    else
                    {
                        if(loreIndex < hat.lore.size()) {
                            hat.lore.remove(loreIndex);
                        }
                    }

                }
            });

            return "Removed hat " + hatId + " lore index " + loreIndex;
        }).register();

        ServerLifecycleEvents.SERVER_STARTED.register(server->{
            load();
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server->{
            save();
        });

        ServerLifecycleEvents.AFTER_SAVE.register((server, a, b)->{
            save();
        });
    }
}
