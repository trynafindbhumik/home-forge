package io.github.homeforge.database;

import io.github.homeforge.models.Home;
import io.github.homeforge.models.PlayerData;

import java.util.List;

/*
 * Database contract for HomeForge.
 * All implementations must be thread-safe — callers invoke these
 * methods from Paper's async scheduler threads.
 */
public interface Database {

    void initialize() throws Exception;
    void close();

    // Homes
    List<Home> getHomesForPlayer(String uuid);
    Home       getHomeByName(String uuid, String name);
    Home       getHomeById(long id);
    boolean    insertHome(Home home);
    boolean    updateHome(Home home);
    boolean    deleteHome(long id);
    long       nextHomeId();

    // Players
    PlayerData getPlayerData(String uuid);
    boolean    upsertPlayerData(PlayerData data);
}
