package exiledsector.skills;

import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.skilleffect.ShieldSkillEffect;
import exiledsector.skills.tags.NodeRequirements;
import exiledsector.skills.tags.ShipProfile;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

public final class NodeEligibility {

    public enum Kind {
        LOCKED, INVALID_OPTION, WRONG_HULL_SIZE, UNMET_HULL_REQUIREMENT, UNMET_SHIP_REQUIREMENT, HULL_MOD_CONFLICT, TYPE_CONFLICT,
        EFFECT_BLOCK, ITEM_COST
    }

    public enum OptionProblem {
        UNEXPECTED, MISSING, INVALID
    }

    public record Block(Kind kind, String detail, SkillType conflictingType, SkillItemCost itemCost) {

        public Block(Kind kind, String detail, SkillType conflictingType) {
            this(kind, detail, conflictingType, null);
        }
    }

    public record Context(List<AllocatedNode> allocated, ShieldType shieldType, ShipFacts shipFacts, Predicate<SkillType> locked,
                          ShipProfile profile, ToDoubleFunction<String> heldItems) {

        public Context(List<AllocatedNode> allocated, ShieldType shieldType, ShipFacts shipFacts, Predicate<SkillType> locked,
                       ShipProfile profile) {
            this(allocated, shieldType, shipFacts, locked, profile, null);
        }

        public static Context of(ShipSkillData data, ShipFacts shipFacts, Predicate<SkillType> locked) {
            return of(data, shipFacts, locked, null);
        }

        public static Context of(ShipSkillData data, ShipFacts shipFacts, Predicate<SkillType> locked, ToDoubleFunction<String> heldItems) {
            return new Context(AllocatedNode.of(data), currentShieldType(data, shipFacts.hullSize(), shipFacts.hullShieldType()), shipFacts, locked,
                    null, heldItems);
        }

        public Context excluding(String nodeId, ShipSkillData data) {
            List<AllocatedNode> remaining = allocated.stream().filter(existing -> !existing.node().getId().equals(nodeId)).toList();
            ShieldType remainingShieldType = ShieldSkillEffect.resolveDisplayShieldType(shipFacts.hullShieldType(),
                    AllocatedSkillEffects.forNodes(data, remaining, shipFacts.hullSize()));
            SkillItemCost refund = data.itemCharge(nodeId);
            ToDoubleFunction<String> heldAfterRefund = refund == null || heldItems == null ? heldItems
                    : itemId -> heldItems.applyAsDouble(itemId) + (itemId.equals(refund.itemId()) ? refund.quantity() : 0f);
            return new Context(remaining, remainingShieldType, shipFacts, locked, profile, heldAfterRefund);
        }
    }

    private NodeEligibility() {
    }

    public static Block check(SkillNode node, SkillType option, Context context) {
        AllocatedNode candidate = AllocatedNode.planned(node, option);
        if (context.locked() != null && (context.locked().test(node.getType()) || context.locked().test(candidate.effectiveType()))) {
            return new Block(Kind.LOCKED, null, null);
        }
        OptionProblem optionProblem = option == null ? null : optionProblem(node.getType(), option.getId());
        if (optionProblem != null) {
            return new Block(Kind.INVALID_OPTION, optionProblem.name(), null);
        }
        ShipFacts shipFacts = context.shipFacts();
        HullSize hullSize = shipFacts.hullSize();
        if (!node.getType().allowsHullSize(hullSize) || !candidate.effectiveType().allowsHullSize(hullSize)) {
            return new Block(Kind.WRONG_HULL_SIZE, null, null);
        }
        String unmetRequirement = NodeRequirements.firstUnmetHullRequirement(node.effectiveTags(option), shipFacts);
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
            if (shipFacts.hasHullMod().test(hullModId)) {
                return new Block(Kind.HULL_MOD_CONFLICT, hullModId, null);
            }
        }
        for (AllocatedNode existing : context.allocated()) {
            if (existing.isExclusiveWith(candidate)) {
                return new Block(Kind.TYPE_CONFLICT, existing.effectiveType().getId(), existing.effectiveType());
            }
        }
        for (SkillTypeEffect effect : candidate.effectiveType().effectsFor(hullSize)) {
            String reason = effect.effect().blockAllocationReason(shipFacts, context.shieldType());
            if (reason != null) {
                return new Block(Kind.EFFECT_BLOCK, reason, null);
            }
        }
        return itemCostBlock(itemCost(node, option), context.heldItems());
    }

    public static SkillItemCost itemCost(SkillNode node, SkillType option) {
        SkillItemCost optionCost = option == null ? null : option.getItemCost();
        return optionCost != null ? optionCost : node.getType().getItemCost();
    }

    private static Block itemCostBlock(SkillItemCost itemCost, ToDoubleFunction<String> heldItems) {
        if (itemCost == null || heldItems != null && heldItems.applyAsDouble(itemCost.itemId()) >= itemCost.quantity()) {
            return null;
        }
        return new Block(Kind.ITEM_COST, itemCost.itemId(), null, itemCost);
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
