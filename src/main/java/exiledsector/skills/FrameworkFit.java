package exiledsector.skills;

import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import exiledsector.skills.skilleffect.FighterSkillEffect;
import exiledsector.skills.skilleffect.ShieldSkillEffect;
import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.skills.tags.ShipProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class FrameworkFit {

    private FrameworkFit() {
    }

    public static ShipProfile profile(ShipHullSpecAPI hullSpec, ShipVariantAPI variant, ShipSkillData shipData) {
        HullSize hullSize = hullSpec.getHullSize();
        List<SkillTypeEffect> treeEffects = treeEffects(shipData, hullSize);
        List<SkillEffect> treeEffectsInOrder = new ArrayList<>(treeEffects.size());
        treeEffects.forEach(effect -> treeEffectsInOrder.add(effect.effect()));
        ShieldType shieldType = ShieldSkillEffect.resolveDisplayShieldType(fittedShieldType(hullSpec.getShieldType(), variant), treeEffectsInOrder);
        return new ShipProfile(hullSize, shieldType, fighterBays(hullSpec.getFighterBays(), variant, treeEffects), Set.of(), false,
                hullSpec.getArmorRating(), hullSpec.isPhase(), ShipSystemCharges.limited(hullSpec), ShipFacts.onlyBuiltInWings(hullSpec));
    }

    static ShieldType fittedShieldType(ShieldType hullShieldType, ShipVariantAPI variant) {
        if (variant == null || hullShieldType == ShieldType.PHASE) {
            return hullShieldType;
        }
        if (variant.hasHullMod(HullMods.SHIELD_SHUNT)) {
            return ShieldType.NONE;
        }
        if (hullShieldType == ShieldType.NONE && variant.hasHullMod(HullMods.MAKESHIFT_GENERATOR)) {
            return ShieldType.FRONT;
        }
        return hullShieldType;
    }

    static int fighterBays(int hullBays, ShipVariantAPI variant, List<SkillTypeEffect> treeEffects) {
        boolean baysRemoved = variant != null && variant.hasHullMod(HullMods.CONVERTED_BAY);
        float addedBays = variant != null && variant.hasHullMod(HullMods.CONVERTED_HANGAR) ? 1f : 0f;
        for (SkillTypeEffect effect : treeEffects) {
            SkillEffect skillEffect = effect.effect();
            if (skillEffect == FighterSkillEffect.REMOVE_ALL_FIGHTER_BAYS) {
                baysRemoved = true;
            } else if (skillEffect == FighterSkillEffect.FIGHTER_BAYS_FLAT || skillEffect == FighterSkillEffect.CONVERTED_HANGAR_FIGHTER_BAYS_FLAT) {
                addedBays += effect.magnitude();
            }
        }
        return Math.max(0, Math.round((baysRemoved ? 0f : hullBays) + addedBays));
    }

    private static List<SkillTypeEffect> treeEffects(ShipSkillData shipData, HullSize hullSize) {
        List<SkillTypeEffect> effects = new ArrayList<>();
        if (shipData == null) {
            return effects;
        }
        for (AllocatedNode allocated : AllocatedNode.of(shipData)) {
            if (allocated.effectiveType().getVanillaHullModId() == null) {
                effects.addAll(AllocatedSkillEffects.appliedEffects(shipData, allocated, hullSize));
            }
        }
        return effects;
    }
}
