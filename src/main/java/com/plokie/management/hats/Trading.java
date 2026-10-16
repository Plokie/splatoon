package com.plokie.management.hats;

import com.plokie.Splatoon;
import com.plokie.dialog.DialogBuilder;
import com.plokie.helpers.CommandBuilder;
import com.plokie.helpers.ScheduleEvent;
import com.plokie.management.PlayerStats;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.common.ClientboundClearDialogPacket;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.CommonColors;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.player.Player;

import java.util.*;
import java.util.List;

public class Trading {

    record IncomingRequest(UUID fromPlayer, UUID toPlayer)
    {

    }

    class Offer {
        int money = 0;
        List<Tuple<HatDatabase.Hat, Integer>> hats = new ArrayList<>();

        Tuple<HatDatabase.Hat, Integer> findHatOffer(String hatId) {
            for(var tuple : hats) {
                if(tuple.getA().id.equals(hatId)) {
                    return tuple;
                }
            }
            return null;
        }
    }

    class OngoingTrade
    {
        public OngoingTrade(Player player1, Player player2)
        {
            this.player1 = player1.getUUID();
            this.player2 = player2.getUUID();
            this.offer1 = new Offer();
            this.offer2 = new Offer();
            this.requestFrom1 = new Offer();
            this.requestFrom2 = new Offer();
        }
        UUID player1;
        UUID player2;

        Offer offer1;
        Offer offer2;

        Offer requestFrom1;
        Offer requestFrom2;

        boolean player1accepted = false;
        boolean player2accepted = false;

        boolean getPlayerAccepted(UUID playerUUID)
        {
            if(player1.equals(playerUUID)) return player1accepted;
            if(player2.equals(playerUUID)) return player2accepted;
            throw new IllegalArgumentException("Player UUID passed into getPlayerAccepted must be either player1 or player2");
        }

        void setPlayerAccepted(UUID playerUUID, boolean value)
        {
            if(player1.equals(playerUUID)) player1accepted = value;
            else if(player2.equals(playerUUID)) player2accepted = value;
            else throw new IllegalArgumentException("Player UUID passed into setPlayerAccepted must be either player1 or player2");
        }

        Offer getOfferFor(UUID playerUUID)
        {
//            Splatoon.LOGGER.info("Check for offer {} agianst {} and {}", playerUUID, player1, player2);
            if(player1.equals(playerUUID)) return offer1;
            if(player2.equals(playerUUID)) return offer2;
            throw new IllegalArgumentException("Player UUID passed into getOfferFor must be either player1 or player2");
        }

        Offer getRequestFrom(UUID playerUUID)
        {
            if(player1.equals(playerUUID)) return requestFrom1;
            if(player2.equals(playerUUID)) return requestFrom2;
            throw new IllegalArgumentException("Player UUID passed into getRequestFrom must be either player1 or player2");
        }

        void setRequestFrom(UUID playerUUID, Offer newRequest)
        {
            if(player1.equals(playerUUID)) requestFrom1 = newRequest;
            else if(player2.equals(playerUUID)) requestFrom2 = newRequest;
            else throw new IllegalArgumentException("Player UUID passed into setRequestFrom must be either player1 or player2");
        }

        Player getOtherPlayer(UUID whoIsntThis)
        {
            UUID otherUUID = player1;
            if(otherUUID.equals(whoIsntThis)) otherUUID = player2;
            return Splatoon.SERVER.getPlayerList().getPlayer(otherUUID);
        }

        void refreshMenus()
        {
            UUID[] players = {player1, player2};
            for(UUID playerUUID : players)
            {
                if(!isOnHomeMenu.containsKey(playerUUID)) continue;

                if(isOnHomeMenu.get(playerUUID))
                {
                    UUID otherUUID = player1;
                    if(otherUUID.equals(playerUUID)) otherUUID = player2;

                    ServerPlayer player = Splatoon.SERVER.getPlayerList().getPlayer(playerUUID);
                    ServerPlayer otherPlayer = Splatoon.SERVER.getPlayerList().getPlayer(otherUUID);

                    if(player == null)
                    {
                        stopOngoingTrade(playerUUID);
                        Splatoon.LOGGER.warn("This player {} must have logged off", playerUUID);
                        continue;
                    }

                    if(otherPlayer == null)
                    {
                        stopOngoingTrade(playerUUID);
                        Splatoon.LOGGER.warn("Other player {} must have logged off", playerUUID);
                        continue;
                    }

                    player.connection.send(ClientboundClearDialogPacket.INSTANCE);

                    ScheduleEvent.schedule(1, (server)->{
                        DialogBuilder.send(createBaseMenuDialog(player, otherPlayer), player);
                    });

                }
            }
        }
    }


