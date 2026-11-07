package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.CombatQueries;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

final class DeferredHits {

    private record Hit(ShipAPI target, Vector2f point, float damage) {
    }

    private final DamageType damageType;
    private final boolean bypassShields;
    private final List<Hit> pending = new ArrayList<>();

    DeferredHits(DamageType damageType, boolean bypassShields) {
        this.damageType = damageType;
        this.bypassShields = bypassShields;
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
            if (CombatQueries.isAliveNotHulk(hit.target())) {
                engine.applyDamage(hit.target(), hit.point(), hit.damage(), damageType, 0f, bypassShields, false, source);
            }
        }
    }
}
