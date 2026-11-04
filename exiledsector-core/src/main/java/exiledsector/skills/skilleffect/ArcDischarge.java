package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.ModSounds;
import exiledsector.skills.HullSizeTable;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public final class ArcDischarge {

    public static final float RANGE = 600f;
    public static final HullSizeTable DAMAGE = new HullSizeTable(0f, 100f, 150f, 200f, 300f, 100f);
    private static final float ARC_MAX_RANGE = 100000f;
    private static final float ARC_THICKNESS = 10f;
    private static final Color ARC_FRINGE = new Color(95, 98, 205, 255);
    private static final Color ARC_CORE = new Color(220, 225, 255, 255);
    private static final String ARC_SOUND = "tachyon_lance_emp_impact";

    private ArcDischarge() {
    }

    public static boolean discharge(ShipAPI sourceShip, int arcCount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        float arcDamage = DAMAGE.of(sourceShip.getHullSize());
        if (engine == null || arcCount <= 0 || arcDamage <= 0f) {
            return false;
        }
        List<ShipAPI> nearestEnemies = CombatQueries.nearestFirst(
                CombatQueries.hostilesNear(sourceShip, sourceShip.getLocation(), RANGE, candidate -> !candidate.isFighter()),
                sourceShip.getLocation(), arcCount);
        if (nearestEnemies.isEmpty()) {
            return false;
        }
        for (ShipAPI target : arcTargets(nearestEnemies, arcCount)) {
            engine.spawnEmpArc(sourceShip, new Vector2f(sourceShip.getLocation()), sourceShip, target, DamageType.ENERGY, arcDamage, arcDamage,
                    ARC_MAX_RANGE, ModSounds.orSilent(ARC_SOUND), ARC_THICKNESS, ARC_FRINGE, ARC_CORE);
        }
        return true;
    }

    static List<ShipAPI> arcTargets(List<ShipAPI> nearestEnemies, int arcCount) {
        List<ShipAPI> targets = new ArrayList<>(Math.max(0, arcCount));
        for (int arc = 0; arc < arcCount && !nearestEnemies.isEmpty(); arc++) {
            targets.add(nearestEnemies.get(arc % nearestEnemies.size()));
        }
        return targets;
    }
}
