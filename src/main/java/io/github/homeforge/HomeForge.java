package io.github.homeforge;

import io.github.homeforge.commands.*;
import io.github.homeforge.config.ConfigManager;
import io.github.homeforge.database.Database;
import io.github.homeforge.database.MySQLDatabase;
import io.github.homeforge.database.SQLiteDatabase;
import io.github.homeforge.listeners.BungeeCordListener;
import io.github.homeforge.listeners.GUIListener;
import io.github.homeforge.listeners.PlayerListener;
import io.github.homeforge.managers.HomeManager;
import io.github.homeforge.managers.TeleportManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * HomeForge — A modern sethome plugin for Paper 26.1.
 *
 * @author Bhumik Jain
 * @see <a href="https://github.com/trynafindbhumik/HomeForge">GitHub</a>
 */
public final class HomeForge extends JavaPlugin {

    private static HomeForge instance;

    private ConfigManager   configManager;
    private Database        database;
    private HomeManager     homeManager;
    private TeleportManager teleportManager;

    @Override
    public void onEnable() {
        instance = this;

        // Config
        saveDefaultConfig();
        configManager = new ConfigManager(this);

        // Database
        try {
            database = configManager.isDatabaseMySQL()
                    ? new MySQLDatabase(this)
                    : new SQLiteDatabase(this);
            database.initialize();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Database initialization failed — disabling HomeForge.", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Managers
        homeManager     = new HomeManager(this, database);
        teleportManager = new TeleportManager(this);

        // Commands
        registerCommands();

        // Listeners
        getServer().getPluginManager().registerEvents(new GUIListener(this),    this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        // BungeeCord
        if (configManager.isBungeeCordEnabled()) {
            getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
            getServer().getMessenger().registerIncomingPluginChannel(
                    this, "BungeeCord", new BungeeCordListener(this));
            getLogger().info("BungeeCord plugin messaging enabled.");
        }

        // Pre-load online players (plugin reload scenario)
        getServer().getOnlinePlayers().forEach(homeManager::loadPlayer);

        getLogger().info("HomeForge v" + getDescription().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        if (teleportManager != null) teleportManager.cancelAllTeleports();
        if (database        != null) database.close();
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
        getLogger().info("HomeForge disabled.");
    }

    private void registerCommands() {
        bind("sethome",      new SetHomeCommand(this),    null);
        HomeCommand       homeCmd   = new HomeCommand(this);
        RemoveHomeCommand removeCmd = new RemoveHomeCommand(this);
        HomesCommand      homesCmd  = new HomesCommand(this);
        bind("home",        homeCmd,   homeCmd);
        bind("removehome",  removeCmd, removeCmd);
        bind("homes",       homesCmd,  homesCmd);
        bind("hfreload",     new ReloadCommand(this),     null);
        bind("importhomes",  new ImportHomesCommand(this),null);
    }

    private void bind(String name,
                      org.bukkit.command.CommandExecutor exec,
                      org.bukkit.command.TabCompleter tab) {
        PluginCommand cmd = getCommand(name);
        if (cmd == null) { getLogger().warning("Command '" + name + "' not found in plugin.yml!"); return; }
        cmd.setExecutor(exec);
        if (tab != null) cmd.setTabCompleter(tab);
    }

    // Hot-reload config without restarting the server.
    public void reload() {
        reloadConfig();
        configManager.reload();
        getLogger().info("Configuration reloaded.");
    }

    public static HomeForge getInstance()        { return instance; }
    public ConfigManager   getConfigManager()    { return configManager; }
    public Database        getDatabase()         { return database; }
    public HomeManager     getHomeManager()      { return homeManager; }
    public TeleportManager getTeleportManager()  { return teleportManager; }
}
