package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.threat.RoilingSwarmEffect;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.DealtHullDamage;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.shipsystem.FragmentSwarms;

final class HullDamageRegrowthListener extends ShipCombatListener implements DealtHullDamage.Receiver {

    static final String FRAGMENTS_PER_STEP_KEY = "exiledSector_hullDamageFragmentRegrowth";
    static final float HULL_DAMAGE_PER_STEP = 300f;

    private float fragmentsOwed;

    HullDamageRegrowthListener(ShipAPI ownerShip) {
        super(ownerShip);
        DealtHullDamage.register(ownerShip, this);
    }

    @Override
    public void onHullDamageDealt(ShipAPI target, float hullDamage) {
        if (target.isHulk() || !CombatQueries.isHostile(ownerShip, target) || !ownerIsAliveNotHulk()) {
            return;
        }
        RoilingSwarmEffect swarm = FragmentSwarms.swarmOf(ownerShip);
        if (swarm == null) {
            return;
        }
        fragmentsOwed += fragmentsEarned(hullDamage, magnitude(FRAGMENTS_PER_STEP_KEY));
        int whole = (int) fragmentsOwed;
        if (whole > 0) {
            fragmentsOwed -= whole;
            FragmentSwarms.regrow(swarm, whole);
        }
    }

    static float fragmentsEarned(float hullDamage, float fragmentsPerStep) {
        return hullDamage <= 0f || fragmentsPerStep <= 0f ? 0f : hullDamage * fragmentsPerStep / HULL_DAMAGE_PER_STEP;
    }
}
