package com.plokie.commands;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public class PingCommand extends Command {
    public static int execute(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("Pong!"), false);
        return 1;
    }
}
