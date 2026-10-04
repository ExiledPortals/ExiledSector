package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

record ChainLink(List<ShipAPI> hitSoFar, int count, float dealtMult) {

    static final String HIT_LIST_KEY = "exiledSector_energyChainHitList";
    static final String COUNT_KEY = "exiledSector_energyChainCount";
    static final String DEALT_MULT_KEY = "exiledSector_energyChainDealtMult";

    // unchecked: the hit list is only ever written by tag() as List<ShipAPI>
    @SuppressWarnings("unchecked")
    static ChainLink of(DamagingProjectileAPI projectile, ShipAPI firingShip) {
        Map<String, Object> data = projectile.getCustomData();
        int count = data.get(COUNT_KEY) instanceof Integer stored ? stored : 0;
        float dealtMult = data.get(DEALT_MULT_KEY) instanceof Float stored ? stored : 1f;
        List<ShipAPI> hitSoFar = data.get(HIT_LIST_KEY) instanceof List<?> stored ? (List<ShipAPI>) stored : List.of(firingShip);
        return new ChainLink(hitSoFar, count, dealtMult);
    }

    static boolean isTagged(DamagingProjectileAPI projectile) {
        return projectile.getCustomData().containsKey(COUNT_KEY);
    }

    void tag(DamagingProjectileAPI projectile) {
        projectile.setCustomData(HIT_LIST_KEY, hitSoFar);
        projectile.setCustomData(COUNT_KEY, count);
        projectile.setCustomData(DEALT_MULT_KEY, dealtMult);
    }

    ChainLink next(ShipAPI hitShip, float falloffPercent) {
        List<ShipAPI> hits = new ArrayList<>(hitSoFar);
        hits.add(hitShip);
        return new ChainLink(hits, count + 1, dealtMult * (1f - falloffPercent / 100f));
    }
}
