package exiledsector.skills;

import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.skilleffect.ShieldSkillEffect;
import exiledsector.skills.tags.NodeRequirements;
import exiledsector.skills.tags.ShipProfile;

import java.util.List;
import java.util.function.Predicate;

public final class NodeEligibility {

    public enum Kind {
        LOCKED, INVALID_OPTION, WRONG_HULL_SIZE, UNMET_HULL_REQUIREMENT, UNMET_SHIP_REQUIREMENT, HULL_MOD_CONFLICT, TYPE_CONFLICT,
        EFFECT_BLOCK
    }

    public enum OptionProblem {
        UNEXPECTED, MISSING, INVALID
    }

    public record Block(Kind kind, String detail, SkillType conflictingType) {
    }

    public record Context(List<AllocatedNode> allocated, ShieldType shieldType, ShipFacts ship, Predicate<SkillType> locked,
                          ShipProfile profile) {

        public static Context of(ShipSkillData data, ShipFacts ship, Predicate<SkillType> locked) {
            return new Context(AllocatedNode.of(data), currentShieldType(data, ship.hullSize(), ship.hullShieldType()), ship, locked, null);
        }
    }

    private NodeEligibility() {
    }

    public static Block check(SkillNode node, SkillType option, Context context) {
        AllocatedNode candidate = AllocatedNode.planned(node, option);
        if (context.locked() != null && context.locked().test(candidate.effectiveType())) {
            return new Block(Kind.LOCKED, null, null);
        }
        OptionProblem optionProblem = option == null ? null : optionProblem(node.getType(), option.getId());
        if (optionProblem != null) {
            return new Block(Kind.INVALID_OPTION, optionProblem.name(), null);
        }
        ShipFacts ship = context.ship();
        HullSize hullSize = ship.hullSize();
        if (!node.getType().allowsHullSize(hullSize) || !candidate.effectiveType().allowsHullSize(hullSize)) {
            return new Block(Kind.WRONG_HULL_SIZE, null, null);
        }
        String unmetRequirement = NodeRequirements.firstUnmetHullRequirement(node.effectiveTags(option), ship);
        if (unmetRequirement != null) {
            return new Block(Kind.UNMET_HULL_REQUIREMENT, unmetRequirement, null);
        }
        if (context.profile() != null) {
            String unmetShipRequirement = NodeRequirements.firstUnmet(node.effectiveTags(option), context.profile());
            if (unmetShipRequirement != null) {
                return new Block(Kind.UNMET_SHIP_REQUIREMENT, unmetShipRequirement, null);
            }
        }
        for (String hullModId : candidate.exclusiveHullModIds()) {
            if (ship.hasHullMod().test(hullModId)) {
                return new Block(Kind.HULL_MOD_CONFLICT, hullModId, null);
            }
        }
        for (AllocatedNode existing : context.allocated()) {
            if (existing.isExclusiveWith(candidate)) {
                return new Block(Kind.TYPE_CONFLICT, existing.effectiveType().getId(), existing.effectiveType());
            }
        }
        for (SkillTypeEffect effect : candidate.effectiveType().effectsFor(hullSize)) {
            String reason = effect.effect().blockAllocationReason(ship, context.shieldType());
            if (reason != null) {
                return new Block(Kind.EFFECT_BLOCK, reason, null);
            }
        }
        return null;
    }

    public static OptionProblem optionProblem(SkillType type, String optionTypeId) {
        if (!type.isOptional()) {
            return optionTypeId == null ? null : OptionProblem.UNEXPECTED;
        }
        if (optionTypeId == null) {
            return OptionProblem.MISSING;
        }
        if (!type.getOptionalOptionIds().contains(optionTypeId) || SkillTree.getType(optionTypeId) == null) {
            return OptionProblem.INVALID;
        }
        return null;
    }

    public static SkillType validOption(SkillType type, String optionTypeId) {
        return optionTypeId != null && optionProblem(type, optionTypeId) == null ? SkillTree.getType(optionTypeId) : null;
    }

    public static ShieldType currentShieldType(ShipSkillData data, HullSize hullSize, ShieldType hullShieldType) {
        return ShieldSkillEffect.resolveDisplayShieldType(hullShieldType, AllocatedSkillEffects.forData(data, hullSize));
    }
}
