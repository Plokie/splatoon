package com.plokie.customitems.items;

import com.plokie.customitems.ICustomItem;
import com.plokie.helpers.ScheduleEvent;
import com.plokie.interfaces.IProjectile;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.world.phys.AABB;

public class CleansingGrenade extends ICustomItem {
    @Override
    public void onUseItem(Player player)
    {
        super.onUseItem(player);

        ScheduleEvent.schedule(
                1, server->{
                    //Splatoon.LOGGER.info("Executing cleansing tick task {}", level.getServer().getTickCount());

                    AABB area = new AABB(player.getOnPos()).inflate(4.0);

                    player.level().getEntitiesOfClass(ThrownExperienceBottle.class, area).forEach(bottle -> {
                        //Splatoon.LOGGER.info("found {}", bottle);
                        ((IProjectile)bottle).setPlayerOwner(player);
                    });
                }
        );
    }
}
