package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.HullModEffect;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import exiledlib.skills.DamageTakenCaps;
import exiledlib.skills.DiminishingReturns;
import exiledlib.skills.EffectContribution;
import exiledlib.skills.EffectTotals;
import exiledlib.skills.SkillTypeEffect;
import exiledlib.skills.StatBaseline;
import exiledlib.skills.skilleffect.SkillEffect;
import exiledsector.skills.ShipSkillData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
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

    record TemporaryNode(float durationSeconds, List<EffectEntry> effects) {
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
        for (EffectContribution contribution : totals.contributions()) {
            if (contribution.vanillaHullModId() != null) {
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

    List<Entry> entries() {
        return entries;
    }

    List<TemporaryNode> temporaryNodes() {
        return temporaryNodes;
    }
}
