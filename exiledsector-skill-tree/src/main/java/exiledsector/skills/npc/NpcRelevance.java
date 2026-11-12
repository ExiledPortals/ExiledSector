package exiledsector.skills.npc;

import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.tags.ShipProfile;
import exiledsector.skills.tags.TagWeights;
import exiledsector.skills.tags.ThemeTags;
import exiledsector.skills.tags.WeaponKind;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class NpcRelevance {

    static final float UNTHEMED = 0.5f;
    static final float FOCUS = 3f;
    static final float PERMANENT_HULLMOD_BONUS = 1.5f;
    private static final Map<String, Float> BASE = Map.ofEntries(
            Map.entry("flux", 1f), Map.entry("hull", 0.8f), Map.entry("armour", 0.8f), Map.entry("range", 1f),
            Map.entry("weapon", 1f), Map.entry("mobility", 0.8f), Map.entry("combat_readiness", 0.8f),
            Map.entry("repair", 0.6f), Map.entry("ammo", 0.6f), Map.entry("point_defense", 0.6f),
            Map.entry("kinetic", 0.5f), Map.entry("high_explosive", 0.5f),
            Map.entry("fragmentation", 0.5f), Map.entry("energy_damage", 0.5f),
            Map.entry("ballistic", 0.1f), Map.entry("energy", 0.1f), Map.entry("non_beam_energy", 0.1f), Map.entry("beam", 0.1f),
            Map.entry("missile", 0.1f), Map.entry("fighter", 0.1f), Map.entry("phase", 0.1f), Map.entry("shield", 0.1f),
            Map.entry("front_shield", 0.1f), Map.entry("omni_shield", 0.1f), Map.entry("shieldless", 0.1f),
            Map.entry("logistics", 0.15f), Map.entry("fleet_support", 0.2f), Map.entry("d_mods", 0.2f));
    static final float LIGHT_ARMOUR_WEIGHT = 0.3f;
    private static final Map<HullSize, Float> HEAVY_ARMOUR = Map.of(
            HullSize.FRIGATE, 250f, HullSize.DESTROYER, 600f, HullSize.CRUISER, 1000f, HullSize.CAPITAL_SHIP, 1400f);
    private static final Map<HullSize, Float> LIGHT_ARMOUR = Map.of(
            HullSize.FRIGATE, 150f, HullSize.DESTROYER, 450f, HullSize.CRUISER, 800f, HullSize.CAPITAL_SHIP, 1100f);

    private final Map<String, Float> weightsByTag;
    private final TagWeights focusWeights;

    private NpcRelevance(Map<String, Float> weightsByTag, TagWeights focusWeights) {
        this.weightsByTag = weightsByTag;
        this.focusWeights = focusWeights;
    }

    static NpcRelevance of(ShipProfile profile, Collection<String> permanentHullMods) {
        return of(profile, permanentHullMods, List.of());
    }

    static NpcRelevance of(ShipProfile profile, Collection<String> permanentHullMods, List<Map<String, Float>> focusSources) {
        Map<String, Float> weightsByTag = new HashMap<>(BASE);
        Set<WeaponKind> weaponKinds = profile.weaponKinds();
        focus(weightsByTag, "ballistic", weaponKinds.contains(WeaponKind.BALLISTIC));
        focus(weightsByTag, "energy", weaponKinds.contains(WeaponKind.ENERGY));
        focus(weightsByTag, "non_beam_energy", weaponKinds.contains(WeaponKind.NON_BEAM_ENERGY));
        focus(weightsByTag, "beam", weaponKinds.contains(WeaponKind.BEAM));
        focus(weightsByTag, "missile", weaponKinds.contains(WeaponKind.MISSILE));
        focus(weightsByTag, "fighter", profile.fighterBays() > 0);
        focus(weightsByTag, "phase", profile.phaseHull() || profile.shieldType() == ShieldType.PHASE);
        boolean shielded = profile.shieldType() == ShieldType.FRONT || profile.shieldType() == ShieldType.OMNI;
        focus(weightsByTag, "shield", shielded);
        focus(weightsByTag, "front_shield", profile.shieldType() == ShieldType.FRONT);
        focus(weightsByTag, "omni_shield", profile.shieldType() == ShieldType.OMNI);
        focus(weightsByTag, "shieldless", !shielded);
        if (shielded) {
            weightsByTag.merge("flux", 1f, Float::sum);
        }
        if (profile.baseArmor() >= HEAVY_ARMOUR.getOrDefault(profile.hullSize(), Float.MAX_VALUE)) {
            focus(weightsByTag, "armour", true);
            weightsByTag.merge("hull", 0.7f, Float::sum);
        } else if (profile.baseArmor() > 0f && profile.baseArmor() <= LIGHT_ARMOUR.getOrDefault(profile.hullSize(), 0f)) {
            weightsByTag.put("armour", LIGHT_ARMOUR_WEIGHT);
        }
        for (SkillType skillType : SkillTree.getAllTypes().values()) {
            if (ownsAny(skillType, permanentHullMods)) {
                for (String tag : skillType.getTags()) {
                    if (ThemeTags.isThemeTag(tag)) {
                        weightsByTag.merge(tag, PERMANENT_HULLMOD_BONUS, Float::sum);
                    }
                }
            }
        }
        return new NpcRelevance(Map.copyOf(weightsByTag), TagWeights.of(focusSources));
    }

    private static void focus(Map<String, Float> weightsByTag, String tag, boolean focused) {
        if (focused) {
            weightsByTag.put(tag, FOCUS);
        }
    }

    private static boolean ownsAny(SkillType skillType, Collection<String> hullModIds) {
        for (String hullModId : skillType.getOwnHullModIds()) {
            if (hullModIds.contains(hullModId)) {
                return true;
            }
        }
        return false;
    }

    float of(Collection<String> tags) {
        float shipWeight = shipWeight(tags);
        return focusWeights.hasSources() ? shipWeight * focusWeights.multiplier(ThemeTags.allWithAncestors(tags)) : shipWeight;
    }

    private float shipWeight(Collection<String> tags) {
        float totalWeight = 0f;
        boolean themed = false;
        for (String tag : tags) {
            Float weight = weightsByTag.get(tag);
            if (weight != null) {
                totalWeight += weight;
                themed = true;
            }
        }
        return themed ? totalWeight : UNTHEMED;
    }
}
