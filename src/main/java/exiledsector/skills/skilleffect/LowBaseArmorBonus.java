package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import exiledsector.i18n.NumberText;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

final class LowBaseArmorBonus {

    private record Scale(float bonusWithNoArmor, float bonusAtCutoff, float cutoff) {
    }

    private static final Map<HullSize, Scale> SCALES = new EnumMap<>(HullSize.class);

    static {
        SCALES.put(HullSize.FRIGATE, new Scale(400f, 150f, 400f));
        SCALES.put(HullSize.DESTROYER, new Scale(700f, 300f, 700f));
        SCALES.put(HullSize.CRUISER, new Scale(900f, 400f, 900f));
        SCALES.put(HullSize.CAPITAL_SHIP, new Scale(1200f, 500f, 1200f));
    }

    private LowBaseArmorBonus() {
    }

    static float bonus(HullSize hullSize, float baseArmor) {
        Scale scale = SCALES.get(hullSize);
        if (scale == null || baseArmor > scale.cutoff()) {
            return 0f;
        }
        float progress = Math.max(0f, baseArmor) / scale.cutoff();
        return scale.bonusWithNoArmor() + (scale.bonusAtCutoff() - scale.bonusWithNoArmor()) * progress;
    }

    static boolean fits(ShipHullSpecAPI hull) {
        Scale scale = SCALES.get(hull.getHullSize());
        return scale != null && !hull.isPhase() && hull.getArmorRating() <= scale.cutoff();
    }

    static String mostByHullSize(float magnitude) {
        return byHullSize(scale -> scale.bonusWithNoArmor() * magnitude);
    }

    static String leastByHullSize(float magnitude) {
        return byHullSize(scale -> scale.bonusAtCutoff() * magnitude);
    }

    static String cutoffByHullSize() {
        return byHullSize(Scale::cutoff);
    }

    private static String byHullSize(ToDoubleFunction<Scale> value) {
        List<String> values = new ArrayList<>();
        for (Scale scale : SCALES.values()) {
            values.add(NumberText.format((float) value.applyAsDouble(scale)));
        }
        return String.join("/", values);
    }
}