    static Map<UUID, IncomingRequest> incomingRequests = new HashMap<>();

    List<OngoingTrade> ongoingTrades = new ArrayList<>();

    Map<UUID, Boolean> isOnHomeMenu = new HashMap<>();

    static void createIncomingRequest(Player fromPlayer, Player toPlayer)
    {
        incomingRequests.put(toPlayer.getUUID(), new IncomingRequest(fromPlayer.getUUID(), toPlayer.getUUID()));

        Style acceptLink = Style.EMPTY;
        acceptLink = acceptLink.withClickEvent(new ClickEvent.RunCommand("/traderequest accept"));
        acceptLink = acceptLink.withColor(CommonColors.GREEN);
        acceptLink = acceptLink.withBold(true);

        MutableComponent message = Component.literal("==============================\n");
        message = message.append(Component.literal("Incoming trade request!\n"));
        message = message.append(Component.literal("From ").append(fromPlayer.getDisplayName()).append(Component.literal("\n")) );
        message = message.append(Component.literal("Click here to accept\n").setStyle(acceptLink));
        message = message.append(Component.literal("(Or do [/traderequest accept])\n"));
        message = message.append(Component.literal("==============================\n"));

        ((ServerPlayer)toPlayer).sendSystemMessage(message);

        // just so i can see it while debugging
//        ((ServerPlayer)fromPlayer).sendSystemMessage(message);
    }

    public Trading()
    {
        CommandBuilder.command("traderequest").subcommand("accept").executes(ctx->{
            ServerPlayer caller = ctx.getStack().getSource().getPlayer();
            if(caller == null) return "! This function must be called by a player";

            if(!incomingRequests.containsKey(caller.getUUID())) return "! You have no incoming trade requests";

            Player requestFromPlayer = Splatoon.SERVER.getPlayerList().getPlayer(incomingRequests.get(caller.getUUID()).fromPlayer);
            if(requestFromPlayer == null) {
                return "! This player is no longer online";
            }

            Splatoon.LOGGER.info("Found trade offer from {}", requestFromPlayer.getName().getString());

            incomingRequests.remove(caller.getUUID());
            startOngoingTrade(requestFromPlayer, caller);

            return "Starting trade...";
        }).permission(ctx->true).register();
    }

    void setIsPlayerOnHomeMenu(UUID player, boolean value)
    {
        isOnHomeMenu.put(player, value);
    }

    void setIsPlayerOnHomeMenu(Player player, boolean value)
    {
        setIsPlayerOnHomeMenu(player.getUUID(), value);
    }

    void stopOngoingTrade(UUID eitherPlayer)
    {
        Splatoon.LOGGER.info("Stopping trade for {}", eitherPlayer);

        for(OngoingTrade ongoingTrade : ongoingTrades)
        {
            if(ongoingTrade.player1.equals(eitherPlayer) || ongoingTrade.player2.equals(eitherPlayer))
            {
                ongoingTrades.remove(ongoingTrade);

                setIsPlayerOnHomeMenu(ongoingTrade.player1, false);
                setIsPlayerOnHomeMenu(ongoingTrade.player2, false);

                ServerPlayer player1 = Splatoon.SERVER.getPlayerList().getPlayer(ongoingTrade.player1);
                if(player1 != null) {
                    player1.connection.send(ClientboundClearDialogPacket.INSTANCE);
                }

                ServerPlayer player2 = Splatoon.SERVER.getPlayerList().getPlayer(ongoingTrade.player2);
                if(player2 != null) {
                    player2.connection.send(ClientboundClearDialogPacket.INSTANCE);
                }

                return;
            }
        }
    }

    void stopOngoingTrade(Player eitherPlayer)
    {
        stopOngoingTrade(eitherPlayer.getUUID());
    }

    OngoingTrade getOngoingTrade(UUID eitherPlayer)
    {
        for(OngoingTrade ongoingTrade : ongoingTrades)
        {
            if(ongoingTrade.player1.equals(eitherPlayer) || ongoingTrade.player2.equals(eitherPlayer))
            {
                return ongoingTrade;
            }
        }
        return null;
    }

    OngoingTrade getOngoingTrade(Player eitherPlayer)
    {
        return getOngoingTrade(eitherPlayer.getUUID());
    }

    void startOngoingTrade(Player player1, Player player2)
    {
        OngoingTrade ongoingTrade = new OngoingTrade(player1, player2);

        ongoingTrades.add(ongoingTrade);

        DialogBuilder.send(createBaseMenuDialog(player1, player2), player1);
        DialogBuilder.send(createBaseMenuDialog(player2, player1), player2);

        setIsPlayerOnHomeMenu(player1, true);
        setIsPlayerOnHomeMenu(player2, true);
    }

