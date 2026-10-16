package com.plokie.dialog;

import com.google.gson.JsonPrimitive;
import com.mojang.datafixers.util.Function3;
import com.mojang.serialization.JsonOps;
import com.plokie.Splatoon;
import com.plokie.helpers.CommandBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundShowDialogPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.dialog.*;
import net.minecraft.server.dialog.action.CommandTemplate;
import net.minecraft.server.dialog.action.ParsedTemplate;
import net.minecraft.server.dialog.action.StaticAction;
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
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class DialogBuilder {
    enum DialogType
    {
        Confirmation,
        MultiAction
    }

    final DialogType type;
    final String id;

    Component title = Component.literal("Title not set");
    Component externalTitle = Component.literal("External title not set");

//    Component submitText = Component.literal("Submit");
//    Component closeText = Component.literal("Close");
    ActionButton submitButton = new ActionButton(new CommonButtonData(Component.literal("Submit"), 200), Optional.empty());
    ActionButton closeButton = new ActionButton(new CommonButtonData(Component.literal("Close"), 200), Optional.empty());

    DialogAction dialogAction = DialogAction.CLOSE;

    List<ActionButton> submitActions = new ArrayList<>();

//    Function<CommandSourceStack, Boolean> permissionCallback = (ctx)->true;

    List<DialogBody> dialogBody = new ArrayList<>();
    Map<String, Input> dialogInputs = new HashMap<>();
//    BiConsumer<ServerPlayer, Map<String, String>> callback = null;

    static Map<String, BiConsumer<ServerPlayer, Map<String, String>>> responseCallbacks = new HashMap<>();
    public static Map<ResourceLocation, BiConsumer<ServerPlayer, Tag>> customActionCallbacks = new HashMap<>();

    final BiFunction<CommonDialogData, DialogBuilder, Dialog> constructorCallback;

    static String safeKey(String key)
    {
        return key.replace(':', '_').replace('/', '_');
    }

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

    public static void send(Dialog dialog, Player player)
    {
        ((ServerPlayer)player).connection.send(new ClientboundShowDialogPacket(Holder.direct(dialog)));
    }

    public static void triggerAction(String actionId, Player player)
    {
        triggerAction(actionId, player, new CompoundTag());
    }

    public static void triggerAction(String actionId, Player player, Tag tag)
    {
        ResourceLocation resourceLocation = ResourceLocation.withDefaultNamespace(actionId);
        if(customActionCallbacks.containsKey(resourceLocation))
        {
            customActionCallbacks.get(resourceLocation).accept((ServerPlayer) player, tag);
        }
    }

    private DialogBuilder(String id, DialogType type, BiFunction<CommonDialogData, DialogBuilder, Dialog> constructorCallback)
    {
        this.id = id;
        this.type = type;
        this.constructorCallback = constructorCallback;
    }

    public static DialogBuilder createDialogConfirmation(String dialogId)
    {
        return new DialogBuilder(dialogId, DialogType.Confirmation, (dialogData, self)->{
            return new ConfirmationDialog(
                    dialogData,
                    self.submitButton,
//                    new ActionButton(new CommonButtonData(Component.literal("Submit"), 200),
//
//                            self.dialogInputs.isEmpty() ? Optional.empty() :
//
//                            Optional.of(
//                              new CommandTemplate(self.constructParsedTemplate())
//                    )),
                    self.closeButton
            );
        });
    }

    public static DialogBuilder createDialogMultiAction(String dialogId, int columns)
    {
        return new DialogBuilder(dialogId, DialogType.MultiAction, (dialogData, self)->{
            return new MultiActionDialog(
                    dialogData,
                    self.submitActions,
                    Optional.of(self.closeButton),
                    columns
            );
        });
    }

//    public DialogBuilder makeCloseButtonSubmit()
//    {
//        this.closeButton = new ActionButton(new CommonButtonData(Component.literal("Submit"), 200), dialogInputs.isEmpty() ? Optional.empty() :Optional.of(
//                new CommandTemplate(constructParsedTemplate())
//        ));
//        return this;
//    }

//    public DialogBuilder addDefaultSubmitAction()
//    {
//        this.submitActions.add(new ActionButton(new CommonButtonData(this.submitText, 200), Optional.of(
//                new CommandTemplate(constructParsedTemplate())
//        )));
//        return this;
//    }

    public DialogBuilder setDialogAction(DialogAction action)
    {
        this.dialogAction = action;
        return this;
    }

    public DialogBuilder addAction(ActionButton actionButton)
    {
        this.submitActions.add(actionButton);
        return this;
    }

    public DialogBuilder addAction(Component label, ClickEvent event, int width)
    {
        return addAction(new ActionButton(new CommonButtonData(label, width), Optional.of(new StaticAction(event)) ) );
    }

    public DialogBuilder addAction(Component label, String id, int width, Consumer<ServerPlayer> callback)
    {
        ResourceLocation resourceLocation = ResourceLocation.withDefaultNamespace(id);

        if(!customActionCallbacks.containsKey(resourceLocation))
        {
            customActionCallbacks.put(resourceLocation, (player, tag)->callback.accept(player));
        }

        return addAction(label, new ClickEvent.Custom(resourceLocation, Optional.empty()), width);
    }

    public DialogBuilder addAction(Component label, String id, int width, Tag tagArg, BiConsumer<ServerPlayer, Tag> callback)
    {
        ResourceLocation resourceLocation = ResourceLocation.withDefaultNamespace(id);

        if(!customActionCallbacks.containsKey(resourceLocation))
        {
            customActionCallbacks.put(resourceLocation, callback);
        }

        return addAction(label, new ClickEvent.Custom(resourceLocation, Optional.of(tagArg)), width);
    }

    public DialogBuilder setCloseAction(Component label, int width, Consumer<ServerPlayer> callback)
    {
        String id = this.id + "_close";
        ResourceLocation resourceLocation = ResourceLocation.withDefaultNamespace(id);

        if(!customActionCallbacks.containsKey(resourceLocation))
        {
            customActionCallbacks.put(resourceLocation, (player, tag)->callback.accept(player));
        }

        this.closeButton = new ActionButton(new CommonButtonData(label, width), Optional.of(new StaticAction(
                new ClickEvent.Custom(resourceLocation, Optional.empty())
        )) );
        return this;
    }

    public DialogBuilder setCloseAction(Component label, int width, Tag tag, BiConsumer<ServerPlayer, Tag> callback)
    {
        String id = this.id + "_close";
        ResourceLocation resourceLocation = ResourceLocation.withDefaultNamespace(id);

        if(!customActionCallbacks.containsKey(resourceLocation))
        {
            customActionCallbacks.put(resourceLocation, callback);
        }

        this.closeButton = new ActionButton(new CommonButtonData(label, width), Optional.of(new StaticAction(
                new ClickEvent.Custom(resourceLocation, Optional.of(tag))
        )) );
        return this;
    }

    public DialogBuilder setSubmitAction(Component label, int width, Consumer<ServerPlayer> callback)
    {
        String id = this.id + "_submit";
        ResourceLocation resourceLocation = ResourceLocation.withDefaultNamespace(id);

        if(!customActionCallbacks.containsKey(resourceLocation))
        {
            customActionCallbacks.put(resourceLocation, (player, tag)->callback.accept(player));
        }

        this.submitButton = new ActionButton(new CommonButtonData(label, width), Optional.of(new StaticAction(
                new ClickEvent.Custom(resourceLocation, Optional.empty())
        )) );
        return this;
    }

    public DialogBuilder setSubmitAction(Component label, int width, Tag tag, BiConsumer<ServerPlayer, Tag> callback)
    {
        String id = this.id + "_submit";
        ResourceLocation resourceLocation = ResourceLocation.withDefaultNamespace(id);

        if(!customActionCallbacks.containsKey(resourceLocation))
        {
            customActionCallbacks.put(resourceLocation, callback);
        }

        this.submitButton = new ActionButton(new CommonButtonData(label, width), Optional.of(new StaticAction(
                new ClickEvent.Custom(resourceLocation, Optional.of(tag))
        )) );
        return this;
    }

    public DialogBuilder title(String title)
    {
        return title(Component.literal(title));
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
        dialogInputs.put(key, new Input(safeKey(key), new BooleanInput(label, defaultValue, trueValue, falseValue)));
        return this;
    }

    public DialogBuilder addInputOption(String key, Component label, List<Component> options, int defaultValueIndex)
    {
        List<SingleOptionInput.Entry> entries = new ArrayList<>();
        int idx=0;
        for(Component option : options)
        {
            entries.add(new SingleOptionInput.Entry(""+idx, Optional.of(option), idx==defaultValueIndex));
            idx++;
        }

        dialogInputs.put(key, new Input(safeKey(key), new SingleOptionInput(
                300,
                entries,
                label,
                true
        )));

        return this;
    }

    public DialogBuilder addInputText(String key, Component label, int boxWidth, String initialValue, int maxLength)
    {
        dialogInputs.put(key, new Input(safeKey(key), new TextInput(boxWidth, label, true, initialValue, maxLength, Optional.empty())));
        return this;
    }

    public DialogBuilder addInputTextMultiline(String key, Component label, int boxWidth, int boxHeight, String initialValue, int maxLength, int maxLines)
    {
        dialogInputs.put(key, new Input(safeKey(key), new TextInput(boxWidth, label, true, initialValue, maxLength, Optional.of(
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

    ParsedTemplate constructParsedTemplate()
    {
        String command = "/dialogresponse "+id+" ";

        for(var entry : dialogInputs.entrySet())
        {
            String valueKey = entry.getKey();
            command += valueKey + "=$(" + safeKey(valueKey) + ")¬";
        }

        return ParsedTemplate.CODEC.parse(JsonOps.INSTANCE, new JsonPrimitive(command)).getOrThrow(IllegalStateException::new);
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



        Dialog dialog = constructorCallback.apply(dialogData, this);

        return dialog;
    }

    public Dialog buildAndSend(Player player)
    {
        Dialog dialog = buildWithoutSending();

        ((ServerPlayer)player).connection.send(new ClientboundShowDialogPacket(Holder.direct(dialog)));

        return dialog;
    }



}
