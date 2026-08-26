package com.plokie.customitems;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.plokie.Splatoon;
import com.plokie.classes.SplatoonClasses;
import com.plokie.interfaces.IPlayerMixin;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

import java.util.*;

public class CustomItemManager {
    static class ExtraCustomItemTick {
        public CustomItem item;
        public int ticksLeft;
        public int timeUsing = 0;
    }
    Map<Player, ExtraCustomItemTick> extraItemUsageTicks = new HashMap<>();

    public CustomItemManager()
    {
        ServerTickEvents.END_SERVER_TICK.register((server)->{
            for(Map.Entry<Player, ExtraCustomItemTick> entry : new HashMap<>(extraItemUsageTicks).entrySet())
            {
                Player player = entry.getKey();
                CustomItem item = entry.getValue().item;

                if(player.getItemInHand(player.getUsedItemHand()).getItemName().equals(item.getItem().getItemName()))
                {
                    if(entry.getValue().timeUsing % item.getItemDefinition().getItemInterface().getUsageRate() == 0)
                    {
                        item.getItemDefinition().getItemInterface().onUse(player);
                    }
                }

                //Splatoon.LOGGER.info("Ticks left: {}, time using: {}", entry.getValue().ticksLeft, entry.getValue().timeUsing);

                entry.getValue().timeUsing++;

                entry.getValue().ticksLeft--;

                if(entry.getValue().ticksLeft <= 0) {
                    extraItemUsageTicks.remove(player);
                }
            }
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            Arrays.stream(CustomItem.values()).forEach(item -> {
                if(player.getItemInHand(hand).getItemName().equals(item.getItem().getItemName()))
                {
                    int useDuration = item.getItemDefinition().getItemInterface().getUseDuration();

                    if(useDuration > 0)
                    {
                        if(extraItemUsageTicks.containsKey(player))
                        {
                            extraItemUsageTicks.get(player).ticksLeft = useDuration;
                        }
                        else
                        {
                            ExtraCustomItemTick itemTick = new ExtraCustomItemTick();
                            itemTick.item = item;
                            itemTick.ticksLeft = useDuration;
                            extraItemUsageTicks.put(player, itemTick);
                        }
                    }
                }
            });

            return InteractionResult.PASS;
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    Commands.literal("givecustomitem")
                            .then(
                                    Commands.argument("target", EntityArgument.player())
                                            .then(
                                                    Commands.argument("itemId", StringArgumentType.word())
                                                            .suggests((ctx, builder) -> {
                                                                ServerPlayer target = EntityArgument.getPlayer(ctx, "target");

                                                                List<String> itemAutocomplete = new java.util.ArrayList<>(Arrays.stream(CustomItem.values()).map(CustomItem::toString).toList());

                                                                return SharedSuggestionProvider.suggest(
                                                                        itemAutocomplete.stream(), builder
                                                                );
                                                            })
                                                            .executes(ctx -> {
                                                                ServerPlayer target = EntityArgument.getPlayer(ctx, "target");

                                                                String itemIdValue = StringArgumentType.getString(ctx, "itemId");
                                                                try {
                                                                    CustomItem itemEnum = CustomItem.valueOf(itemIdValue);

                                                                    ItemEntity itemEntity = EntityType.ITEM.create(target.level(), EntitySpawnReason.COMMAND);
                                                                    if(itemEntity == null) return 0;

                                                                    itemEntity.setItem(itemEnum.getItem());

                                                                    target.level().addFreshEntity(itemEntity);

                                                                    itemEntity.setPos(target.getEyePosition());

                                                                    ctx.getSource().sendSuccess(() ->
                                                                                    Component.literal("Gave ")
                                                                                            .append(target.getDisplayName())
                                                                                            .append(" 1 ")
                                                                                            .append(itemEnum.toString())
                                                                            , true
                                                                    );

                                                                    return 1;
                                                                } catch (IllegalArgumentException e) {
                                                                    ctx.getSource().sendFailure(
                                                                            Component.literal("Unrecognised class: ").append(itemIdValue)
                                                                    );

                                                                    return 0;
                                                                }



                                                            })
                                            )
                            )

            );
        });
    }

    public void tick()
    {

    }
}