    OngoingTrade getOngoingTradeFromTag(Tag tag)
    {
        CompoundTag compoundTag = tag.asCompound().orElse(null);
        if(compoundTag == null) return null;

        String thisPlayerUUIDString = compoundTag.getStringOr("thisPlayer", "");
        if(thisPlayerUUIDString.isEmpty()) return null;

        UUID uuid = UUID.fromString(thisPlayerUUIDString);

        return getOngoingTrade(uuid);
    }

    void closeBaseMenuAction(Player player, Tag tag)
    {
        OngoingTrade ongoingTrade = getOngoingTradeFromTag(tag);

        if(ongoingTrade != null)
        {
            stopOngoingTrade(player);
//            ongoingTrades.remove(ongoingTrade);
        }
    }

    void submitBaseMenuAction(Player player, Tag tag)
    {
        OngoingTrade ongoingTrade = getOngoingTradeFromTag(tag);

        if(ongoingTrade != null)
        {
            stopOngoingTrade(player);
//            ongoingTrades.remove(ongoingTrade);
        }
    }

    void closeSubMenuAction(Player player, Tag tag)
    {
        reloadBaseMenuAction(player, tag);
    }

    void reloadBaseMenuAction(Player player, Tag tag)
    {
        OngoingTrade ongoingTrade = getOngoingTradeFromTag(tag);

        UUID thisPlayerUUID = player.getUUID();

        UUID otherPlayerUUID = ongoingTrade.player1;
        if(otherPlayerUUID.equals(thisPlayerUUID)) {
            otherPlayerUUID = ongoingTrade.player2;
        }

        Player otherPlayer = Splatoon.SERVER.getPlayerList().getPlayer(otherPlayerUUID);
        if(otherPlayer == null) {
            stopOngoingTrade(player);
            return;
        }

        DialogBuilder.send(createBaseMenuDialog(player, otherPlayer), player);

        setIsPlayerOnHomeMenu(player, true);
    }

    void removeHatFromOfferAction(Player player, Tag tag, int count)
    {
        OngoingTrade ongoingTrade = getOngoingTradeFromTag(tag);

        CompoundTag compoundTag = tag.asCompound().orElse(new CompoundTag());

        String hatId = compoundTag.getStringOr("hat_id", "");
        HatDatabase.Hat hat = HatDatabase.getHatById(hatId);
        if(hat == null) {
            Splatoon.LOGGER.error("(Removal) Couldnt find create pick hat dialog hat id: '{}'", hatId);
            stopOngoingTrade(player);
            return;
        }

        HatDatabase.PlayerHat playerHat = HatDatabase.getPlayersHatById(player, hatId);
        if(playerHat == null) {
            Splatoon.LOGGER.error("(Removal) Couldnt find player hat belonging to player: '{}'", hatId);
            stopOngoingTrade(player);
            return;
        }

        Offer offer = ongoingTrade.getOfferFor(player.getUUID());
        var offerTuple = offer.findHatOffer(hatId);

        int amountInOffer = offerTuple.getB();
        int amountInInv = playerHat.count;

        if(count < 0) count = amountInOffer;

        int newCount = amountInOffer - count;

        offerTuple.setB(newCount);

        if(newCount <= 0)
        {
            offer.hats.remove(offerTuple);
        }

        ongoingTrade.refreshMenus();
    }

    void addHatToOfferAction(Player player, Tag tag, int count)
    {
        OngoingTrade ongoingTrade = getOngoingTradeFromTag(tag);

        CompoundTag compoundTag = tag.asCompound().orElse(new CompoundTag());

        String hatId = compoundTag.getStringOr("hat_id", "");
        HatDatabase.Hat hat = HatDatabase.getHatById(hatId);
        if(hat == null) {
            Splatoon.LOGGER.error("Couldnt find create pick hat dialog hat id: '{}'", hatId);
            stopOngoingTrade(player);
            return;
        }

        HatDatabase.PlayerHat playerHat = HatDatabase.getPlayersHatById(player, hatId);
        if(playerHat == null) {
            Splatoon.LOGGER.error("Couldnt find player hat belonging to player: '{}'", hatId);
            stopOngoingTrade(player);
            return;
        }

        if(count < 0) count = playerHat.count;
        if(count <= 0) return;

        Offer offer = ongoingTrade.getOfferFor(player.getUUID());

        boolean didAlreadyHaveHat = false;
        for(var hatIntTuple : offer.hats)
        {
            if(hatIntTuple.getA().id.equals(hatId))
            {
                int newCount = hatIntTuple.getB() + count;

                hatIntTuple.setB(Math.min(newCount, playerHat.count));

                didAlreadyHaveHat = true;
                break;
            }
        }

        if(!didAlreadyHaveHat) {
            offer.hats.add(new Tuple<>(hat, count));
        }


        ongoingTrade.refreshMenus();
    }

