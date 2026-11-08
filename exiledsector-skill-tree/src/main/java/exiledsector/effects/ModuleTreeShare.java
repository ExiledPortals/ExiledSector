package exiledsector.effects;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import exiledsector.skills.AffinitySteps;
import exiledsector.skills.DamageTakenCaps;
import exiledsector.skills.EffectContribution;
import exiledsector.skills.EffectTotals;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.skills.skilleffect.defense.DefenseSkillEffect;
import exiledsector.skills.skilleffect.stationmodule.StationModuleSkillEffect;
import exiledsector.skills.AllocatedNode;
import exiledsector.skills.InstalledHullMods;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ModuleTreeShare {

    private static final Object AFFINITY_BONUS_RATIOS_KEY = new Object();
    private static final Map<StationModuleSkillEffect, SkillEffect> MODULE_BONUS_TARGETS = Map.of(
            StationModuleSkillEffect.MODULE_HULL_PERCENT, DefenseSkillEffect.HULL_PERCENT,
            StationModuleSkillEffect.MODULE_ARMOR_PERCENT, DefenseSkillEffect.ARMOR_PERCENT);

    private ModuleTreeShare() {
    }

    static Map<SkillEffect, Float> moduleTotals(ShipEffectContext parentContext, ResolvedTree parentTree, ShipVariantAPI moduleVariant) {
        Map<SkillEffect, Float> totals = new LinkedHashMap<>();
        float share = share(parentContext);
        if (share > 0f) {
            totals.putAll(sharedTotals(SkillTreeEffectSource.nodeEffects(parentTree), integratedNodeIds(parentContext),
                    parentTree.derived(AFFINITY_BONUS_RATIOS_KEY, () -> affinityBonusRatios(parentTree.contributions())),
                    parentTree.hullSize(), moduleVariant, share));
        }
        Map<SkillEffect, Float> parentTotals = parentTree.permanentTotals();
        MODULE_BONUS_TARGETS.forEach((moduleBonus, moduleStat) -> {
            float moduleBonusTotal = parentTotals.getOrDefault(moduleBonus, 0f);
            if (moduleBonusTotal != 0f) {
                totals.merge(moduleStat, moduleBonusTotal, Float::sum);
            }
        });
        return totals;
    }

    static float share(ShipEffectContext parentContext) {
        return ModuleShareSetting.shareFor(parentContext.member() != null && parentContext.member().isStation());
    }

    private static Set<String> integratedNodeIds(ShipEffectContext parentContext) {
        return parentContext.shipData() == null ? Set.of() : Set.copyOf(parentContext.shipData().getIntegratedNodeIds());
    }

    static Map<SkillEffect, Float> affinityBonusRatios(List<EffectContribution> resolvedContributions) {
        Map<SkillEffect, Float> resolvedBonuses = new HashMap<>();
        for (EffectContribution contribution : resolvedContributions) {
            if (contribution.origin() instanceof AffinitySteps.Origin) {
                contribution.effects().forEach(bonus -> resolvedBonuses.merge(bonus.effect(), bonus.magnitude(), Float::sum));
            }
        }
        Map<SkillEffect, Float> ratios = new LinkedHashMap<>();
        for (SkillTypeEffect rawBonus : AffinitySteps.bonuses(AffinitySteps.totals(resolvedContributions))) {
            if (rawBonus.magnitude() != 0f) {
                ratios.put(rawBonus.effect(), resolvedBonuses.getOrDefault(rawBonus.effect(), 0f) / rawBonus.magnitude());
            }
        }
        for (SkillEffect affinity : AffinitySteps.affinities()) {
            AffinitySteps.stepsFor(affinity).forEach(step -> ratios.putIfAbsent(step.effect(), 0f));
        }
        return ratios;
    }

    static Map<SkillEffect, Float> sharedTotals(List<SkillTreeEffectSource.NodeEffects> nodeEffects, Set<String> integratedNodeIds,
                                                Map<SkillEffect, Float> affinityBonusRatios,
                                                HullSize hullSize, ShipVariantAPI moduleVariant, float share) {
        Map<SkillEffect, Float> sums = new LinkedHashMap<>();
        Map<SkillEffect, Float> affinityTotals = new LinkedHashMap<>();
        for (SkillTreeEffectSource.NodeEffects node : nodeEffects) {
            boolean integrated = integratedNodeIds.contains(node.node().node().getId());
            if (!node.node().effectiveType().sharesWithModules(hullSize, integrated) || hasOwnCopy(moduleVariant, node.node())) {
                continue;
            }
            for (SkillTypeEffect effect : node.effects()) {
                if (effect.effect().moduleShare().isShared()) {
                    sums.merge(effect.effect(), effect.magnitude(), Float::sum);
                } else if (AffinitySteps.isAffinity(effect.effect())) {
                    affinityTotals.merge(effect.effect(), effect.magnitude(), Float::sum);
                }
            }
        }
        for (SkillTypeEffect bonus : AffinitySteps.bonuses(affinityTotals)) {
            if (bonus.effect().moduleShare().isShared()) {
                sums.merge(bonus.effect(), bonus.magnitude() * affinityBonusRatios.getOrDefault(bonus.effect(), 1f), Float::sum);
            }
        }
        Map<SkillEffect, Float> totals = EffectTotals.permanentTotals(sums);
        Map<SkillEffect, Float> capped = DamageTakenCaps.capped(totals, DamageTakenCaps.reachedBy(totals));
        capped.replaceAll((effect, total) -> total * share);
        return capped;
    }

    private static boolean hasOwnCopy(ShipVariantAPI moduleVariant, AllocatedNode node) {
        for (String hullModId : node.exclusiveHullModIds()) {
            if (InstalledHullMods.hasHullModOfItsOwn(moduleVariant, hullModId)) {
                return true;
            }
        }
        return false;
    }
}
