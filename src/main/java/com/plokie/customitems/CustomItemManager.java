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
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.stream.Stream;

public class CustomItemManager {
    static class ExtraCustomItemTick {
        public CustomItem item;
        public int ticksLeft;
        public int timeUsing = 0;
    }
    Map<UUID, Map<CustomItem, ExtraCustomItemTick>> extraItemUsageTicks = new HashMap<>();

    Map<CustomItem, List<Player>> isPlayerHolding = new HashMap<>();

    public CustomItemManager()
    {
        ServerTickEvents.END_SERVER_TICK.register((server)->{
            server.getPlayerList().getPlayers().forEach(player -> {

                Arrays.stream(CustomItem.values()).forEach(item -> {
                    ItemStack itemInHand = player.getItemBySlot(EquipmentSlot.MAINHAND);

                    List<Player> playersHolding = isPlayerHolding.get(item);
                    if(playersHolding == null)
                    {
                        isPlayerHolding.put(item, new ArrayList<>());
                        playersHolding = isPlayerHolding.get(item);
                    }

                    if(item.is(itemInHand))
                    {
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

//            server.getPlayerList().getPlayers().forEach(player -> {
//                MutableComponent actionbarDebug = Component.literal("");
//                for(Map.Entry<UUID, Map<CustomItem, ExtraCustomItemTick>> entry : new HashMap<>(extraItemUsageTicks).entrySet()) {
//                    if(entry.getKey() != player.getUUID()) continue;
//                    for(var itemEntry : entry.getValue().entrySet()) {
//                        ExtraCustomItemTick extra = itemEntry.getValue();
//                        CustomItem item = extra.item;
//                        int ticksLeft = extra.ticksLeft;
//                        int timeUsing = extra.timeUsing;
//                        int modResult = itemEntry.getValue().timeUsing % item.getItemDefinition().getItemInterface().getUsageRate();
//
//                        actionbarDebug.append(Component.literal(item.toString()+":"));
//                        actionbarDebug.append(Component.literal(" "+ticksLeft));
//                        actionbarDebug.append(Component.literal(" "+timeUsing));
//                        actionbarDebug.append(Component.literal(" "+modResult));
//                        actionbarDebug.append(Component.literal("\n"));
//                    }
//                }
//                player.connection.send(new ClientboundSetActionBarTextPacket(actionbarDebug));
//            });

            for(Map.Entry<UUID, Map<CustomItem, ExtraCustomItemTick>> entry : new HashMap<>(extraItemUsageTicks).entrySet())
            {
                Player player = Splatoon.SERVER.getPlayerList().getPlayer(entry.getKey());
                if(player == null) continue;

                var itemsInUse = entry.getValue();

                for(var itemEntry : new ArrayList<>(itemsInUse.entrySet().stream().toList()) ) {

                    ExtraCustomItemTick extra = itemEntry.getValue();
                    CustomItem item = extra.item;

                    if(extra.timeUsing % item.getItemDefinition().getItemInterface().getUsageRate() == 0)
                    {
                        if(extra.ticksLeft <= 0) {
                            itemsInUse.remove(item);
                        }
                        else
                        {
                            if(item.is(player.getItemBySlot(EquipmentSlot.MAINHAND)))
                            {
                                item.getItemDefinition().getItemInterface().onUseItem(player);
                            }

                        }
                    }

                    itemEntry.getValue().timeUsing++;
//                    if(item.is(player.getItemBySlot(EquipmentSlot.MAINHAND))
//                    {
//                    }


                    itemEntry.getValue().ticksLeft--;

                }

            }
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if(hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;

            //Splatoon.LOGGER.info("useitemcallback {}", player.getItemBySlot(EquipmentSlot.MAINHAND).getItemName());


            Arrays.stream(CustomItem.values()).forEach(item -> {
                //Splatoon.LOGGER.info("\tCheck if {}", item.getItem().getItemName());
                if(item.is(player.getItemBySlot(EquipmentSlot.MAINHAND)))
                {
                    //Splatoon.LOGGER.info("\tit is");
                    int useDuration = item.getItemDefinition().getItemInterface().getUseDuration();

                    if(useDuration > 0)
                    {
                        if(!extraItemUsageTicks.containsKey(player.getUUID()))
                        {
                            extraItemUsageTicks.put(player.getUUID(), new HashMap<>());
                        }

                        var itemsInUse = extraItemUsageTicks.get(player.getUUID());

                        if(itemsInUse.containsKey(item)) {
                            itemsInUse.get(item).ticksLeft = useDuration;
                        }
                        else {
                            ExtraCustomItemTick itemTick = new ExtraCustomItemTick();
                            itemTick.item = item;
                            itemTick.ticksLeft = useDuration;
                            itemsInUse.put(item, itemTick);
                        }

                    }
                }
            });

            return InteractionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if(hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;

            Arrays.stream(CustomItem.values()).forEach(item->{

                if(item.is(player.getItemBySlot(EquipmentSlot.MAINHAND))) {
                    //.LOGGER.info("\tit is");
                    item.getItemDefinition().getItemInterface().onUseBlock(player, hitResult);
                }
            });
            return InteractionResult.PASS;
        });

        AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult)->{
            if(hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;

            Arrays.stream(CustomItem.values()).forEach(item -> {
                if(item.is(player.getItemBySlot(EquipmentSlot.MAINHAND)))
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
