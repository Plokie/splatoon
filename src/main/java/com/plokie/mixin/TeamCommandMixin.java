package com.plokie.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.plokie.interfaces.IPlayerTeamMixin;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.TeamArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.PermissionCheck;
import net.minecraft.server.commands.TeamCommand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.scores.PlayerTeam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(TeamCommand.class)
public class TeamCommandMixin {

    @Inject(method = "register", at = @At("HEAD"))
    private static void onRegister(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext context,
            CallbackInfo ci
    )
    {
        dispatcher.register(
                Commands.literal("team")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("modify")
                                .then(Commands.argument("team", TeamArgument.team())
                                        .then(Commands.literal("groundBlock")
                                                .then(Commands.argument("block", ResourceArgument.resource(context, Registries.BLOCK))
                                                        .requires(source -> source.hasPermission(2))
                                                        .executes(TeamCommandMixin::setTeamGroundBlock)
                                        )

                                )
                        )
                )
        );

        dispatcher.register(
                Commands.literal("team")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("modify")
                                .then(Commands.argument("team", TeamArgument.team())
                                        .then(Commands.literal("wallBlock")
                                                .then(Commands.argument("block", ResourceArgument.resource(context, Registries.BLOCK))
                                                        .requires(source -> source.hasPermission(2))
                                                        .executes(TeamCommandMixin::setTeamWallBlock)
                                        )
                                )
                        )
                )
        );



        dispatcher.register(
                Commands.literal("team")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("query")
                                .then(Commands.argument("team", TeamArgument.team())
                                        .then(Commands.literal("groundBlock")
                                                .requires(source -> source.hasPermission(2))
                                                .executes(TeamCommandMixin::getTeamGroundBlock)
                                        )
                                        .then(Commands.literal("wallBlock")
                                                .requires(source -> source.hasPermission(2))
                                                .executes(TeamCommandMixin::getTeamWallBlock)
                                        )
                                )
                        )
        );
    }

    @Unique
    private static int setTeamGroundBlock(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        PlayerTeam team = TeamArgument.getTeam(ctx, "team");

        Holder.Reference<Block> blockHolder = ResourceArgument.getResource(ctx, "block", Registries.BLOCK);
        Block block = blockHolder.value();

        ((IPlayerTeamMixin)team).setGroundBlock(block);

        ctx.getSource().sendSuccess(() ->
                Component.literal("Set ground block for team ")
                        .append(team.getFormattedDisplayName())
                        .append(" to ")
                        .append(block.getName())
                , true
        );

        return 1;
    }

    @Unique
    private static int getTeamGroundBlock(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        PlayerTeam team = TeamArgument.getTeam(ctx, "team");

        Block block = ((IPlayerTeamMixin)team).getGroundBlock();
        String blockId = BuiltInRegistries.BLOCK.getKey(block).toString();

        ctx.getSource().sendSuccess(()->Component.literal("Got: " + blockId), false);

        return 1;
    }

    @Unique
    private static int setTeamWallBlock(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        PlayerTeam team = TeamArgument.getTeam(ctx, "team");

        Holder.Reference<Block> blockHolder = ResourceArgument.getResource(ctx, "block", Registries.BLOCK);
        Block block = blockHolder.value();

        ((IPlayerTeamMixin)team).setWallBlock(block);

        ctx.getSource().sendSuccess(() ->
                        Component.literal("Set wall block for team ")
                                .append(team.getFormattedDisplayName())
                                .append(" to ")
                                .append(block.getName())
                , true
        );

        return 1;
    }

    @Unique
    private static int getTeamWallBlock(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        PlayerTeam team = TeamArgument.getTeam(ctx, "team");

        Block block = ((IPlayerTeamMixin)team).getWallBlock();
        String blockId = BuiltInRegistries.BLOCK.getKey(block).toString();

        ctx.getSource().sendSuccess(()->Component.literal("Got: " + blockId), false);

        return 1;
    }
}
