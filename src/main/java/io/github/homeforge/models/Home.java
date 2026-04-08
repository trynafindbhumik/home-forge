package io.github.homeforge.models;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

// Represents a single saved home.
public class Home {

    private long   id;
    private String owner;
    private String serverName;
    private String world;
    private double locX;
    private double locY;
    private double locZ;
    private float  locYaw;
    private float  locPitch;
    private String name;
    private String symbol;
    private long   lastUsed;

    public Home() {}

    public Home(long id, String owner, String serverName, String world,
                double locX, double locY, double locZ,
                float locYaw, float locPitch,
                String name, String symbol, long lastUsed) {
        this.id         = id;
        this.owner      = owner;
        this.serverName = serverName;
        this.world      = world;
        this.locX       = locX;
        this.locY       = locY;
        this.locZ       = locZ;
        this.locYaw     = locYaw;
        this.locPitch   = locPitch;
        this.name       = name;
        this.symbol     = symbol;
        this.lastUsed   = lastUsed;
    }

    // Builds a Bukkit Location. Returns null if the world isn't loaded.
    public Location toBukkitLocation() {
        World w = Bukkit.getWorld(world);
        if (w == null) return null;
        return new Location(w, locX, locY, locZ, locYaw, locPitch);
    }

    // Returns the Material to use as this home's GUI icon.
    public Material getSymbolMaterial() {
        if (symbol == null || symbol.isBlank()) return Material.GRASS_BLOCK;
        Material mat = Material.matchMaterial(symbol);
        return (mat != null && !mat.isAir()) ? mat : Material.GRASS_BLOCK;
    }

    // Updates coordinates from a Bukkit Location.
    public void updateLocation(Location loc) {
        this.world    = loc.getWorld().getName();
        this.locX     = loc.getX();
        this.locY     = loc.getY();
        this.locZ     = loc.getZ();
        this.locYaw   = loc.getYaw();
        this.locPitch = loc.getPitch();
        this.lastUsed = System.currentTimeMillis();
    }

    // Getters
    public long   getId()         { return id; }
    public String getOwner()      { return owner; }
    public String getServerName() { return serverName; }
    public String getWorld()      { return world; }
    public double getLocX()       { return locX; }
    public double getLocY()       { return locY; }
    public double getLocZ()       { return locZ; }
    public float  getLocYaw()     { return locYaw; }
    public float  getLocPitch()   { return locPitch; }
    public String getName()       { return name; }
    public String getSymbol()     { return symbol; }
    public long   getLastUsed()   { return lastUsed; }

    // Setters
    public void setId(long id)               { this.id = id; }
    public void setOwner(String owner)       { this.owner = owner; }
    public void setServerName(String s)      { this.serverName = s; }
    public void setWorld(String world)       { this.world = world; }
    public void setLocX(double locX)         { this.locX = locX; }
    public void setLocY(double locY)         { this.locY = locY; }
    public void setLocZ(double locZ)         { this.locZ = locZ; }
    public void setLocYaw(float locYaw)      { this.locYaw = locYaw; }
    public void setLocPitch(float locPitch)  { this.locPitch = locPitch; }
    public void setName(String name)         { this.name = name; }
    public void setSymbol(String symbol)     { this.symbol = symbol; }
    public void setLastUsed(long lastUsed)   { this.lastUsed = lastUsed; }
}
