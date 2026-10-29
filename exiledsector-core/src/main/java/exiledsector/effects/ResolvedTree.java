package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.HullModEffect;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import exiledsector.skills.CombatGate;
import exiledsector.skills.DamageTakenCaps;
import exiledsector.skills.DiminishingReturns;
import exiledsector.skills.EffectContribution;
import exiledsector.skills.EffectTotals;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.StatBaseline;
import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.skills.ShipSkillData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Supplier;

public final class ResolvedTree {

    static final String MULTIPLIER_MOD_ID_PREFIX = "exiledSector_skillMult_";
    static final String REDUCTION_CAP_MOD_ID_PREFIX = "exiledSector_skillCapped_";

    private static final Map<ShipSkillData, ResolvedTree> CACHE = new WeakHashMap<>();

    sealed interface Entry permits VanillaEntry, EffectEntry {
    }

    record VanillaEntry(HullModEffect effect, String hullModId) implements Entry {
    }

    record EffectEntry(SkillEffect effect, String modId, float magnitude) implements Entry {
    }

    public record AppliedModifier(String modId, SkillEffect effect, List<EffectContribution> sources, boolean reductionCap) {
    }

    private static final Object APPLIED_MODIFIERS_KEY = new Object();
    private static final int CAP_SCALE_STEPS = 24;

    record TemporaryNode(float durationSeconds, List<EffectEntry> effects) {
    }

    record GatedNode(CombatGate gate, List<EffectEntry> effects) {
    }

    private final List<ShipEffectSource> sources;
    private final HullSize hullSize;
    private final float bonusScale;
    private final int shipDataRevision;
    private final List<Object> sourceCacheKeys;
    private final StatBaseline statBaseline;
    private final DiminishingReturns.Config diminishing;
    private final EffectTotals totals;
    private final List<Entry> entries;
    private final List<TemporaryNode> temporaryNodes;
    private final List<GatedNode> gatedNodes;
    private final Map<Object, Object> derived = new HashMap<>();

    private ResolvedTree(ShipEffectContext context, List<ShipEffectSource> sources, float bonusScale, List<Object> sourceCacheKeys,
                         StatBaseline statBaseline, DiminishingReturns.Config diminishing) {
        this.sources = sources;
        this.hullSize = context.hullSize();
        this.bonusScale = bonusScale;
        this.shipDataRevision = context.shipData().revision();
        this.sourceCacheKeys = sourceCacheKeys;
        this.statBaseline = statBaseline;
        this.diminishing = diminishing;
        List<EffectContribution> contributions = new ArrayList<>();
        sources.forEach(source -> contributions.addAll(source.contributions(context)));
        this.totals = EffectTotals.of(contributions, bonusScale, statBaseline, diminishing);
        List<Entry> resolvedEntries = new ArrayList<>();
        List<TemporaryNode> resolvedTemporaryNodes = new ArrayList<>();
        List<GatedNode> resolvedGatedNodes = new ArrayList<>();
        for (EffectContribution contribution : totals.contributions()) {
            if (contribution.gate() != null) {
                addGatedNode(contribution, resolvedGatedNodes);
            } else if (contribution.vanillaHullModId() != null) {
                addVanillaEntry(contribution.vanillaHullModId(), resolvedEntries);
                addEffects(contribution, resolvedEntries);
            } else if (!contribution.permanent()) {
                addTemporaryNode(contribution, resolvedEntries, resolvedTemporaryNodes);
            } else {
                addEffects(contribution, resolvedEntries);
            }
        }
        totals.pooledMultipliers().forEach((effect, addedMultiplier) ->
                resolvedEntries.add(new EffectEntry(effect, MULTIPLIER_MOD_ID_PREFIX + effect.name(), addedMultiplier)));
        Set<EffectEntry> temporaryEntries = Collections.newSetFromMap(new IdentityHashMap<>());
        resolvedTemporaryNodes.forEach(temporaryNode -> temporaryEntries.addAll(temporaryNode.effects()));
        totals.reachedCaps().forEach(reductionCap -> replaceWithCap(reductionCap, resolvedEntries, temporaryEntries));
        this.entries = List.copyOf(resolvedEntries);
        this.temporaryNodes = List.copyOf(resolvedTemporaryNodes);
        this.gatedNodes = List.copyOf(withinDamageTakenCaps(resolvedGatedNodes, permanentTotals(totals)));
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
        HullModEffect vanillaEffect = hullModSpec == null ? null : WrappingHullModEffect.unwrapped(hullModSpec.getEffect());
        if (vanillaEffect != null) {
            resolvedEntries.add(new VanillaEntry(vanillaEffect, vanillaHullModId));
        }
    }