    void openPickHatDialog(Player player, Tag tag)
    {
        Dialog dialog = createPickHatDialog(player, tag);
        setIsPlayerOnHomeMenu(player, false);
        DialogBuilder.send(dialog, player);
    }

    Dialog createPickHatDialog(Player player, Tag tag)
    {
        CompoundTag compoundTag = tag.asCompound().orElse(new CompoundTag());

        DialogBuilder dialogBuilder = DialogBuilder.createDialogMultiAction("trade_pick_hat", 1)
                .title("Add hat to offer");

        String hatId = compoundTag.getStringOr("hat_id", "");
        HatDatabase.Hat hat = HatDatabase.getHatById(hatId);
        if(hat == null) {
            Splatoon.LOGGER.error("Couldnt find create pick hat dialog hat id: '{}'", hatId);
            stopOngoingTrade(player);
            return dialogBuilder.buildWithoutSending();
        }

        HatDatabase.PlayerHat playerHat = HatDatabase.getPlayersHatById(player, hatId);
        if(playerHat == null) {
            Splatoon.LOGGER.error("Couldnt find player hat belonging to player: '{}'", hatId);
            stopOngoingTrade(player);
            return dialogBuilder.buildWithoutSending();
        }

        dialogBuilder.addBodyItem(hat.createItemStack());
        dialogBuilder.addBodyText(Component.literal(playerHat.count+"x ").append(hat.getFormattedName()), 100);
        dialogBuilder.addBodyText(hat.rarity.formattedName(), 100);

        if(hat.rarity.value < 0) {
            dialogBuilder.addBodyText("Quicksell value: Invaluable", 100);
        }
        else
        {
            dialogBuilder.addBodyText("Quicksell value: $" + hat.rarity.value, 100);
        }

        dialogBuilder.addAction(Component.literal("Add one to offer"), "trade_pick_hat_one", 100, tag, (player1, tag1)->{
            addHatToOfferAction(player1, tag1, 1);
            reloadBaseMenuAction(player1, tag1);
        });

        if(playerHat.count > 1) {
            dialogBuilder.addAction(Component.literal("Add all ("+playerHat.count+") to offer"), "trade_pick_hat_one", 100, tag, (player1, tag1)->{
                addHatToOfferAction(player1, tag1, -1);
                reloadBaseMenuAction(player1, tag1);
            });
        }

        dialogBuilder.setCloseAction(Component.literal("Back"), 150, tag, this::openAddHatDialog);

        return dialogBuilder.buildWithoutSending();
    }


    void openAddHatDialog(Player player, Tag tag)
    {
        Dialog dialog = createAddHatDialog(player, tag);
        setIsPlayerOnHomeMenu(player, false);
        DialogBuilder.send(dialog, player);
    }

    Dialog createAddHatDialog(Player player, Tag tag)
    {
        OngoingTrade ongoingTrade = getOngoingTradeFromTag(tag);

        DialogBuilder dialogBuilder = DialogBuilder.createDialogMultiAction("trade_pick_hats", 4)
                .title("Pick hats to add to the trade")
                .setCloseAction(Component.literal("Back"), 150, tag, this::reloadBaseMenuAction)
                ;

        List<HatDatabase.PlayerHat> hats = HatDatabase.getPlayerHats(player);

        for(HatDatabase.PlayerHat playerHat : hats)
        {
            HatDatabase.Hat hat = playerHat.getHat();
            if(hat == null) continue;

            MutableComponent label = Component.literal(playerHat.count + "x ");
            label = label.append(hat.getFormattedName());

            CompoundTag hatTag = tag.asCompound().orElse(new CompoundTag()).copy();
            hatTag.putString("hat_id", hat.id);

            dialogBuilder.addAction(label, "trade_pick_hat", 100, hatTag, this::openPickHatDialog);
        }

        return dialogBuilder.buildWithoutSending();
    }

    void openInspectMyHatOfferDialog(Player player, Tag tag)
    {
        OngoingTrade ongoingTrade = getOngoingTradeFromTag(tag);
        Offer offer = ongoingTrade.getOfferFor(player.getUUID());

        CompoundTag compoundTag = tag.asCompound().orElse(new CompoundTag());
        String hatId = compoundTag.getStringOr("hat_id", "");
        HatDatabase.Hat hat = HatDatabase.getHatById(hatId);
        if(hat == null) {
            Splatoon.LOGGER.error("Could not find hat by id: {}", hatId);
            stopOngoingTrade(player);
            return;
        }

        HatDatabase.PlayerHat playerHat = HatDatabase.getPlayersHatById(player, hatId);
        if(playerHat == null) {
            Splatoon.LOGGER.error("Could not find hat belonging to {} by id: {}", player.getName().getString(), hatId);
            stopOngoingTrade(player);
            return;
        }

        Dialog dialog = createInspectMyHatOfferDialog(tag, player, offer, hat, playerHat);
        DialogBuilder.send(dialog, player);
        setIsPlayerOnHomeMenu(player, false);
    }

