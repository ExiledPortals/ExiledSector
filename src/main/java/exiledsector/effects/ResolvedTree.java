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
    private final int revision;
    private final List<AllocatedNode> allocated;
    private final List<Entry> entries;
    private final List<TemporaryNode> temporaryNodes;
    private final Set<String> phantomHullModIds;

    private ResolvedTree(ShipSkillData data, HullSize hullSize) {
        this.hullSize = hullSize;
        this.revision = data.revision();
        this.allocated = List.copyOf(AllocatedNode.of(data));
        List<Entry> resolved = new ArrayList<>();
        List<TemporaryNode> temporary = new ArrayList<>();
        Map<SkillEffect, Float> multipliers = new LinkedHashMap<>();
        for (AllocatedNode node : allocated) {
            String vanillaHullModId = node.effectiveType().getVanillaHullModId();
            if (vanillaHullModId != null) {
                addVanillaEntry(vanillaHullModId, resolved);
            } else if (node.effectiveType().getTemporaryAfterDeploymentSeconds() != null) {
                addTemporaryNode(data, node, hullSize, resolved, temporary);
            } else {
                addEffects(data, node, hullSize, resolved, multipliers);
            }
        }
        multipliers.forEach((effect, total) ->
                resolved.add(new EffectEntry(effect, MULTIPLIER_MOD_ID_PREFIX + effect.name(), SkillEffect.addedMultiplier(total))));
        this.entries = List.copyOf(resolved);
        this.temporaryNodes = List.copyOf(temporary);
        this.phantomHullModIds = phantomHullModIdsOf(allocated);
    }

    private static void addVanillaEntry(String vanillaHullModId, List<Entry> resolved) {
        HullModSpecAPI spec = Global.getSettings().getHullModSpec(vanillaHullModId);
        HullModEffect vanillaEffect = spec == null ? null : PhantomHullMods.vanillaEffect(spec.getEffect());
        if (vanillaEffect != null) {
            resolved.add(new VanillaEntry(vanillaEffect, vanillaHullModId));
        }
    }

    private static void addTemporaryNode(ShipSkillData data, AllocatedNode node, HullSize hullSize, List<Entry> resolved,
                                         List<TemporaryNode> temporary) {
        String modId = MOD_ID_PREFIX + node.node().getId();
        List<EffectEntry> nodeEffects = new ArrayList<>();
        for (SkillTypeEffect effect : AllocatedSkillEffects.appliedEffects(data, node, hullSize)) {
            EffectEntry entry = new EffectEntry(effect.effect(), modId, effect.magnitude());
            nodeEffects.add(entry);
            resolved.add(entry);
        }
        if (!nodeEffects.isEmpty()) {
            temporary.add(new TemporaryNode(node.effectiveType().getTemporaryAfterDeploymentSeconds(), List.copyOf(nodeEffects)));
        }
    }

    private static void addEffects(ShipSkillData data, AllocatedNode node, HullSize hullSize, List<Entry> resolved,
                                   Map<SkillEffect, Float> multipliers) {
        String modId = MOD_ID_PREFIX + node.node().getId();
        for (SkillTypeEffect effect : AllocatedSkillEffects.appliedEffects(data, node, hullSize)) {
            if (effect.effect().isMultiplicative()) {
                multipliers.merge(effect.effect(), effect.magnitude(), Float::sum);
            } else {
                resolved.add(new EffectEntry(effect.effect(), modId, effect.magnitude()));
            }
        }
    }

    static ResolvedTree of(ShipSkillData data, HullSize hullSize) {
        if (data == null) {
            return null;
        }
        ResolvedTree cached = CACHE.get(data);
        if (cached != null && cached.revision == data.revision() && cached.hullSize == hullSize) {
            return cached;
        }
        ResolvedTree tree = new ResolvedTree(data, hullSize);
        CACHE.put(data, tree);
        return tree;
    }

    static Set<String> phantomHullModIdsOf(List<AllocatedNode> allocatedNodes) {
        Set<String> ids = new LinkedHashSet<>();
        for (AllocatedNode node : allocatedNodes) {
            for (String phantomHullModId : node.effectiveType().getPhantomHullModIds()) {
                if (PhantomHullMods.isActive(phantomHullModId)) {
                    ids.add(phantomHullModId);
                }
            }
        }
        return Collections.unmodifiableSet(ids);
    }

    public static void clearCache() {
        CACHE.clear();
    }

    List<AllocatedNode> allocated() {
        return allocated;
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
