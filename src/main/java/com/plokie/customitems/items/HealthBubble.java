package com.plokie.customitems.items;

import com.plokie.customitems.ICustomItem;
import com.plokie.helpers.ScheduleEvent;
import com.plokie.helpers.Teams;
import com.plokie.interfaces.IPlayerTeamMixin;
import com.plokie.interfaces.IProjectile;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AdventureModePredicate;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

public class HealthBubble extends ICustomItem {

    public static void dataCallback(Player player, ItemStack item)
    {
        BlockPredicate blockPredicate = BlockPredicate.Builder.block().build();
        IPlayerTeamMixin playerTeam = Teams.getTeamMixinFromPlayer(player);
//            if(playerTeam != null)
//            {
//                blockPredicate = BlockPredicate.Builder.block().of(BuiltInRegistries.BLOCK, playerTeam.getGroundBlock()).build();
//            }
        AdventureModePredicate canPlaceOn = new AdventureModePredicate(List.of(blockPredicate));
        item.set(
                DataComponents.CAN_PLACE_ON,
                canPlaceOn
        );
        CompoundTag entityNbt = new CompoundTag();
        entityNbt.putString("id", "minecraft:shulker");

        if(playerTeam != null) {
            entityNbt.putByte("Color", playerTeam.getTeamColourByte());
        }
        entityNbt.putByte("NoAI", (byte)1);
        entityNbt.putByte("Peek", (byte)100);
        entityNbt.putByte("AttachFace", (byte)0);
        entityNbt.putByte("Glowing", (byte)1);
        entityNbt.putFloat("Health", 100.0f);

        {
            ListTag attributesList = new ListTag();

            CompoundTag maxHealth = new CompoundTag();
            maxHealth.putString("id", "minecraft:max_health");
            maxHealth.putDouble("base", 150.0d);
            attributesList.add(maxHealth);

            entityNbt.put("attributes", attributesList);
        }

        {
            ListTag tagList = new ListTag();
            tagList.add(StringTag.valueOf("HealthBubble"));

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

        ScheduleEvent.schedule(
                1, server->{
                    AABB area = new AABB(hit.getBlockPos()).inflate(2.0);

                    player.level().getEntitiesOfClass(Shulker.class, area).forEach(shulker -> {
                        ((IProjectile)shulker).setPlayerOwner(player);
                    });
                }
        );
    }
}
