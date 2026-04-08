package io.github.homeforge.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.homeforge.HomeForge;
import io.github.homeforge.models.Home;
import io.github.homeforge.models.PlayerData;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

public class MySQLDatabase implements Database {

    private final HomeForge plugin;
    private HikariDataSource dataSource;
    private final AtomicLong idCounter = new AtomicLong(0);

    public MySQLDatabase(HomeForge plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() throws Exception {
        var cm = plugin.getConfigManager();
        HikariConfig cfg = new HikariConfig();
        cfg.setPoolName("HomeForge-MySQL");
        cfg.setDriverClassName("com.mysql.cj.jdbc.Driver");
        cfg.setJdbcUrl("jdbc:mysql://" + cm.getMysqlHost() + ":" + cm.getMysqlPort()
                + "/" + cm.getMysqlDatabase()
                + "?useSSL=false&characterEncoding=UTF-8&serverTimezone=UTC");
        cfg.setUsername(cm.getMysqlUser());
        cfg.setPassword(cm.getMysqlPass());
        cfg.setMaximumPoolSize(cm.getMysqlPoolSize());
        cfg.setConnectionTimeout(cm.getMysqlConnectionTimeout());
        cfg.setMaxLifetime(cm.getMysqlMaxLifetime());
        cfg.addDataSourceProperty("cachePrepStmts", "true");
        cfg.addDataSourceProperty("prepStmtCacheSize", "250");

        dataSource = new HikariDataSource(cfg);
        createTables();
        seedIdCounter();
        plugin.getLogger().info("[DB] MySQL ready @ " + cm.getMysqlHost()
                + ":" + cm.getMysqlPort() + "/" + cm.getMysqlDatabase());
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) dataSource.close();
    }

