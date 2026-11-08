package exiledsector.effects;

import com.fs.starfarer.api.campaign.CampaignUIAPI.CoreUITradeMode;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.persistence.ShipRecords;
import exiledsector.skills.DamageTakenCaps;
import exiledsector.skills.EffectTotals;
import exiledsector.skills.skilleffect.SkillEffect;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class ModuleHullMod extends BaseHullMod {

    public static final String ID = "exiledSector_module";
    static final String MOD_ID_PREFIX = "exiledSector_moduleShare_";

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        applyBeforeCreation(stats, ShipEffectSources.all());
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI module, String id) {
        applyAfterCreation(module, ShipEffectSources.all());
    }

    static void applyBeforeCreation(MutableShipStatsAPI stats, List<ShipEffectSource> sources) {
        ModuleParent parent = parentOf(stats.getFleetMember(), stats.getVariant());
        if (parent == null) return;

        Map<SkillEffect, Float> totals = moduleTotals(parent, stats.getVariant(), sources);
        String modIdPrefix = modIdPrefix(parent);
        applyTotals(stats, totals, modIdPrefix, false);
        applyTotals(stats, totals, modIdPrefix, true);
        sources.forEach(source -> source.applyToModule(parent.context(), stats));
    }

    static void applyAfterCreation(ShipAPI module, List<ShipEffectSource> sources) {
        ModuleParent parent = parentOf(module.getMutableStats().getFleetMember(), module.getVariant());
        if (parent == null) return;

        String modIdPrefix = modIdPrefix(parent);
        Map<SkillEffect, Float> totals = moduleTotals(parent, module.getVariant(), sources);
        finishModule(module, totals, modIdPrefix, SkillEffect::reshapesDealtDamage);
        finishModule(module, totals, modIdPrefix, effect -> !effect.reshapesDealtDamage() && !effect.readsFinalDealtDamage());
        finishModule(module, totals, modIdPrefix, SkillEffect::readsFinalDealtDamage);
        sources.forEach(source -> source.afterModuleCreation(parent.context(), module));
    }

    private static void finishModule(ShipAPI module, Map<SkillEffect, Float> totals, String modIdPrefix, Predicate<SkillEffect> inThisPass) {
        totals.forEach((effect, magnitude) -> {
            if (inThisPass.test(effect)) {
                effect.applyAfterShipCreation(module, modIdPrefix + effect.name(), magnitude);
            }
        });
    }

    private static ModuleParent parentOf(FleetMemberAPI moduleMember, ShipVariantAPI moduleVariant) {
        return ShipRecords.taggedTree(moduleVariant) == null ? ModuleParents.find(moduleMember, moduleVariant) : null;
    }

    private static Map<SkillEffect, Float> moduleTotals(ModuleParent parent, ShipVariantAPI moduleVariant, List<ShipEffectSource> sources) {
        Map<SkillEffect, Float> sums = new LinkedHashMap<>();
        for (ShipEffectSource source : sources) {
            source.moduleContributions(parent.context(), parent.resolvedTree(), moduleVariant).forEach((effect, total) -> sums.merge(effect, total, Float::sum));
        }
        Map<SkillEffect, Float> totals = EffectTotals.permanentTotals(sums);
        return DamageTakenCaps.capped(totals, DamageTakenCaps.reachedBy(totals));
    }

    private static void applyTotals(MutableShipStatsAPI stats, Map<SkillEffect, Float> totals, String modIdPrefix, boolean afterOtherEffects) {
        totals.forEach((effect, magnitude) -> {
            if (effect.appliesAfterOtherEffects() == afterOtherEffects) {
                effect.apply(stats, modIdPrefix + effect.name(), magnitude);
            }
        });
    }

    private static String modIdPrefix(ModuleParent parent) {
        return MOD_ID_PREFIX + parent.memberId() + "_";
    }

    @Override
    public boolean canBeAddedOrRemovedNow(ShipAPI ship, MarketAPI marketOrNull, CoreUITradeMode mode) {
        return false;
    }
}
