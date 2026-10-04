package com.plokie.customitems.items;

import com.plokie.customitems.CustomItem;
import com.plokie.customitems.ICustomItem;
import com.plokie.helpers.Teams;
import com.plokie.interfaces.IPlayerTeamMixin;
import com.plokie.interfaces.IProjectile;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.CommonColors;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.item.AdventureModePredicate;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class BuilderPickaxe extends ICustomItem {
    public static void dataCallback(Player player, ItemStack item)
    {
        BlockPredicate blockPredicate = BlockPredicate.Builder.block().build();
        IPlayerTeamMixin playerTeam = Teams.getTeamMixinFromPlayer(player);
        if(playerTeam != null)
        {
            blockPredicate = BlockPredicate.Builder.block().of(BuiltInRegistries.BLOCK, playerTeam.getAuxBlock(), Blocks.RAIL).build();
        }

        AdventureModePredicate canBreak = new AdventureModePredicate(List.of(blockPredicate));
        item.set(
                DataComponents.CAN_BREAK,
                canBreak
        );
    }

    public BuilderPickaxe()
    {
        this.useDuration = 5;
        this.usageRate = 15;
    }

    @Override
    public void whileHeld(Player player) {
//        BuildingRails.PlacedRails placedRails = BuildingRails.getRailsPlacedBy(player);
//        if(placedRails != null) {
//
//            ServerLevel level = (ServerLevel)player.level();
//            Vec3 eyePos = player.getEyePosition();
//
//            for(List<BuildingRails.RailNode> channel : placedRails.channels)
//            {
//                if(channel.isEmpty()) continue;
//
//                BuildingRails.RailNode node = channel.getFirst();
//                Vec3 nodePos = node.position.getCenter();
//
//                Vec3 dirToNode = nodePos.subtract(eyePos).normalize();
//
//                Vec3 particlePos = eyePos.add(dirToNode.scale(0.1));
//
////                level.sendParticles(
////                        (ServerPlayer)player,
////                        ParticleTypes.ENCHANTED_HIT,
////                        true, true,
////                        particlePos.x, particlePos.y, particlePos.z,
////                        1,
////                        0.0, 0.0, 0.0,
////                        0.0
////                );
//
//                DustParticleOptions dustParticleOptions = new DustParticleOptions(CommonColors.WHITE, 0.02f);
//
//                level.sendParticles(
//                        (ServerPlayer)player,
//                        dustParticleOptions,
//                        true, true,
//                        particlePos.x, particlePos.y, particlePos.z,
//                        1,
//                        0.0, 0.0, 0.0,
//                        0.0
//                );
//            }
//
//        }
    }
    @Override
    public void onStartHeld(Player player) {

    }
    @Override
    public void onEndHeld(Player player) {

    }

    @Override
    public void onUseItem(Player player)
    {
        ItemStack itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if(!CustomItem.BuilderPickaxe.is(itemInHand)) {
            return;
        }

        ResourceLocation model = itemInHand.get(DataComponents.ITEM_MODEL);
        if(model == null) {
            return;
        }

        boolean isThrown = model.equals(ResourceLocation.fromNamespaceAndPath("minecraft", "air"));
//        boolean isThrown = false;

        if(!isThrown) {
            itemInHand.set(DataComponents.ITEM_MODEL, ResourceLocation.fromNamespaceAndPath("minecraft", "air"));

            player.getCooldowns().addCooldown(itemInHand, 200);

            player.getCooldowns().addCooldown(BuiltInRegistries.ITEM.getKey(Items.CARROT_ON_A_STICK), 20);

            ThrownEgg thrownPickaxe = EntityType.EGG.create(player.level(), EntitySpawnReason.SPAWN_ITEM_USE);
            if(thrownPickaxe == null) return;

            thrownPickaxe.setItem(new ItemStack(Items.DIAMOND_PICKAXE));

            thrownPickaxe.addTag("ThrownPickaxe");

            thrownPickaxe.setOwner(player);

            player.level().addFreshEntity(thrownPickaxe);

            Vec3 forward = player.getForward();
            Vec3 spawnPos = player.getEyePosition();
            spawnPos.add(forward.multiply(1.0,1.0,1.0));
            spawnPos.add(player.getDeltaMovement().multiply(2.0,2.0,2.0));

            thrownPickaxe.setPos(player.getEyePosition());
            thrownPickaxe.setXRot(player.getXRot());
            thrownPickaxe.setYRot(player.getYRot());

            float force = 1.1f;
            Vec3 forceDir = forward.multiply(force, force, force);

            thrownPickaxe.setDeltaMovement(forceDir);

            ((IProjectile)thrownPickaxe).setPlayerOwner(player);
        }
    }
}
