package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.HullModEffect;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import exiledsector.skills.AllocatedNode;
import exiledsector.skills.DamageTakenCaps;
import exiledsector.skills.EffectTotals;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillType;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.skilleffect.SkillEffect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class ResolvedTree {

    static final String MOD_ID_PREFIX = "exiledSector_skill_";
    static final String MULTIPLIER_MOD_ID_PREFIX = "exiledSector_skillMult_";
    static final String REDUCTION_CAP_MOD_ID_PREFIX = "exiledSector_skillCapped_";

    private static final Map<ShipSkillData, ResolvedTree> CACHE = new WeakHashMap<>();

    sealed interface Entry permits VanillaEntry, EffectEntry {
    }

    record VanillaEntry(HullModEffect effect, String hullModId) implements Entry {
    }

    record EffectEntry(SkillEffect effect, String modId, float magnitude) implements Entry {
    }

    record TemporaryNode(float durationSeconds, List<EffectEntry> effects) {
    }

    private final HullSize hullSize;
    private final float bonusScale;
    private final int shipDataRevision;
    private final List<AllocatedNode> allocatedNodes;
    private final List<Entry> entries;
    private final List<TemporaryNode> temporaryNodes;
    private final Set<String> phantomHullModIds;

    private ResolvedTree(ShipSkillData shipData, HullSize hullSize, float bonusScale) {
        this.hullSize = hullSize;
        this.bonusScale = bonusScale;
        this.shipDataRevision = shipData.revision();
        this.allocatedNodes = List.copyOf(AllocatedNode.of(shipData));
        EffectTotals effectTotals = EffectTotals.of(shipData, allocatedNodes, hullSize, bonusScale);
        List<Entry> resolvedEntries = new ArrayList<>();
        List<TemporaryNode> resolvedTemporaryNodes = new ArrayList<>();
        for (EffectTotals.NodeEffects nodeEffects : effectTotals.nodeEffects()) {
            SkillType type = nodeEffects.node().effectiveType();
            if (type.getVanillaHullModId() != null) {
                addVanillaEntry(type.getVanillaHullModId(), resolvedEntries);
            } else if (type.getTemporaryAfterDeploymentSeconds() != null) {
                addTemporaryNode(nodeEffects, resolvedEntries, resolvedTemporaryNodes);
            } else {
                addEffects(nodeEffects, resolvedEntries);
            }
        }
        effectTotals.pooledMultipliers().forEach((effect, addedMultiplier) ->
                resolvedEntries.add(new EffectEntry(effect, MULTIPLIER_MOD_ID_PREFIX + effect.name(), addedMultiplier)));
        Set<EffectEntry> temporaryEntries = Collections.newSetFromMap(new IdentityHashMap<>());
        resolvedTemporaryNodes.forEach(temporaryNode -> temporaryEntries.addAll(temporaryNode.effects()));
        effectTotals.reachedCaps().forEach(reductionCap -> replaceWithCap(reductionCap, resolvedEntries, temporaryEntries));
        this.entries = List.copyOf(resolvedEntries);
        this.temporaryNodes = List.copyOf(resolvedTemporaryNodes);
        this.phantomHullModIds = phantomHullModIdsOf(allocatedNodes);
    }

    private static void replaceWithCap(DamageTakenCaps.Cap reductionCap, List<Entry> resolvedEntries, Set<EffectEntry> temporaryEntries) {
        int firstContributorIndex = 0;
        while (firstContributorIndex < resolvedEntries.size()
                && !isCapContributor(resolvedEntries.get(firstContributorIndex), reductionCap, temporaryEntries)) {
            firstContributorIndex++;
        }
        resolvedEntries.removeIf(entry -> isCapContributor(entry, reductionCap, temporaryEntries));
        SkillEffect cappedEffect = reductionCap.cappedEffect();
        resolvedEntries.add(Math.min(firstContributorIndex, resolvedEntries.size()),
                new EffectEntry(cappedEffect, REDUCTION_CAP_MOD_ID_PREFIX + cappedEffect.name(), -DamageTakenCaps.MAX_REDUCTION_PERCENT));
    }

    private static boolean isCapContributor(Entry entry, DamageTakenCaps.Cap reductionCap, Set<EffectEntry> temporaryEntries) {
        return entry instanceof EffectEntry effectEntry && !temporaryEntries.contains(effectEntry)
                && reductionCap.contributingEffects().contains(effectEntry.effect());
    }

    private static void addVanillaEntry(String vanillaHullModId, List<Entry> resolvedEntries) {
        HullModSpecAPI hullModSpec = Global.getSettings().getHullModSpec(vanillaHullModId);
        HullModEffect vanillaEffect = hullModSpec == null ? null : PhantomHullMods.vanillaEffect(hullModSpec.getEffect());
        if (vanillaEffect != null) {
            resolvedEntries.add(new VanillaEntry(vanillaEffect, vanillaHullModId));
        }
    }

    private static void addTemporaryNode(EffectTotals.NodeEffects nodeEffects, List<Entry> resolvedEntries,
                                         List<TemporaryNode> resolvedTemporaryNodes) {
        EffectModIds modIds = new EffectModIds(nodeEffects.node());
        List<EffectEntry> temporaryEffects = new ArrayList<>();
        for (SkillTypeEffect effect : nodeEffects.effects()) {
            EffectEntry effectEntry = new EffectEntry(effect.effect(), modIds.next(effect.effect()), effect.magnitude());
            temporaryEffects.add(effectEntry);
            resolvedEntries.add(effectEntry);
        }
        if (!temporaryEffects.isEmpty()) {
            resolvedTemporaryNodes.add(new TemporaryNode(nodeEffects.node().effectiveType().getTemporaryAfterDeploymentSeconds(),
                    List.copyOf(temporaryEffects)));
        }
    }

    private static void addEffects(EffectTotals.NodeEffects nodeEffects, List<Entry> resolvedEntries) {
        EffectModIds modIds = new EffectModIds(nodeEffects.node());
        for (SkillTypeEffect effect : nodeEffects.effects()) {
            if (!effect.effect().isMultiplicative()) {
                resolvedEntries.add(new EffectEntry(effect.effect(), modIds.next(effect.effect()), effect.magnitude()));
            }
        }
    }

    private static final class EffectModIds {

        private final String nodePrefix;
        private final Map<SkillEffect, Integer> uses = new HashMap<>();

        EffectModIds(AllocatedNode node) {
            this.nodePrefix = MOD_ID_PREFIX + node.node().getId() + "_";
        }

        String next(SkillEffect effect) {
            int use = uses.merge(effect, 1, Integer::sum);
            return use == 1 ? nodePrefix + effect.name() : nodePrefix + effect.name() + "_" + use;
        }
    }

    static ResolvedTree of(ShipSkillData shipData, HullSize hullSize) {
        return of(shipData, hullSize, 1f);
    }

    static ResolvedTree of(ShipSkillData shipData, HullSize hullSize, float bonusScale) {
        if (shipData == null) {
            return null;
        }
        ResolvedTree cachedTree = CACHE.get(shipData);
        if (cachedTree != null && cachedTree.shipDataRevision == shipData.revision() && cachedTree.hullSize == hullSize
                && Float.compare(cachedTree.bonusScale, bonusScale) == 0) {
            return cachedTree;
        }
        ResolvedTree resolvedTree = new ResolvedTree(shipData, hullSize, bonusScale);
        CACHE.put(shipData, resolvedTree);
        return resolvedTree;
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

    public static void clearCache() {
        CACHE.clear();
    }

    List<AllocatedNode> allocated() {
        return allocatedNodes;
    }

    List<Entry> entries() {
        return entries;
    }

    List<TemporaryNode> temporaryNodes() {
        return temporaryNodes;
    }

    Set<String> phantomHullModIds() {
        return phantomHullModIds;
    }
}
