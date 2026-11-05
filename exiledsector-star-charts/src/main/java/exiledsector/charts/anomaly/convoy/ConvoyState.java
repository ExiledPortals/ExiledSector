package exiledsector.charts.anomaly.convoy;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;

import java.util.ArrayList;
import java.util.List;

public final class ConvoyState {

    public enum Phase {
        WAITING,
        EN_ROUTE,
        UNLOADING,
        DELIVERED,
        LOST
    }

    private final int index;
    private String fleetId;
    private final List<String> waypointIds;
    private final String destinationId;
    private final float entryX;
    private final float entryY;
    private final float exitX;
    private final float exitY;
    private final long cargoSeed;
    private float startFreighterPoints;
    private float carriedFreighterPoints;
    private float cargoSteps;
    private boolean civilianOnly;
    private float itemChanceMult;
    private Phase phase = Phase.WAITING;
    private boolean hostile;
    private float deliveredShare;
    private CargoAPI cache;
    private boolean cacheTaken;
    private int dropsRolled;
    private SectorEntityToken entryToken;
    private SectorEntityToken exitToken;

    public ConvoyState(int index, List<String> waypointIds, String destinationId, float entryX, float entryY, float exitX, float exitY, long cargoSeed) {
        this.index = index;
        this.waypointIds = new ArrayList<>(waypointIds);
        this.destinationId = destinationId;
        this.entryX = entryX;
        this.entryY = entryY;
        this.exitX = exitX;
        this.exitY = exitY;
        this.cargoSeed = cargoSeed;
    }

    public int index() {
        return index;
    }

    public String fleetId() {
        return fleetId;
    }

    void setFleetId(String fleetId) {
        this.fleetId = fleetId;
    }

    public List<String> waypointIds() {
        return List.copyOf(waypointIds);
    }

    public String destinationId() {
        return destinationId;
    }

    float entryX() {
        return entryX;
    }

    float entryY() {
        return entryY;
    }

    float exitX() {
        return exitX;
    }

    float exitY() {
        return exitY;
    }

    long cargoSeed() {
        return cargoSeed;
    }

    void placeTokens(SectorEntityToken entry, SectorEntityToken exit) {
        entryToken = entry;
        exitToken = exit;
    }

    SectorEntityToken entryToken() {
        return entryToken;
    }

    SectorEntityToken exitToken() {
        return exitToken;
    }

    void loadCargo(float freighterPoints, float steps, float chanceMult) {
        startFreighterPoints = Math.max(0f, freighterPoints);
        carriedFreighterPoints = startFreighterPoints;
        cargoSteps = Math.max(0f, steps);
        itemChanceMult = Math.max(0f, chanceMult);
    }

    public float cargoSteps() {
        return cargoSteps;
    }

    public float itemChanceMult() {
        return itemChanceMult;
    }

    void countCivilianOnly(boolean civilianOnly) {
        this.civilianOnly = civilianOnly;
    }

    boolean countsCivilianOnly() {
        return civilianOnly;
    }

    float carriedFreighterPoints() {
        return carriedFreighterPoints;
    }

    public float carriedShare() {
        return startFreighterPoints > 0f ? carriedFreighterPoints / startFreighterPoints : 0f;
    }

    float recordFreighterPoints(float remainingPoints) {
        float remaining = Math.max(0f, Math.min(carriedFreighterPoints, remainingPoints));
        float lost = carriedFreighterPoints - remaining;
        carriedFreighterPoints = remaining;
        if (phase == Phase.DELIVERED) {
            return 0f;
        }
        if (remaining <= 0f) {
            phase = Phase.LOST;
        }
        return startFreighterPoints > 0f ? lost / startFreighterPoints : 0f;
    }

    public Phase phase() {
        return phase;
    }

    void setPhase(Phase phase) {
        this.phase = phase;
    }

    public boolean departed() {
        return phase != Phase.WAITING;
    }

    public boolean isHostile() {
        return hostile;
    }

    boolean markHostile() {
        if (hostile) {
            return false;
        }
        hostile = true;
        return true;
    }

    boolean deliver() {
        if (phase == Phase.DELIVERED || phase == Phase.LOST) {
            return false;
        }
        phase = Phase.DELIVERED;
        deliveredShare = carriedShare();
        return true;
    }

    public float deliveredShare() {
        return deliveredShare;
    }

    void storeCache(CargoAPI cargo) {
        cache = cargo;
    }

    public boolean hasCache() {
        return phase == Phase.DELIVERED && cache != null && !cache.isEmpty() && !cacheTaken;
    }

    CargoAPI takeCache() {
        if (!hasCache()) {
            return null;
        }
        cacheTaken = true;
        CargoAPI taken = cache;
        cache = null;
        return taken;
    }

    public boolean isCacheTaken() {
        return cacheTaken;
    }

    long nextDropSeed() {
        dropsRolled++;
        return cargoSeed ^ (dropsRolled * 0x9E3779B97F4A7C15L);
    }
}