    Dialog createInspectMyHatOfferDialog(Tag tag, Player player, Offer offer, HatDatabase.Hat hat, HatDatabase.PlayerHat playerHat)
    {
        DialogBuilder dialogBuilder = DialogBuilder.createDialogMultiAction("trade_inspect_my_hat", 1)
                .title("Inspect hat offer")
                .setCloseAction(Component.literal("Back"), 150, tag, this::reloadBaseMenuAction);

        var hatOffer = offer.findHatOffer(hat.id);
        if(hatOffer == null) {

            stopOngoingTrade(player);
            return null;
        }

        int currentOfferCount = hatOffer.getB();
        int currentInvCount = playerHat.count;

        dialogBuilder.addBodyItem(hat.createItemStack());
        dialogBuilder.addBodyText(Component.literal(currentOfferCount+"x ").append(hat.getFormattedName()), 200);
        dialogBuilder.addBodyText(hat.rarity.formattedName(), 100);
        dialogBuilder.addBodyText("1x quick-sell value: " + (hat.rarity.value>=0?String.valueOf(hat.rarity.value):"Invaluable"), 200);

        dialogBuilder.addAction(Component.literal("Remove one from offer"), "trade_inspect_remove_one", 100, tag, (player1,tag1)->{
            removeHatFromOfferAction(player1, tag1, 1);
            reloadBaseMenuAction(player1, tag1);
        });

        if(currentOfferCount > 1) {
            dialogBuilder.addAction(Component.literal("Remove all from offer"), "trade_inspect_remove_all", 100, tag, (player1,tag1)->{
                removeHatFromOfferAction(player1, tag1, -1);
                reloadBaseMenuAction(player1, tag1);
            });
        }

        if(currentInvCount > currentOfferCount) {
            dialogBuilder.addAction(Component.literal("Add one to offer"), "trade_inspect_add_one", 100, tag, (player1,tag1)->{
                addHatToOfferAction(player1, tag1, 1);
                reloadBaseMenuAction(player1, tag1);
            });
        }

        return dialogBuilder.buildWithoutSending();
    }

    void openMoneyInputDialog(Player player, Tag tag)
    {
        Dialog dialog = createMoneyInputDialog(player, tag);
        DialogBuilder.send(dialog, player);
        setIsPlayerOnHomeMenu(player, false);
    }

    Dialog createMoneyInputDialog(Player player, Tag tag)
    {
        DialogBuilder dialogBuilder = DialogBuilder.createDialogConfirmation("trade_money_input")
                .title("Additional gold for offer")
                .setCloseAction(Component.literal("Back"), 200, tag, this::reloadBaseMenuAction);

        int maxMoney = PlayerStats.get(player).get(PlayerStats.MONEY);

        OngoingTrade ongoingTrade = getOngoingTrade(player);
        Offer offer = ongoingTrade.getOfferFor(player.getUUID());
        int currentMoneyOffer = offer.money;

        dialogBuilder.addBodyText("Select amount of money to include with your offer", 200);

        dialogBuilder.addInputIntegerRange("money_input", Component.literal("Money"), 300, 0, maxMoney, currentMoneyOffer);

        dialogBuilder.onSubmit((player1, values) -> {
            OngoingTrade ongoingTrade1 = getOngoingTrade(player1);

            String value = values.getOrDefault("money_input", "0");
            Splatoon.LOGGER.info("\tInput trade money '{}'", value);

            try {
                int intValue = Integer.parseInt(value);
                ongoingTrade1.getOfferFor(player1.getUUID()).money = intValue;
            }
            catch(NumberFormatException e) {
                Splatoon.LOGGER.info("Received malformed money input string '{}'", value);
            }

            setIsPlayerOnHomeMenu(player1, true);

            ongoingTrade1.refreshMenus();
        });

        return dialogBuilder.buildWithoutSending();
    }

    void openRequestDialog(Player player, Tag tag)
    {
        OngoingTrade ongoingTrade = getOngoingTrade(player);
        Player otherPlayer = ongoingTrade.getOtherPlayer(player.getUUID());

        Dialog dialog = createRequestDialog(player, otherPlayer, tag);
        setIsPlayerOnHomeMenu(player, false);
        DialogBuilder.send(dialog, player);
    }

