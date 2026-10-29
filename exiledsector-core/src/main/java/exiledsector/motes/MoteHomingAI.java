package exiledsector.motes;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.GuidedMissileAI;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.util.Misc;
import exiledsector.skills.skilleffect.CombatQueries;
import org.lwjgl.util.vector.Vector2f;

public final class MoteHomingAI implements MissileAIPlugin, GuidedMissileAI {

    static final float DRIFT_SECONDS = 0.35f;
    static final float RETARGET_RANGE = 1500f;
    static final float RETARGET_SCAN_SECONDS = 0.075f;
    private static final float LEAD_ACCURACY = 1.5f;
    private static final float LEAD_MAX_RANGE_PAD = 50f;
    private static final float HEAD_IN_DIRECTION_TOLERANCE = 10000f;

    private final MissileAPI mote;
    private final boolean spinsLeft;
    private CombatEntityAPI target;
    private float secondsAlive;
    private float secondsUntilRetargetScan;

    MoteHomingAI(MissileAPI mote, CombatEntityAPI target, boolean spinsLeft) {
        this.mote = mote;
        this.target = target;
        this.spinsLeft = spinsLeft;
    }

    @Override
    public CombatEntityAPI getTarget() {
        return target;
    }

    @Override
    public void setTarget(CombatEntityAPI target) {
        if (target == null || target instanceof ShipAPI ship && isHostileShip(mote.getSource(), ship)) {
            this.target = target;
        }
    }

    static boolean isHostileShip(ShipAPI source, ShipAPI candidate) {
        return source != null && !candidate.isFighter() && CombatQueries.isHostile(source, candidate);
    }

    @Override
    public void advance(float amount) {
        if (mote.isFizzling() || mote.isFading()) {
            return;
        }
        ShipAPI source = mote.getSource();
        if (source == null || !source.isAlive() || source.isHulk()) {
            mote.flameOut();
            return;
        }
        mote.getEngineController().forceShowAccelerating();
        secondsAlive += amount;
        if (secondsAlive < DRIFT_SECONDS) {
            return;
        }
        CombatEngineAPI engine = Global.getCombatEngine();
        if (!isValidTarget(engine, target)) {
            target = null;
            secondsUntilRetargetScan -= amount;
            if (secondsUntilRetargetScan > 0f) {
                return;
            }
            secondsUntilRetargetScan = RETARGET_SCAN_SECONDS;
            target = CombatQueries.nearestShip(mote.getLocation(), RETARGET_RANGE,
                    candidate -> isHostileShip(source, candidate) && isValidTarget(engine, candidate));
            if (target == null) {
                return;
            }
        }
        Vector2f aimPoint = engine.getAimPointWithLeadForAutofire(mote, LEAD_ACCURACY, target, LEAD_MAX_RANGE_PAD);
        engine.headInDirectionWithoutTurning(mote, Misc.getAngleInDegreesStrict(mote.getLocation(), aimPoint), HEAD_IN_DIRECTION_TOLERANCE);
        mote.giveCommand(spinsLeft ? ShipCommand.TURN_LEFT : ShipCommand.TURN_RIGHT);
    }

    static boolean isValidTarget(CombatEngineAPI engine, CombatEntityAPI candidate) {
        return candidate instanceof ShipAPI ship && ship.isAlive() && !ship.isHulk() && !ship.isPhased() && engine.isEntityInPlay(ship);
    }
}
