package exiledsector.items;

import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledlib.skills.ShipFacts;
import exiledlib.skills.SkillTypeEffect;
import exiledlib.skills.skilleffect.SkillEffect;
import exiledlib.skills.skilleffect.shield.ShieldSkillEffect;
import exiledsector.skills.AllocatedNode;
import exiledsector.skills.NodeEligibility;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.layout.SkillNodeDecoration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.function.Supplier;

public final class DoctrineOverrides {

    private static final Set<SkillTier> REWRITTEN_TIERS = Set.of(SkillTier.SMALL, SkillTier.NOTABLE, SkillTier.KEYSTONE);
    private static final Map<ShipSkillData, DoctrineLayer> LAYERS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<ShipSkillData, Supplier<ShipFacts>> BOUND_FACTS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final ThreadLocal<Set<ShipSkillData>> BUILDING = ThreadLocal.withInitial(() -> Collections.newSetFromMap(new IdentityHashMap<>()));

    private record FactsFingerprint(HullSize hullSize, String hullId, ShieldType shieldType, boolean phaseHull, float baseArmor,
                                    boolean limitedSystemCharges, boolean onlyBuiltInWings, Set<String> presentHullModIds,
                                    Set<String> fixedHullModIds) {
    }

    private DoctrineOverrides() {
    }

    public static SkillType replacementType(ShipSkillData data, SkillNode node) {
        if (data == null || node == null) {
            return null;
        }
        DoctrineRewrite rewrite = layer(data).rewrite(node.getId());
        return rewrite == null ? null : rewrite.replacementType();
    }

    public static SkillNode asTransformed(ShipSkillData data, SkillNode node) {
        SkillType replacement = replacementType(data, node);
        return replacement == null ? node : rewrittenNode(node, replacement);
    }

    public static List<SkillTypeEffect> addedEffects(ShipSkillData data, SkillNode node, HullSize hullSize) {
        if (data == null || node == null) {
            return List.of();
        }
        DoctrineRewrite rewrite = layer(data).rewrite(node.getId());
        return rewrite == null ? List.of() : rewrite.addedEffects(hullSize);
    }

    public static DoctrineRewrite rewrite(ShipSkillData data, String nodeId) {
        return data == null ? null : layer(data).rewrite(nodeId);
    }

    public static DoctrineLayer layer(ShipSkillData data) {
        if (data == null) {
            return DoctrineLayer.NONE;
        }
        DoctrineLayer cached = LAYERS.get(data);
        if (cached != null && cached.isCurrentFor(data)) {
            return cached;
        }
        if (BUILDING.get().contains(data)) {
            return DoctrineLayer.NONE;
        }
        DoctrineLayer built = data.getSocketedItems().isEmpty() ? DoctrineLayer.empty(data) : build(data);
        LAYERS.put(data, built);
        return built;
    }

    public static void bindShipFacts(ShipSkillData data, Supplier<ShipFacts> facts) {
        if (data == null || facts == null) {
            return;
        }
        BOUND_FACTS.put(data, facts);
        DoctrineLayer cached = LAYERS.get(data);
        if (cached != null && !cached.isEmpty() && !Objects.equals(fingerprint(cached.archive(), facts.get()), cached.factsFingerprint())) {
            LAYERS.remove(data);
        }
    }

    public static void forget(ShipSkillData data) {
        if (data != null) {
            LAYERS.remove(data);
        }
    }

    public static void clearCache() {
        LAYERS.clear();
        BOUND_FACTS.clear();
    }

    private static DoctrineLayer build(ShipSkillData data) {
        Set<ShipSkillData> building = BUILDING.get();
        building.add(data);
        try {
            return buildLayer(data);
        } finally {
            building.remove(data);
            if (building.isEmpty()) {
                BUILDING.remove();
            }
        }
    }

    private static DoctrineLayer buildLayer(ShipSkillData data) {
        DoctrineLayer.Stamp stamp = DoctrineLayer.Stamp.of(data);
        Map<String, String> socketedItems = data.getSocketedItems();
        if (socketedItems.isEmpty() || DoctrineArchives.all().isEmpty()) {
            return DoctrineLayer.empty(data);
        }
        for (Map.Entry<String, String> socketed : new TreeMap<>(socketedItems).entrySet()) {
            SkillNode socket = SkillTree.get(socketed.getKey());
            if (socket == null || !data.isAllocated(socket.getId())) {
                continue;
            }
            Item item = ItemStore.lookup(socketed.getValue());
            if (item instanceof Subroutine subroutine && subroutine.subroutineKind() == SubroutineKind.DOCTRINE && subroutine.canSocketInto(socket)) {
                DoctrineArchive archive = DoctrineArchives.get(subroutine.definitionId());
                if (archive != null) {
                    return layerFor(data, socket, subroutine, archive, stamp);
                }
            }
        }
        return DoctrineLayer.empty(data);
    }

