package com.mnyacat.mvtpwrapper;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.PortalType;
import org.bukkit.World;
import org.bukkit.World.Environment;

import com.onarandombox.MultiverseNetherPortals.MultiverseNetherPortals;

import net.kyori.adventure.text.logger.slf4j.ComponentLogger;

public class DatabaseManager {

    private final String databaseFile = "database.db";
    private final String databasePath = "plugins/MvtpWrapper/" + databaseFile;
    private Connection connection = null;

    public DatabaseManager() throws ClassNotFoundException, SQLException {
        Connection con = DriverManager
                .getConnection("jdbc:sqlite:" + databasePath);
        Class.forName("org.sqlite.JDBC");
        this.connection = con;
        ComponentLogger logger = Bukkit.getPluginManager().getPlugin("MvtpWrapper").getComponentLogger();
        logger.info("Connected to database. path=" + databasePath);

        checkTable();

    }

    public Connection getConnection() {
        return connection;
    }

    private void checkTable() {
        try (Statement locationStmt = connection.createStatement();
                Statement lastPlayerDimension = connection.createStatement()) {
            String locationSql = "CREATE TABLE IF NOT EXISTS location ("
                    + "id INTEGER PRIMARY KEY,"
                    + "player_uuid TEXT NOT NULL,"
                    + "world_uuid TEXT NOT NULL,"
                    + "x_coordinate DOUBLE NOT NULL,"
                    + "y_coordinate DOUBLE NOT NULL,"
                    + "z_coordinate DOUBLE NOT NULL,"
                    + "yaw FLOAT NOT NULL,"
                    + "pitch FLOAT NOT NULL,"
                    + "UNIQUE(player_uuid, world_uuid))";
            String lastPlayerDimensionSql = "CREATE TABLE IF NOT EXISTS last_player_dimension ("
                    + "id INTEGER PRIMARY KEY,"
                    + "player_uuid TEXT NOT NULL,"
                    + "overworld_uuid TEXT NOT NULL,"
                    + "dimension_name TEXT NOT NULL,"
                    + "dimension_uuid TEXT NOT NULL,"
                    + "UNIQUE(player_uuid, overworld_uuid))";
            locationStmt.executeUpdate(locationSql);
            lastPlayerDimension.executeUpdate(lastPlayerDimensionSql);
        } catch (SQLException e) {
            ComponentLogger logger = Bukkit.getPluginManager().getPlugin("MvtpWrapper").getComponentLogger();
            logger.warn(e.toString());
        }
    }

