package com.plokie.classes;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.plokie.classes.abilities.AbilityManager;
import com.plokie.interfaces.IPlayerMixin;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Arrays;
import java.util.List;

public class SplatoonClasses {
    public enum SplatoonClass {
        Bomber("Bomber", Arrays.asList("CleansingGrenade", "InkBombs")),
        Roller("Roller", Arrays.asList("Hook")),
        Scout("Scout", Arrays.asList("EnderPearl", "WindCharges")),
        Support("Support", Arrays.asList("HealthBubble", "HealthPotions")),
        Jumper("Jumper", Arrays.asList("WindCharges")),
        Sniper("Sniper", Arrays.asList("FocusApple", "SmokeGrenade"));

        public final String name;
        public final List<String> abilities;

        SplatoonClass(String name, List<String> abilities)
        {
            this.name = name;
            this.abilities = abilities;
        }

        public String getName()  { return this.name; }
        public String getID()  { return this.toString(); }
    }

    public SplatoonClasses()
    {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    Commands.literal("class")
                            .then(Commands.literal("set")
                                    .then(
                                            Commands.argument("target", EntityArgument.player())
                                                    .then(
                                                            Commands.argument("classId", StringArgumentType.word())
                                                                    .suggests((ctx, builder) -> {
                                                                                ServerPlayer target = EntityArgument.getPlayer(ctx, "target");

                                                                                List<String> classesAutocomplete = new java.util.ArrayList<>();
                                                                                for (String classId : Arrays.stream(SplatoonClasses.SplatoonClass.values()).map(SplatoonClasses.SplatoonClass::getID).toList()) {
                                                                                    classesAutocomplete.add(classId);
                                                                                }
                                                                                classesAutocomplete.add("none");

                                                                                return SharedSuggestionProvider.suggest(
                                                                                        classesAutocomplete.stream(), builder
                                                                                );
                                                                            }
                                                                    )
                                                                    .executes(ctx -> {
                                                                        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");

                                                                        String classIdValue = StringArgumentType.getString(ctx, "classId");
                                                                        if(classIdValue.equals("none"))
                                                                        {
                                                                            ((IPlayerMixin)(Player)target).setClass(null);

                                                                            return 1;
                                                                        }
                                                                        else {
                                                                            try {
                                                                                SplatoonClasses.SplatoonClass classEnum = SplatoonClasses.SplatoonClass.valueOf(classIdValue);

                                                                                ((IPlayerMixin)(Player)target).setClass(classEnum);

                                                                                ctx.getSource().sendSuccess(() ->
                                                                                                Component.literal("Set ")
                                                                                                        .append(target.getDisplayName())
                                                                                                        .append(" class to ")
                                                                                                        .append(classEnum.toString())
                                                                                        , true
                                                                                );

                                                                                return 1;
                                                                            } catch (IllegalArgumentException e) {
                                                                                ctx.getSource().sendFailure(
                                                                                        Component.literal("Unrecognised class: ").append(classIdValue)
                                                                                );

                                                                                return 0;
                                                                            }
                                                                        }


                                                                    })
                                                    )
                                    )
                            )
            );
        });
    }
}
