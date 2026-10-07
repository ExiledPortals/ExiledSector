package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.HullModEffect;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import exiledsector.skills.AllocatedNode;
import exiledsector.skills.AllocatedSkillEffects;
import exiledsector.skills.DamageTakenCaps;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.skilleffect.SkillEffect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
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
    private final int shipDataRevision;
    private final List<AllocatedNode> allocatedNodes;
    private final List<Entry> entries;
    private final List<TemporaryNode> temporaryNodes;
    private final Set<String> phantomHullModIds;

    private ResolvedTree(ShipSkillData shipData, HullSize hullSize) {
        this.hullSize = hullSize;
        this.shipDataRevision = shipData.revision();
        this.allocatedNodes = List.copyOf(AllocatedNode.of(shipData));
        List<Entry> resolvedEntries = new ArrayList<>();
        List<TemporaryNode> resolvedTemporaryNodes = new ArrayList<>();
        Map<SkillEffect, Float> multiplierTotals = new LinkedHashMap<>();
        for (AllocatedNode node : allocatedNodes) {
            String vanillaHullModId = node.effectiveType().getVanillaHullModId();
            if (vanillaHullModId != null) {
                addVanillaEntry(vanillaHullModId, resolvedEntries);
            } else if (node.effectiveType().getTemporaryAfterDeploymentSeconds() != null) {
                addTemporaryNode(shipData, node, hullSize, resolvedEntries, resolvedTemporaryNodes);
            } else {
                addEffects(shipData, node, hullSize, resolvedEntries, multiplierTotals);
            }
        }
        multiplierTotals.forEach((effect, multiplierTotal) ->
                resolvedEntries.add(new EffectEntry(effect, MULTIPLIER_MOD_ID_PREFIX + effect.name(), SkillEffect.addedMultiplier(multiplierTotal))));
        Set<EffectEntry> temporaryEntries = Collections.newSetFromMap(new IdentityHashMap<>());
        resolvedTemporaryNodes.forEach(temporaryNode -> temporaryEntries.addAll(temporaryNode.effects()));
        DamageTakenCaps.CAPS.forEach(reductionCap -> capReduction(reductionCap, resolvedEntries, temporaryEntries));
        this.entries = List.copyOf(resolvedEntries);
        this.temporaryNodes = List.copyOf(resolvedTemporaryNodes);
        this.phantomHullModIds = phantomHullModIdsOf(allocatedNodes);
    }

    private static void capReduction(DamageTakenCaps.Cap reductionCap, List<Entry> resolvedEntries, Set<EffectEntry> temporaryEntries) {
        float percentTotal = 0f;
        float multiplierProduct = 1f;
        int firstContributorIndex = -1;
        for (int i = 0; i < resolvedEntries.size(); i++) {
            if (resolvedEntries.get(i) instanceof EffectEntry effectEntry && !temporaryEntries.contains(effectEntry)
                    && reductionCap.contributingEffects().contains(effectEntry.effect())) {
                if (effectEntry.effect().isMultiplicative()) {
                    multiplierProduct *= 1f + effectEntry.magnitude() / 100f;
                } else {
                    percentTotal += effectEntry.magnitude();
                }
                firstContributorIndex = firstContributorIndex < 0 ? i : firstContributorIndex;
            }
        }
        if (firstContributorIndex < 0 || !DamageTakenCaps.exceedsCap(percentTotal, multiplierProduct)) {
            return;
        }
        resolvedEntries.removeIf(entry -> entry instanceof EffectEntry effectEntry && !temporaryEntries.contains(effectEntry)
                && reductionCap.contributingEffects().contains(effectEntry.effect()));
        SkillEffect cappedEffect = reductionCap.cappedEffect();
        resolvedEntries.add(Math.min(firstContributorIndex, resolvedEntries.size()),
                new EffectEntry(cappedEffect, REDUCTION_CAP_MOD_ID_PREFIX + cappedEffect.name(), -DamageTakenCaps.MAX_REDUCTION_PERCENT));
    }

    private static void addVanillaEntry(String vanillaHullModId, List<Entry> resolvedEntries) {
        HullModSpecAPI hullModSpec = Global.getSettings().getHullModSpec(vanillaHullModId);
        HullModEffect vanillaEffect = hullModSpec == null ? null : PhantomHullMods.vanillaEffect(hullModSpec.getEffect());
        if (vanillaEffect != null) {
            resolvedEntries.add(new VanillaEntry(vanillaEffect, vanillaHullModId));
        }
    }

    private static void addTemporaryNode(ShipSkillData shipData, AllocatedNode node, HullSize hullSize, List<Entry> resolvedEntries,
                                         List<TemporaryNode> resolvedTemporaryNodes) {
        EffectModIds modIds = new EffectModIds(node);
        List<EffectEntry> nodeEffects = new ArrayList<>();
        for (SkillTypeEffect effect : AllocatedSkillEffects.appliedEffects(shipData, node, hullSize)) {
            EffectEntry effectEntry = new EffectEntry(effect.effect(), modIds.next(effect.effect()), effect.magnitude());
            nodeEffects.add(effectEntry);
            resolvedEntries.add(effectEntry);
        }
        if (!nodeEffects.isEmpty()) {
            resolvedTemporaryNodes.add(new TemporaryNode(node.effectiveType().getTemporaryAfterDeploymentSeconds(), List.copyOf(nodeEffects)));
        }
    }

    private static void addEffects(ShipSkillData shipData, AllocatedNode node, HullSize hullSize, List<Entry> resolvedEntries,
                                   Map<SkillEffect, Float> multiplierTotals) {
        EffectModIds modIds = new EffectModIds(node);
        for (SkillTypeEffect effect : AllocatedSkillEffects.appliedEffects(shipData, node, hullSize)) {
            if (effect.effect().isMultiplicative()) {
                multiplierTotals.merge(effect.effect(), effect.magnitude(), Float::sum);
            } else {
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
        if (shipData == null) {
            return null;
        }
        ResolvedTree cachedTree = CACHE.get(shipData);
        if (cachedTree != null && cachedTree.shipDataRevision == shipData.revision() && cachedTree.hullSize == hullSize) {
            return cachedTree;
        }
        ResolvedTree resolvedTree = new ResolvedTree(shipData, hullSize);
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
