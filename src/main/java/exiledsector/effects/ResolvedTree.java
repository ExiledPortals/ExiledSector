package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.HullModEffect;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import exiledsector.skills.AllocatedNode;
import exiledsector.skills.AllocatedSkillEffects;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.skilleffect.SkillEffect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class ResolvedTree {

    static final String MOD_ID_PREFIX = "exiledSector_skill_";
    static final String MULTIPLIER_MOD_ID_PREFIX = "exiledSector_skillMult_";

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
        this.entries = List.copyOf(resolvedEntries);
        this.temporaryNodes = List.copyOf(resolvedTemporaryNodes);
        this.phantomHullModIds = phantomHullModIdsOf(allocatedNodes);
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
        String modId = MOD_ID_PREFIX + node.node().getId();
        List<EffectEntry> nodeEffects = new ArrayList<>();
        for (SkillTypeEffect effect : AllocatedSkillEffects.appliedEffects(shipData, node, hullSize)) {
            EffectEntry effectEntry = new EffectEntry(effect.effect(), modId, effect.magnitude());
            nodeEffects.add(effectEntry);
            resolvedEntries.add(effectEntry);
        }
        if (!nodeEffects.isEmpty()) {
            resolvedTemporaryNodes.add(new TemporaryNode(node.effectiveType().getTemporaryAfterDeploymentSeconds(), List.copyOf(nodeEffects)));
        }
    }

    private static void addEffects(ShipSkillData shipData, AllocatedNode node, HullSize hullSize, List<Entry> resolvedEntries,
                                   Map<SkillEffect, Float> multiplierTotals) {
        String modId = MOD_ID_PREFIX + node.node().getId();
        for (SkillTypeEffect effect : AllocatedSkillEffects.appliedEffects(shipData, node, hullSize)) {
            if (effect.effect().isMultiplicative()) {
                multiplierTotals.merge(effect.effect(), effect.magnitude(), Float::sum);
            } else {
                resolvedEntries.add(new EffectEntry(effect.effect(), modId, effect.magnitude()));
            }
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
