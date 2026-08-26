package com.plokie.classes.abilities;

import com.plokie.Splatoon;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AdventureModePredicate;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import javax.xml.crypto.Data;
import java.util.List;
import java.util.Objects;

public class HealthBubble extends Ability {
    static boolean isStaticInitialised = false;

    public HealthBubble()
    {
        this.rechargeTime = 20*20;
        this.maxCount = 1;

        this.createItemFunc = player -> {
            ItemStack item = new ItemStack(Items.SHULKER_SPAWN_EGG);

            item.set(
                    DataComponents.ITEM_NAME,
                    Component.literal("Health Bubble")
            );

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

//            item.set(
//                    Data.ITEM_MODEL,
//                    ResourceLocation.fromNamespaceAndPath("splatoon","health_bubble")
//            );

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

            return item;
        };
    }

    void staticInitialise()
    {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if(!world.isClientSide() && player.getItemInHand(hand).getItem() == Items.SHULKER_SPAWN_EGG)
            {
                ((IPlayerMixin)player).onUseAbilityBlock("HealthBubble", player, hand, hitResult);
            }

            return InteractionResult.PASS;
        });

        isStaticInitialised = true;
    }

    @Override
    public void onUseBlock(Player player, InteractionHand hand, BlockHitResult hitResult, int abilityIndex) {
        super.onUse();

        Level level = player.level();

        Objects.requireNonNull(level.getServer()).schedule(
                new TickTask(level.getServer().getTickCount() + 1, ()->{
                    AABB area = new AABB(hitResult.getBlockPos()).inflate(2.0);

                    level.getEntitiesOfClass(Shulker.class, area).forEach(shulker -> {
                        ((IProjectile)shulker).setPlayerOwner(player);
                    });
                })
        );
    }

    @Override
    public void tick(Player player, int abilityIndex)
    {
        if(!isStaticInitialised)
        {
            staticInitialise();
        }

        super.tick(player, abilityIndex);
    }
}
