package com.plokie.customitems.items.guns;

import com.plokie.Splatoon;
import com.plokie.customitems.CustomItem;
import com.plokie.customitems.ICustomItem;
import com.plokie.helpers.Teams;
import com.plokie.interfaces.IGunProjectileMixin;
import com.plokie.interfaces.IPlayerTeamMixin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Gun extends ICustomItem {
    protected int damage = 1;
    protected int inkUsage = 1;
    protected float accuracy = 1.0f;
    protected int projectilesPerShot = 1;
    protected int burst = 0;
    protected int inkSpread = 1;

    public int getDamage() { return damage; }
    public float getAccuracy() { return accuracy; }
    public int getProjectilesPerShot() { return projectilesPerShot; }
    public int getInkSpread() { return inkSpread; }

    public Gun()
    {
        this.useDuration = 5;
    }

    class BurstRemain
    {
        public BurstRemain(int tickFired, int firesLeft) { this.tickFired = tickFired; this.firesLeft = firesLeft; }
        public int tickFired = 0;
        public int firesLeft = 0;
    }
    static Map<Player, BurstRemain> burstRemaining = new HashMap<>();


    @Override
    public void onUse(Player player)
    {
        shootAll(player);//item.getItemDefinition().getItemInterface().onUse(player)

        if(burst > 0)
        {
            burstRemaining.put(player, new BurstRemain(player.tickCount, burst));
        }

    }

    void shootAll(Player player)
    {
        for(int i=0; i<projectilesPerShot; i++)
        {
            shoot(player, i);
        }
    }

    void shoot(Player player, int bulletIndex)
    {
        Snowball snowball = EntityType.SNOWBALL.create(player.level(), EntitySpawnReason.SPAWN_ITEM_USE);
        if(snowball == null) return;

        IPlayerTeamMixin playerTeam = Teams.getTeamMixinFromPlayer(player);
        if(playerTeam!=null)
        {
            ItemStack itemStack = new ItemStack(playerTeam.getWallBlock());
            snowball.setItem(itemStack);
        }

        String customItemName = this.getClass().getSimpleName();

        try {
            CustomItem customItem = CustomItem.valueOf(customItemName);
            ((IGunProjectileMixin)snowball).setShotByCustomItem(customItem);
        }
        catch (IllegalArgumentException e)
        {
            Splatoon.LOGGER.warn("Shot unknown gun '{}', make sure its a recognised customitem id", customItemName);
        }

        snowball.setOwner(player);

        player.level().addFreshEntity(snowball);

        Vec3 playerPos = player.getEyePosition();
        Vec3 forward = player.getForward();


        float forwardMult = 0.5f;

        Vec3 spawnPos = new Vec3(playerPos.x + (forward.x * forwardMult), (playerPos.y - 0.15f) + (forward.y * forwardMult), playerPos.z + (forward.z * forwardMult));

        float spreadSegmentSize = (float) (accuracy / this.getProjectilesPerShot());

        float randomOffset = spreadSegmentSize * bulletIndex;
        randomOffset += (float)Math.random() * spreadSegmentSize;
        randomOffset -= accuracy * 0.5f;

        //double randomOffset = (Math.random() * accuracy * 2.0) - accuracy;

        Vec2 rotVec = player.getRotationVector();
        rotVec = new Vec2(rotVec.x, rotVec.y + randomOffset);
        Vec3 shootForward = Vec3.directionFromRotation(rotVec);

        snowball.setPos(spawnPos);
        Vec3 shootForce = new Vec3(shootForward.x * 0.5f, shootForward.y * 0.5f, shootForward.z * 0.5f);
        snowball.setDeltaMovement(shootForce);

        player.level().playSound(
                null, // everyone
                spawnPos.x, spawnPos.y, spawnPos.z,
                SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("splatoon", "splattershot.shoot")),
                SoundSource.HOSTILE,
                1.0f, // volume
                1.0f // pitch
        );
    }

    @Override
    public void whileHeld(Player player)
    {
        int burstFireRate = 2;

        if(burstRemaining.containsKey(player))
        {
            BurstRemain burstRemain = burstRemaining.get(player);


            if((player.tickCount - burstRemain.tickFired) % burstFireRate == 0)
            {
                shootAll(player);

                burstRemain.firesLeft -= 1;

                if(burstRemain.firesLeft <= 0)
                {
                    burstRemaining.remove(player);
                }
            }

            if(burstRemain.tickFired + (burstFireRate*burstRemain.firesLeft) > player.tickCount)
            {
                if(burstRemain.firesLeft <= 0)
                {
                    burstRemaining.remove(player);
                }
            }

        }
    }
}
