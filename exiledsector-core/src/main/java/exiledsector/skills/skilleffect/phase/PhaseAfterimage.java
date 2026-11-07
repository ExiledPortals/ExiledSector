package exiledsector.skills.skilleffect.phase;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAIConfig;
import com.fs.starfarer.api.combat.ShipAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.input.InputEventAPI;
import exiledsector.skills.skilleffect.DeploymentRecordPruner;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.List;

final class PhaseAfterimage extends BaseEveryFrameCombatPlugin {

    static final String VARIANT_ID_PREFIX = "exiledSector_phaseAfterimage_";
    static final String STATS_MOD_ID = "exiledSector_phaseAfterimage";
    static final float AFTERIMAGE_READINESS = 0.7f;
    static final float START_ALPHA = 0.6f;
    static final Color JITTER_COLOR = new Color(170, 120, 255, 140);
    static final int JITTER_COPIES = 3;
    static final float JITTER_RANGE = 6f;
    private static final Logger LOG = Logger.getLogger(PhaseAfterimage.class);

    private final ShipAPI afterimage;
    private final CombatFleetManagerAPI fleetManager;
    private final float lifetimeSeconds;
    private float secondsLeft;
    private boolean dismissed;

    private PhaseAfterimage(ShipAPI afterimage, CombatFleetManagerAPI fleetManager, float lifetimeSeconds) {
        this.afterimage = afterimage;
        this.fleetManager = fleetManager;
        this.lifetimeSeconds = lifetimeSeconds;
        this.secondsLeft = lifetimeSeconds;
    }

    static boolean canLeaveAfterimage(ShipAPI ship) {
        ShipHullSpecAPI hullSpec = ship.getHullSpec();
        return hullSpec != null && !ship.isFighter() && !ship.isStationModule() && ship.getChildModulesCopy().isEmpty()
                && hullSpec.getBuiltInWings().isEmpty();
    }

    static PhaseAfterimage leave(ShipAPI ownerShip, float lifetimeSeconds, CombatEngineAPI engine) {
        if (lifetimeSeconds <= 0f || !canLeaveAfterimage(ownerShip)) {
            return null;
        }
        CombatFleetManagerAPI fleetManager = engine.getFleetManager(ownerShip.getOwner());
        ShipVariantAPI variant = Global.getSettings().createEmptyVariant(VARIANT_ID_PREFIX + ownerShip.getId(), ownerShip.getHullSpec());
        FleetMemberAPI temporaryMember = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variant);
        temporaryMember.setOwner(ownerShip.getOwner());
        temporaryMember.setAlly(ownerShip.isAlly());
        temporaryMember.getRepairTracker().setCR(AFTERIMAGE_READINESS);
        ShipAPI afterimage = spawnQuietly(fleetManager, temporaryMember, ownerShip);
        if (afterimage == null) {
            return null;
        }
        DeploymentRecordPruner.forget(fleetManager, temporaryMember);
        makeInert(afterimage, ownerShip.getLocation(), ownerShip.getFacing());
        PhaseAfterimage plugin = new PhaseAfterimage(afterimage, fleetManager, lifetimeSeconds);
        engine.addPlugin(plugin);
        return plugin;
    }

    private static ShipAPI spawnQuietly(CombatFleetManagerAPI fleetManager, FleetMemberAPI temporaryMember, ShipAPI ownerShip) {
        boolean wasSuppressed = fleetManager.isSuppressDeploymentMessages();
        fleetManager.setSuppressDeploymentMessages(true);
        try {
            return fleetManager.spawnFleetMember(temporaryMember, new Vector2f(ownerShip.getLocation()), ownerShip.getFacing(), 0f);
        } catch (RuntimeException e) {
            ShipAPI settling = fleetManager.getShipFor(temporaryMember);
            LOG.warn("The afterimage of " + ownerShip.getHullSpec().getHullId() + " threw while settling in after spawning"
                    + (settling == null ? "; Phantom Wake leaves none" : "; Phantom Wake keeps it"), e);
            return settling;
        } finally {
            fleetManager.setSuppressDeploymentMessages(wasSuppressed);
        }
    }

    static void makeInert(ShipAPI afterimage, Vector2f entryLocation, float entryFacing) {
        MutableShipStatsAPI stats = afterimage.getMutableStats();
        stats.getHullDamageTakenMult().modifyMult(STATS_MOD_ID, 0f);
        stats.getArmorDamageTakenMult().modifyMult(STATS_MOD_ID, 0f);
        stats.getEmpDamageTakenMult().modifyMult(STATS_MOD_ID, 0f);
        afterimage.setShield(ShieldAPI.ShieldType.NONE, 0f, 1f, 0f);
        afterimage.setCollisionClass(CollisionClass.FIGHTER);
        afterimage.setShipAI(new IdleAI());
        afterimage.setHoldFire(true);
        afterimage.getLocation().set(entryLocation);
        afterimage.setFacing(entryFacing);
        afterimage.getVelocity().set(0f, 0f);
        afterimage.setAngularVelocity(0f);
        afterimage.setAlphaMult(START_ALPHA);
    }

    static float fadeShare(float secondsLeft, float lifetimeSeconds) {
        return lifetimeSeconds <= 0f ? 0f : Math.max(0f, Math.min(1f, secondsLeft / lifetimeSeconds));
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) {
            return;
        }
        secondsLeft -= amount;
        if (secondsLeft <= 0f || !engine.isEntityInPlay(afterimage)) {
            dismiss(engine);
            return;
        }
        float share = fadeShare(secondsLeft, lifetimeSeconds);
        afterimage.getVelocity().set(0f, 0f);
        afterimage.setAngularVelocity(0f);
        afterimage.setAlphaMult(START_ALPHA * share);
        afterimage.setJitter(this, JITTER_COLOR, share, JITTER_COPIES, JITTER_RANGE);
    }

    void dismiss(CombatEngineAPI engine) {
        if (dismissed) {
            return;
        }
        dismissed = true;
        engine.removePlugin(this);
        if (engine.isEntityInPlay(afterimage)) {
            fleetManager.removeDeployed(afterimage, false);
            engine.removeEntity(afterimage);
        }
    }

    private static final class IdleAI implements ShipAIPlugin {

        private final ShipwideAIFlags flags = new ShipwideAIFlags();
        private final ShipAIConfig config = new ShipAIConfig();

        @Override
        public void setDoNotFireDelay(float amount) {
        }

        @Override
        public void forceCircumstanceEvaluation() {
        }

        @Override
        public void advance(float amount) {
        }

        @Override
        public boolean needsRefit() {
            return false;
        }

        @Override
        public ShipwideAIFlags getAIFlags() {
            return flags;
        }

        @Override
        public void cancelCurrentManeuver() {
        }

        @Override
        public ShipAIConfig getConfig() {
            return config;
        }
    }
}
