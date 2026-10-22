package exiledsector.effects;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.skills.AllocatedNode;
import exiledsector.skills.DamageTakenCaps;
import exiledsector.skills.EffectTotals;
import exiledsector.skills.InstalledHullMods;
import exiledsector.skills.ShipFacts;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.skilleffect.ModuleShare;
import exiledsector.skills.skilleffect.SkillEffect;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ModuleTreeShare implements AdvanceableListener {

    static final String MOD_ID_PREFIX = "exiledSector_moduleShare_";
    static final float DEFAULT_SHARE = 0.5f;

    private final ShipAPI parentShip;
    private final List<EffectTotals.NodeEffects> parentNodeEffects;
    private final HullSize parentHullSize;
    private boolean shared;

    ModuleTreeShare(ShipAPI parentShip, List<EffectTotals.NodeEffects> parentNodeEffects, HullSize parentHullSize) {
        this.parentShip = parentShip;
        this.parentNodeEffects = parentNodeEffects;
        this.parentHullSize = parentHullSize;
    }

    static void attach(ShipAPI ship, ResolvedTree resolvedTree) {
        if (share() <= 0f || ship.isStation() || ship.isStationModule() || ship.getHullSpec() == null
                || !ShipFacts.hasModuleSlots(ship.getHullSpec())) {
            return;
        }
        List<ModuleTreeShare> existing = ship.getListeners(ModuleTreeShare.class);
        if (existing == null || existing.isEmpty()) {
            ship.addListener(new ModuleTreeShare(ship, resolvedTree.nodeEffects(), resolvedTree.hullSize()));
        }
    }

    // TODO step 5: read the LunaLib setting exiledSector_moduleTreeShare
    static float share() {
        return DEFAULT_SHARE;
    }

    @Override
    public void advance(float amount) {
        if (shared) return;
        shared = true;
        List<ShipAPI> modules = parentShip.getChildModulesCopy();
        if (modules == null) return;
        for (ShipAPI module : modules) {
            if (module.isAlive() && !module.isHulk() && module.getVariant() != null) {
                push(module, sharedTotals(parentNodeEffects, parentHullSize, module.getVariant(), share()));
            }
        }
    }

    // TODO step 4: rebuild hull, armor, ammo, shield arc and PD reclassify for the SHARE_REBUILD effects
    private void push(ShipAPI module, Map<SkillEffect, Float> totals) {
        MutableShipStatsAPI stats = module.getMutableStats();
        String modIdPrefix = MOD_ID_PREFIX + parentShip.getId() + "_";
        totals.forEach((effect, magnitude) -> {
            if (effect.moduleShare() == ModuleShare.SHARE && !effect.appliesAfterOtherEffects()) {
                effect.apply(stats, modIdPrefix + effect.name(), magnitude);
            }
        });
        totals.forEach((effect, magnitude) -> {
            if (effect.moduleShare() == ModuleShare.SHARE && effect.appliesAfterOtherEffects()) {
                effect.apply(stats, modIdPrefix + effect.name(), magnitude);
            }
        });
    }

    static Map<SkillEffect, Float> sharedTotals(List<EffectTotals.NodeEffects> nodeEffects, HullSize hullSize, ShipVariantAPI moduleVariant,
                                                float share) {
        Map<SkillEffect, Float> sums = new LinkedHashMap<>();
        for (EffectTotals.NodeEffects node : nodeEffects) {
            if (!node.node().effectiveType().sharesWithModules(hullSize) || hasOwnCopy(moduleVariant, node.node())) {
                continue;
            }
            for (SkillTypeEffect effect : node.effects()) {
                if (effect.effect().moduleShare().isShared()) {
                    sums.merge(effect.effect(), effect.magnitude(), Float::sum);
                }
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
