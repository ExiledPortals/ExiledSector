package exiledsector.effects;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.items.DoctrineOverrides;
import exiledsector.skills.AllocatedNode;
import exiledsector.skills.AllocatedSkillEffects;
import exiledlib.skills.EffectContribution;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillDataResolver;
import exiledsector.skills.SkillType;
import exiledlib.skills.SkillTypeEffect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SkillTreeEffectSource implements ShipEffectSource {

    public static final SkillTreeEffectSource INSTANCE = new SkillTreeEffectSource();
    static final String MOD_ID_PREFIX = "exiledSector_skill_";
    static final Set<String> UNSCALED_NODE_TAGS = Set.of("logistics", "fleet_support", "campaign_only");

    private static final Object NODE_EFFECTS_KEY = new Object();
    private static final Object ALLOCATED_KEY = new Object();
    private static final Object PHANTOM_HULL_MOD_IDS_KEY = new Object();

    public record NodeEffects(AllocatedNode node, List<SkillTypeEffect> effects) {
    }

    private SkillTreeEffectSource() {
    }

    @Override
    public float bonusScale(ShipEffectContext context) {
        return SkillDataResolver.isNpcTree(context.variant()) ? NpcBonusScale.forShip(context.member(), context.variant()) : 1f;
    }

    @Override
    public Object cacheKey(ShipEffectContext context) {
        return DoctrineOverrides.layer(context.shipData());
    }

    @Override
    public List<EffectContribution> contributions(ShipEffectContext context) {
        return contributions(context.shipData(), context.hullSize());
    }

    public static List<EffectContribution> contributions(ShipSkillData shipData, HullSize hullSize) {
        List<EffectContribution> contributions = new ArrayList<>();
        for (AllocatedNode node : AllocatedNode.of(shipData)) {
            SkillType type = node.effectiveType();
            boolean vanillaPassthrough = AllocatedSkillEffects.isVanillaPassthrough(node);
            boolean scaled = scalesNode(node);
            contributions.add(new EffectContribution(node, MOD_ID_PREFIX + node.node().getId() + "_",
                    Collections.unmodifiableList(AllocatedSkillEffects.treeEffects(shipData, node, hullSize)), type.getTemporaryAfterDeploymentSeconds(),
                    vanillaPassthrough ? type.getVanillaHullModId() : null, scaled, scaled && !vanillaPassthrough));
        }
        return contributions;
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
        return resolvedTree.derived(NODE_EFFECTS_KEY, () -> {
            List<NodeEffects> nodeEffects = new ArrayList<>();
            for (EffectContribution contribution : resolvedTree.contributions()) {
                if (contribution.origin() instanceof AllocatedNode node) {
                    nodeEffects.add(new NodeEffects(node, contribution.effects()));
                }
            }
            return List.copyOf(nodeEffects);
        });
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
