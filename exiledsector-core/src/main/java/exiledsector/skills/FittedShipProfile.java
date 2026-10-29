package exiledsector.skills;

import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.skills.skilleffect.fighter.FighterSkillEffect;
import exiledsector.skills.skilleffect.shield.ShieldSkillEffect;
import exiledsector.ExiledSector;
import exiledsector.skills.tags.ShipProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class FittedShipProfile {

    private FittedShipProfile() {
    }

    public static ShipProfile profile(ShipHullSpecAPI hullSpec, ShipVariantAPI variant, ShipSkillData shipData) {
        HullSize hullSize = hullSpec.getHullSize();
        List<SkillTypeEffect> fitEffects = shipData == null ? List.of() : fitEffects(shipData, hullSize);
        List<SkillEffect> fitEffectsInOrder = new ArrayList<>(fitEffects.size());
        fitEffects.forEach(effect -> fitEffectsInOrder.add(effect.effect()));
        ShieldType shieldType = ShieldSkillEffect.resolveDisplayShieldType(fittedShieldType(hullSpec.getShieldType(), variant), fitEffectsInOrder);
        return new ShipProfile(hullSize, shieldType, fighterBays(hullSpec.getFighterBays(), variant, fitEffects), Set.of(), false,
                hullSpec.getArmorRating(), hullSpec.isPhase(), ShipSystemCharges.limited(hullSpec), ShipFacts.onlyBuiltInWings(hullSpec), false,
                ShipProfile.automated(variant));
    }

    private static List<SkillTypeEffect> fitEffects(ShipSkillData shipData, HullSize hullSize) {
        List<SkillTypeEffect> fitEffects = new ArrayList<>();
        ExiledSector.enabledDomains().forEach(domain -> fitEffects.addAll(domain.fitEffects(shipData, hullSize)));
        return fitEffects;
    }

    public static ShieldType fittedShieldType(ShieldType hullShieldType, ShipVariantAPI variant) {
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

    static int fighterBays(int hullBays, ShipVariantAPI variant, List<SkillTypeEffect> fitEffects) {
        boolean baysRemoved = variant != null && variant.hasHullMod(HullMods.CONVERTED_BAY);
        float addedBays = variant != null && variant.hasHullMod(HullMods.CONVERTED_HANGAR) ? 1f : 0f;
        for (SkillTypeEffect effect : fitEffects) {
            SkillEffect skillEffect = effect.effect();
            if (skillEffect == FighterSkillEffect.REMOVE_ALL_FIGHTER_BAYS) {
                baysRemoved = true;
            } else if (skillEffect == FighterSkillEffect.FIGHTER_BAYS_FLAT || skillEffect == FighterSkillEffect.CONVERTED_HANGAR_FIGHTER_BAYS_FLAT) {
                addedBays += effect.magnitude();
            }
        }
        return Math.max(0, Math.round((baysRemoved ? 0f : hullBays) + addedBays));
    }
}
