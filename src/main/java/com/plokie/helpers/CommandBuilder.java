package com.plokie.helpers;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.datafixers.types.Func;
import com.plokie.Splatoon;
import com.plokie.classes.abilities.AbilityManager;
import com.plokie.management.gamemodes.Gamemodes;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class CommandBuilder {
    public static class ExecuteContext
    {
        final CommandContext<CommandSourceStack> stack;
        public ExecuteContext(CommandContext<CommandSourceStack> stack) {
            this.stack = stack;
        }

        public CommandContext<CommandSourceStack> getStack() { return stack; }

        public ServerPlayer getArgumentPlayer(String name) throws CommandSyntaxException {
            return EntityArgument.getPlayer(stack, name);
        }

        public int getArgumentInteger(String name) {
            return IntegerArgumentType.getInteger(stack, name);
        }

        public String getArgumentString(String name) {
            return StringArgumentType.getString(stack, name);
        }

        public <T extends Enum<T>> T getArgumentEnum(String name, Class<T> tEnum) throws IllegalArgumentException
        {
            String enumString = StringArgumentType.getString(stack, name);
            for(T enumVal : tEnum.getEnumConstants())
            {
                if(enumVal.toString().equals(enumString))
                {
                    return enumVal;
                }
            }

            String errorMessage = "Unrecognised enum value ";
            errorMessage += enumString;
            Splatoon.LOGGER.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
    }

    public static class CommandStackNode
    {
        public enum Type {
            Subcommand,
            Argument,
            Execute,
            Permission
        }
        public String name;
        public Type type;
        public Supplier<ArgumentBuilder<CommandSourceStack, ?>> argumentSupplier = null;
        public Function<ExecuteContext, String> callback = null;
        public Function<CommandSourceStack, Boolean> permissionCallback = null;

        CommandStackNode(Type type)
        {
            this.type = type;
        }

        public static CommandStackNode executes(Function<ExecuteContext, String> callback) {
            CommandStackNode node = new CommandStackNode(Type.Execute);
            node.callback = callback;
            node.name = "executes";
            return node;
        }

        public static CommandStackNode permission(Function<CommandSourceStack, Boolean> callback) {
            CommandStackNode node = new CommandStackNode(Type.Permission);
            node.permissionCallback = callback;
            node.name = "permissionCb";
            return node;
        }

        public static CommandStackNode subcommand(String name) {
            CommandStackNode node = new CommandStackNode(Type.Subcommand);
            node.name = name;
            return node;
        }

        public static CommandStackNode argumentInteger(String name) {
            CommandStackNode node = new CommandStackNode(Type.Argument);
            node.name = name;
            node.argumentSupplier = ()->Commands.argument(name, IntegerArgumentType.integer());
            return node;
        }

        public static CommandStackNode argumentString(String name) {
            CommandStackNode node = new CommandStackNode(Type.Argument);
            node.name = name;
            node.argumentSupplier = ()->Commands.argument(name, StringArgumentType.word());
            return node;
        }

        public static CommandStackNode argumentString(String name, List<String> autocomplete) {
            CommandStackNode node = new CommandStackNode(Type.Argument);
            node.name = name;
            node.argumentSupplier = ()->Commands.argument(name, StringArgumentType.word()).suggests(
            (ctx, builder)->{
                return SharedSuggestionProvider.suggest(autocomplete, builder);
            });
            return node;
        }

        public static CommandStackNode argumentPlayer(String name) {
            CommandStackNode node = new CommandStackNode(Type.Argument);
            node.name = name;
            node.argumentSupplier = ()->Commands.argument(name, EntityArgument.player());
            return node;
        }
    }

    Stack<CommandStackNode> commandStackQueue = new Stack<>();

//    LiteralArgumentBuilder<CommandSourceStack> commandStack;
//    CommandNode<CommandSourceStack> commandStackTop;

    CommandBuilder(String command)
    {
        commandStackQueue.add(CommandStackNode.subcommand(command));
        //this.commandStack = Commands.literal(command);
    }

    public static CommandBuilder command(String command)
    {
        return new CommandBuilder(command);
    }

    public CommandBuilder subcommand(String command)
    {
        //commandStack = commandStack.then(Commands.literal(command));
        commandStackQueue.add(CommandStackNode.subcommand(command));
        return this;
    }

    public CommandBuilder argumentInteger(String name)
    {
        //commandStack = commandStack.then(Commands.argument(name, IntegerArgumentType.integer()));
        commandStackQueue.add(CommandStackNode.argumentInteger(name));
        return this;
    }

    public CommandBuilder argumentPlayer(String name)
    {
        //commandStack = commandStack.then(Commands.argument(name, EntityArgument.player()));
        commandStackQueue.add(CommandStackNode.argumentPlayer(name));
        return this;
    }

    public CommandBuilder argumentString(String name)
    {
        //commandStack = commandStack.then(Commands.argument(name, StringArgumentType.word()));
        commandStackQueue.add(CommandStackNode.argumentString(name));
        return this;
    }

    public CommandBuilder argumentString(String name, List<String> autocomplete)
    {
        commandStackQueue.add(CommandStackNode.argumentString(name, autocomplete));
//        Commands.argument("abilityName", StringArgumentType.word())
//                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
//                        Arrays.stream(AbilityManager.AbilityEnum.values()).map(AbilityManager.AbilityEnum::toString), builder
//                )
//        commandStack = commandStack.then(Commands.argument(name, StringArgumentType.word()).suggests(
//                (ctx,builder)->SharedSuggestionProvider.suggest(
//                        autocomplete, builder
//                )
//        ));
        return this;
    }

    public <T extends Enum<T>> CommandBuilder argumentEnum(String name, Class<T> tEnum)
    {
        List<String> enumStrings = Arrays.stream(tEnum.getEnumConstants()).map(T::toString).toList();
        return argumentString(name, enumStrings);
    }



    public CommandBuilder executes(Function<ExecuteContext, String> callback)
    {
        commandStackQueue.add(CommandStackNode.executes(callback));
//        commandStack.executes(ctx->{
//            String responseMessage = callback.apply(new ExecuteContext(ctx));
//            boolean failed = responseMessage.startsWith("!");
//            if(failed) {
//                ctx.getSource().sendFailure(
//                        Component.literal(responseMessage)
//                );
//            }
//            else {
//                ctx.getSource().sendSuccess(()->Component.literal(responseMessage), true);
//            }
//            return failed ? 0 : 1;
//        });
        return this;
    }

    public CommandBuilder permission(Function<CommandSourceStack, Boolean> permissionCallback)
    {
        commandStackQueue.add(CommandStackNode.permission(permissionCallback));
        return this;
    }

    public void register()
    {
//        LiteralArgumentBuilder<CommandSourceStack> command;
//        LiteralCommandNode<CommandSourceStack> topCommand;

        var ref = new Object() {
            LiteralArgumentBuilder<CommandSourceStack> topLiteralCommand = null;
        };
        ArgumentBuilder<CommandSourceStack, ?> topCommand = null;

        var lambdaContext = new Object() {
            Function<ExecuteContext, String> pendingExecuteCallback = null;
            Function<CommandSourceStack, Boolean> permissionCallback = null;
        };

        while(!commandStackQueue.empty())
        {
            CommandStackNode top = commandStackQueue.peek();

            Splatoon.LOGGER.info("command stack node {}", top.name);

            if(top.type == CommandStackNode.Type.Subcommand) {
                //command.then()
                if(topCommand != null) {
                    ref.topLiteralCommand = Commands.literal(top.name).then(topCommand);
                }
                else {
                    ref.topLiteralCommand = Commands.literal(top.name);
                }
                topCommand = ref.topLiteralCommand;
            }
            else if(top.type == CommandStackNode.Type.Argument)
            {
                if(topCommand != null) {
                    topCommand = top.argumentSupplier.get().then(topCommand);
                }
                else {
                    topCommand = top.argumentSupplier.get();
                }
            }
            else if(top.type == CommandStackNode.Type.Execute)
            {
                lambdaContext.pendingExecuteCallback = top.callback;
            }
            else if(top.type == CommandStackNode.Type.Permission)
            {
                lambdaContext.permissionCallback = top.permissionCallback;
            }

            if(top.type == CommandStackNode.Type.Argument || top.type == CommandStackNode.Type.Subcommand)
            {
                if(lambdaContext.pendingExecuteCallback != null) {
                    topCommand = topCommand.executes((ctx->{
                        String responseMessage = lambdaContext.pendingExecuteCallback.apply(new ExecuteContext(ctx));

                        boolean hasPermission = true;
                        if(lambdaContext.permissionCallback == null) {
                            hasPermission = ctx.getSource().hasPermission(2);
                        }
                        else {
                            hasPermission = lambdaContext.permissionCallback.apply(ctx.getSource());
                        }

                        boolean failed = responseMessage.startsWith("!");
                        if(failed) {
                            ctx.getSource().sendFailure(
                                    Component.literal(responseMessage)
                            );
                            throw new SimpleCommandExceptionType(Component.literal(responseMessage)).create();

                        }
                        else {
                            ctx.getSource().sendSuccess(()->Component.literal(responseMessage), true);
                            return Command.SINGLE_SUCCESS;
                        }
                    }));

                    //lambdaContext.pendingExecuteCallback = null;
                }
            }


            commandStackQueue.pop();
        }

        CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> {
            dispatcher.register(ref.topLiteralCommand);
        }));
    }

}
