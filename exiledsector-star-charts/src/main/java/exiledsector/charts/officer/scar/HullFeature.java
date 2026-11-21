package exiledsector.charts.officer.scar;

import java.util.Locale;
import java.util.function.Predicate;

public enum HullFeature {
    ANY(hull -> true),
    SHIELDS(HullFacts::shields),
    NO_SHIELDS(hull -> !hull.shields()),
    FRONT_SHIELD(HullFacts::frontShield),
    OMNI_SHIELD(HullFacts::omniShield),
    FIGHTER_BAYS(HullFacts::fighterBays),
    MISSILE_SLOTS(HullFacts::missileSlots),
    PHASE(HullFacts::phase);

    private final Predicate<HullFacts> test;

    HullFeature(Predicate<HullFacts> test) {
        this.test = test;
    }

    public boolean fits(HullFacts hull) {
        return test.test(hull);
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    static HullFeature byId(String id) {
        for (HullFeature feature : values()) {
            if (feature.id().equals(id)) {
                return feature;
            }
        }
        return null;
    }
}