    private static DoctrineLayer layerFor(ShipSkillData data, SkillNode socket, Subroutine doctrine, DoctrineArchive archive,
                                          DoctrineLayer.Stamp stamp) {
        Supplier<ShipFacts> factsSupplier = BOUND_FACTS.get(data);
        ShipFacts facts = factsSupplier == null ? null : factsSupplier.get();
        List<SkillNode> rangeNodes = new ArrayList<>();
        for (String nodeId : new TreeSet<>(SkillTree.radii().nodeIdsWithin(socket.getId(), archive.radiusSize()))) {
            SkillNode node = SkillTree.get(nodeId);
            if (node != null && REWRITTEN_TIERS.contains(node.getType().getTier())) {
                rangeNodes.add(node);
            }
        }
        int revision = DoctrineRewriter.revision(doctrine.seed());
        Map<String, DoctrineRewrite> rewrites = archive.transforms()
                ? transformed(data, rangeNodes, doctrine, archive, revision, facts)
                : augmented(data, rangeNodes, doctrine, archive, revision);
        return new DoctrineLayer(socket.getId(), archive, rewrites, stamp, fingerprint(archive, facts));
    }

    private static Map<String, DoctrineRewrite> augmented(ShipSkillData data, List<SkillNode> rangeNodes, Subroutine doctrine,
                                                         DoctrineArchive archive, int revision) {
        Map<String, DoctrineRewrite> rewrites = new LinkedHashMap<>();
        for (SkillNode node : rangeNodes) {
            SkillType originalType = node.resolveBaseType(data);
            if (originalType.getTemporaryAfterDeploymentSeconds() != null) {
                continue;
            }
            List<DoctrineRewriter.AugmentLine> lines = DoctrineRewriter.augmentLines(archive, doctrine.seed(), node.getId());
            if (!lines.isEmpty()) {
                rewrites.put(node.getId(), new DoctrineRewrite(node.getId(), archive.definitionId(), revision, originalType, null, lines));
            }
        }
        return rewrites;
    }

    private static Map<String, DoctrineRewrite> transformed(ShipSkillData data, List<SkillNode> rangeNodes, Subroutine doctrine,
                                                           DoctrineArchive archive, int revision, ShipFacts facts) {
        Set<String> transformableIds = new HashSet<>();
        for (SkillNode node : rangeNodes) {
            if (!archive.replacementPool(node.getType().getTier()).isEmpty()) {
                transformableIds.add(node.getId());
            }
        }
        Set<String> keptNodeIds = new HashSet<>();
        while (true) {
            Set<String> unresolvedNodeIds = new HashSet<>();
            Map<String, DoctrineRewrite> rewrites = transformPass(data, rangeNodes, doctrine, archive, revision, facts, transformableIds, keptNodeIds,
                    unresolvedNodeIds);
            if (unresolvedNodeIds.isEmpty()) {
                return rewrites;
            }
            keptNodeIds.addAll(unresolvedNodeIds);
        }
    }