    private static void addTemporaryNode(EffectContribution contribution, List<Entry> resolvedEntries, List<TemporaryNode> resolvedTemporaryNodes) {
        EffectModIds modIds = new EffectModIds(contribution.modIdPrefix());
        List<EffectEntry> temporaryEffects = new ArrayList<>();
        for (SkillTypeEffect effect : contribution.effects()) {
            EffectEntry effectEntry = new EffectEntry(effect.effect(), modIds.next(effect.effect()), effect.magnitude());
            temporaryEffects.add(effectEntry);
            resolvedEntries.add(effectEntry);
        }
        if (!temporaryEffects.isEmpty()) {
            resolvedTemporaryNodes.add(new TemporaryNode(contribution.temporarySeconds(), List.copyOf(temporaryEffects)));
        }
    }

    private static Map<SkillEffect, Float> permanentTotals(EffectTotals totals) {
        for (EffectTotals.Group group : totals.groups()) {
            if (group.temporarySeconds() == null && group.gate() == null) {
                return group.totals();
            }
        }
        return Map.of();
    }

    static List<GatedNode> withinDamageTakenCaps(List<GatedNode> gatedNodes, Map<SkillEffect, Float> permanentTotals) {
        List<GatedNode> limited = new ArrayList<>(gatedNodes);
        for (CombatGate gate : CombatGate.values()) {
            for (DamageTakenCaps.Cap cap : DamageTakenCaps.CAPS) {
                float scale = capScale(gatedNodes, gate, cap, permanentTotals);
                if (scale < 1f) {
                    limited.replaceAll(node -> node.gate() == gate ? scaled(node, cap, scale) : node);
                }
            }
        }
        return limited;
    }

    private static float capScale(List<GatedNode> gatedNodes, CombatGate gate, DamageTakenCaps.Cap cap, Map<SkillEffect, Float> permanentTotals) {
        if (!exceedsWith(gatedNodes, gate, cap, permanentTotals, 1f)) {
            return 1f;
        }
        float low = 0f;
        float high = 1f;
        for (int step = 0; step < CAP_SCALE_STEPS; step++) {
            float middle = (low + high) / 2f;
            if (exceedsWith(gatedNodes, gate, cap, permanentTotals, middle)) {
                high = middle;
            } else {
                low = middle;
            }
        }
        return low;
    }

    private static boolean exceedsWith(List<GatedNode> gatedNodes, CombatGate gate, DamageTakenCaps.Cap cap, Map<SkillEffect, Float> permanentTotals,
                                       float scale) {
        float percentTotal = 0f;
        float multiplierProduct = 1f;
        for (SkillEffect effect : cap.contributingEffects()) {
            float permanent = permanentTotals.getOrDefault(effect, 0f);
            if (effect.isMultiplicative()) {
                multiplierProduct *= 1f + permanent / 100f;
            } else {
                percentTotal += permanent;
            }
        }
        for (GatedNode node : gatedNodes) {
            if (node.gate() != gate) {
                continue;
            }
            for (EffectEntry entry : node.effects()) {
                if (cap.contributingEffects().contains(entry.effect()) && entry.magnitude() < 0f) {
                    if (entry.effect().isMultiplicative()) {
                        multiplierProduct *= 1f + entry.magnitude() * scale / 100f;
                    } else {
                        percentTotal += entry.magnitude() * scale;
                    }
                }
            }
        }
        return DamageTakenCaps.exceedsCap(percentTotal, multiplierProduct);
    }

    private static GatedNode scaled(GatedNode node, DamageTakenCaps.Cap cap, float scale) {
        List<EffectEntry> effects = new ArrayList<>(node.effects().size());
        for (EffectEntry entry : node.effects()) {
            boolean limited = cap.contributingEffects().contains(entry.effect()) && entry.magnitude() < 0f;
            effects.add(limited ? new EffectEntry(entry.effect(), entry.modId(), entry.magnitude() * scale) : entry);
        }
        return new GatedNode(node.gate(), List.copyOf(effects));
    }

    private static void addGatedNode(EffectContribution contribution, List<GatedNode> resolvedGatedNodes) {
        EffectModIds modIds = new EffectModIds(contribution.modIdPrefix());
        List<EffectEntry> gatedEffects = new ArrayList<>();
        for (SkillTypeEffect effect : contribution.effects()) {
            gatedEffects.add(new EffectEntry(effect.effect(), modIds.next(effect.effect()), effect.magnitude()));
        }
        if (!gatedEffects.isEmpty()) {
            resolvedGatedNodes.add(new GatedNode(contribution.gate(), List.copyOf(gatedEffects)));
        }
    }

    private static void addEffects(EffectContribution contribution, List<Entry> resolvedEntries) {
        EffectModIds modIds = new EffectModIds(contribution.modIdPrefix());
        for (SkillTypeEffect effect : contribution.effects()) {
            if (!effect.effect().isMultiplicative()) {
                resolvedEntries.add(new EffectEntry(effect.effect(), modIds.next(effect.effect()), effect.magnitude()));
            }
        }
    }

    private static final class EffectModIds {

        private final String prefix;
        private final Map<SkillEffect, Integer> uses = new HashMap<>();

        EffectModIds(String prefix) {
            this.prefix = prefix;
        }

