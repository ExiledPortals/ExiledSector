package exiledlib.skills.skilleffect.logistics;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledlib.PlayerFleet;

import java.util.HashMap;
import java.util.Map;

public final class FleetCrewLedger {

    private static final String LEDGER_KEY = "exiledSector_fleetCrewLedger";
    private static final float ROUNDING_TOLERANCE = 1e-4f;
    private static float pendingStolen;
    private static int pendingSacrificed;

    private final boolean recorded;
    private final boolean unlimitedCrew;
    private final Map<Object, Float> creditedWrecks = new HashMap<>();
    private int remainingCrew;
    private float crewStolen;
    private int crewSacrificed;

    FleetCrewLedger(int remainingCrew, boolean recorded, boolean unlimitedCrew) {
        this.remainingCrew = remainingCrew;
        this.recorded = recorded;
        this.unlimitedCrew = unlimitedCrew;
    }

    public record CrewChange(int stolen, int sacrificed) {

        public int net() {
            return stolen - sacrificed;
        }
    }

    public static final class Lazy {

        private FleetCrewLedger ledger;

        public FleetCrewLedger get() {
            if (ledger == null) {
                ledger = forCurrentCombat();
            }
            return ledger;
        }
    }

    public static boolean usesFleetCrew(ShipAPI ship) {
        return ship.getOwner() == 0 && !ship.isAlly();
    }

    public static FleetCrewLedger forCurrentCombat() {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) {
            return new FleetCrewLedger(0, false, false);
        }
        if (engine.getCustomData().get(LEDGER_KEY) instanceof FleetCrewLedger existing) {
            return existing;
        }
        CampaignFleetAPI playerFleet = PlayerFleet.orNull();
        boolean recorded = playerFleet != null && engine.isInCampaign() && !engine.isInCampaignSim() && !engine.isSimulation();
        int crew = playerFleet == null ? 0 : (int) playerFleet.getCargo().getCrew();
        FleetCrewLedger ledger = new FleetCrewLedger(crew, recorded, playerFleet == null);
        if (recorded) {
            discardPending();
        }
        engine.getCustomData().put(LEDGER_KEY, ledger);
        return ledger;
    }

    public boolean hasCrew() {
        return unlimitedCrew || remainingCrew > 0;
    }

    public void sacrifice() {
        if (!hasCrew()) {
            return;
        }
        if (!unlimitedCrew) {
            remainingCrew--;
        }
        crewSacrificed++;
        if (recorded) {
            publishSacrificed(crewSacrificed);
        }
    }

    public void credit(Object wreck, float crew) {
        Float previous = creditedWrecks.get(wreck);
        float already = previous == null ? 0f : previous;
        if (crew <= already) {
            return;
        }
        creditedWrecks.put(wreck, crew);
        int wholeBefore = roundUp(crewStolen);
        crewStolen += crew - already;
        remainingCrew += roundUp(crewStolen) - wholeBefore;
        if (recorded) {
            publishStolen(crewStolen);
        }
    }

    private static void publishSacrificed(int value) {
        pendingSacrificed = value;
    }

    private static void publishStolen(float value) {
        pendingStolen = value;
    }

    static int roundUp(float crew) {
        return crew <= ROUNDING_TOLERANCE ? 0 : (int) Math.ceil(crew - ROUNDING_TOLERANCE);
    }

    public static CrewChange drain() {
        CrewChange change = new CrewChange(roundUp(pendingStolen), pendingSacrificed);
        discardPending();
        return change;
    }

    public static void discardPending() {
        pendingStolen = 0f;
        pendingSacrificed = 0;
    }
}
