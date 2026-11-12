package exiledsector.charts.officer.scar;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.HullModSpecAPI;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

public final class ScarDMods {

    public static final int MAX_DMODS = 5;
    static final String DAMAGE = "damage";
    static final String DESTROYED_DAMAGE_ALWAYS = "destroyedDamageAlways";
    static final String FIGHTER_BAY_DAMAGE = "fighterBayDamage";
    static final String PHASE_DAMAGE = "phaseDamage";
    static final String CARRIER_ALWAYS = "carrierAlways";
    static final String DAMAGE_STRUCT = "damageStruct";
    static final String PEAK_TIME = "peak_time";
    static final String NOT_PHASE = "notPhase";
    static final String NOT_AUTO = "notAuto";
    static final String NOT_CIV = "notCiv";
    static final String CIV = "civ";
    static final String CIV_ONLY = "civOnly";
    static final String REQ_SHIELDS = "reqShields";

    private ScarDMods() {
    }

    public static String pick(ScarRow row, List<String> currentDMods, HullFacts hull, Random random) {
        List<String> pool = new ArrayList<>();
        for (HullModSpecAPI spec : Global.getSettings().getAllHullModSpecs()) {
            if (spec.hasTag(Tags.HULLMOD_DMOD)) {
                pool.add(spec.getId());
            }
        }
        return pick(row, currentDMods, hull, ScarDMods::vanillaTags, pool, random);
    }

    private static Set<String> vanillaTags(String hullModId) {
        HullModSpecAPI spec = Global.getSettings().getHullModSpec(hullModId);
        return spec == null ? null : spec.getTags();
    }

    static String pick(ScarRow row, List<String> currentDMods, HullFacts hull, Function<String, Set<String>> tagsOf,
                       List<String> pool, Random random) {
        List<String> held = currentDMods == null ? List.of() : currentDMods;
        if (held.size() >= MAX_DMODS || hull == null) {
            return null;
        }
        if (row != null) {
            for (String dModId : row.dModIds()) {
                if (!held.contains(dModId) && suits(tagsOf.apply(dModId), hull, held, tagsOf)) {
                    return dModId;
                }
            }
        }
        List<String> candidates = new ArrayList<>();
        for (String dModId : pool) {
            Set<String> tags = tagsOf.apply(dModId);
            if (!held.contains(dModId) && inRandomPool(tags, hull) && suits(tags, hull, held, tagsOf)) {
                candidates.add(dModId);
            }
        }
        return candidates.isEmpty() ? null : candidates.get(random.nextInt(candidates.size()));
    }

    static boolean inRandomPool(Set<String> tags, HullFacts hull) {
        return tags != null && (tags.contains(DAMAGE) || tags.contains(DESTROYED_DAMAGE_ALWAYS)
                || hull.fighterBays() && tags.contains(FIGHTER_BAY_DAMAGE)
                || hull.phase() && tags.contains(PHASE_DAMAGE)
                || hull.carrier() && tags.contains(CARRIER_ALWAYS));
    }

    static boolean suits(Set<String> tags, HullFacts hull, Collection<String> held, Function<String, Set<String>> tagsOf) {
        if (tags == null || !tags.contains(Tags.HULLMOD_DMOD)) {
            return false;
        }
        return !(hull.peakTimeless() && tags.contains(PEAK_TIME)
                || hull.phase() && tags.contains(NOT_PHASE)
                || hull.automated() && tags.contains(NOT_AUTO)
                || hull.civilian() && (tags.contains(NOT_CIV) || !tags.contains(CIV) && !tags.contains(CIV_ONLY))
                || !hull.civilian() && tags.contains(CIV_ONLY)
                || !hull.shields() && tags.contains(REQ_SHIELDS)
                || !hull.fighterBays() && tags.contains(FIGHTER_BAY_DAMAGE)
                || !hull.phase() && tags.contains(PHASE_DAMAGE)
                || tags.contains(DAMAGE_STRUCT) && holdsStructural(held, tagsOf));
    }

    private static boolean holdsStructural(Collection<String> held, Function<String, Set<String>> tagsOf) {
        for (String dModId : held) {
            Set<String> tags = tagsOf.apply(dModId);
            if (tags != null && tags.contains(DAMAGE_STRUCT)) {
                return true;
            }
        }
        return false;
    }
}
