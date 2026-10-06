package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.HullModSpecAPI;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

final class SkillEffectSupport {

    static final int MAX_COUNTED_DMODS = 5;

    private SkillEffectSupport() {
    }

    static void applyMult(MutableStat stat, String modId, float magnitude) {
        stat.modifyMult(modId, multFrom(magnitude));
    }

    static void applyMult(StatBonus stat, String modId, float magnitude) {
        stat.modifyMult(modId, multFrom(magnitude));
    }

    static float multFrom(float magnitude) {
        return 1f + magnitude / 100f;
    }

    static void ensureListener(ShipAPI ship, Class<?> listenerType, Function<ShipAPI, ?> listenerFactory) {
        if (!ship.hasListenerOfClass(listenerType)) {
            ship.addListener(listenerFactory.apply(ship));
        }
    }

    static float compoundMultPerDMod(MutableShipStatsAPI stats, float magnitudePerDMod) {
        return (float) Math.pow(multFrom(magnitudePerDMod), Math.min(countDMods(stats), MAX_COUNTED_DMODS));
    }

    private static int countDMods(MutableShipStatsAPI stats) {
        return dModSpecs(stats.getVariant()).size();
    }

    static void applyDModEffectMult(MutableShipStatsAPI stats, String modId, float magnitude) {
        applyMult(stats.getDynamic().getStat(Stats.DMOD_EFFECT_MULT), modId, magnitude);
        ShipVariantAPI variant = stats.getVariant();
        for (HullModSpecAPI spec : dModSpecs(variant)) {
            if (spec.getEffect() != null) {
                spec.getEffect().applyEffectsBeforeShipCreation(variant.getHullSize(), stats, spec.getId());
            }
        }
    }

    private static List<HullModSpecAPI> dModSpecs(ShipVariantAPI variant) {
        List<HullModSpecAPI> specs = new ArrayList<>();
        if (variant == null) {
            return specs;
        }
        for (String hullModId : variant.getHullMods()) {
            HullModSpecAPI spec = Global.getSettings().getHullModSpec(hullModId);
            if (spec != null && spec.hasTag(Tags.HULLMOD_DMOD)) {
                specs.add(spec);
            }
        }
        return specs;
    }
}
