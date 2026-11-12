package exiledsector.charts.officer.scar;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.BattleObjectiveAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.input.InputEventAPI;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScarCombatPlugin extends BaseEveryFrameCombatPlugin {

    public static final String CUSTOM_DATA_KEY = "exiledSector_scarEvidence";
    static final float SAMPLE_SECONDS = 1f;
    static final int PLAYER_OWNER = 0;
    static final int ENEMY_OWNER = 1;
    private static final Logger LOG = Logger.getLogger(ScarCombatPlugin.class);
    private static boolean failureReported;

    private CombatEngineAPI engine;
    private final List<ScarFlagshipTracker> trackers = new ArrayList<>();
    private boolean active;
    private boolean started;
    private float secondsSinceSample;

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
        trackers.clear();
        started = false;
        secondsSinceSample = 0f;
        active = false;
        if (engine == null || !engine.isInCampaign() || engine.isSimulation() || engine.isInCampaignSim()) {
            return;
        }
        try {
            Map<String, ScarEvidence> evidenceByFlagshipId = new LinkedHashMap<>();
            for (String flagshipMemberId : trackedFlagshipIds(engine)) {
                ScarFlagshipTracker tracker = new ScarFlagshipTracker(engine, flagshipMemberId);
                trackers.add(tracker);
                evidenceByFlagshipId.put(flagshipMemberId, tracker.evidence());
            }
            if (!trackers.isEmpty()) {
                engine.getCustomData().put(CUSTOM_DATA_KEY, evidenceByFlagshipId);
                active = true;
            }
        } catch (RuntimeException | LinkageError e) {
            reportFailure(e);
        }
    }

    static Set<String> trackedFlagshipIds(CombatEngineAPI engine) {
        Set<CampaignFleetAPI> fleets = new LinkedHashSet<>();
        BattleCreationContext context = engine.getContext();
        if (context != null && context.getOtherFleet() != null) {
            fleets.add(context.getOtherFleet());
        }
        CampaignFleetAPI playerFleet = Global.getSector() == null ? null : Global.getSector().getPlayerFleet();
        BattleAPI battle = playerFleet == null ? null : playerFleet.getBattle();
        if (battle != null && battle.getNonPlayerSide() != null) {
            fleets.addAll(battle.getNonPlayerSide());
        }
        Set<String> flagshipIds = new LinkedHashSet<>();
        for (CampaignFleetAPI fleet : fleets) {
            String flagshipId = ScarMemory.flagshipId(fleet.getMemoryWithoutUpdate());
            if (flagshipId != null) {
                flagshipIds.add(flagshipId);
            }
        }
        return flagshipIds;
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (!active || engine == null || engine.isPaused()) {
            return;
        }
        try {
            for (ScarFlagshipTracker tracker : trackers) {
                tracker.advanceFrame(amount);
            }
            secondsSinceSample += amount;
            if (started && secondsSinceSample < SAMPLE_SECONDS) {
                return;
            }
            float sampledSeconds = started ? secondsSinceSample : 0f;
            started = true;
            secondsSinceSample = 0f;
            sample(sampledSeconds);
            active = !allFinished();
        } catch (RuntimeException | LinkageError e) {
            active = false;
            reportFailure(e);
        }
    }

    private boolean allFinished() {
        for (ScarFlagshipTracker tracker : trackers) {
            if (!tracker.finished()) {
                return false;
            }
        }
        return true;
    }

    void sample(float seconds) {
        float playerWarfare = 0f;
        float bossSideWarfare = 0f;
        for (ShipAPI ship : engine.getShips()) {
            if (ship.isFighter() || ship.isHulk() || !ship.isAlive() || ship.getFleetMemberId() == null) {
                continue;
            }
            noteSighting(ship);
            if (ship.getOwner() == PLAYER_OWNER) {
                playerWarfare += electronicWarfare(ship);
            } else if (ship.getOwner() == ENEMY_OWNER) {
                bossSideWarfare += electronicWarfare(ship);
            }
        }
        int heldObjectives = 0;
        List<BattleObjectiveAPI> objectives = engine.getObjectives();
        for (BattleObjectiveAPI objective : objectives) {
            if (objective.getOwner() == PLAYER_OWNER) {
                heldObjectives++;
            }
        }
        for (ScarFlagshipTracker tracker : trackers) {
            if (tracker.found() && !tracker.finished()) {
                tracker.evidence().fleet().noteElectronicWarfareLead(playerWarfare - bossSideWarfare);
                tracker.evidence().fleet().sampleObjectives(heldObjectives, objectives.size());
            }
            tracker.sample(seconds);
        }
    }

    private void noteSighting(ShipAPI ship) {
        for (ScarFlagshipTracker tracker : trackers) {
            if (tracker.finished() || !tracker.evidence().fleet().firstSighting(ship.getFleetMemberId())) {
                continue;
            }
            if (ship.getOwner() == PLAYER_OWNER) {
                tracker.evidence().fleet().addPlayerShip(ScarFleetSnapshot.DeployedShip.of(ship));
            } else if (ship.getOwner() == ENEMY_OWNER) {
                FleetMemberAPI member = ship.getFleetMember();
                tracker.evidence().fleet().addBossSideDeploymentPoints(member == null ? 0f : member.getDeploymentPointsCost());
                if (ship.getParentStation() == null && tracker.flagshipMemberId().equals(ship.getFleetMemberId())) {
                    tracker.attach(ship);
                } else {
                    tracker.noteEscort(ship);
                }
            }
        }
    }

    static float electronicWarfare(ShipAPI ship) {
        return ship.getMutableStats().getDynamic().getMod(Stats.ELECTRONIC_WARFARE_FLAT).computeEffective(0f);
    }

    private static void reportFailure(Throwable failure) {
        if (!failureReported) {
            failureReported = true;
            LOG.error("[ExiledSector] Tallying the Atlas officer flagship's battle for scars failed; no scar is read from this battle", failure);
        }
    }
}