    private void createTables() throws SQLException {
        try (Connection c = dataSource.getConnection();
             Statement  s = c.createStatement()) {
            s.execute("""
                CREATE TABLE IF NOT EXISTS `Homes` (
                    `id`         BIGINT(255)   NOT NULL,
                    `owner`      VARCHAR(40)   NOT NULL,
                    `serverName` TEXT,
                    `world`      TEXT          NOT NULL,
                    `loc_x`      DECIMAL(65,5) NOT NULL,
                    `loc_y`      DECIMAL(65,5) NOT NULL,
                    `loc_z`      DECIMAL(65,5) NOT NULL,
                    `loc_yaw`    DECIMAL(65,5) NOT NULL,
                    `loc_pitch`  DECIMAL(65,5) NOT NULL,
                    `name`       TEXT          NOT NULL,
                    `symbol`     VARCHAR(128)  NOT NULL,
                    `last_used`  BIGINT(255)   NOT NULL,
                    PRIMARY KEY (`id`),
                    INDEX `idx_homes_owner` (`owner`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
            s.execute("""
                CREATE TABLE IF NOT EXISTS `Players` (
                    `uuid`         VARCHAR(40) NOT NULL,
                    `primary_home` BIGINT(255) NULL,
                    `extra_homes`  BIGINT(255) NULL,
                    PRIMARY KEY (`uuid`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
        }
    }

    private void seedIdCounter() throws SQLException {
        try (Connection c = dataSource.getConnection();
             Statement  s = c.createStatement();
             ResultSet  r = s.executeQuery("SELECT COALESCE(MAX(id),0) FROM Homes")) {
            if (r.next()) idCounter.set(r.getLong(1));
        }
    }

    @Override public synchronized long nextHomeId() { return idCounter.incrementAndGet(); }

    @Override
    public List<Home> getHomesForPlayer(String uuid) {
        List<Home> result = new ArrayList<>();
        try (Connection c  = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM Homes WHERE owner = ?")) {
            ps.setString(1, uuid);
            try (ResultSet r = ps.executeQuery()) {
                while (r.next()) result.add(mapHome(r));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed getHomesForPlayer", e);
        }
        return result;
    }

    @Override
    public Home getHomeByName(String uuid, String name) {
        try (Connection c  = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT * FROM Homes WHERE owner=? AND LOWER(name)=LOWER(?)")) {
            ps.setString(1, uuid); ps.setString(2, name);
            try (ResultSet r = ps.executeQuery()) { if (r.next()) return mapHome(r); }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed getHomeByName", e);
        }
        return null;
    }

    @Override
    public Home getHomeById(long id) {
        try (Connection c  = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM Homes WHERE id=?")) {
            ps.setLong(1, id);
            try (ResultSet r = ps.executeQuery()) { if (r.next()) return mapHome(r); }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed getHomeById", e);
        }
        return null;
    }

    @Override
    public boolean insertHome(Home h) {
        String sql = "INSERT INTO Homes (id,owner,serverName,world,loc_x,loc_y,loc_z,loc_yaw,loc_pitch,name,symbol,last_used) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c  = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            bindHome(ps, h); ps.executeUpdate(); return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed insertHome", e); return false;
        }
    }

    @Override
    public boolean updateHome(Home h) {
        String sql = "UPDATE Homes SET owner=?,serverName=?,world=?,loc_x=?,loc_y=?,loc_z=?,loc_yaw=?,loc_pitch=?,name=?,symbol=?,last_used=? WHERE id=?";
        try (Connection c  = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1,h.getOwner()); ps.setString(2,h.getServerName());
            ps.setString(3,h.getWorld()); ps.setDouble(4,h.getLocX());
            ps.setDouble(5,h.getLocY()); ps.setDouble(6,h.getLocZ());
            ps.setFloat(7,h.getLocYaw()); ps.setFloat(8,h.getLocPitch());
            ps.setString(9,h.getName()); ps.setString(10,h.getSymbol());
            ps.setLong(11,h.getLastUsed()); ps.setLong(12,h.getId());
            ps.executeUpdate(); return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed updateHome", e); return false;
        }
    }

    @Override
    public boolean deleteHome(long id) {
        try (Connection c  = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM Homes WHERE id=?")) {
            ps.setLong(1,id); ps.executeUpdate(); return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed deleteHome", e); return false;
        }
    }

    @Override
    public PlayerData getPlayerData(String uuid) {
        try (Connection c  = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM Players WHERE uuid=?")) {
            ps.setString(1, uuid);
            try (ResultSet r = ps.executeQuery()) { if (r.next()) return mapPlayer(r); }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed getPlayerData", e);
        }
        return null;
    }

    @Override
    public boolean upsertPlayerData(PlayerData pd) {
        String sql = "INSERT INTO Players (uuid,primary_home,extra_homes) VALUES (?,?,?) ON DUPLICATE KEY UPDATE primary_home=VALUES(primary_home), extra_homes=VALUES(extra_homes)";
        try (Connection c  = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, pd.getUuid());
            if (pd.getPrimaryHome() != null) ps.setLong(2, pd.getPrimaryHome());
            else ps.setNull(2, Types.BIGINT);
            ps.setLong(3, pd.getExtraHomes());
            ps.executeUpdate(); return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed upsertPlayerData", e); return false;
        }
    }

    private Home mapHome(ResultSet r) throws SQLException {
        Home h = new Home();
        h.setId(r.getLong("id")); h.setOwner(r.getString("owner"));
        h.setServerName(r.getString("serverName")); h.setWorld(r.getString("world"));
        h.setLocX(r.getDouble("loc_x")); h.setLocY(r.getDouble("loc_y"));
        h.setLocZ(r.getDouble("loc_z")); h.setLocYaw(r.getFloat("loc_yaw"));
        h.setLocPitch(r.getFloat("loc_pitch")); h.setName(r.getString("name"));
        h.setSymbol(r.getString("symbol")); h.setLastUsed(r.getLong("last_used"));
        return h;
    }

    private PlayerData mapPlayer(ResultSet r) throws SQLException {
        PlayerData pd = new PlayerData();
        pd.setUuid(r.getString("uuid"));
        long ph = r.getLong("primary_home");
        pd.setPrimaryHome(r.wasNull() ? null : ph);
        pd.setExtraHomes(r.getLong("extra_homes"));
        return pd;
    }

    private void bindHome(PreparedStatement ps, Home h) throws SQLException {
        ps.setLong(1,h.getId()); ps.setString(2,h.getOwner());
        ps.setString(3,h.getServerName()); ps.setString(4,h.getWorld());
        ps.setDouble(5,h.getLocX()); ps.setDouble(6,h.getLocY());
        ps.setDouble(7,h.getLocZ()); ps.setFloat(8,h.getLocYaw());
        ps.setFloat(9,h.getLocPitch()); ps.setString(10,h.getName());
        ps.setString(11,h.getSymbol()); ps.setLong(12,h.getLastUsed());
    }
}
