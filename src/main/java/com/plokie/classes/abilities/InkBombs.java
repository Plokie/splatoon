package com.plokie.classes.abilities;

import com.plokie.Splatoon;
import com.plokie.helpers.Affects;
import com.plokie.helpers.ScheduleEvent;
import com.plokie.helpers.items.Enchantments;
import com.plokie.helpers.Teams;
import com.plokie.interfaces.IPlayerMixin;
import com.plokie.interfaces.IPlayerTeamMixin;
import com.plokie.interfaces.IProjectile;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AdventureModePredicate;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BlockTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class InkBombs extends Ability {
    private static boolean isStaticInitialised = false;

    public InkBombs()
    {


        this.rechargeTime = 10 * 20; // 10 seconds to recharge an item
        this.maxCount = 5; // Up to 5 at a time

        this.createItemFunc = player -> {
            ItemStack item = new ItemStack(Items.SHEEP_SPAWN_EGG);

            item.set(
                    DataComponents.ITEM_NAME,
                    Component.literal("Ink Bombs")
            );

            item.set(
                    DataComponents.ITEM_MODEL,
                    ResourceLocation.fromNamespaceAndPath("minecraft", "tnt")
            );

//            BlockPredicate blockPredicate = BlockPredicate.Builder.block().of(BuiltInRegistries.BLOCK, BlockTags.CONCRETE_POWDER).build();
            BlockPredicate blockPredicate = BlockPredicate.Builder.block().build();

            AdventureModePredicate canPlaceOn = new AdventureModePredicate(List.of(blockPredicate));

            item.set(
                    DataComponents.CAN_PLACE_ON,
                    canPlaceOn
            );


            Enchantments.AddEnchantmentToItem(item, "knockback", 5);

            CompoundTag entityNbt = new CompoundTag();
            entityNbt.putString("id", "minecraft:sheep");
            entityNbt.putFloat("Health", 150.0f);

            IPlayerTeamMixin playerTeam = Teams.getTeamMixinFromPlayer(player);
            if(playerTeam != null) {
                entityNbt.putByte("Color", playerTeam.getTeamColourByte());
            }

            {
                ListTag attributesList = new ListTag();

                CompoundTag movementSpeed = new CompoundTag();
                movementSpeed.putString("id", "minecraft:movement_speed");
                movementSpeed.putDouble("base", -10.0d);
                attributesList.add(movementSpeed);

                CompoundTag maxHealth = new CompoundTag();
                maxHealth.putString("id", "minecraft:max_health");
                maxHealth.putDouble("base", 150.0d);
                attributesList.add(maxHealth);

                entityNbt.put("attributes", attributesList);
            }

            {
                ListTag effectsList = new ListTag();

                CompoundTag invisibility = new CompoundTag();
                invisibility.putString("id", "minecraft:invisibility");
                invisibility.putInt("amplifier", 1);
                invisibility.putInt("duration", -1);
                invisibility.putByte("show_particles", (byte)0);
                effectsList.add(invisibility);

                entityNbt.put("active_effects", effectsList);
            }

            {
                ListTag tagList = new ListTag();
                tagList.add(StringTag.valueOf("InkBomb"));

                entityNbt.put("Tags", tagList);
            }

            item.set(
                    DataComponents.ENTITY_DATA,
                    CustomData.of(entityNbt)
            );

            return item;
        };
    }

    void staticInitialise()
    {
        Splatoon.LOGGER.info("\tStatic initialising InkBomb");

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if(!world.isClientSide() && player.getItemInHand(hand).getItem() == Items.SHEEP_SPAWN_EGG)
            {
                Splatoon.LOGGER.info("\tUsing spawn egg {}", player.getName());

                ((IPlayerMixin)player).onUseAbilityBlock("InkBombs", player, hand, hitResult);
            }

            return InteractionResult.PASS;
        });

        isStaticInitialised = true;
    }

    @Override
    public void onGranted(Player player, int abilityIndex)
    {
        super.onGranted(player, abilityIndex);
    }

    @Override
    public void onRevoked(Player player, int abilityIndex)
    {
        super.onRevoked(player, abilityIndex);
    }

    @Override
    public void onUseBlock(Player player, InteractionHand hand, BlockHitResult hitResult, int abilityIndex)
    {
        super.onUse();

        Level level = player.level();

        ScheduleEvent.schedule(1, server->{
                //Splatoon.LOGGER.info("\tTick task {}", player.getName());
                AABB area = new AABB(hitResult.getBlockPos()).inflate(2.0);

                level.getEntitiesOfClass(Sheep.class, area).forEach(sheep -> {
                    //Splatoon.LOGGER.info("\tFound sheep {}", sheep.getName());
                    ((IProjectile)sheep).setPlayerOwner(player);
                });
            }
        );
    }

    @Override
    public void tick(Player player, int abilityIndex)
    {
        for (Player otherPlayer : player.level().getEntitiesOfClass(Player.class, new AABB(player.getOnPos()).inflate(200.0))) {
            if(otherPlayer == player) continue;

            if(otherPlayer.distanceTo(player) <= 5.0) {
                Affects.setAttributeModifier(otherPlayer, "knockback_resistance", "bomberknockback", 100.0, AttributeModifier.Operation.ADD_VALUE);
            }
            else {
                Affects.removeAttributeModifier(otherPlayer, "knockback_resistance", "bomberknockback");
            }
        }

        if(!isStaticInitialised)
        {
            staticInitialise();
        }

        super.tick(player, abilityIndex);
    }


}
