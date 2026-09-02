package com.plokie.classes.abilities;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.datafixers.types.Func;
import com.plokie.commands.PingCommand;
import com.plokie.customitems.CustomItem;
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
        InkBombs("Ink Bombs", CustomItem.InkBomb, Ability.UsageTypeFlags.Block.value, 10.0f, 5),
        CleansingGrenade("Cleansing Grenade", CustomItem.CleansingGrenade, Ability.UsageTypeFlags.Item.value, 30.0f, 1),
        Hook("Hook", CustomItem.Hook, 0, 15.0f, 1),
        EnderPearl("Ender Pearl", CustomItem.EnderPearl, Ability.UsageTypeFlags.Item.value, 25.0f, 1),
        WindCharges("Wind Charges", CustomItem.WindCharge, Ability.UsageTypeFlags.Item.value, 5.0f, 5),
        HealthBubble("Health Bubble", CustomItem.HealthBubble, Ability.UsageTypeFlags.Block.value, 20.0f, 1),
        HealthPotions("Health Potions", CustomItem.HealthPotion, Ability.UsageTypeFlags.Item.value, 5.0f, 16),
        SuperJump("Super Jump", com.plokie.classes.abilities.SuperJump::new),
        SmokeGrenade("Smoke Grenade", CustomItem.SmokeGrenade, Ability.UsageTypeFlags.Item.value, 22.5f, 1),
        FocusApple("Focus Apple", CustomItem.FocusApple, Ability.UsageTypeFlags.Item.value, 30.f, 1),
        InkPuck("Ink Puck", CustomItem.InkPuck, Ability.UsageTypeFlags.Block.value,10.0f, 3)
        ;

        public static final Codec<AbilityEnum> CODEC = StringRepresentable.fromEnum(AbilityEnum::values);

        private final String name;
        private final Function<AbilityEnum, Ability> constructor;

        AbilityEnum(final String name, Function<AbilityEnum, Ability> constructor) {
            this.name = name;
            this.constructor = constructor;
        }

        AbilityEnum(final String name, CustomItem item, int usageTypeFlags, float rechargeTimeSeconds, int maxCount)
        {
            this.name = name;
            this.constructor = (enumVal)->{
                return new Ability(this, item, usageTypeFlags, rechargeTimeSeconds, maxCount);
            };
        }

        public Ability Construct() {
            return constructor.apply(this);
        }

        public String getID()  { return this.toString(); }

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
                                                                    .requires(source -> source.hasPermission(2))
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
                                                                    .requires(source -> source.hasPermission(2))
                                                    )
                                    )
                            )
            );
        });
    }
}