        String next(SkillEffect effect) {
            int use = uses.merge(effect, 1, Integer::sum);
            return use == 1 ? prefix + effect.name() : prefix + effect.name() + "_" + use;
        }
    }

    static ResolvedTree of(ShipEffectContext context, StatBaseline statBaseline, DiminishingReturns.Config diminishing) {
        return of(context, statBaseline, diminishing, ShipEffectSources.all());
    }

    static ResolvedTree of(ShipEffectContext context, StatBaseline statBaseline, DiminishingReturns.Config diminishing,
                           List<ShipEffectSource> sources) {
        if (context.shipData() == null) {
            return null;
        }
        float bonusScale = 1f;
        for (ShipEffectSource source : sources) {
            bonusScale *= source.bonusScale(context);
        }
        return of(context, bonusScale, statBaseline, diminishing, sources);
    }

    static ResolvedTree of(ShipEffectContext context, float bonusScale, StatBaseline statBaseline, DiminishingReturns.Config diminishing,
                           List<ShipEffectSource> sources) {
        ShipSkillData shipData = context.shipData();
        if (shipData == null) {
            return null;
        }
        List<Object> sourceCacheKeys = new ArrayList<>(sources.size());
        sources.forEach(source -> sourceCacheKeys.add(source.cacheKey(context)));
        ResolvedTree cachedTree = CACHE.get(shipData);
        if (cachedTree != null && cachedTree.sources.equals(sources) && cachedTree.shipDataRevision == shipData.revision()
                && cachedTree.hullSize == context.hullSize() && Float.compare(cachedTree.bonusScale, bonusScale) == 0
                && cachedTree.sourceCacheKeys.equals(sourceCacheKeys)
                && cachedTree.statBaseline.equals(statBaseline) && cachedTree.diminishing.equals(diminishing)) {
            return cachedTree;
        }
        ResolvedTree resolvedTree = new ResolvedTree(context, sources, bonusScale, sourceCacheKeys, statBaseline, diminishing);
        CACHE.put(shipData, resolvedTree);
        return resolvedTree;
    }

    public static void clearCache() {
        CACHE.clear();
    }

    public HullSize hullSize() {
        return hullSize;
    }

    public EffectTotals totals() {
        return totals;
    }

    public List<EffectContribution> contributions() {
        return totals.contributions();
    }

    // each key is only ever stored with the one type its supplier returns
    @SuppressWarnings("unchecked")
    public <T> T derived(Object key, Supplier<T> derivation) {
        Object value = derived.get(key);
        if (value == null) {
            value = derivation.get();
            derived.put(key, value);
        }
        return (T) value;
    }

    public Map<String, AppliedModifier> appliedModifiers() {
        return derived(APPLIED_MODIFIERS_KEY, () -> appliedModifiersOf(totals));
    }

    static Map<String, AppliedModifier> appliedModifiersOf(EffectTotals totals) {
        Map<String, AppliedModifier> modifiers = new LinkedHashMap<>();
        Map<SkillEffect, List<EffectContribution>> pooledSources = new LinkedHashMap<>();
        for (EffectContribution contribution : totals.contributions()) {
            if (contribution.gate() != null) {
                continue;
            }
            if (contribution.vanillaHullModId() != null) {
                modifiers.putIfAbsent(contribution.vanillaHullModId(), new AppliedModifier(contribution.vanillaHullModId(), null, List.of(contribution), false));
            }
            boolean temporaryNode = contribution.vanillaHullModId() == null && !contribution.permanent();
            EffectModIds modIds = new EffectModIds(contribution.modIdPrefix());
            for (SkillTypeEffect effect : contribution.effects()) {
                if (temporaryNode || !effect.effect().isMultiplicative()) {
                    String modId = modIds.next(effect.effect());
                    modifiers.put(modId, new AppliedModifier(modId, effect.effect(), List.of(contribution), false));
                } else if (contribution.permanent()) {
                    pooledSources.computeIfAbsent(effect.effect(), key -> new ArrayList<>()).add(contribution);
                }
            }
        }
        pooledSources.forEach((effect, sources) -> {
            String modId = MULTIPLIER_MOD_ID_PREFIX + effect.name();
            modifiers.put(modId, new AppliedModifier(modId, effect, List.copyOf(sources), false));
        });
        for (DamageTakenCaps.Cap reductionCap : totals.reachedCaps()) {
            List<EffectContribution> sources = totals.contributions().stream().filter(contribution -> contribution.permanent()
                    && contribution.effects().stream().anyMatch(effect -> reductionCap.contributingEffects().contains(effect.effect()))).toList();
            String modId = REDUCTION_CAP_MOD_ID_PREFIX + reductionCap.cappedEffect().name();
            modifiers.put(modId, new AppliedModifier(modId, reductionCap.cappedEffect(), sources, true));
        }
        return Collections.unmodifiableMap(modifiers);
    }

    List<Entry> entries() {
        return entries;
    }

    List<TemporaryNode> temporaryNodes() {
        return temporaryNodes;
    }

    List<GatedNode> gatedNodes() {
        return gatedNodes;
    }
}
