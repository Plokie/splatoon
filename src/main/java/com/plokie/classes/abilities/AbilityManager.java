package com.plokie.classes.abilities;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.plokie.commands.PingCommand;
import com.plokie.interfaces.IPlayerMixin;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.recipebook.ServerPlaceRecipe;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import com.mojang.serialization.Codec;
import net.minecraft.world.entity.player.Player;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class AbilityManager {
    public enum AbilityEnum implements StringRepresentable {
        InkBombs("Ink Bombs", InkBombs::new),
        CleansingGrenade("Cleansing Grenade", CleansingGrenade::new),
        Hook("Hook", Hook::new),
        EnderPearl("Ender Pearl", EnderPearl::new),
        WindCharges("Wind Charges", WindCharges::new),
        HealthBubble("Health Bubble", HealthBubble::new),
        HealthPotions("Health Potions", HealthPotions::new),
        SuperJump("Super Jump", SuperJump::new),
        SmokeGrenade("Smoke Grenade", SmokeGrenade::new),
        FocusApple("Focus Apple", FocusApple::new);

        public static final Codec<AbilityEnum> CODEC = StringRepresentable.fromEnum(AbilityEnum::values);

        private final String name;
        private final Supplier<Ability> constructor;

        AbilityEnum(final String name, Supplier<Ability> constructor) {
            this.name = name;
            this.constructor = constructor;
        }

        public Ability Construct() {
            return constructor.get();
        }

        public String getID()  { return this.getClass().getSimpleName(); }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public AbilityManager()
    {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    Commands.literal("ability")
                            .then(Commands.literal("grant")
                                    .then(
                                            Commands.argument("target", EntityArgument.player())
                                                    .then(
                                                            Commands.argument("abilityName", StringArgumentType.word())
                                                                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                                            Arrays.stream(AbilityEnum.values()).map(AbilityEnum::toString), builder
                                                                    ))
                                                                    .executes(ctx -> {
                                                                        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");

                                                                        String abilityValue = StringArgumentType.getString(ctx, "abilityName");
                                                                        try {
                                                                            AbilityEnum abilityEnum = AbilityEnum.valueOf(abilityValue);

                                                                            ((IPlayerMixin)(Player)target).grantAbility(abilityEnum.Construct());

                                                                            ctx.getSource().sendSuccess(() ->
                                                                                    Component.literal("Granted ")
                                                                                    .append(target.getDisplayName())
                                                                                    .append(" ability ")
                                                                                    .append(abilityEnum.toString())
                                                                                , true
                                                                            );

                                                                            return 1;
                                                                        }
                                                                        catch (IllegalArgumentException e) {
                                                                            ctx.getSource().sendFailure(
                                                                                Component.literal("Unrecognised ability: ").append(abilityValue)
                                                                            );

                                                                            return 0;
                                                                        }

                                                                    })
                                                    )
                                    )
                            )
            );

            dispatcher.register(
                    Commands.literal("ability")
                            .then(Commands.literal("revoke")
                                    .then(
                                            Commands.argument("target", EntityArgument.player())
                                                    .then(
                                                            Commands.argument("abilityName", StringArgumentType.word())
                                                                    .suggests((ctx, builder) -> {
                                                                        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");

                                                                        List<String> abilitiesAutocomplete = new java.util.ArrayList<>();
                                                                        for(Ability ability : ((IPlayerMixin)(Player)target).getAbilities()) {
                                                                            abilitiesAutocomplete.add(ability.getClass().getSimpleName());
                                                                        }
                                                                        abilitiesAutocomplete.add("all");

                                                                        return SharedSuggestionProvider.suggest(
                                                                                abilitiesAutocomplete.stream(), builder
                                                                        );
                                                                    })
                                                                    .executes(ctx -> {
                                                                        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");

                                                                        String abilityValue = StringArgumentType.getString(ctx, "abilityName");
                                                                        if(abilityValue.equals("all"))
                                                                        {
                                                                            //ability.getClass().getSimpleName()
                                                                            ((IPlayerMixin)(Player)target).revokeAllAbilities();

                                                                            for(Ability ability : new java.util.ArrayList<>(((IPlayerMixin)(Player)target).getAbilities()))
                                                                            {

                                                                            }
                                                                            return 1;
                                                                        }
                                                                        else {
                                                                            try {
                                                                                AbilityEnum abilityEnum = AbilityEnum.valueOf(abilityValue);

                                                                                ((IPlayerMixin)(Player)target).revokeAbility(abilityValue);

                                                                                ctx.getSource().sendSuccess(() ->
                                                                                                Component.literal("Revoked ")
                                                                                                        .append(target.getDisplayName())
                                                                                                        .append(" ability ")
                                                                                                        .append(abilityEnum.toString())
                                                                                        , true
                                                                                );

                                                                                return 1;
                                                                            }
                                                                            catch (IllegalArgumentException e) {
                                                                                ctx.getSource().sendFailure(
                                                                                        Component.literal("Unrecognised ability: ").append(abilityValue)
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
