package exiledsector.effects;

import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;
import exiledsector.items.MasterSchematic;
import exiledsector.skills.AllocatedNode;
import exiledsector.skills.AllocatedSkillEffects;
import exiledsector.skills.EffectContribution;
import exiledsector.skills.NodeEligibility;
import exiledsector.skills.ShipFacts;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillTreeSetting;
import exiledsector.skills.SkillType;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.skills.skilleffect.shield.ShieldSkillEffect;

import java.util.ArrayList;
import java.util.List;

public final class SkillTreeGrantedNodeEffects implements GrantedNodeEffects {

    public static final SkillTreeGrantedNodeEffects INSTANCE = new SkillTreeGrantedNodeEffects();

    private SkillTreeGrantedNodeEffects() {
    }

    @Override
    public List<EffectContribution> withoutTree(List<String> nodeIds, ShipEffectContext context, String modIdPrefix) {
        ShipVariantAPI variant = context.variant();
        if (variant == null || variant.getHullSpec() == null) {
            return List.of();
        }
        ShipSkillData shipData = context.shipData();
        boolean treeApplies = SkillTreeSetting.enabled() && shipData != null && MasterSchematic.treeOpen(shipData);
        ShipSkillData appliedTree = treeApplies ? shipData : new ShipSkillData();
        ShipFacts shipFacts = ShipFacts.of(variant.getHullSpec(), hullModId -> PlayerShipFacts.hasHullMod(variant, hullModId),
                hullModId -> PlayerShipFacts.hasFixedHullMod(variant, hullModId));
        List<AllocatedNode> allocated = new ArrayList<>(AllocatedNode.of(appliedTree));
        List<SkillEffect> appliedEffects = new ArrayList<>(AllocatedSkillEffects.forData(appliedTree, context.hullSize()));
        List<EffectContribution> contributions = new ArrayList<>();
        for (String nodeId : nodeIds) {
            SkillNode node = SkillTree.get(nodeId);
            if (node == null || appliedTree.isAllocated(nodeId) || allocated.stream().anyMatch(held -> held.node() == node)
                    || !ShipTrees.isFleetGrantable(node)) {
                continue;
            }
            ShieldType shieldType = ShieldSkillEffect.resolveDisplayShieldType(shipFacts.hullShieldType(), appliedEffects);
            NodeEligibility.Context eligibility = new NodeEligibility.Context(allocated, shieldType, shipFacts, null, null);
            if (NodeEligibility.check(node, null, eligibility) != null) {
                continue;
            }
            AllocatedNode granted = AllocatedNode.planned(node, null);
            EffectContribution contribution = contribution(granted, context, modIdPrefix + nodeId + "_");
            if (contribution != null) {
                allocated.add(granted);
                contribution.effects().forEach(effect -> appliedEffects.add(effect.effect()));
                contributions.add(contribution);
            }
        }
        return contributions;
    }

    @Override
    public boolean exists(String nodeId) {
        return SkillTree.get(nodeId) != null;
    }

    @Override
    public String displayName(String nodeId) {
        SkillNode node = SkillTree.get(nodeId);
        return node == null ? null : node.getDisplayName();
    }

    @Override
    public List<StyledText> descriptionLines(String nodeId) {
        SkillNode node = SkillTree.get(nodeId);
        if (node == null) {
            return List.of();
        }
        SkillType type = node.getType();
        List<StyledText> lines = new ArrayList<>();
        SkillNode.describeMechanics(type, null).all().forEach(line -> lines.add(line.display()));
        if (type.getFlavourText() != null) {
            lines.add(StyledText.styled(type.getFlavourText(), Style.MUTED));
        }
        return lines;
    }

    private static EffectContribution contribution(AllocatedNode granted, ShipEffectContext context, String modIdPrefix) {
        SkillType type = granted.effectiveType();
        GrantedNodeOrigin origin = new GrantedNodeOrigin(granted.node().getId());
        Float temporarySeconds = type.getTemporaryAfterDeploymentSeconds();
        if (AllocatedSkillEffects.isVanillaPassthrough(granted)) {
            return new EffectContribution(origin, modIdPrefix, List.of(), temporarySeconds, type.getVanillaHullModId(), false, false);
        }
        List<SkillTypeEffect> effects = type.effectsFor(context.hullSize()).stream().filter(effect -> effect.effect().appliesToNpcShips()).toList();
        return effects.isEmpty() ? null : new EffectContribution(origin, modIdPrefix, effects, temporarySeconds, null, false, false);
    }
}
