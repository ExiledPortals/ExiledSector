package exiledlib.skills.skilleffect;

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
import java.util.function.Supplier;

final class SkillEffectSupport {

    static final int MAX_COUNTED_DMODS = 5;
    static final String NON_LOGISTICS_DMOD_EFFECT_MULT = "exiledSector_nonLogisticsDModEffectMult";
    static final String LOGISTICS_DMOD_EFFECT_MULT = "exiledSector_logisticsDModEffectMult";
    static final String LOGISTICS_COPY_PREFIX = "exiledSector_logisticsDModCopy_";
    private static final String COPY_SCALE_ID = "exiledSector_logisticsDModCopyScale";
    private static final float MIN_EFFECT_MULT = 0.0001f;

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

    static <T> T ensureListener(ShipAPI ship, Class<T> listenerType, Function<ShipAPI, ? extends T> listenerFactory) {
        List<T> existing = ship.getListeners(listenerType);
        if (existing != null && !existing.isEmpty()) {
            return existing.get(0);
        }
        T created = listenerFactory.apply(ship);
        ship.addListener(created);
        return created;
    }

    static <T> T existingEngineListener(Class<T> listenerType) {
        List<T> existing = Global.getCombatEngine().getListenerManager().getListeners(listenerType);
        return existing.isEmpty() ? null : existing.get(0);
    }

    static <T> T ensureEngineListener(Class<T> listenerType, Supplier<? extends T> listenerFactory) {
        T existing = existingEngineListener(listenerType);
        if (existing != null) {
            return existing;
        }
        T created = listenerFactory.get();
        Global.getCombatEngine().getListenerManager().addListener(created);
        return created;
    }

    static float compoundMultPerDMod(MutableShipStatsAPI stats, float magnitudePerDMod) {
        return (float) Math.pow(multFrom(magnitudePerDMod), Math.min(countDMods(stats), MAX_COUNTED_DMODS));
    }

    private static int countDMods(MutableShipStatsAPI stats) {
        return dModSpecs(stats.getVariant()).size();
    }

    static void applyDModEffectMult(MutableShipStatsAPI stats, String modId, float magnitude) {
        applyMult(stats.getDynamic().getStat(Stats.DMOD_EFFECT_MULT), modId, magnitude);
        reapplyDMods(stats);
        refreshLogisticsDModCopies(stats);
    }

    static void applyNonLogisticsDModEffectMult(MutableShipStatsAPI stats, String modId, float magnitude) {
        applyMult(stats.getDynamic().getStat(Stats.DMOD_EFFECT_MULT), modId, magnitude);
        applyMult(stats.getDynamic().getStat(NON_LOGISTICS_DMOD_EFFECT_MULT), modId, magnitude);
        reapplyDMods(stats);
        refreshLogisticsDModCopies(stats);
    }

    static void applyLogisticsDModEffectMult(MutableShipStatsAPI stats, String modId, float magnitude) {
        applyMult(stats.getDynamic().getStat(LOGISTICS_DMOD_EFFECT_MULT), modId, magnitude);
        refreshLogisticsDModCopies(stats);
    }

    private static void reapplyDMods(MutableShipStatsAPI stats) {
        ShipVariantAPI variant = stats.getVariant();
        for (HullModSpecAPI spec : dModSpecs(variant)) {
            if (spec.getEffect() != null) {
                spec.getEffect().applyEffectsBeforeShipCreation(variant.getHullSize(), stats, spec.getId());
            }
        }
    }

    private static void refreshLogisticsDModCopies(MutableShipStatsAPI stats) {
        float nonLogisticsMult = stats.getDynamic().getValue(NON_LOGISTICS_DMOD_EFFECT_MULT);
        float logisticsMult = stats.getDynamic().getValue(LOGISTICS_DMOD_EFFECT_MULT);
        MutableStat sharedEffect = stats.getDynamic().getStat(Stats.DMOD_EFFECT_MULT);
        float sharedValue = sharedEffect.getModifiedValue();
        if (nonLogisticsMult <= MIN_EFFECT_MULT || sharedValue <= MIN_EFFECT_MULT || Math.abs(logisticsMult - nonLogisticsMult) < MIN_EFFECT_MULT) {
            return;
        }
        float copyEffect = sharedValue / nonLogisticsMult * (logisticsMult - nonLogisticsMult);
        ShipVariantAPI variant = stats.getVariant();
        for (HullModSpecAPI spec : dModSpecs(variant)) {
            if (spec.getEffect() == null || !CsvIdList.LOGISTICS_DMODS.contains(spec.getId())) {
                continue;
            }
            String copyId = LOGISTICS_COPY_PREFIX + spec.getId();
            sharedEffect.modifyMult(COPY_SCALE_ID, copyEffect / sharedValue);
            spec.getEffect().applyEffectsBeforeShipCreation(variant.getHullSize(), stats, copyId);
            sharedEffect.unmodifyMult(COPY_SCALE_ID);
            stats.getSuppliesToRecover().unmodifyMult(copyId);
            stats.getSuppliesPerMonth().unmodifyMult(copyId);
            stats.getMaxBurnLevel().unmodifyFlat(copyId);
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
