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
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.DeploymentRecordPruner;
import exiledsector.skills.skilleffect.Taper;
import exiledsector.skills.skilleffect.TemporaryCombatSpawn;
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
    private final FleetMemberAPI temporaryMember;
    private final CombatFleetManagerAPI fleetManager;
    private final Taper fade = new Taper();
    private boolean dismissed;

    private PhaseAfterimage(ShipAPI afterimage, FleetMemberAPI temporaryMember, CombatFleetManagerAPI fleetManager, float lifetimeSeconds) {
        this.afterimage = afterimage;
        this.temporaryMember = temporaryMember;
        this.fleetManager = fleetManager;
        fade.start(lifetimeSeconds);
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
        variant.addTag(CombatQueries.AFTERIMAGE_TAG);
        FleetMemberAPI temporaryMember = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variant);
        temporaryMember.setOwner(ownerShip.getOwner());
        temporaryMember.setAlly(ownerShip.isAlly() || ownerShip.getOwner() == Misc.OWNER_PLAYER);
        temporaryMember.getRepairTracker().setCR(AFTERIMAGE_READINESS);
        ShipAPI afterimage = TemporaryCombatSpawn.spawnQuietly(fleetManager, temporaryMember, ownerShip.getLocation(), ownerShip.getFacing(),
                (error, settledShip) -> LOG.warn("The afterimage of " + ownerShip.getHullSpec().getHullId() + " threw while settling in after spawning"
                        + (settledShip == null ? "; Phantom Wake leaves none" : "; Phantom Wake keeps it"), error));
        if (afterimage == null) {
            return null;
        }
        DeploymentRecordPruner.forget(fleetManager, temporaryMember);
        temporaryMember.getStats().getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyMult(STATS_MOD_ID, 0f);
        afterimage.setInvalidTransferCommandTarget(true);
        makeInert(afterimage, ownerShip.getLocation(), ownerShip.getFacing());
        PhaseAfterimage plugin = new PhaseAfterimage(afterimage, temporaryMember, fleetManager, lifetimeSeconds);
        engine.addPlugin(plugin);
        return plugin;
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
        holdAt(afterimage, entryLocation, entryFacing);
    }

    private static void holdAt(ShipAPI afterimage, Vector2f entryLocation, float entryFacing) {
        afterimage.getLocation().set(entryLocation);
        afterimage.setFacing(entryFacing);
        afterimage.getVelocity().set(0f, 0f);
        afterimage.setAngularVelocity(0f);
        afterimage.setAlphaMult(START_ALPHA);
    }

    boolean restartAt(ShipAPI ownerShip, float lifetimeSeconds, CombatEngineAPI engine) {
        if (dismissed || lifetimeSeconds <= 0f || !engine.isEntityInPlay(afterimage)) {
            dismiss(engine);
            return false;
        }
        holdAt(afterimage, ownerShip.getLocation(), ownerShip.getFacing());
        fade.start(lifetimeSeconds);
        return true;
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) {
            return;
        }
        fade.advance(amount);
        if (!fade.isRunning() || !engine.isEntityInPlay(afterimage)) {
            dismiss(engine);
            return;
        }
        float share = fade.share();
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
            TemporaryCombatSpawn.dismiss(engine, fleetManager, afterimage, temporaryMember);
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
