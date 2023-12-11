package com.mnyacat.mvtpwrapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class MWLocation {
    public final UUID playerUUID;
    public final UUID worldUUID;
    public final Double x;
    public final Double y;
    public final Double z;
    public final Float yaw;
    public final Float pitch;

    public MWLocation(ResultSet result) throws SQLException {
        this.playerUUID = UUID.fromString(result.getString("player_uuid"));
        this.worldUUID = UUID.fromString(result.getString("world_uuid"));
        this.x = result.getDouble("x_coordinate");
        this.y = result.getDouble("y_coordinate");
        this.z = result.getDouble("z_coordinate");
        this.yaw = result.getFloat("yaw");
        this.pitch = result.getFloat("pitch");
    }

    public Location toLocation() {
        World world = Bukkit.getServer().getWorld(worldUUID);
        return new Location(world, x, y, z, yaw, pitch);
    }
}
