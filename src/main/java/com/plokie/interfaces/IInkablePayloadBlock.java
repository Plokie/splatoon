package com.plokie.interfaces;

import com.plokie.helpers.Teams;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public interface IInkablePayloadBlock {

    void setLinkedBlockDisplay(UUID blockDisplayUUID);
    IPlayerTeamMixin getTeam();
    void setTeam(IPlayerTeamMixin team);

    default void setTeamByPlayer(Player sameTeamAs) {
        IPlayerTeamMixin team = Teams.getTeamMixinFromPlayer(sameTeamAs);
        if(team != null) {
            setTeam(team);
        }
    }

}
