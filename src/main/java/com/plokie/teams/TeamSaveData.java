package com.plokie.teams;

import com.mojang.serialization.Codec;
import com.plokie.Splatoon;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.*;

import java.util.HashMap;
import java.util.Map;

public class TeamSaveData extends SavedData {

    private final Map<String, TeamData> m_teamDataMap;

    private static final Codec<Map<String, TeamData>> MAP_CODEC = Codec.unboundedMap(Codec.STRING, TeamData.CODEC);

    private static final Codec<TeamSaveData> CODEC = MAP_CODEC.xmap(
            //TeamSaveData::new,
            (Map<String, TeamData> immutableMap) -> new TeamSaveData(new HashMap<>(immutableMap)),
            state -> state.m_teamDataMap
    );

    public static final SavedDataType<TeamSaveData> TYPE = new SavedDataType<>(
        "splatoon_team_data",
        TeamSaveData::new,
        CODEC,
        DataFixTypes.SAVED_DATA_SCOREBOARD
    );

    public TeamSaveData()
    {
        m_teamDataMap = new HashMap<>();
    }

    public TeamSaveData(Map<String, TeamData> dataMap)
    {
        m_teamDataMap = dataMap;
    }

    public TeamData getTeamData(String teamName) {
        Splatoon.LOGGER.info("\tLooking for team data key {}", teamName);
        TeamData teamData = m_teamDataMap.get(teamName);

        if(teamData == null) {
            Splatoon.LOGGER.info("\tNot found, creating new entry...");
            return m_teamDataMap.put(teamName, new TeamData());
        }

        Splatoon.LOGGER.info("\tFound, returning...");

        return teamData;
    }

    public void saveTeamData() {
        this.setDirty();
    }
}
