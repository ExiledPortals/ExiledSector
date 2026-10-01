package exiledsector.skills;

import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.skilleffect.ShieldSkillEffect;

import java.util.List;

public final class NodeEligibility {

    public enum Kind {
        WRONG_HULL_SIZE, HULL_MOD_CONFLICT, TYPE_CONFLICT, EFFECT_BLOCK
    }

    public record Block(Kind kind, String detail, SkillType conflictingType) {
    }

    private NodeEligibility() {
    }

    public static Block check(SkillNode node, SkillType option, ShipSkillData data, ShipFacts ship) {
        return check(node, option, AllocatedNode.of(data), currentShieldType(data, ship.hullSize(), ship.hullShieldType()), ship);
    }

    public static Block check(SkillNode node, SkillType option, List<AllocatedNode> allocated, ShieldType currentShieldType,
                              ShipFacts ship) {
        AllocatedNode candidate = AllocatedNode.planned(node, option);
        HullSize hullSize = ship.hullSize();
        if (!node.getType().allowsHullSize(hullSize) || !candidate.effectiveType().allowsHullSize(hullSize)) {
            return new Block(Kind.WRONG_HULL_SIZE, null, null);
        }
        for (String hullModId : candidate.exclusiveHullModIds()) {
            if (ship.hasHullMod().test(hullModId)) {
                return new Block(Kind.HULL_MOD_CONFLICT, hullModId, null);
            }
        }
        for (AllocatedNode existing : allocated) {
            if (existing.isExclusiveWith(candidate)) {
                return new Block(Kind.TYPE_CONFLICT, existing.effectiveType().getId(), existing.effectiveType());
            }
        }
        for (SkillTypeEffect effect : candidate.effectiveType().effectsFor(hullSize)) {
            String reason = effect.effect().blockAllocationReason(ship, currentShieldType);
            if (reason != null) {
                return new Block(Kind.EFFECT_BLOCK, reason, null);
            }
        }
        return null;
    }

    public static ShieldType currentShieldType(ShipSkillData data, HullSize hullSize, ShieldType hullShieldType) {
        return ShieldSkillEffect.resolveDisplayShieldType(hullShieldType, AllocatedSkillEffects.forData(data, hullSize));
    }
}
