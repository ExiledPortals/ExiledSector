package exiledsector.effects;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledlib.skills.AffinitySteps;
import exiledlib.skills.DamageTakenCaps;
import exiledlib.skills.EffectTotals;
import exiledlib.skills.ShipFacts;
import exiledlib.skills.SkillTypeEffect;
import exiledlib.skills.skilleffect.SkillEffect;
import exiledlib.skills.skilleffect.hullmodule.ModuleBoost;
import exiledsector.skills.AllocatedNode;
import exiledsector.skills.InstalledHullMods;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ModuleTreeShare implements AdvanceableListener {

    static final String MOD_ID_PREFIX = "exiledSector_moduleShare_";

    private final ShipAPI parentShip;
    private final List<SkillTreeEffectSource.NodeEffects> parentNodeEffects;
    private final HullSize parentHullSize;
    private boolean shared;

    ModuleTreeShare(ShipAPI parentShip, List<SkillTreeEffectSource.NodeEffects> parentNodeEffects, HullSize parentHullSize) {
        this.parentShip = parentShip;
        this.parentNodeEffects = parentNodeEffects;
        this.parentHullSize = parentHullSize;
    }

    static void attach(ShipAPI ship, ResolvedTree resolvedTree) {
        if (ship.isStation() || ship.isStationModule() || ship.getHullSpec() == null || !ShipFacts.hasModuleSlots(ship.getHullSpec())
                || (share() <= 0f && !ModuleBoost.hasModuleBonus(ship.getMutableStats()))) {
            return;
        }
        List<ModuleTreeShare> existing = ship.getListeners(ModuleTreeShare.class);
        if (existing == null || existing.isEmpty()) {
            ship.addListener(new ModuleTreeShare(ship, SkillTreeEffectSource.nodeEffects(resolvedTree), resolvedTree.hullSize()));
        }
    }

    static float share() {
        return ModuleShareSetting.share();
    }

    @Override
    public void advance(float amount) {
        if (shared) return;
        shared = true;
        List<ShipAPI> modules = parentShip.getChildModulesCopy();
        if (modules == null) return;
        float share = share();
        String modIdPrefix = MOD_ID_PREFIX + parentShip.getId() + "_";
        for (ShipAPI module : modules) {
            if (module.isAlive() && !module.isHulk() && module.getVariant() != null && module.getHullSpec() != null) {
                Map<SkillEffect, Float> totals = share > 0f ? sharedTotals(parentNodeEffects, parentHullSize, module.getVariant(), share) : Map.of();
                ModuleBoost.boost(parentShip, module, totals, modIdPrefix);
            }
        }
    }

    static Map<SkillEffect, Float> sharedTotals(List<SkillTreeEffectSource.NodeEffects> nodeEffects, HullSize hullSize, ShipVariantAPI moduleVariant,
                                                float share) {
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
                sums.merge(bonus.effect(), bonus.magnitude(), Float::sum);
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
