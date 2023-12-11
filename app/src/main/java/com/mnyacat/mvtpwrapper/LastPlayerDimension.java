package com.mnyacat.mvtpwrapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.bukkit.World.Environment;

public class LastPlayerDimension {
    public final UUID playerUUID;
    public final UUID overworldUUID;
    public final Environment dimension;
    public final UUID dimensionUUID;

    public LastPlayerDimension(ResultSet result) throws SQLException {
        this.playerUUID = UUID.fromString(result.getString("player_uuid"));
        this.overworldUUID = UUID.fromString(result.getString("overworld_uuid"));
        this.dimension = Environment.valueOf(result.getString("dimension_name"));
        this.dimensionUUID = UUID.fromString(result.getString("dimension_uuid"));
    }

}
