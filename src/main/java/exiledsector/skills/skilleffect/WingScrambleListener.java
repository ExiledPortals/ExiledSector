package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

final class WingScrambleListener extends ShipCombatListener implements AdvanceableListener {

    static final String COPIES_KEY = "exiledSector_wingScrambleCopies";
    static final float MAX_WAIT_FOR_LAUNCH_SECONDS = 4f;
    static final float ROUTING_INTERVAL_SECONDS = 0.25f;
    private static final Logger LOG = Logger.getLogger(WingScrambleListener.class);

    private final Map<ShipAPI, FighterWingAPI> donorByFighter = new IdentityHashMap<>();
    private List<List<FighterWingAPI>> roleGroups;
    private float secondsWaited;
    private float secondsSinceRouting;

    WingScrambleListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    @Override
    public void advance(float amount) {
        if (ownerShip.isFighter() || !ownerIsAliveNotHulk()) {
            return;
        }
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) {
            return;
        }
        int copiesPerDonor = Math.round(magnitude(COPIES_KEY));
        if (copiesPerDonor <= 0) {
            return;
        }
        if (roleGroups == null) {
            secondsWaited += amount;
            List<FighterWingAPI> wings = ownerShip.getAllWings();
            if (wings == null || wings.isEmpty() || !initialLaunchDone(wings) && secondsWaited < MAX_WAIT_FOR_LAUNCH_SECONDS) {
                return;
            }
            roleGroups = WingScramble.roleGroups(wings, wing -> wing.getSpec() == null ? null : wing.getSpec().getRole());
            routeAll(engine, copiesPerDonor, true);
            return;
        }
        secondsSinceRouting += amount;
        if (secondsSinceRouting >= ROUTING_INTERVAL_SECONDS) {
            secondsSinceRouting = 0f;
            routeAll(engine, copiesPerDonor, false);
        }
    }

    private boolean initialLaunchDone(List<FighterWingAPI> wings) {
        int fittedWings = ownerShip.getVariant() == null ? 0 : ownerShip.getVariant().getWings().size();
        if (wings.size() < fittedWings) {
            return false;
        }
        for (FighterWingAPI wing : wings) {
            if (wing.getSpec() != null && wing.getWingMembers().size() < wing.getSpec().getNumFighters()) {
                return false;
            }
        }
        return true;
    }

    private void routeAll(CombatEngineAPI engine, int copiesPerDonor, boolean spawnMissing) {
        donorByFighter.keySet().removeIf(fighter -> !fighter.isAlive() || !engine.isEntityInPlay(fighter));
        for (List<FighterWingAPI> group : roleGroups) {
            Map<FighterWingAPI, List<ShipAPI>> movableMembers = new IdentityHashMap<>();
            for (FighterWingAPI wing : group) {
                movableMembers.put(wing, new ArrayList<>(wing.getWingMembers()));
            }
            WingScramble.Routing<ShipAPI, FighterWingAPI> routing = WingScramble.route(group, movableMembers, donorByFighter, copiesPerDonor,
                    spawnMissing);
            for (WingScramble.Move<ShipAPI, FighterWingAPI> move : routing.moves()) {
                if (isDocked(move.fighter())) {
                    continue;
                }
                donorByFighter.put(move.fighter(), donorOf(move.fighter()));
                rehome(move.fighter(), move.toWing());
            }
            for (ShipAPI surplus : routing.discards()) {
                if (!isDocked(surplus)) {
                    discard(engine, surplus);
                }
            }
            for (WingScramble.Spawn<FighterWingAPI> spawn : routing.spawns()) {
                try {
                    ShipAPI fighter = launchCopy(engine, spawn.donorWing().getSpec(), spawn.hostWing());
                    if (fighter != null) {
                        donorByFighter.put(fighter, spawn.donorWing());
                    }
                } catch (RuntimeException e) {
                    LOG.error("Couldn't add a " + spawn.donorWing().getSpec().getId() + " to a wing of " + ownerShip.getName(), e);
                }
            }
        }
    }

    private FighterWingAPI donorOf(ShipAPI fighter) {
        FighterWingAPI donor = donorByFighter.get(fighter);
        return donor != null ? donor : fighter.getWing();
    }

    private static boolean isDocked(ShipAPI fighter) {
        return fighter.isLanding() || fighter.isFinishedLanding();
    }

    private static void rehome(ShipAPI fighter, FighterWingAPI toWing) {
        FighterWingAPI fromWing = fighter.getWing();
        if (fromWing == toWing) {
            return;
        }
        if (fromWing != null) {
            fromWing.removeMember(fighter);
        }
        fighter.setWing(toWing);
        toWing.addMember(fighter);
        fighter.setShipAI(Global.getSettings().pickShipAIPlugin(null, fighter));
    }

    private void discard(CombatEngineAPI engine, ShipAPI fighter) {
        donorByFighter.remove(fighter);
        if (fighter.getWing() != null) {
            fighter.getWing().removeMember(fighter);
        }
        engine.removeEntity(fighter);
    }

    private ShipAPI launchCopy(CombatEngineAPI engine, FighterWingSpecAPI copySpec, FighterWingAPI hostWing) {
        CombatFleetManagerAPI fleetManager = engine.getFleetManager(ownerShip.getOwner());
        boolean wasSuppressed = fleetManager.isSuppressDeploymentMessages();
        fleetManager.setSuppressDeploymentMessages(true);
        ShipAPI fighter;
        try {
            fighter = fleetManager.spawnShipOrWing(copySpec.getId(), new Vector2f(ownerShip.getLocation()), ownerShip.getFacing());
        } finally {
            fleetManager.setSuppressDeploymentMessages(wasSuppressed);
        }
        if (fighter == null) {
            return null;
        }
        FighterWingAPI spawnedWing = fighter.getWing();
        if (spawnedWing != null) {
            for (ShipAPI spare : new ArrayList<>(spawnedWing.getWingMembers())) {
                spawnedWing.removeMember(spare);
                if (spare != fighter) {
                    engine.removeEntity(spare);
                }
            }
            fleetManager.removeDeployed(spawnedWing, false);
        }
        fighter.setWing(hostWing);
        hostWing.addMember(fighter);
        fighter.setAlly(ownerShip.isAlly());
        fighter.setCurrentCR(ownerShip.getCurrentCR());
        applyCarrierHullMods(fighter);
        fighter.setAnimatedLaunch();
        fighter.setLaunchingShip(ownerShip);
        fighter.setShipAI(Global.getSettings().pickShipAIPlugin(null, fighter));
        return fighter;
    }

    private void applyCarrierHullMods(ShipAPI fighter) {
        if (ownerShip.getVariant() == null) {
            return;
        }
        for (String hullModId : ownerShip.getVariant().getHullMods()) {
            HullModSpecAPI spec = Global.getSettings().getHullModSpec(hullModId);
            if (spec != null && spec.getEffect() != null) {
                spec.getEffect().applyEffectsToFighterSpawnedByShip(fighter, ownerShip, hullModId);
            }
        }
    }
}
