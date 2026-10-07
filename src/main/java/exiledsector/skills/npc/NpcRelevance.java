package exiledsector.skills.npc;

import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.tags.ShipProfile;
import exiledsector.skills.tags.SkillTags;
import exiledsector.skills.tags.WeaponKind;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

final class NpcRelevance {

    static final float UNTHEMED = 0.5f;
    static final float FOCUS = 3f;
    static final float PERMANENT_HULLMOD_BONUS = 1.5f;
    private static final Map<String, Float> BASE = Map.ofEntries(
            Map.entry("flux", 1f), Map.entry("hull", 0.8f), Map.entry("armour", 0.8f), Map.entry("range", 1f),
            Map.entry("weapons", 1f), Map.entry("speed", 0.8f), Map.entry("combat_readiness", 0.8f),
            Map.entry("repair", 0.6f), Map.entry("ammo", 0.6f), Map.entry("point_defense", 0.6f),
            Map.entry("dmgtypekinetic", 0.5f), Map.entry("dmgtypehighexplosive", 0.5f),
            Map.entry("dmgtypefragmentation", 0.5f), Map.entry("dmgtypeenergy", 0.5f),
            Map.entry("ballistic", 0.1f), Map.entry("energy", 0.1f), Map.entry("beam", 0.1f), Map.entry("missile", 0.1f),
            Map.entry("fighter", 0.1f), Map.entry("phase", 0.1f), Map.entry("shield", 0.1f),
            Map.entry("logistics", 0.15f), Map.entry("fleet_support", 0.2f), Map.entry("d_mods", 0.2f));
    private static final Map<HullSize, Float> HEAVY_ARMOUR = Map.of(
            HullSize.FRIGATE, 250f, HullSize.DESTROYER, 600f, HullSize.CRUISER, 1000f, HullSize.CAPITAL_SHIP, 1400f);

    private final Map<String, Float> weightsByTag;

    private NpcRelevance(Map<String, Float> weightsByTag) {
        this.weightsByTag = weightsByTag;
    }

    static NpcRelevance of(ShipProfile profile, Collection<String> permanentHullMods) {
        Map<String, Float> weightsByTag = new HashMap<>(BASE);
        Set<WeaponKind> weaponKinds = profile.weaponKinds();
        focus(weightsByTag, "ballistic", weaponKinds.contains(WeaponKind.BALLISTIC));
        focus(weightsByTag, "energy", weaponKinds.contains(WeaponKind.ENERGY));
        focus(weightsByTag, "beam", weaponKinds.contains(WeaponKind.BEAM));
        focus(weightsByTag, "missile", weaponKinds.contains(WeaponKind.MISSILE));
        focus(weightsByTag, "fighter", profile.fighterBays() > 0);
        focus(weightsByTag, "phase", profile.phaseHull() || profile.shieldType() == ShieldType.PHASE);
        boolean shielded = profile.shieldType() == ShieldType.FRONT || profile.shieldType() == ShieldType.OMNI;
        focus(weightsByTag, "shield", shielded);
        if (shielded) {
            weightsByTag.merge("flux", 1f, Float::sum);
        }
        if (profile.baseArmor() >= HEAVY_ARMOUR.getOrDefault(profile.hullSize(), Float.MAX_VALUE)) {
            focus(weightsByTag, "armour", true);
            weightsByTag.merge("hull", 0.7f, Float::sum);
        }
        for (SkillType skillType : SkillTree.getAllTypes().values()) {
            if (ownsAny(skillType, permanentHullMods)) {
                for (String tag : skillType.getTags()) {
                    if (SkillTags.THEME.contains(tag)) {
                        weightsByTag.merge(tag, PERMANENT_HULLMOD_BONUS, Float::sum);
                    }
                }
            }
        }
        return new NpcRelevance(Map.copyOf(weightsByTag));
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
