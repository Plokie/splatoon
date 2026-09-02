package com.plokie.customitems.items;

import com.mojang.math.Transformation;
import com.plokie.Splatoon;
import com.plokie.customitems.ICustomItem;
import com.plokie.helpers.Teams;
import com.plokie.interfaces.IPlayerMixin;
import com.plokie.interfaces.IPlayerTeamMixin;
import com.plokie.interfaces.IProjectile;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.commands.data.DataCommands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.joml.Vector3f;

public class InkPuck extends ICustomItem {
    @Override
    public void onUseBlock(Player player, BlockHitResult hit) {
        super.onUseBlock(player, hit);

        Splatoon.LOGGER.info("Use ink puck on block {} @ {}", hit.getBlockPos(), hit.getLocation());

        IPlayerTeamMixin teamMixin = Teams.getTeamMixinFromPlayer(player);
        if(teamMixin == null) return;

        ServerLevel level = (ServerLevel)player.level();

        Display.BlockDisplay puck = EntityType.BLOCK_DISPLAY.create(player.level(), EntitySpawnReason.SPAWN_ITEM_USE);
        if(puck == null) return;
        puck.setBlockState(teamMixin.getWallBlock().defaultBlockState());

        { // this fucking sucks

            HolderLookup.Provider registries = puck.level().registryAccess();

            TagValueOutput vo = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
            puck.saveWithoutId(vo);

            CompoundTag nbt = vo.buildResult();

            nbt.putInt("teleport_duration", 1);

            TagValueInput vi = (TagValueInput) TagValueInput.create(ProblemReporter.DISCARDING, registries, nbt);

            puck.load(vi);
        }

        float scale = 1.0f;
        Transformation transform = new Transformation(
                new Vector3f(-scale * 0.5f, -scale * 0.5f, -scale * 0.5f),
                null,
                new Vector3f(scale, scale, scale),
                null
        );
        puck.setTransformation(transform);

        puck.setPos(hit.getLocation());
        puck.setYRot(player.getYRot());
        puck.addTag("InkPuck");

        level.addFreshEntity(puck);

        ((IProjectile)puck).setPlayerOwner(player);
    }

}