    public void upsertLocation(UUID playerUUID, UUID worldUUID, double x, double y, double z, float yaw,
            float pitch) {
        try (PreparedStatement pstmt = connection.prepareStatement(
                "INSERT INTO location(player_uuid, world_uuid, x_coordinate, y_coordinate, z_coordinate, yaw, pitch)"
                        + "VALUES(?, ?, ?, ?, ?, ?, ?)"
                        + "ON CONFLICT(player_uuid, world_uuid)"
                        + "DO UPDATE SET x_coordinate=?, y_coordinate=?, z_coordinate=?, yaw=?, pitch=?")) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.setString(2, worldUUID.toString());
            pstmt.setDouble(3, x);
            pstmt.setDouble(4, y);
            pstmt.setDouble(5, z);
            pstmt.setFloat(6, yaw);
            pstmt.setFloat(7, pitch);
            pstmt.setDouble(8, x);
            pstmt.setDouble(9, y);
            pstmt.setDouble(10, z);
            pstmt.setFloat(11, yaw);
            pstmt.setFloat(12, pitch);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            ComponentLogger logger = Bukkit.getPluginManager().getPlugin("MvtpWrapper").getComponentLogger();
            logger.warn(e.toString());
        }
    }

    public @Nullable MWLocation getLastLocation(UUID playerUUID, UUID worldUUID) {
        try (PreparedStatement pstmt = connection.prepareStatement(
                "SELECT * FROM location WHERE player_uuid=? AND world_uuid=? LIMIT 1")) {
            MWLocation result;
            pstmt.setString(1, playerUUID.toString());
            pstmt.setString(2, worldUUID.toString());
            try (ResultSet resultSet = pstmt.executeQuery()) {
                if (!resultSet.isBeforeFirst()) {
                    ComponentLogger logger = Bukkit.getPluginManager().getPlugin("MvtpWrapper").getComponentLogger();
                    logger.debug(
                            String.format("条件に合うレコードが存在しません. player_uuid: %s, world_uuid: %s", playerUUID, worldUUID));
                    result = null;
                } else {
                    result = new MWLocation(resultSet);
                }

            }
            return result;
        } catch (SQLException e) {
            ComponentLogger logger = Bukkit.getPluginManager().getPlugin("MvtpWrapper").getComponentLogger();
            logger.warn(e.toString());
            return null;
        }
    }

    public void upsertLastPlayerDimension(UUID playerUUID, UUID overworldUUID, Environment dimension,
            UUID dimensionUUID) {
        try (PreparedStatement pstmt = connection.prepareStatement(
                "INSERT INTO last_player_dimension(player_uuid, overworld_uuid, dimension_name, dimension_uuid)"
                        + "VALUES(?, ?, ?, ?)"
                        + "ON CONFLICT(player_uuid, overworld_uuid)"
                        + "DO UPDATE SET dimension_name=?, dimension_uuid=?")) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.setString(2, overworldUUID.toString());
            pstmt.setString(3, dimension.name());
            pstmt.setString(4, dimensionUUID.toString());
            pstmt.setString(5, dimension.name());
            pstmt.setString(6, dimensionUUID.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            ComponentLogger logger = Bukkit.getPluginManager().getPlugin("MvtpWrapper").getComponentLogger();
            logger.warn(e.toString());
        }
    }

    public void upsertLastPlayerDimension(UUID playerUUID, World fromWorld) {
        MultiverseNetherPortals netherportals = (MultiverseNetherPortals) Bukkit.getServer().getPluginManager()
                .getPlugin("Multiverse-NetherPortals");
        @Nullable
        String overworldName;
        @Nullable
        World overworld;
        // Multiverse-NetherPortalsはCUSTOMディメンションと紐づけられないので、NORMALと同じ扱いにする
        switch (fromWorld.getEnvironment()) {
            case NETHER:
                overworldName = netherportals.getWorldLink(fromWorld.getName(), PortalType.NETHER);
                overworld = Objects.nonNull(overworldName) ? Bukkit.getWorld(overworldName) : null;
                break;
            case THE_END:
                overworldName = netherportals.getWorldLink(fromWorld.getName(), PortalType.ENDER);
                overworld = Objects.nonNull(overworldName) ? Bukkit.getWorld(overworldName) : null;
                break;
            default:
                overworld = fromWorld;
                break;
        }
        if (Objects.isNull(overworld)) {
            return;
        } else {
            upsertLastPlayerDimension(playerUUID, overworld.getUID(), fromWorld.getEnvironment(), fromWorld.getUID());
        }
    }

    public @Nullable LastPlayerDimension getLastPlayerDimension(UUID playerUUID, UUID overworldUUID) {
        try (PreparedStatement pstmt = connection.prepareStatement(
                "SELECT * FROM last_player_dimension WHERE player_uuid=? AND overworld_uuid=? LIMIT 1")) {
            LastPlayerDimension result;
            pstmt.setString(1, playerUUID.toString());
            pstmt.setString(2, overworldUUID.toString());
            try (ResultSet resultSet = pstmt.executeQuery()) {
                if (!resultSet.isBeforeFirst()) {
                    ComponentLogger logger = Bukkit.getPluginManager().getPlugin("MvtpWrapper").getComponentLogger();
                    logger.debug(
                            String.format("条件に合うレコードが存在しません. player_uuid: %s, overworld_uuid: %s", playerUUID,
                                    overworldUUID));
                    result = null;
                } else {
                    result = new LastPlayerDimension(resultSet);
                }

            }
            return result;
        } catch (SQLException e) {
            ComponentLogger logger = Bukkit.getPluginManager().getPlugin("MvtpWrapper").getComponentLogger();
            logger.warn(e.toString());
            return null;
        }
    }
}
