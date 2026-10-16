package com.plokie.dialog;

import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import com.plokie.Splatoon;
import com.plokie.helpers.CommandBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundShowDialogPacket;
import net.minecraft.server.dialog.*;
import net.minecraft.server.dialog.action.CommandTemplate;
import net.minecraft.server.dialog.action.ParsedTemplate;
import net.minecraft.server.dialog.body.DialogBody;
import net.minecraft.server.dialog.body.ItemBody;
import net.minecraft.server.dialog.body.PlainMessage;
import net.minecraft.server.dialog.input.BooleanInput;
import net.minecraft.server.dialog.input.SingleOptionInput;
import net.minecraft.server.dialog.input.TextInput;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class DialogBuilder {
    enum DialogType {
        Confirmation
    }

    final DialogType type;
    final String id;

    Component title = Component.literal("Title not set");
    Component externalTitle = Component.literal("External title not set");

    Component submitText = Component.literal("Submit");
    Component closeText = Component.literal("Close");

//    Function<CommandSourceStack, Boolean> permissionCallback = (ctx)->true;

    List<DialogBody> dialogBody = new ArrayList<>();
    Map<String, Input> dialogInputs = new HashMap<>();
//    BiConsumer<ServerPlayer, Map<String, String>> callback = null;

    static Map<String, BiConsumer<ServerPlayer, Map<String, String>>> responseCallbacks = new HashMap<>();

    public static void initialiseCommand()
    {
        CommandBuilder
                .command("dialogresponse")
                .argumentString("dialog_id")
                .argumentGreedyString("packet")
                .executes(ctx->{
            ServerPlayer player = ctx.getStack().getSource().getPlayer();
            if(player == null) {
                return "! This command must be executed by a player";
            }

            String dialogId = ctx.getArgumentString("dialog_id");
            String packet = ctx.getArgumentString("packet");

            Splatoon.LOGGER.info("Receiving dialogresponse for {}...", dialogId);

            if(responseCallbacks.containsKey(dialogId))
            {
                String[] subPackets = packet.split("¬");

                Splatoon.LOGGER.info("\t{} entries:", subPackets.length);

                Map<String,String> values = new HashMap<>();

                for(String subPacket : subPackets)
                {
                    String[] packetParts = subPacket.split("=");
                    if(packetParts.length < 2) { return "! Malformed packet: " + subPacket; }

                    String key = packetParts[0];
                    String value = packetParts[1];

                    Splatoon.LOGGER.info("\t\t{}={}", key, value);

                    values.put(key, value);
                }

                responseCallbacks.get(dialogId).accept(player, values);

                return "Executing dialog response " + dialogId;
            }
            else
            {
                return "! This dialog is not associated with a callback response";
            }
        }).permission(ctx->true).register();
    }


    private DialogBuilder(String id, DialogType type)
    {
        this.id = id;
        this.type = type;
    }

    public static DialogBuilder create(String dialogId)
    {
        return new DialogBuilder(dialogId, DialogType.Confirmation);
    }

    public DialogBuilder title(Component title)
    {
        this.title = title;
        return this;
    }

    public DialogBuilder externalTitle(Component externalTitle)
    {
        this.externalTitle = externalTitle;
        return this;
    }

    public DialogBuilder submitText(Component text)
    {
        this.submitText = text;
        return this;
    }

    public DialogBuilder closeText(Component text)
    {
        this.closeText = text;
        return this;
    }

    public DialogBuilder addBodyText(String message, int width)
    {
        return addBodyText(Component.literal(message), width);
    }

    public DialogBuilder addBodyText(Component message, int width)
    {
        dialogBody.add(new PlainMessage(message, width));
        return this;
    }

    public DialogBuilder addBodyItem(Item item)
    {
        return addBodyItem(item, null);
    }

    public DialogBuilder addBodyItem(Item item, Component message)
    {
        return addBodyItem(new ItemStack(item), message);
    }

    public DialogBuilder addBodyItem(ItemStack item)
    {
        return addBodyItem(item, null);
    }

    public DialogBuilder addBodyItem(ItemStack item, Component message)
    {
        dialogBody.add(new ItemBody(
                item,
                (message==null ? Optional.empty() : Optional.of(new PlainMessage(message, 100)) ),
                true, // show decorations
                true, // show tooltip
                16, // width
                16 // height
        ));
        return this;
    }

    public DialogBuilder addInputBoolean(String key, Component label, String trueValue, String falseValue, boolean defaultValue)
    {
        dialogInputs.put(key, new Input(key, new BooleanInput(label, defaultValue, trueValue, falseValue)));
        return this;
    }

    public DialogBuilder addInputOption(String key, Component label, List<Component> options, int defaultValueIndex)
    {
        List<SingleOptionInput.Entry> entries = new ArrayList<>();
        int idx=0;
        for(Component option : options)
        {
            entries.add(new SingleOptionInput.Entry("id0", Optional.of(option), idx==defaultValueIndex));
            idx++;
        }

        dialogInputs.put(key, new Input(key, new SingleOptionInput(
                300,
                entries,
                label,
                true
        )));

        return this;
    }

    public DialogBuilder addInputText(String key, Component label, int boxWidth, String initialValue, int maxLength)
    {
        dialogInputs.put(key, new Input(key, new TextInput(boxWidth, label, true, initialValue, maxLength, Optional.empty())));
        return this;
    }

    public DialogBuilder addInputTextMultiline(String key, Component label, int boxWidth, int boxHeight, String initialValue, int maxLength, int maxLines)
    {
        dialogInputs.put(key, new Input(key, new TextInput(boxWidth, label, true, initialValue, maxLength, Optional.of(
                new TextInput.MultilineOptions(Optional.of(boxHeight), Optional.of(maxLines))
        ))));
        return this;
    }

    public DialogBuilder onSubmit(BiConsumer<ServerPlayer, Map<String, String>> callback)
    {
        if(!responseCallbacks.containsKey(this.id))
        {
            responseCallbacks.put(this.id, callback);
        }

        return this;
    }

    public Dialog buildWithoutSending()
    {
        CommonDialogData dialogData = new CommonDialogData(
                this.title,
                Optional.of(this.externalTitle),
                true, // Can close with escape
                false, // Does Pause
                DialogAction.CLOSE,
                this.dialogBody,
                this.dialogInputs.values().stream().toList()
        );

        String command = "/dialogresponse "+id+" ";

        for(var entry : dialogInputs.entrySet())
        {
            String valueKey = entry.getKey();
            command += valueKey + "=$(" + valueKey + ")¬";
        }

        ParsedTemplate parsedTemplate = ParsedTemplate.CODEC.parse(JsonOps.INSTANCE, new JsonPrimitive(command)).getOrThrow(IllegalStateException::new);

        Dialog dialog = new ConfirmationDialog(
                dialogData,
                new ActionButton(new CommonButtonData(this.submitText, 200), Optional.of(
                        new CommandTemplate(parsedTemplate)
                )),
                new ActionButton(new CommonButtonData(this.closeText, 200), Optional.empty())
        );

        return dialog;
    }

    public Dialog buildAndSend(Player player)
    {
        Dialog dialog = buildWithoutSending();

        ((ServerPlayer)player).connection.send(new ClientboundShowDialogPacket(Holder.direct(dialog)));

        return dialog;
    }



}