    Dialog createRequestDialog(Player thisPlayer, Player otherPlayer, Tag tag)
    {
        DialogBuilder dialogBuilder = DialogBuilder.createDialogConfirmation("trade_request_dialog")
                .title("Request from them")
                .setCloseAction(Component.literal("Back"), 200, tag, this::reloadBaseMenuAction);

        int theirMoney = PlayerStats.get(otherPlayer).get(PlayerStats.MONEY);

        dialogBuilder.addInputIntegerRange("money_request", Component.literal("Money"), 300, 0, theirMoney, 0);

        List<HatDatabase.PlayerHat> theirHats = HatDatabase.getPlayerHats(otherPlayer);
        for(HatDatabase.PlayerHat playerHat : theirHats)
        {
            HatDatabase.Hat hat = playerHat.getHat();
            if(hat == null) continue;

            dialogBuilder.addInputBoolean(playerHat.id, hat.getFormattedName(), "y", "n", false);
        }

        dialogBuilder.onSubmit((player1, values)->{
            Offer request = new Offer();

            for(var entry : values.entrySet()) {
                if(entry.getKey().equals("money_request"))
                {
                    try {
                        request.money = Integer.parseInt(entry.getValue());
                    } catch (NumberFormatException e) {
                        Splatoon.LOGGER.error("Couldnt parse money request value '{}'", entry.getValue());
                        stopOngoingTrade(player1);
                    }
                }
                else if(entry.getValue().equals("y"))
                {
                    String hatId = entry.getKey();
                    HatDatabase.Hat hat = HatDatabase.getHatById(hatId);
                    if(hat != null) {
                        request.hats.add(new Tuple<>(hat, 1));
                    }
                }
            }

            OngoingTrade ongoingTrade = getOngoingTrade(player1);
            ongoingTrade.setRequestFrom(player1.getUUID(), request);
            setIsPlayerOnHomeMenu(player1, true);
            ongoingTrade.refreshMenus();
        });

        return dialogBuilder.buildWithoutSending();
    }

