package exiledsector.effects;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.items.DoctrineLayer;
import exiledsector.items.DoctrineOverrides;
import exiledsector.items.EmptyShells;
import exiledsector.items.GatedSubroutines;
import exiledsector.items.MasterSchematic;
import exiledsector.items.RadiusLineOrigin;
import exiledsector.items.SubroutineOrigin;
import exiledsector.skills.AllocatedNode;
import exiledsector.skills.AllocatedSkillEffects;
import exiledsector.skills.EffectContribution;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillDataResolver;
import exiledsector.skills.SkillType;
import exiledsector.skills.SkillTypeEffect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SkillTreeEffectSource implements ShipEffectSource {

    public static final SkillTreeEffectSource INSTANCE = new SkillTreeEffectSource();
    static final String MOD_ID_PREFIX = "exiledSector_skill_";
    static final String SUBROUTINE_MOD_ID_PART = "subroutine_";
    static final String DOCTRINE_MOD_ID_PART = "doctrine_";
    static final Set<String> UNSCALED_NODE_TAGS = Set.of("logistics", "fleet_support", "campaign_only");

    private static final Object NODE_EFFECTS_KEY = new Object();
    private static final Object ALLOCATED_KEY = new Object();
    private static final Object PHANTOM_HULL_MOD_IDS_KEY = new Object();

    public record NodeEffects(AllocatedNode node, List<SkillTypeEffect> effects) {
    }

    private record CacheKey(DoctrineLayer doctrineLayer, boolean treeOpen) {
    }

    private SkillTreeEffectSource() {
    }

    @Override
    public float bonusScale(ShipEffectContext context) {
        return SkillDataResolver.isNpcTree(context.variant()) ? NpcBonusScale.forShip(context.member(), context.variant()) : 1f;
    }

    @Override
    public Object cacheKey(ShipEffectContext context) {
        return new CacheKey(DoctrineOverrides.layer(context.shipData()), MasterSchematic.treeOpen(context.shipData()));
    }

    @Override
    public List<EffectContribution> contributions(ShipEffectContext context) {
        if (!MasterSchematic.treeOpen(context.shipData())) {
            return List.of();
        }
        return contributions(context.shipData(), context.hullSize());
    }

    public static List<EffectContribution> contributions(ShipSkillData shipData, HullSize hullSize) {
        List<EffectContribution> contributions = new ArrayList<>();
        Map<String, GatedSubroutines.Gating> gatedNodeIds = GatedSubroutines.gatedNodeIds(shipData);
        Set<String> hollowNodeIds = EmptyShells.hollowNodeIds(shipData);
        String doctrineSocketId = DoctrineOverrides.layer(shipData).socketNodeId();
        for (AllocatedNode node : AllocatedNode.of(shipData)) {
            if (hollowNodeIds.contains(node.node().getId())) {
                continue;
            }
            SkillType type = node.effectiveType();
            boolean vanillaPassthrough = AllocatedSkillEffects.isVanillaPassthrough(node);
            boolean scaled = scalesNode(node);
            String modIdPrefix = MOD_ID_PREFIX + node.node().getId() + "_";
            AllocatedSkillEffects.NodeParts parts = AllocatedSkillEffects.nodeParts(shipData, node, hullSize);
            GatedSubroutines.Gating gating = gating(node, gatedNodeIds);
            if (gating != null) {
                if (!vanillaPassthrough) {
                    contributions.add(new EffectContribution(new RadiusLineOrigin(node, gating.socketNodeId()), modIdPrefix,
                            gatedEffects(parts.all(), gating.strength()), null, null, scaled, scaled, gating.gate()));
                }
                continue;
            }
            Float temporarySeconds = type.getTemporaryAfterDeploymentSeconds();
            boolean bonusScaled = scaled && !vanillaPassthrough;
            contributions.add(new EffectContribution(node, modIdPrefix, parts.own(), temporarySeconds,
                    vanillaPassthrough ? type.getVanillaHullModId() : null, scaled, bonusScaled));
            if (!parts.subroutine().isEmpty()) {
                contributions.add(new EffectContribution(new SubroutineOrigin(node), modIdPrefix + SUBROUTINE_MOD_ID_PART, parts.subroutine(),
                        temporarySeconds, null, scaled, bonusScaled));
            }
            if (!parts.doctrine().isEmpty()) {
                contributions.add(new EffectContribution(new RadiusLineOrigin(node, doctrineSocketId), modIdPrefix + DOCTRINE_MOD_ID_PART,
                        parts.doctrine(), temporarySeconds, null, scaled, bonusScaled));
            }
        }
        return contributions;
    }

    static List<AllocatedNode> placingNodes(ShipSkillData shipData) {
        Map<String, GatedSubroutines.Gating> gatedNodeIds = GatedSubroutines.gatedNodeIds(shipData);
        Set<String> hollowNodeIds = EmptyShells.hollowNodeIds(shipData);
        return AllocatedNode.of(shipData).stream()
                .filter(node -> !hollowNodeIds.contains(node.node().getId()) && gating(node, gatedNodeIds) == null).toList();
    }

    private static GatedSubroutines.Gating gating(AllocatedNode node, Map<String, GatedSubroutines.Gating> gatedNodeIds) {
        return node.effectiveType().getTemporaryAfterDeploymentSeconds() == null ? gatedNodeIds.get(node.node().getId()) : null;
    }

    static List<SkillTypeEffect> gatedEffects(List<SkillTypeEffect> effects, float strength) {
        List<SkillTypeEffect> gated = new ArrayList<>();
        for (SkillTypeEffect effect : effects) {
            if (effect.effect().supportsTemporaryGating()) {
                gated.add(new SkillTypeEffect(effect.effect(), effect.magnitude() * strength));
            }
        }
        return Collections.unmodifiableList(gated);
    }

    static boolean scalesNode(AllocatedNode node) {
        return Collections.disjoint(node.node().effectiveTags(node.effectiveType()), UNSCALED_NODE_TAGS);
    }

    @Override
    public void beforeStatsApplied(ShipEffectContext context) {
        ShipSkillData shipData = context.shipData();
        FleetMemberAPI member = context.member();
        ShipVariantAPI variant = context.variant();
        if (member != null && variant != null && !shipData.isNpcBuild() && !shipData.getSocketedItems().isEmpty()
                && !SkillDataResolver.isNpcTree(variant)) {
            DoctrineOverrides.bindShipFacts(shipData, () -> PlayerShipFacts.of(member, variant));
        }
    }

    @Override
    public void afterStatsApplied(ShipEffectContext context, ResolvedTree resolvedTree) {
        ShipTreeSync.syncVariant(context.member(), context.variant(), allocated(resolvedTree), phantomHullModIds(resolvedTree));
    }

    @Override
    public void afterShipCreation(ShipAPI ship, ResolvedTree resolvedTree) {
        ModuleTreeShare.attach(ship, resolvedTree);
        boolean playerTree = !SkillDataResolver.isNpcTree(ship.getVariant());
        FleetMemberAPI member = SkillTreeHullMod.memberOf(ship);
        PhantomConflictWatch.inspect(member, ship.getVariant(), phantomHullModIds(resolvedTree), playerTree);
        HullModConflictResolver.removeHullModsThatTriedToStripAPhantom(allocated(resolvedTree), ship.getVariant(), playerTree);
    }

    static List<NodeEffects> nodeEffects(ResolvedTree resolvedTree) {
        return resolvedTree.derived(NODE_EFFECTS_KEY, () -> nodeEffectsOf(resolvedTree.contributions()));
    }

    static List<NodeEffects> nodeEffectsOf(List<EffectContribution> contributions) {
        Map<String, AllocatedNode> nodesById = new LinkedHashMap<>();
        Map<String, List<SkillTypeEffect>> effectsById = new HashMap<>();
        for (EffectContribution contribution : contributions) {
            AllocatedNode node = contribution.gate() == null ? nodeOf(contribution.origin()) : null;
            if (node != null) {
                String nodeId = node.node().getId();
                if (contribution.origin() instanceof AllocatedNode) {
                    nodesById.putIfAbsent(nodeId, node);
                }
                effectsById.computeIfAbsent(nodeId, id -> new ArrayList<>()).addAll(contribution.effects());
            }
        }
        List<NodeEffects> nodeEffects = new ArrayList<>(nodesById.size());
        nodesById.forEach((nodeId, node) -> nodeEffects.add(new NodeEffects(node, List.copyOf(effectsById.get(nodeId)))));
        return List.copyOf(nodeEffects);
    }

    private static AllocatedNode nodeOf(Object origin) {
        if (origin instanceof AllocatedNode node) {
            return node;
        }
        if (origin instanceof SubroutineOrigin subroutine) {
            return subroutine.socket();
        }
        return origin instanceof RadiusLineOrigin radiusLine ? radiusLine.node() : null;
    }

    static List<AllocatedNode> allocated(ResolvedTree resolvedTree) {
        return resolvedTree.derived(ALLOCATED_KEY, () -> nodeEffects(resolvedTree).stream().map(NodeEffects::node).toList());
    }

    static Set<String> phantomHullModIds(ResolvedTree resolvedTree) {
        return resolvedTree.derived(PHANTOM_HULL_MOD_IDS_KEY, () -> phantomHullModIdsOf(allocated(resolvedTree)));
    }

    static Set<String> phantomHullModIdsOf(List<AllocatedNode> allocatedNodes) {
        Set<String> phantomIds = new LinkedHashSet<>();
        for (AllocatedNode node : allocatedNodes) {
            for (String phantomHullModId : node.effectiveType().getPhantomHullModIds()) {
                if (PhantomHullMods.isActive(phantomHullModId)) {
                    phantomIds.add(phantomHullModId);
                }
            }
        }
        return Collections.unmodifiableSet(phantomIds);
    }
}
