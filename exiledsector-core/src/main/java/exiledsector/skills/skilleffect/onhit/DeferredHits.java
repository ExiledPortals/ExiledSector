package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.CombatQueries;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

final class DeferredHits {

    private record Hit(ShipAPI target, Vector2f point, float damage) {
    }

    private final DamageType damageType;
    private final boolean shieldHitsOnly;
    private final List<Hit> pending = new ArrayList<>();

    private DeferredHits(DamageType damageType, boolean shieldHitsOnly) {
        this.damageType = damageType;
        this.shieldHitsOnly = shieldHitsOnly;
    }

    static DeferredHits onShields(DamageType damageType) {
        return new DeferredHits(damageType, true);
    }

    static DeferredHits pastShields(DamageType damageType) {
        return new DeferredHits(damageType, false);
    }

    void add(ShipAPI target, Vector2f point, float damage) {
        pending.add(new Hit(target, new Vector2f(point), damage));
    }

    void applyAll(ShipAPI source) {
        if (pending.isEmpty()) {
            return;
        }
        CombatEngineAPI engine = Global.getCombatEngine();
        List<Hit> hits = new ArrayList<>(pending);
        pending.clear();
        if (engine == null) {
            return;
        }
        for (Hit hit : hits) {
            if (CombatQueries.isAliveNotHulk(hit.target()) && (!shieldHitsOnly || shieldCovers(hit.target(), hit.point()))) {
                engine.applyDamage(hit.target(), hit.point(), hit.damage(), damageType, 0f, !shieldHitsOnly, false, source);
            }
        }
    }

    private static boolean shieldCovers(ShipAPI target, Vector2f point) {
        ShieldAPI shield = target.getShield();
        return shield != null && shield.isWithinArc(point);
    }
}