    void toggleAcceptedAction(Player player, Tag tag)
    {
        OngoingTrade ongoingTrade = getOngoingTradeFromTag(tag);

        ongoingTrade.setPlayerAccepted(player.getUUID(), !ongoingTrade.getPlayerAccepted(player.getUUID()));

        if(ongoingTrade.player1accepted && ongoingTrade.player2accepted)
        {
            Splatoon.LOGGER.info("Make the trade!");

            Player player1 = Splatoon.SERVER.getPlayerList().getPlayer(ongoingTrade.player1);
            Player player2 = Splatoon.SERVER.getPlayerList().getPlayer(ongoingTrade.player2);

            if(player1 == null) {
                Splatoon.LOGGER.warn("p1 left");
                stopOngoingTrade(player);
                return;
            }
            if(player2 == null) {
                Splatoon.LOGGER.warn("p2 left");
                stopOngoingTrade(player);
                return;
            }

            // First pass is only for validations
            int player1currentMoney = PlayerStats.get(player1).get(PlayerStats.MONEY);
            int player2currentMoney = PlayerStats.get(player2).get(PlayerStats.MONEY);

            if(ongoingTrade.offer1.money > player1currentMoney) {
                Splatoon.LOGGER.warn("p1 {} doesnt have enough money, potentially malformed request", player1.getScoreboardName());
                stopOngoingTrade(player);
                return;
            }
            if(ongoingTrade.offer2.money > player2currentMoney) {
                Splatoon.LOGGER.warn("p2 {} doesnt have enough money, potentially malformed request", player2.getScoreboardName());
                stopOngoingTrade(player);
                return;
            }

            for(var tuple : ongoingTrade.offer1.hats) {
                String hatId = tuple.getA().id;
                int needAmount = tuple.getB();
                HatDatabase.PlayerHat playerHat = HatDatabase.getPlayersHatById(player1, hatId);
                if(playerHat == null) {
                    Splatoon.LOGGER.warn("p1 {} doesnt have requested hat ", hatId);
                    stopOngoingTrade(player);
                    return;
                }
                int hasAmount = playerHat.count;
                if(needAmount > hasAmount) {
                    Splatoon.LOGGER.warn("p1 {} doesnt have enough of requested hat ({}>{})", hatId, needAmount, hasAmount);
                    stopOngoingTrade(player);
                    return;
                }
            }

            for(var tuple : ongoingTrade.offer2.hats) {
                String hatId = tuple.getA().id;
                int needAmount = tuple.getB();
                HatDatabase.PlayerHat playerHat = HatDatabase.getPlayersHatById(player2, hatId);
                if(playerHat == null) {
                    Splatoon.LOGGER.warn("p2 {} doesnt have requested hat ", hatId);
                    stopOngoingTrade(player);
                    return;
                }
                int hasAmount = playerHat.count;
                if(needAmount > hasAmount) {
                    Splatoon.LOGGER.warn("p2 {} doesnt have enough of requested hat ({}>{})", hatId, needAmount, hasAmount);
                    stopOngoingTrade(player);
                    return;
                }
            }

            MutableComponent player1SentMessage = Component.literal("Sent:\n");
            MutableComponent player2SentMessage = Component.literal("Sent:\n");
            MutableComponent player1ReceivedMessage = Component.literal("Received:\n");
            MutableComponent player2ReceivedMessage = Component.literal("Received:\n");

            // Second pass and the trade actually happens
            PlayerStats.get(player1).forceAddNoMatch(PlayerStats.MONEY, -ongoingTrade.offer1.money);
            PlayerStats.get(player2).forceAddNoMatch(PlayerStats.MONEY, ongoingTrade.offer1.money);
            player1SentMessage = player1SentMessage.append("$" + ongoingTrade.offer1.money + "\n");
            player2ReceivedMessage = player2ReceivedMessage.append("$" + ongoingTrade.offer1.money + "\n");

            PlayerStats.get(player2).forceAddNoMatch(PlayerStats.MONEY, -ongoingTrade.offer2.money);
            PlayerStats.get(player1).forceAddNoMatch(PlayerStats.MONEY, ongoingTrade.offer2.money);
            player2SentMessage = player1SentMessage.append("$" + ongoingTrade.offer2.money + "\n");
            player1ReceivedMessage = player2ReceivedMessage.append("$" + ongoingTrade.offer2.money + "\n");

            for(var tuple : ongoingTrade.offer1.hats) {
                String hatId = tuple.getA().id;
                int needAmount = tuple.getB();
                HatDatabase.revokeHat(player1, hatId, needAmount);
                HatDatabase.giveHat(player2, hatId, needAmount);

                player1SentMessage = player1SentMessage.append(needAmount+"x " + tuple.getA().getName()+"\n");
                player2ReceivedMessage = player2ReceivedMessage.append(needAmount+"x " + tuple.getA().getName()+"\n");
            }

            for(var tuple : ongoingTrade.offer2.hats) {
                String hatId = tuple.getA().id;
                int needAmount = tuple.getB();
                HatDatabase.revokeHat(player2, hatId, needAmount);
                HatDatabase.giveHat(player1, hatId, needAmount);

                player2SentMessage = player2SentMessage.append(needAmount+"x " + tuple.getA().getName()+"\n");
                player1ReceivedMessage = player1ReceivedMessage.append(needAmount+"x " + tuple.getA().getName()+"\n");
            }



            ((ServerPlayer)player1).sendSystemMessage(Component.literal("").append(player1SentMessage).append("\n").append(player1ReceivedMessage)  );
            ((ServerPlayer)player2).sendSystemMessage(Component.literal("").append(player2SentMessage).append("\n").append(player2ReceivedMessage)  );

            stopOngoingTrade(player);
        }
        else
        {
            reloadBaseMenuAction(player, tag);
        }
    }

