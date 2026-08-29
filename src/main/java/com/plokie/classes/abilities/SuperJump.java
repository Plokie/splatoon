package com.plokie.classes.abilities;

import com.plokie.Splatoon;
import com.plokie.customitems.items.guns.SniperGun;
import com.plokie.interfaces.IPlayerMixin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class SuperJump extends Ability {
    boolean alreadyDashed = false;

    public SuperJump()
    {
        this.rechargeTime = (int)7.5 * 20;
        this.maxCount = 1;

        this.hideWhileInInk = false;

        this.createItemFunc = player -> {
            ItemStack item = new ItemStack(Items.FEATHER);

            item.set(
                    DataComponents.ITEM_NAME,
                    Component.literal("Super jump")
            );

            return item;
        };
    }

    @Override
    public void tick(Player player, int abilityIndex) {
        IPlayerMixin playerMixin = (IPlayerMixin)player;

        if(count > 0)
        {
            // && !playerMixin.isInInkOnWall()
            if(playerMixin.isInInk() && playerMixin.getInput().jump())
            {
                Vec3 velocity = player.getDeltaMovement();
                velocity = new Vec3(velocity.x, velocity.y + 1.1f, velocity.z);
                player.setDeltaMovement(velocity);

                ((ServerPlayer)player).connection.send(new ClientboundSetEntityMotionPacket(player));

                super.onUse();
            }
        }

        BlockPos playerPos = player.getOnPos();
        boolean highInAir = true;
        for(int i=1; i<=2; i++)
        {
            BlockPos pos = new BlockPos(playerPos.getX(), playerPos.getY() - i, playerPos.getZ());
            if(player.level().getBlockState(pos).getBlock() != Blocks.AIR)
            {
                highInAir = false;
                break;
            }
        }

        if(player.onGround())
        {
            alreadyDashed = false;
        }

        if(
                !playerMixin.getPreviousInput().shift() && playerMixin.getInput().shift()
                && !player.onGround()
                && highInAir
                && !alreadyDashed
        )
        {
            Vec3 forward = player.getForward();
            float dashStrength = 1.3f;

            Vec3 velocity = player.getDeltaMovement();

            velocity = new Vec3(
                    velocity.x + (forward.x * dashStrength),
                    velocity.y + (forward.y * dashStrength),
                    velocity.z + (forward.z * dashStrength)
            );

            player.setDeltaMovement(velocity);

            alreadyDashed = true;

            ((ServerPlayer)player).connection.send(new ClientboundSetEntityMotionPacket(player));
        }

        super.tick(player, abilityIndex);
    }
}
