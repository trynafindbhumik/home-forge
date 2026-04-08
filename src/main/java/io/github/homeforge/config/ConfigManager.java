package io.github.homeforge.config;

import io.github.homeforge.HomeForge;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final HomeForge plugin;
    private FileConfiguration cfg;

    public ConfigManager(HomeForge plugin) {
        this.plugin = plugin;
        this.cfg    = plugin.getConfig();
    }

    public void reload() {
        plugin.reloadConfig();
        this.cfg = plugin.getConfig();
    }

    // Database
    public boolean isDatabaseMySQL()           { return "MYSQL".equalsIgnoreCase(cfg.getString("database.type","SQLITE")); }
    public String  getSQLiteFile()             { return cfg.getString("database.sqlite.file", "homes.db"); }
    public String  getMysqlHost()              { return cfg.getString("database.mysql.host", "localhost"); }
    public int     getMysqlPort()              { return cfg.getInt("database.mysql.port", 3306); }
    public String  getMysqlDatabase()          { return cfg.getString("database.mysql.database", "homeforge"); }
    public String  getMysqlUser()              { return cfg.getString("database.mysql.username", "root"); }
    public String  getMysqlPass()              { return cfg.getString("database.mysql.password", ""); }
    public int     getMysqlPoolSize()          { return cfg.getInt("database.mysql.pool-size", 10); }
    public long    getMysqlConnectionTimeout() { return cfg.getLong("database.mysql.connection-timeout", 30_000); }
    public long    getMysqlMaxLifetime()       { return cfg.getLong("database.mysql.max-lifetime", 1_800_000); }

    // Settings
    public boolean usePermissions()          { return cfg.getBoolean("settings.use_permissions", true); }
    public int     getDefaultHomeLimit()     { return cfg.getInt("settings.default_home_limit", 3); }
    public int     getMaxHomeLimit()         { return cfg.getInt("settings.max_home_limit", 54); }
    public boolean stackPermissionLimits()   { return cfg.getBoolean("settings.count_home_permission_limit_together", false); }
    public int     getHomeCooldown()         { return cfg.getInt("settings.command_cooldown_home", 0); }
    public int     getTeleportDelay()        { return cfg.getInt("settings.teleport_delay", 0); }
    public boolean checkMovementOnDelay()    { return cfg.getBoolean("settings.teleport_delay_check_movement", true); }
    public boolean playSoundOnTeleport()     { return cfg.getBoolean("settings.sound_on_teleport", true); }
    public boolean playParticleOnTeleport()  { return cfg.getBoolean("settings.particle_on_teleport", true); }
    public String  getServerName()           { return cfg.getString("settings.server_name", ""); }
    public boolean isBungeeCordEnabled()     { return !getServerName().isBlank(); }

    // GUI
    public String getGuiTitle()             { return cfg.getString("gui.title", "&8Your Homes"); }
    public String getGuiAdminTitle()        { return cfg.getString("gui.admin_title", "&8Homes of %player%"); }
    public String getGuiSettingsTitle()     { return cfg.getString("gui.settings_title", "&8%name%"); }
    public String getGuiSymbolPickerTitle() { return cfg.getString("gui.symbol_picker_title", "&8Choose Symbol"); }

    // Messages
    public String getPrefix() { return cfg.getString("messages.prefix", "&8[&bHomeForge&8] &r"); }
    public String msg(String key) {
        return cfg.getString("messages." + key, "&cMissing message: " + key);
    }
}