    Dialog createBaseMenuDialog(Player thisPlayer, Player otherPlayer)
    {
        CompoundTag tradeTag = new CompoundTag();
        tradeTag.putString("thisPlayer", thisPlayer.getUUID().toString());
        tradeTag.putString("otherPlayer", otherPlayer.getUUID().toString());

        OngoingTrade ongoingTrade = getOngoingTrade(thisPlayer);

        if(ongoingTrade == null) {
            Splatoon.LOGGER.info("Could not find trade for {} ({})", thisPlayer.getName().getString(), thisPlayer.getUUID().toString());
            stopOngoingTrade(otherPlayer);
            return null;
        }

        Offer thisOffer = ongoingTrade.getOfferFor(thisPlayer.getUUID());
        Offer theirOffer = ongoingTrade.getOfferFor(otherPlayer.getUUID());

        if(thisOffer == null) {
            Splatoon.LOGGER.error("thisOffer is null? {}", thisPlayer.getUUID());
            stopOngoingTrade(thisPlayer);
            return null;
        }
        if(theirOffer == null) {
            Splatoon.LOGGER.error("theirOffer is null? {}", otherPlayer.getUUID());
            stopOngoingTrade(thisPlayer);
            return null;
        }

        DialogBuilder dialogBuilder = DialogBuilder
                .createDialogMultiAction("trade_menu", 3)
                .title("Trading with " + otherPlayer.getName().getString())
//                .setSubmitAction(Component.literal("Accept trade"), 200, tradeTag, this::submitBaseMenuAction)
                .setCloseAction(Component.literal("Exit trade"), 200, tradeTag, this::closeBaseMenuAction);

        {
            Offer request = ongoingTrade.getRequestFrom(otherPlayer.getUUID());
            int requestMoney = request.money;

            if(requestMoney > 0 || !request.hats.isEmpty())
            {
                MutableComponent requestedMessage = Component.literal("");
                {
                    requestedMessage = requestedMessage.append(otherPlayer.getDisplayName()).append(Component.literal(" is requesting:\n"));
                    if(requestMoney > 0) {
                        requestedMessage = requestedMessage.append(Component.literal("$"+requestMoney+"\n"));
                    }
                    for(var tuple : request.hats) {
                        requestedMessage = requestedMessage.append(Component.literal("1x ").append(tuple.getA().getFormattedName()).append("\n"));
                    }
                }
                dialogBuilder.addBodyText(requestedMessage, 400);
            }
        }

        dialogBuilder.addAction(Component.literal("Your offer").withStyle(ChatFormatting.BOLD), "trade_blank", 150, tradeTag, this::reloadBaseMenuAction);
        dialogBuilder.addAction(Component.literal(" "), "trade_blank", 10, tradeTag, this::reloadBaseMenuAction);
        dialogBuilder.addAction(Component.literal(otherPlayer.getScoreboardName() + "'s offer").withStyle(ChatFormatting.BOLD), "trade_blank", 150, tradeTag, this::reloadBaseMenuAction);

        dialogBuilder.addAction(Component.literal("$"+thisOffer.money), "trade_open_money_input", 150, tradeTag, this::openMoneyInputDialog);
        dialogBuilder.addAction(Component.literal(" "), "trade_blank", 10, tradeTag, this::reloadBaseMenuAction);
        dialogBuilder.addAction(Component.literal("$"+theirOffer.money), "trade_blank", 150, tradeTag, this::reloadBaseMenuAction);

        for(int i=0; i<Math.max(thisOffer.hats.size(), theirOffer.hats.size()); i++)
        {
            Tuple<HatDatabase.Hat, Integer> thisHatOffer = null;
            Tuple<HatDatabase.Hat, Integer> theirHatOffer = null;

            if(i < thisOffer.hats.size()) {
                thisHatOffer = thisOffer.hats.get(i);
            }

            if(i < theirOffer.hats.size()) {
                theirHatOffer = theirOffer.hats.get(i);
            }

            if(thisHatOffer == null) {
                dialogBuilder.addAction(Component.literal(""), "trade_blank", 150, tradeTag, this::reloadBaseMenuAction);
            }
            else
            {
                HatDatabase.Hat hat = thisHatOffer.getA();
                int count = thisHatOffer.getB();

                CompoundTag offerTag = tradeTag.copy();
                offerTag.putString("hat_id", hat.id);

                MutableComponent label = Component.literal( count + "x ").append(hat.getFormattedName());
                dialogBuilder.addAction(label, "trade_inspect_hat_offer", 150, offerTag, this::openInspectMyHatOfferDialog);
            }

            dialogBuilder.addAction(Component.literal(" "), "trade_blank", 10, tradeTag, this::reloadBaseMenuAction);

            if(theirHatOffer == null) {
                dialogBuilder.addAction(Component.literal(""), "trade_blank", 150, tradeTag, this::reloadBaseMenuAction);
            }
            else
            {
                HatDatabase.Hat hat = theirHatOffer.getA();
                int count = theirHatOffer.getB();

                MutableComponent label = Component.literal( count + "x ").append(hat.getFormattedName());
                dialogBuilder.addAction(label, "trade_blank", 150, tradeTag, this::reloadBaseMenuAction);
            }
        }

        dialogBuilder.addAction(Component.literal("[Add a hat to offer]").withStyle(ChatFormatting.BOLD), "trade_open_add_hat", 150, tradeTag, this::openAddHatDialog);
        dialogBuilder.addAction(Component.literal(" "), "trade_blank", 10, tradeTag, this::reloadBaseMenuAction);
        dialogBuilder.addAction(Component.literal("[Ask them for something]").withStyle(ChatFormatting.BOLD), "trade_open_request", 150, tradeTag, this::openRequestDialog);

        boolean hasAccepted = ongoingTrade.getPlayerAccepted(thisPlayer.getUUID());

        dialogBuilder.addAction(Component.literal(hasAccepted ? "Waiting for response... Click again to cancel" : "Accept trade"), "accept_trade", 300, tradeTag, this::toggleAcceptedAction);

        Dialog dialog = dialogBuilder.buildWithoutSending();
        return dialog;
    }


}
