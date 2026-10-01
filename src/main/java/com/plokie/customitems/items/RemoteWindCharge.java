package com.plokie.customitems.items;

import com.plokie.customitems.ICustomItem;
import com.plokie.interfaces.IProjectile;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public class RemoteWindCharge extends ICustomItem {
    public RemoteWindCharge()
    {
        this.useDuration = 5;
        this.usageRate = 15;
    }

    @Override
    public void onUseItem(Player player) {
        super.onUseItem(player);

        ServerLevel level = (ServerLevel)player.level();

        ThrownEgg nade = EntityType.EGG.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if(nade == null) return;

        ItemStack item = new ItemStack(Items.PHANTOM_MEMBRANE);
        item.set(DataComponents.ITEM_MODEL, ResourceLocation.fromNamespaceAndPath("splatoon", "remote_charge"));
        nade.setItem(item);

        nade.setOwner(player);

        level.addFreshEntity(nade);

        nade.addTag("RemoteWindCharge");

        Vec3 forward = player.getForward();
        Vec3 spawnPos = player.getEyePosition();
        spawnPos.add(forward.multiply(1.0,1.0,1.0));
        spawnPos.add(player.getDeltaMovement().multiply(2.0,2.0,2.0));

        nade.setPos(player.getEyePosition());
        nade.setXRot(player.getXRot());
        nade.setYRot(player.getYRot());

        float force = 1.1f;
        Vec3 forceDir = forward.multiply(force, force, force);

        nade.setDeltaMovement(forceDir);

        ((IProjectile)nade).setPlayerOwner(player);
    }
}
