package com.plokie.customitems.items.guns;

import com.plokie.Splatoon;
import com.plokie.customitems.CustomItem;
import com.plokie.customitems.ICustomItem;
import com.plokie.helpers.Teams;
import com.plokie.interfaces.IGunProjectileMixin;
import com.plokie.interfaces.IPlayerTeamMixin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class Splattershot extends Gun {

    public Splattershot()
    {
        this.usageRate = 2;
        this.damage = 3;
        this.accuracy = 35.0f;

        this.inkUsage = 1.0f / 50.0f;
    }
}
