package com.plokie.mixin;

import com.mojang.math.Transformation;
import com.plokie.Splatoon;
import com.plokie.interfaces.IProjectile;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mixin(ThrownEgg.class)
public class RemoteWindChargeMixin implements IProjectile {
    @Unique
    UUID ownerUUID;

    @Override
    public void setPlayerOwner(Player player) {
        ownerUUID = player.getUUID();
    }

//    @Inject(method="registerGoals", at=@At("HEAD"), cancellable = true)
//    void registerGoals(CallbackInfo ci)
//    {
//        ci.cancel();
//    }

//    @Inject(method = "onHitEntity", at=@At("HEAD"), cancellable = true)
//    void onHitEntity(EntityHitResult entityHitResult, CallbackInfo ci)
//    {
//        ThrownEgg self = (ThrownEgg)(Object)this;
//
//        if(ownerUUID==null) {
//            Splatoon.LOGGER.info("onHitEntity ThrownEgg OwnerUUID is null");
//            return;
//        }
//        Entity owner = self.level().getEntity(ownerUUID);
//        if(owner == null) {
//            Splatoon.LOGGER.info("onHitEntity ThrownEgg Couldnt find owner by uuid");
//            return;
//        }
//
//        if(entityHitResult.getEntity() == owner) {
//            ci.cancel();
//        }
//    }
//
    @Inject(method = "onHit", at=@At("TAIL"))
    void onHit(CallbackInfo ci) {
        ThrownEgg self = (ThrownEgg)(Object)this;

        if(!self.getTags().contains("RemoteWindCharge")) return;

        Display.ItemDisplay blockDisplay = EntityType.ITEM_DISPLAY.create(self.level(), EntitySpawnReason.SPAWN_ITEM_USE);
        if(blockDisplay == null) return;

        ItemStack item = new ItemStack(Items.PHANTOM_MEMBRANE);
        item.set(DataComponents.ITEM_MODEL, ResourceLocation.fromNamespaceAndPath("splatoon", "remote_charge"));

        blockDisplay.setItemStack(item);

        blockDisplay.setPos(self.position());

        Vec3 pos = self.position();

        List<Vec3> rayDirs = List.of(
                new Vec3(0, 1, 0),
                new Vec3(0, -1, 0),
                new Vec3(1, 0, 0),
                new Vec3(-1, 0, 0),
                new Vec3(0, 0, 1),
                new Vec3(0, 0, -1)
        );

        Vec3 normalResult = Vec3.ZERO;

        for(Vec3 dir : rayDirs) {
            BlockHitResult hit = self.level().clip(new ClipContext(
                    pos, pos.add(dir),
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    CollisionContext.empty()
            ));

            if(hit.getType() == HitResult.Type.BLOCK) {
                Splatoon.LOGGER.info("\t\thit");
                normalResult = normalResult.add(hit.getDirection().getUnitVec3());
            }
        }

        Splatoon.LOGGER.info("NormalResult {}", normalResult);

        if(normalResult.length() > 0.001) {
            normalResult = normalResult.normalize();

            blockDisplay.lookAt(EntityAnchorArgument.Anchor.EYES, pos.add(normalResult));
        }


//        float scale = 1.0f;
//        Transformation transform = new Transformation(
//                new Vector3f(-scale * 0.5f, -scale * 0.5f, -scale * 0.5f),
//                null,
//                new Vector3f(scale, scale, scale),
//                null
//        );
//        blockDisplay.setTransformation(transform);

        blockDisplay.addTag("RemoteWindCharge");

        self.level().addFreshEntity(blockDisplay);

        if(ownerUUID==null) {
            Splatoon.LOGGER.info("ThrownEgg OwnerUUID is null");
            return;
        }
        Entity owner = self.level().getEntity(ownerUUID);
        if(owner == null) {
            Splatoon.LOGGER.info("ThrownEgg Couldnt find owner by uuid");
            return;
        }
        if(owner instanceof Player player) {
            ((IProjectile)blockDisplay).setPlayerOwner(player);
        }
    }

}
