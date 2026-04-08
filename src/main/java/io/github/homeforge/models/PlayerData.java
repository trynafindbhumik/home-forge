package io.github.homeforge.models;


 // Persistent per-player metadata stored in the Players table.
public class PlayerData {

    private String uuid;
    private Long   primaryHome;  // FK → Homes.id; null = none set
    private long   extraHomes;   // Admin-granted bonus slots

    public PlayerData() {}

    public PlayerData(String uuid, Long primaryHome, long extraHomes) {
        this.uuid        = uuid;
        this.primaryHome = primaryHome;
        this.extraHomes  = extraHomes;
    }

    public String getUuid()        { return uuid; }
    public Long   getPrimaryHome() { return primaryHome; }
    public long   getExtraHomes()  { return extraHomes; }

    public void setUuid(String uuid)             { this.uuid = uuid; }
    public void setPrimaryHome(Long primaryHome) { this.primaryHome = primaryHome; }
    public void setExtraHomes(long extraHomes)   { this.extraHomes = extraHomes; }
}
