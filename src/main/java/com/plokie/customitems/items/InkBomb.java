package com.plokie.customitems.items;

import com.plokie.customitems.ICustomItem;
import com.plokie.helpers.Affects;
import com.plokie.helpers.ScheduleEvent;
import com.plokie.helpers.Teams;
import com.plokie.helpers.items.Enchantments;
import com.plokie.interfaces.IPlayerTeamMixin;
import com.plokie.interfaces.IProjectile;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AdventureModePredicate;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

public class InkBomb extends ICustomItem {

    public static void dataCallback(Player player, ItemStack item)
    {
        BlockPredicate blockPredicate = BlockPredicate.Builder.block().build();
        AdventureModePredicate canPlaceOn = new AdventureModePredicate(List.of(blockPredicate));
        item.set(
                DataComponents.CAN_PLACE_ON,
                canPlaceOn
        );

        //Enchantments.AddEnchantmentToItem(item, "knockback", 5);

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
    }

    @Override
    public void onUseBlock(Player player, BlockHitResult hit)
    {
        super.onUseBlock(player, hit);

        ScheduleEvent.schedule(1, server->{
                    //Splatoon.LOGGER.info("\tTick task {}", player.getName());
                    AABB area = new AABB(hit.getBlockPos()).inflate(2.0);

                    player.level().getEntitiesOfClass(Sheep.class, area).forEach(sheep -> {
                        //Splatoon.LOGGER.info("\tFound sheep {}", sheep.getName());
                        ((IProjectile)sheep).setPlayerOwner(player);
                    });
                }
        );
    }

    @Override
    public void whileHeld(Player player) {
        for (Player otherPlayer : player.level().getEntitiesOfClass(Player.class, new AABB(player.getOnPos()).inflate(200.0))) {
            if(otherPlayer == player) continue;

            if(otherPlayer.distanceTo(player) <= 5.0) {
                Affects.setAttributeModifier(otherPlayer, "knockback_resistance", "bomberknockback", 100.0, AttributeModifier.Operation.ADD_VALUE);
            }
            else {
                Affects.removeAttributeModifier(otherPlayer, "knockback_resistance", "bomberknockback");
            }
        }
    }

    @Override
    public void onStartHeld(Player player) {

    }

    @Override
    public void onEndHeld(Player player) {
        for (Player otherPlayer : player.level().players()) {
            Affects.removeAttributeModifier(otherPlayer, "knockback_resistance", "bomberknockback");
        }
    }
}
