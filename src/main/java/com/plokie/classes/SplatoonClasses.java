package com.plokie.classes;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.plokie.Splatoon;
import com.plokie.classes.abilities.Ability;
import com.plokie.classes.abilities.AbilityManager;
import com.plokie.customitems.CustomItem;
import com.plokie.interfaces.IPlayerMixin;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.w3c.dom.Attr;

import java.util.*;

public class SplatoonClasses {
    public enum SplatoonClass {
        Bomber("Bomber", SplatoonClassDefinition.Builder.start()
                .customItem(CustomItem.StandardSword)
                .customItem(CustomItem.Splattershot)
                .customItem(CustomItem.KnockbackBrush)
                .ability(AbilityManager.AbilityEnum.CleansingGrenade)
                .ability(AbilityManager.AbilityEnum.InkBombs)
                .attribute("scale", 0.3, AttributeModifier.Operation.ADD_VALUE)
                .effect(MobEffects.HEALTH_BOOST, 4)
        .build()),
        Roller("Roller", SplatoonClassDefinition.Builder.start()
                .customItem(CustomItem.InkRoller)
                .customItem(CustomItem.Shotgun)
                .ability(AbilityManager.AbilityEnum.Hook)
                .attribute("scale", 0.3, AttributeModifier.Operation.ADD_VALUE)
                .effect(MobEffects.HEALTH_BOOST, 4)
        .build()),
        Scout("Scout", SplatoonClassDefinition.Builder.start()
                .customItem(CustomItem.ScoutSword)
                .customItem(CustomItem.Dualies)
                .ability(AbilityManager.AbilityEnum.EnderPearl)
                .ability(AbilityManager.AbilityEnum.WindCharges)
                .attribute("scale", 0.1, AttributeModifier.Operation.ADD_VALUE)
                .attribute("gravity", 0.8, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                .attribute("jump_strength", 0.8, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
                .attribute("movement_speed", 0.8, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                .attribute("knockback_resistance", -2.0, AttributeModifier.Operation.ADD_VALUE)
                .attribute("explosion_knockback_resistance", 0.8, AttributeModifier.Operation.ADD_VALUE)
        .build()),
        Support("Support", SplatoonClassDefinition.Builder.start()
                .customItem(CustomItem.SupportAxe)
                .customItem(CustomItem.Burstshot)
                .ability(AbilityManager.AbilityEnum.HealthBubble)
                .ability(AbilityManager.AbilityEnum.HealthPotions)
                .attribute("scale", 0.3, AttributeModifier.Operation.ADD_VALUE)
                .effect(MobEffects.HEALTH_BOOST, 9)
        .build()),
        Jumper("Jumper", SplatoonClassDefinition.Builder.start()
                .customItem(CustomItem.JumperMace)
                .customItem(CustomItem.Revolver)
                .ability(AbilityManager.AbilityEnum.WindCharges)
                .ability(AbilityManager.AbilityEnum.SuperJump)
                .attribute("scale", 0.3, AttributeModifier.Operation.ADD_VALUE)
                .effect(MobEffects.HEALTH_BOOST, 4)
                .effect(MobEffects.HASTE, 5)
        .build()),
        Sniper("SniperGun", SplatoonClassDefinition.Builder.start()
                .customItem(CustomItem.SniperSword)
                .customItem(CustomItem.SniperGun)
                .ability(AbilityManager.AbilityEnum.FocusApple)
                .ability(AbilityManager.AbilityEnum.SmokeGrenade)
                .attribute("scale", 0.3, AttributeModifier.Operation.ADD_VALUE)
                .effect(MobEffects.HEALTH_BOOST, 4)
        .build());

        public final String name;
        public final SplatoonClassDefinition definition;

        SplatoonClass(String name, SplatoonClassDefinition classDefinition)
        {
            this.name = name;
            this.definition = classDefinition;

        }

        public String getName()  { return this.name; }
        public String getID()  { return this.toString(); }
    }

    public static class SplatoonClassDefinition
    {
        public final List<AbilityManager.AbilityEnum> abilities;
        public final List<CustomItem> customItems;
        public final Map<String, AttributeModifier> attributes;
        public final Map<Holder<MobEffect>, Integer> effects;

        public SplatoonClassDefinition(List<AbilityManager.AbilityEnum> abilities, List<CustomItem> customItems, Map<String, AttributeModifier> attributes, Map<Holder<MobEffect>, Integer> effects)
        {
            this.abilities = abilities;
            this.customItems = customItems;
            this.attributes = attributes;
            this.effects = effects;
        }

        public static class Builder
        {
            List<AbilityManager.AbilityEnum> abilities = new ArrayList<>();
            List<CustomItem> customItems = new ArrayList<>();
            Map<String, AttributeModifier> attributes = new HashMap<>();
            Map<Holder<MobEffect>, Integer> effects = new HashMap<>();

            public static SplatoonClassDefinition.Builder start()
            {
                return new SplatoonClassDefinition.Builder();
            }

            public Builder ability(AbilityManager.AbilityEnum ability)
            {
                abilities.add(ability);
                return this;
            }

            public Builder customItem(CustomItem customItem)
            {
                customItems.add(customItem);
                return this;
            }

            public Builder attribute(String attribute, double modifierValue, AttributeModifier.Operation operation)
            {
                AttributeModifier modifier = new AttributeModifier(
                        ResourceLocation.withDefaultNamespace("classattribute"),
                        modifierValue,
                        operation
                );

                attributes.put(attribute, modifier);

                return this;
            }

            public Builder effect(Holder<MobEffect> effect, int strength)
            {
                effects.put(effect, strength);
                return this;
            }

            public SplatoonClassDefinition build()
            {
                return new SplatoonClassDefinition(abilities, customItems, attributes, effects);
            }
        }
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
