package exiledlib.skills;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;

public record StatBaseline(float armor, float hull, float fluxCapacity, float fluxDissipation, float topSpeed) {

    public static final StatBaseline NONE = new StatBaseline(0f, 0f, 0f, 0f, 0f);

    public static StatBaseline of(ShipVariantAPI variant) {
        return variant == null ? NONE : of(variant.getHullSpec());
    }

    public static StatBaseline of(ShipHullSpecAPI hullSpec) {
        if (hullSpec == null) {
            return NONE;
        }
        ShipHullSpecAPI.EngineSpecAPI engineSpec = hullSpec.getEngineSpec();
        return new StatBaseline(hullSpec.getArmorRating(), hullSpec.getHitpoints(), hullSpec.getFluxCapacity(),
                hullSpec.getFluxDissipation(), engineSpec == null ? 0f : engineSpec.getMaxSpeed());
    }
}