    private static Map<String, DoctrineRewrite> transformPass(ShipSkillData data, List<SkillNode> rangeNodes, Subroutine doctrine,
                                                             DoctrineArchive archive, int revision, ShipFacts facts, Set<String> transformableIds,
                                                             Set<String> keptNodeIds, Set<String> unresolvedNodeIds) {
        List<AllocatedNode> context = new ArrayList<>();
        Set<String> takenTypeIds = new HashSet<>();
        for (String nodeId : data.getAllocatedNodeIds()) {
            SkillNode node = SkillTree.get(nodeId);
            if (node != null && !transformableIds.contains(nodeId)) {
                SkillType baseType = node.resolveBaseType(data);
                context.add(new AllocatedNode(node, baseType));
                takenTypeIds.add(baseType.getId());
            }
        }
        ShieldType shieldType = facts == null ? null : shieldTypeOf(facts, context);
        for (SkillNode node : rangeNodes) {
            if (keptNodeIds.contains(node.getId())) {
                SkillType baseType = node.resolveBaseType(data);
                context.add(new AllocatedNode(node, baseType));
                takenTypeIds.add(baseType.getId());
            }
        }
        Map<String, DoctrineRewrite> rewrites = new LinkedHashMap<>();
        for (SkillNode node : rangeNodes) {
            if (!transformableIds.contains(node.getId()) || keptNodeIds.contains(node.getId())) {
                continue;
            }
            SkillTier tier = node.getType().getTier();
            boolean uniquePerShip = tier != SkillTier.SMALL;
            SkillType replacement = null;
            for (String typeId : DoctrineRewriter.candidateOrder(doctrine.seed(), node.getId(), archive.replacementPool(tier))) {
                SkillType candidate = SkillTree.getType(typeId);
                if (candidate != null && !typeId.equals(node.getType().getId()) && !(uniquePerShip && takenTypeIds.contains(typeId))
                        && eligible(node, candidate, context, facts, shieldType)) {
                    replacement = candidate;
                    break;
                }
            }
            if (replacement != null) {
                rewrites.put(node.getId(), new DoctrineRewrite(node.getId(), archive.definitionId(), revision, node.resolveBaseType(data), replacement,
                        List.of()));
                context.add(new AllocatedNode(rewrittenNode(node, replacement), replacement));
                if (uniquePerShip) {
                    takenTypeIds.add(replacement.getId());
                }
            } else {
                unresolvedNodeIds.add(node.getId());
            }
        }
        return rewrites;
    }

    private static boolean eligible(SkillNode node, SkillType candidate, List<AllocatedNode> context, ShipFacts facts, ShieldType shieldType) {
        if (candidate.isOptional() || candidate.getItemCost() != null) {
            return false;
        }
        if (facts == null) {
            return context.stream().noneMatch(existing -> existing.isExclusiveWith(candidate));
        }
        NodeEligibility.Context eligibility = new NodeEligibility.Context(context, shieldType, facts, null, null, null, null);
        return NodeEligibility.check(rewrittenNode(node, candidate), null, eligibility) == null;
    }

    private static SkillNode rewrittenNode(SkillNode node, SkillType replacement) {
        return new SkillNode(node.getId(), replacement, node.getConnectedNodeIds(), node.getOffsetX(), node.getOffsetY(), SkillNodeDecoration.NONE,
                node.getTags());
    }

    private static ShieldType shieldTypeOf(ShipFacts facts, List<AllocatedNode> context) {
        List<SkillEffect> effectsInOrder = new ArrayList<>();
        for (AllocatedNode allocated : context) {
            for (SkillTypeEffect effect : allocated.effectiveType().effectsFor(facts.hullSize())) {
                effectsInOrder.add(effect.effect());
            }
        }
        return ShieldSkillEffect.resolveDisplayShieldType(facts.hullShieldType(), effectsInOrder);
    }

    private static Object fingerprint(DoctrineArchive archive, ShipFacts facts) {
        if (archive == null || facts == null) {
            return null;
        }
        Set<String> relevantHullModIds = new LinkedHashSet<>();
        for (SkillTier tier : REWRITTEN_TIERS) {
            for (String typeId : archive.replacementPool(tier)) {
                SkillType type = SkillTree.getType(typeId);
                if (type != null) {
                    relevantHullModIds.addAll(type.getExclusiveHullModIds());
                    relevantHullModIds.addAll(type.getPhantomHullModIds());
                }
            }
        }
        Set<String> present = new LinkedHashSet<>();
        Set<String> fixed = new LinkedHashSet<>();
        for (String hullModId : relevantHullModIds) {
            if (facts.hasHullMod() != null && facts.hasHullMod().test(hullModId)) {
                present.add(hullModId);
            }
            if (facts.hasFixedHullMod() != null && facts.hasFixedHullMod().test(hullModId)) {
                fixed.add(hullModId);
            }
        }
        return new FactsFingerprint(facts.hullSize(), facts.hullId(), facts.hullShieldType(), facts.phaseHull(), facts.baseArmor(),
                facts.limitedSystemCharges(), facts.onlyBuiltInWings(), Set.copyOf(present), Set.copyOf(fixed));
    }
}
