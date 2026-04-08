package io.github.homeforge.managers;

import io.github.homeforge.HomeForge;
import io.github.homeforge.database.Database;
import io.github.homeforge.models.Home;
import io.github.homeforge.models.PlayerData;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class HomeManager {

    public static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-]{1,32}$");

    private final HomeForge plugin;
    private final Database  db;

    private final Map<String, List<Home>>  homeCache = new ConcurrentHashMap<>();
    private final Map<String, PlayerData>  dataCache = new ConcurrentHashMap<>();

    public HomeManager(HomeForge plugin, Database db) {
        this.plugin = plugin;
        this.db     = db;
    }

    // Cache
    public void loadPlayer(Player player) {
        String uuid = player.getUniqueId().toString();
        runAsync(() -> {
            List<Home>  homes = db.getHomesForPlayer(uuid);
            PlayerData  data  = db.getPlayerData(uuid);
            if (data == null) data = new PlayerData(uuid, null, 0);
            homeCache.put(uuid, new ArrayList<>(homes));
            dataCache.put(uuid, data);
        });
    }

    public void unloadPlayer(Player player) {
        String uuid = player.getUniqueId().toString();
        homeCache.remove(uuid);
        dataCache.remove(uuid);
    }

    // Reads
    public List<Home> getHomes(String uuid) {
        return homeCache.computeIfAbsent(uuid, db::getHomesForPlayer);
    }

    public Home getHome(String uuid, String name) {
        return getHomes(uuid).stream()
                .filter(h -> h.getName().equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }

    public Home getPrimaryHome(String uuid) {
        PlayerData data = getPlayerData(uuid);
        if (data.getPrimaryHome() == null) return null;
        long pid = data.getPrimaryHome();
        return getHomes(uuid).stream().filter(h -> h.getId() == pid).findFirst().orElse(null);
    }

    public PlayerData getPlayerData(String uuid) {
        return dataCache.computeIfAbsent(uuid, u -> {
            PlayerData pd = db.getPlayerData(u);
            return pd != null ? pd : new PlayerData(u, null, 0);
        });
    }

    public int getHomeLimit(Player player) {
        if (!plugin.getConfigManager().usePermissions()) {
            return plugin.getConfigManager().getMaxHomeLimit();
        }
        if (player.hasPermission("homeforge.limit.*")) {
            return plugin.getConfigManager().getMaxHomeLimit()
                    + (int) getPlayerData(player.getUniqueId().toString()).getExtraHomes();
        }
        int max = plugin.getConfigManager().getMaxHomeLimit();
        int base = 0;
        if (plugin.getConfigManager().stackPermissionLimits()) {
            for (int i = 1; i <= max; i++)
                if (player.hasPermission("homeforge.limit." + i)) base += i;
        } else {
            for (int i = max; i >= 1; i--) {
                if (player.hasPermission("homeforge.limit." + i)) { base = i; break; }
            }
        }
        if (base == 0) base = plugin.getConfigManager().getDefaultHomeLimit();
        return base + (int) getPlayerData(player.getUniqueId().toString()).getExtraHomes();
    }

    // Async writes
    public CompletableFuture<Boolean> setHome(Player player, String name, boolean overwrite) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        String uuid       = player.getUniqueId().toString();
        String serverName = plugin.getConfigManager().getServerName();
        runAsync(() -> {
            Home existing = getHome(uuid, name);
            if (existing != null) {
                if (!overwrite) { complete(future, false); return; }
                existing.updateLocation(player.getLocation());
                existing.setServerName(serverName.isBlank() ? null : serverName);
                db.updateHome(existing);
                complete(future, true);
            } else {
                Home h = new Home();
                h.setId(db.nextHomeId()); h.setOwner(uuid);
                h.setServerName(serverName.isBlank() ? null : serverName);
                h.setName(name); h.setSymbol("GRASS_BLOCK");
                h.setLastUsed(System.currentTimeMillis());
                h.updateLocation(player.getLocation());
                db.insertHome(h);
                homeCache.computeIfAbsent(uuid, u -> new ArrayList<>()).add(h);
                complete(future, true);
            }
        });
        return future;
    }

    public CompletableFuture<Boolean> removeHome(String uuid, String name) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        runAsync(() -> {
            Home h = getHome(uuid, name);
            if (h == null) { complete(future, false); return; }
            db.deleteHome(h.getId());
            PlayerData data = getPlayerData(uuid);
            if (data.getPrimaryHome() != null && data.getPrimaryHome() == h.getId()) {
                data.setPrimaryHome(null);
                db.upsertPlayerData(data);
            }
            List<Home> cached = homeCache.get(uuid);
            if (cached != null) cached.removeIf(x -> x.getId() == h.getId());
            complete(future, true);
        });
        return future;
    }

    public CompletableFuture<Boolean> setPrimaryHome(String uuid, String name) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        runAsync(() -> {
            Home h = getHome(uuid, name);
            if (h == null) { complete(future, false); return; }
            PlayerData data = getPlayerData(uuid);
            data.setPrimaryHome(h.getId());
            db.upsertPlayerData(data);
            complete(future, true);
        });
        return future;
    }

    public CompletableFuture<Boolean> updateHomeLocation(Player player, String name) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        String uuid = player.getUniqueId().toString();
        runAsync(() -> {
            Home h = getHome(uuid, name);
            if (h == null) { complete(future, false); return; }
            h.updateLocation(player.getLocation());
            h.setServerName(plugin.getConfigManager().getServerName().isBlank()
                    ? null : plugin.getConfigManager().getServerName());
            db.updateHome(h);
            complete(future, true);
        });
        return future;
    }

    public CompletableFuture<Boolean> updateSymbol(String uuid, String name, String symbol) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        runAsync(() -> {
            Home h = getHome(uuid, name);
            if (h == null) { complete(future, false); return; }
            h.setSymbol(symbol);
            h.setLastUsed(System.currentTimeMillis());
            db.updateHome(h);
            complete(future, true);
        });
        return future;
    }

    public CompletableFuture<PlayerData> addExtraHomes(String uuid, long amount) {
        CompletableFuture<PlayerData> f = new CompletableFuture<>();
        runAsync(() -> { PlayerData d = getPlayerData(uuid); d.setExtraHomes(Math.max(0, d.getExtraHomes() + amount)); db.upsertPlayerData(d); runSync(() -> f.complete(d)); });
        return f;
    }

    public CompletableFuture<PlayerData> removeExtraHomes(String uuid, long amount) {
        CompletableFuture<PlayerData> f = new CompletableFuture<>();
        runAsync(() -> { PlayerData d = getPlayerData(uuid); d.setExtraHomes(Math.max(0, d.getExtraHomes() - amount)); db.upsertPlayerData(d); runSync(() -> f.complete(d)); });
        return f;
    }

    public CompletableFuture<PlayerData> setExtraHomes(String uuid, long amount) {
        CompletableFuture<PlayerData> f = new CompletableFuture<>();
        runAsync(() -> { PlayerData d = getPlayerData(uuid); d.setExtraHomes(Math.max(0, amount)); db.upsertPlayerData(d); runSync(() -> f.complete(d)); });
        return f;
    }

    public CompletableFuture<List<Home>> loadHomesAsync(String uuid) {
        CompletableFuture<List<Home>> f = new CompletableFuture<>();
        runAsync(() -> { List<Home> h = homeCache.computeIfAbsent(uuid, db::getHomesForPlayer); runSync(() -> f.complete(h)); });
        return f;
    }

    // Internals
    private void runAsync(Runnable r) { plugin.getServer().getScheduler().runTaskAsynchronously(plugin, r); }
    private void runSync(Runnable r)  { plugin.getServer().getScheduler().runTask(plugin, r); }
    private void complete(CompletableFuture<Boolean> f, boolean v) { runSync(() -> f.complete(v)); }

    public Database getDatabase() { return db; }
}
