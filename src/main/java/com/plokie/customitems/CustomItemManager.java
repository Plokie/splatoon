package com.plokie.customitems;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.plokie.Splatoon;
import com.plokie.helpers.CommandBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class CustomItemManager {
    static class ExtraCustomItemTick {
        public CustomItem item;
        public int ticksLeft;
        public int timeUsing = 0;
    }
    Map<Player, ExtraCustomItemTick> extraItemUsageTicks = new HashMap<>();

    Map<CustomItem, List<Player>> isPlayerHolding = new HashMap<>();

    public CustomItemManager()
    {
        ServerTickEvents.END_SERVER_TICK.register((server)->{
            server.getPlayerList().getPlayers().forEach(player -> {
                Arrays.stream(CustomItem.values()).forEach(item -> {
                    ItemStack itemInHand = player.getItemInHand(player.getUsedItemHand());

                    List<Player> playersHolding = isPlayerHolding.get(item);
                    if(playersHolding == null)
                    {
                        isPlayerHolding.put(item, new ArrayList<>());
                        playersHolding = isPlayerHolding.get(item);
                    }

                    //if(itemInHand.getItemName().equals(item.getItem().getItemName()))
                    if(item.is(itemInHand))
                    {
                        //item.getItemDefinition().getItemInterface().whileHeld(player);

                        if(!playersHolding.contains(player)  )
                        {
                            playersHolding.add(player);
                            item.getItemDefinition().getItemInterface().onStartHeld(player);
                        }
                    }
                    else
                    {
                        if(playersHolding.contains(player))
                        {
                            item.getItemDefinition().getItemInterface().onEndHeld(player);
                            playersHolding.remove(player);
                        }
                    }
                });
            });

            for(Map.Entry<Player, ExtraCustomItemTick> entry : new HashMap<>(extraItemUsageTicks).entrySet())
            {
                Player player = entry.getKey();
                CustomItem item = entry.getValue().item;

                if(entry.getValue().timeUsing % item.getItemDefinition().getItemInterface().getUsageRate() == 0)
                {
                    if(entry.getValue().ticksLeft <= 0) {
                        extraItemUsageTicks.remove(player);
                    }
                    else
                    {
                        //if(player.getItemInHand(player.getUsedItemHand()).getItemName().equals(item.getItem().getItemName()))
                        if(item.is(player.getItemInHand(player.getUsedItemHand())))
                        {
                            item.getItemDefinition().getItemInterface().onUseItem(player);
                        }

                    }
                }

                //Splatoon.LOGGER.info("Ticks left: {}, time using: {}", entry.getValue().ticksLeft, entry.getValue().timeUsing);

                entry.getValue().timeUsing++;

                entry.getValue().ticksLeft--;

//                if(entry.getValue().ticksLeft <= 0) {
//                    extraItemUsageTicks.remove(player);
//                }
            }
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            //Splatoon.LOGGER.info("Use {}", player.getItemInHand(hand).getItemName());
            //Splatoon.LOGGER.info("test: {}", player.getItemInHand(hand).getHoverName());


            Arrays.stream(CustomItem.values()).forEach(item -> {
                //Splatoon.LOGGER.info("\tCheck if {}", item.getItem().getItemName());
                if(item.is(player.getItemInHand(hand)))
                {
                    //Splatoon.LOGGER.info("\tit is");
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

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            //Splatoon.LOGGER.info("Use block {}", player.getItemInHand(hand).getItemName());
            Arrays.stream(CustomItem.values()).forEach(item->{
                //Splatoon.LOGGER.info("\tCheck if {}", item.getItem().getItemName());
                if(item.is(player.getItemInHand(hand))) {
                    //.LOGGER.info("\tit is");
                    item.getItemDefinition().getItemInterface().onUseBlock(player, hitResult);
                }
            });
            return InteractionResult.PASS;
        });

        AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult)->{

            Arrays.stream(CustomItem.values()).forEach(item -> {
                //if (player.getItemInHand(hand).getItemName().equals(item.getItem().getItemName()))
                if(item.is(player.getItemInHand(hand)))
                {
                    item.getItemDefinition().getItemInterface().onAttackHit(player, entity);
                }
            });

            return InteractionResult.PASS;
        });

        CommandBuilder.command("givecustomitem").argumentPlayer("target").argumentEnum("item_id", CustomItem::values).argumentInteger("count").executes(ctx->{
            try {
                int count = ctx.getArgumentInteger("count");
                CustomItem item = ctx.getArgumentEnum("item_id", CustomItem.class);
                ServerPlayer target = ctx.getArgumentPlayer("target");
                ItemEntity itemEntity = EntityType.ITEM.create(target.level(), EntitySpawnReason.COMMAND);
                if(itemEntity == null) return "! Failed to spawn item entity on target";

                ItemStack itemStack = item.construct(target);
                itemStack.setCount(count);
                itemEntity.setItem(itemStack);

                target.level().addFreshEntity(itemEntity);

                itemEntity.setPos(target.getEyePosition());

                return "Gave " + target.getName().getString() + " " + count + " " + item.toString();
            } catch (CommandSyntaxException e) {
                return "! Invalid target";
            } catch(IllegalArgumentException e) {
                return "! Invalid item id or missing count argument";
            }

        }).register();
    }

    public void tick()
    {

    }
}
