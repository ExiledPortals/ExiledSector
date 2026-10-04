package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;

import java.util.HashMap;
import java.util.Map;

public final class FleetCrewLedger {

    private static final String LEDGER_KEY = "exiledSector_fleetCrewLedger";
    private static final float ROUNDING_TOLERANCE = 1e-4f;
    private static float pendingStolen;
    private static int pendingSacrificed;

    private final boolean recorded;
    private final boolean unlimited;
    private final Map<Object, Float> creditedWrecks = new HashMap<>();
    private int remaining;
    private float stolen;
    private int sacrificed;

    FleetCrewLedger(int remaining, boolean recorded, boolean unlimited) {
        this.remaining = remaining;
        this.recorded = recorded;
        this.unlimited = unlimited;
    }

    public record CrewChange(int stolen, int sacrificed) {

        public int net() {
            return stolen - sacrificed;
        }
    }

    static FleetCrewLedger forCurrentCombat() {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) {
            return new FleetCrewLedger(0, false, false);
        }
        if (engine.getCustomData().get(LEDGER_KEY) instanceof FleetCrewLedger existing) {
            return existing;
        }
        CampaignFleetAPI playerFleet = playerFleet();
        boolean recorded = playerFleet != null && engine.isInCampaign() && !engine.isInCampaignSim() && !engine.isSimulation();
        int crew = playerFleet == null ? 0 : (int) playerFleet.getCargo().getCrew();
        FleetCrewLedger ledger = new FleetCrewLedger(crew, recorded, playerFleet == null);
        if (recorded) {
            pendingStolen = 0f;
            pendingSacrificed = 0;
        }
        engine.getCustomData().put(LEDGER_KEY, ledger);
        return ledger;
    }

    private static CampaignFleetAPI playerFleet() {
        SectorAPI sector = Global.getSector();
        return sector == null ? null : sector.getPlayerFleet();
    }

    boolean hasCrew() {
        return unlimited || remaining > 0;
    }

    void sacrifice() {
        if (!hasCrew()) {
            return;
        }
        if (!unlimited) {
            remaining--;
        }
        sacrificed++;
        if (recorded) {
            pendingSacrificed = sacrificed;
        }
    }

    void credit(Object wreck, float crew) {
        Float previous = creditedWrecks.get(wreck);
        float already = previous == null ? 0f : previous;
        if (crew <= already) {
            return;
        }
        creditedWrecks.put(wreck, crew);
        int wholeBefore = roundUp(stolen);
        stolen += crew - already;
        remaining += roundUp(stolen) - wholeBefore;
        if (recorded) {
            pendingStolen = stolen;
        }
    }

    static int roundUp(float crew) {
        return crew <= ROUNDING_TOLERANCE ? 0 : (int) Math.ceil(crew - ROUNDING_TOLERANCE);
    }

    public static CrewChange drain() {
        CrewChange change = new CrewChange(roundUp(pendingStolen), pendingSacrificed);
        pendingStolen = 0f;
        pendingSacrificed = 0;
        return change;
    }
}
