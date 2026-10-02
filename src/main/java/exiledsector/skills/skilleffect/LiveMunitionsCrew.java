package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;

public final class LiveMunitionsCrew {

    private static final String POOL_KEY = "exiledSector_liveMunitionsCrewPool";
    private static int pendingDeaths;

    private final boolean recorded;
    private int remaining;

    LiveMunitionsCrew(int remaining, boolean recorded) {
        this.remaining = remaining;
        this.recorded = recorded;
    }

    static LiveMunitionsCrew forCurrentCombat() {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) {
            return new LiveMunitionsCrew(0, false);
        }
        if (engine.getCustomData().get(POOL_KEY) instanceof LiveMunitionsCrew existing) {
            return existing;
        }
        CampaignFleetAPI playerFleet = playerFleet();
        boolean recorded = playerFleet != null && engine.isInCampaign() && !engine.isInCampaignSim() && !engine.isSimulation();
        int crew = playerFleet == null ? Integer.MAX_VALUE : (int) playerFleet.getCargo().getCrew();
        LiveMunitionsCrew pool = new LiveMunitionsCrew(crew, recorded);
        if (recorded) {
            pendingDeaths = 0;
        }
        engine.getCustomData().put(POOL_KEY, pool);
        return pool;
    }

    private static CampaignFleetAPI playerFleet() {
        SectorAPI sector = Global.getSector();
        return sector == null ? null : sector.getPlayerFleet();
    }

    boolean hasCrew() {
        return remaining > 0;
    }

    void sacrifice() {
        if (remaining <= 0) {
            return;
        }
        remaining--;
        if (recorded) {
            pendingDeaths++;
        }
    }

    public static int drainPendingDeaths() {
        int deaths = pendingDeaths;
        pendingDeaths = 0;
        return deaths;
    }
}
