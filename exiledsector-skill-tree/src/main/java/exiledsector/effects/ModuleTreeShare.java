package exiledsector.effects;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledlib.skills.AffinitySteps;
import exiledlib.skills.DamageTakenCaps;
import exiledlib.skills.EffectContribution;
import exiledlib.skills.EffectTotals;
import exiledlib.skills.ShipFacts;
import exiledlib.skills.SkillTypeEffect;
import exiledlib.skills.skilleffect.SkillEffect;
import exiledlib.skills.skilleffect.hullmodule.ModuleBoost;
import exiledsector.skills.AllocatedNode;
import exiledsector.skills.InstalledHullMods;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ModuleTreeShare implements AdvanceableListener {

    static final String MOD_ID_PREFIX = "exiledSector_moduleShare_";

    private final ShipAPI parentShip;
    private final List<SkillTreeEffectSource.NodeEffects> parentNodeEffects;
    private final Map<SkillEffect, Float> affinityBonusRatios;
    private final HullSize parentHullSize;
    private boolean shared;

    ModuleTreeShare(ShipAPI parentShip, List<SkillTreeEffectSource.NodeEffects> parentNodeEffects, Map<SkillEffect, Float> affinityBonusRatios,
                    HullSize parentHullSize) {
        this.parentShip = parentShip;
        this.parentNodeEffects = parentNodeEffects;
        this.affinityBonusRatios = affinityBonusRatios;
        this.parentHullSize = parentHullSize;
    }

    static void attach(ShipAPI ship, ResolvedTree resolvedTree) {
        if (ship.isStationModule() || ship.getHullSpec() == null || !ShipFacts.hasModuleSlots(ship.getHullSpec())
                || (share(ship) <= 0f && !ModuleBoost.hasModuleBonus(ship.getMutableStats()))) {
            return;
        }
        List<ModuleTreeShare> existing = ship.getListeners(ModuleTreeShare.class);
        if (existing == null || existing.isEmpty()) {
            ship.addListener(new ModuleTreeShare(ship, SkillTreeEffectSource.nodeEffects(resolvedTree),
                    affinityBonusRatios(resolvedTree.contributions()), resolvedTree.hullSize()));
        }
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
        return ratios;
    }

    static float share(ShipAPI parentShip) {
        return ModuleShareSetting.shareFor(parentShip.isStation());
    }

    @Override
    public void advance(float amount) {
        if (shared) return;
        shared = true;
        List<ShipAPI> modules = parentShip.getChildModulesCopy();
        if (modules == null) return;
        float share = share(parentShip);
        String modIdPrefix = MOD_ID_PREFIX + parentShip.getId() + "_";
        for (ShipAPI module : modules) {
            if (module.isAlive() && !module.isHulk() && module.getVariant() != null && module.getHullSpec() != null) {
                Map<SkillEffect, Float> totals = share > 0f
                        ? sharedTotals(parentNodeEffects, affinityBonusRatios, parentHullSize, module.getVariant(), share) : Map.of();
                ModuleBoost.boost(parentShip, module, totals, modIdPrefix);
            }
        }
    }

    static Map<SkillEffect, Float> sharedTotals(List<SkillTreeEffectSource.NodeEffects> nodeEffects, Map<SkillEffect, Float> affinityBonusRatios,
                                                HullSize hullSize, ShipVariantAPI moduleVariant, float share) {
        Map<SkillEffect, Float> sums = new LinkedHashMap<>();
        Map<SkillEffect, Float> affinityTotals = new LinkedHashMap<>();
        for (SkillTreeEffectSource.NodeEffects node : nodeEffects) {
            if (!node.node().effectiveType().sharesWithModules(hullSize) || hasOwnCopy(moduleVariant, node.node())) {
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
