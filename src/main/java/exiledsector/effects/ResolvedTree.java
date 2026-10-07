package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.HullModEffect;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import exiledsector.skills.AllocatedNode;
import exiledsector.skills.AllocatedSkillEffects;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.skilleffect.DefenseSkillEffect;
import exiledsector.skills.skilleffect.SkillEffect;

import java.util.ArrayList;
import java.util.Collections;
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
    static final float MAX_DAMAGE_TAKEN_REDUCTION_PERCENT = 80f;

    private static final Map<ShipSkillData, ResolvedTree> CACHE = new WeakHashMap<>();
    private static final List<ReductionCap> REDUCTION_CAPS = List.of(
            new ReductionCap(DefenseSkillEffect.EMP_DAMAGE_TAKEN_MULT,
                    Set.of(DefenseSkillEffect.EMP_DAMAGE_TAKEN_PERCENT, DefenseSkillEffect.EMP_DAMAGE_TAKEN_MULT)),
            new ReductionCap(DefenseSkillEffect.ENERGY_DAMAGE_TAKEN_PERCENT, Set.of(DefenseSkillEffect.ENERGY_DAMAGE_TAKEN_PERCENT)),
            new ReductionCap(DefenseSkillEffect.KINETIC_DAMAGE_TAKEN_PERCENT, Set.of(DefenseSkillEffect.KINETIC_DAMAGE_TAKEN_PERCENT)),
            new ReductionCap(DefenseSkillEffect.HIGH_EXPLOSIVE_DAMAGE_TAKEN_PERCENT, Set.of(DefenseSkillEffect.HIGH_EXPLOSIVE_DAMAGE_TAKEN_PERCENT)),
            new ReductionCap(DefenseSkillEffect.FRAGMENTATION_DAMAGE_TAKEN_PERCENT, Set.of(DefenseSkillEffect.FRAGMENTATION_DAMAGE_TAKEN_PERCENT)));

    private record ReductionCap(SkillEffect cappedEffect, Set<SkillEffect> contributingEffects) {
    }

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
        NpcBonusScaling scaling = NpcBonusScaling.of(bonusScale, shipData, allocatedNodes, hullSize);
        List<Entry> resolvedEntries = new ArrayList<>();
        List<TemporaryNode> resolvedTemporaryNodes = new ArrayList<>();
        Map<SkillEffect, Float> multiplierTotals = new LinkedHashMap<>();
        for (AllocatedNode node : allocatedNodes) {
            String vanillaHullModId = node.effectiveType().getVanillaHullModId();
            if (vanillaHullModId != null) {
                addVanillaEntry(vanillaHullModId, resolvedEntries);
            } else if (node.effectiveType().getTemporaryAfterDeploymentSeconds() != null) {
                addTemporaryNode(shipData, node, hullSize, scaling, resolvedEntries, resolvedTemporaryNodes);
            } else {
                addEffects(shipData, node, hullSize, scaling, resolvedEntries, multiplierTotals);
            }
        }
        multiplierTotals.forEach((effect, multiplierTotal) ->
                resolvedEntries.add(new EffectEntry(effect, MULTIPLIER_MOD_ID_PREFIX + effect.name(), SkillEffect.addedMultiplier(multiplierTotal))));
        Set<EffectEntry> temporaryEntries = Collections.newSetFromMap(new IdentityHashMap<>());
        resolvedTemporaryNodes.forEach(temporaryNode -> temporaryEntries.addAll(temporaryNode.effects()));
        REDUCTION_CAPS.forEach(reductionCap -> capReduction(reductionCap, resolvedEntries, temporaryEntries));
        this.entries = List.copyOf(resolvedEntries);
        this.temporaryNodes = List.copyOf(resolvedTemporaryNodes);
        this.phantomHullModIds = phantomHullModIdsOf(allocatedNodes);
    }

    private static void capReduction(ReductionCap reductionCap, List<Entry> resolvedEntries, Set<EffectEntry> temporaryEntries) {
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
        float damageTakenFactor = Math.max(0f, 1f + percentTotal / 100f) * multiplierProduct;
        float lowestFactor = 1f - MAX_DAMAGE_TAKEN_REDUCTION_PERCENT / 100f;
        if (firstContributorIndex < 0 || damageTakenFactor >= lowestFactor) {
            return;
        }
        resolvedEntries.removeIf(entry -> entry instanceof EffectEntry effectEntry && !temporaryEntries.contains(effectEntry)
                && reductionCap.contributingEffects().contains(effectEntry.effect()));
        SkillEffect cappedEffect = reductionCap.cappedEffect();
        resolvedEntries.add(Math.min(firstContributorIndex, resolvedEntries.size()),
                new EffectEntry(cappedEffect, REDUCTION_CAP_MOD_ID_PREFIX + cappedEffect.name(), -MAX_DAMAGE_TAKEN_REDUCTION_PERCENT));
    }

    private static void addVanillaEntry(String vanillaHullModId, List<Entry> resolvedEntries) {
        HullModSpecAPI hullModSpec = Global.getSettings().getHullModSpec(vanillaHullModId);
        HullModEffect vanillaEffect = hullModSpec == null ? null : PhantomHullMods.vanillaEffect(hullModSpec.getEffect());
        if (vanillaEffect != null) {
            resolvedEntries.add(new VanillaEntry(vanillaEffect, vanillaHullModId));
        }
    }

    private static void addTemporaryNode(ShipSkillData shipData, AllocatedNode node, HullSize hullSize, NpcBonusScaling scaling,
                                         List<Entry> resolvedEntries,
                                         List<TemporaryNode> resolvedTemporaryNodes) {
        String modId = MOD_ID_PREFIX + node.node().getId();
        List<EffectEntry> nodeEffects = new ArrayList<>();
        for (SkillTypeEffect effect : AllocatedSkillEffects.appliedEffects(shipData, node, hullSize)) {
            EffectEntry effectEntry = new EffectEntry(effect.effect(), modId, scaling.scaled(node, effect.effect(), effect.magnitude()));
            nodeEffects.add(effectEntry);
            resolvedEntries.add(effectEntry);
        }
        if (!nodeEffects.isEmpty()) {
            resolvedTemporaryNodes.add(new TemporaryNode(node.effectiveType().getTemporaryAfterDeploymentSeconds(), List.copyOf(nodeEffects)));
        }
    }

    private static void addEffects(ShipSkillData shipData, AllocatedNode node, HullSize hullSize, NpcBonusScaling scaling,
                                   List<Entry> resolvedEntries, Map<SkillEffect, Float> multiplierTotals) {
        String modId = MOD_ID_PREFIX + node.node().getId();
        for (SkillTypeEffect effect : AllocatedSkillEffects.appliedEffects(shipData, node, hullSize)) {
            float magnitude = scaling.scaled(node, effect.effect(), effect.magnitude());
            if (effect.effect().isMultiplicative()) {
                multiplierTotals.merge(effect.effect(), magnitude, Float::sum);
            } else {
                resolvedEntries.add(new EffectEntry(effect.effect(), modId, magnitude));
            }
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
