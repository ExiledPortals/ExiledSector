package exiledsector.effects;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI.CoreUITradeMode;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.HullModFleetEffect;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillDataResolver;
import exiledsector.skills.skilleffect.FleetWideEffects;
import exiledsector.ui.VanillaText;
import exiledsector.ui.inspect.ShipTreeLookup;
import exiledsector.ui.inspect.ShipTreeSummaryRenderer;
import exiledsector.ui.refit.PhantomHullModRefitHider;

import java.util.Map;

public class SkillTreeHullMod extends BaseHullMod implements HullModFleetEffect {

    public static final String ID = "exiledSector_core";

    private static final float TOOLTIP_PAD = 10f;
    private static final float TOOLTIP_WIDTH = 480f;
    private static final int MAX_TOOLTIP_BONUS_LINES = 25;
    private static final int DISPLAY_SORT_ORDER = 0;
    private static final String COMBAT_PLAN_KEY = "exiledSector_combatPlan";

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        ShipSkillData shipData = SkillDataResolver.resolve(stats.getFleetMember(), stats.getVariant());
        if (shipData == null) return;

        ResolvedTree resolvedTree = ResolvedTree.of(shipData, hullSize, bonusScale(stats.getVariant()));
        for (ResolvedTree.Entry entry : resolvedTree.entries()) {
            if (entry instanceof ResolvedTree.VanillaEntry vanillaEntry) {
                vanillaEntry.effect().applyEffectsBeforeShipCreation(hullSize, stats, vanillaEntry.hullModId());
            } else if (entry instanceof ResolvedTree.EffectEntry effectEntry && !effectEntry.effect().appliesAfterOtherEffects()) {
                effectEntry.effect().apply(stats, effectEntry.modId(), effectEntry.magnitude());
            }
        }
        for (ResolvedTree.Entry entry : resolvedTree.entries()) {
            if (entry instanceof ResolvedTree.EffectEntry effectEntry && effectEntry.effect().appliesAfterOtherEffects()) {
                effectEntry.effect().apply(stats, effectEntry.modId(), effectEntry.magnitude());
            }
        }
        if (isOpCostPass(stats)) return;
        boolean isNpcTree = SkillDataResolver.isNpcTree(stats.getVariant());
        if (!isNpcTree) {
            SkillDataResolver.syncShipTag(stats.getFleetMember(), stats.getVariant());
            OpReserveHullMods.sync(stats.getFleetMember(), stats.getVariant());
        }
        PhantomInstallSync.sync(resolvedTree.phantomHullModIds(), stats.getVariant());
        if (isNpcTree) {
            HullModConflictResolver.removeHullModsThatTriedToStripAPhantom(resolvedTree.allocated(), stats.getVariant(), false);
        } else {
            HullModConflictResolver.removeConflicts(resolvedTree.allocated(), stats.getVariant());
        }
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ResolvedTree resolvedTree = treeFor(ship);
        if (resolvedTree == null) return;

        for (ResolvedTree.Entry entry : resolvedTree.entries()) {
            if (entry instanceof ResolvedTree.VanillaEntry vanillaEntry) {
                vanillaEntry.effect().applyEffectsAfterShipCreation(ship, vanillaEntry.hullModId());
            } else if (entry instanceof ResolvedTree.EffectEntry effectEntry) {
                effectEntry.effect().applyAfterShipCreation(ship, effectEntry.modId(), effectEntry.magnitude());
            }
        }
        HullModConflictResolver.removeHullModsThatTriedToStripAPhantom(resolvedTree.allocated(), ship.getVariant(),
                !SkillDataResolver.isNpcTree(ship.getVariant()));
    }

    @Override
    public boolean affectsOPCosts() {
        return true;
    }

    static boolean isOpCostPass(MutableShipStatsAPI stats) {
        return stats.getFleetMember() == null;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        if (ship == null || isForModSpec) return;

        FleetMemberAPI member = ship.getFleetMember() != null ? ship.getFleetMember() : ship.getMutableStats().getFleetMember();
        ShipTreeLookup.ShipTree shipTree = ShipTreeLookup.forShip(member, ship.getVariant());
        if (shipTree == null) {
            I18n.forGameText(() -> VanillaText.addPara(tooltip, Translation.styled("hullmod.exiledSector_core.noTree"), TOOLTIP_PAD,
                    Misc.getGrayColor()));
            return;
        }
        ShipTreeSummaryRenderer.render(tooltip, shipTree, hullSize, TOOLTIP_PAD, MAX_TOOLTIP_BONUS_LINES);
    }

    @Override
    public float getTooltipWidth() {
        return TOOLTIP_WIDTH;
    }

    @Override
    public boolean canBeAddedOrRemovedNow(ShipAPI ship, MarketAPI marketOrNull, CoreUITradeMode mode) {
        return false;
    }

    @Override
    public String getCanNotBeInstalledNowReason(ShipAPI ship, MarketAPI marketOrNull, CoreUITradeMode mode) {
        return I18n.forGameText(() -> Translation.text("hullmod.exiledSector_core.locked"));
    }

    @Override
    public int getDisplaySortOrder() {
        return DISPLAY_SORT_ORDER;
    }

    @Override
    public int getDisplayCategoryIndex() {
        PhantomHullModRefitHider.requestRefresh();
        return super.getDisplayCategoryIndex();
    }

    @Override
    public void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship, String id) {
        ResolvedTree resolvedTree = treeFor(ship);
        if (resolvedTree == null) return;

        for (ResolvedTree.Entry entry : resolvedTree.entries()) {
            if (entry instanceof ResolvedTree.VanillaEntry vanillaEntry) {
                vanillaEntry.effect().applyEffectsToFighterSpawnedByShip(fighter, ship, vanillaEntry.hullModId());
            } else if (entry instanceof ResolvedTree.EffectEntry effectEntry) {
                effectEntry.effect().applyToFighterSpawnedByShip(fighter, ship, effectEntry.modId(), effectEntry.magnitude());
            }
        }
    }

    @Override
    public boolean withOnFleetSync() {
        return true;
    }

    @Override
    public void onFleetSync(CampaignFleetAPI fleet) {
        if (fleet != null && fleet.isPlayerFleet()) {
            FleetWideEffects.markPhaseFieldStale();
        }
        FleetWideEffects.applyAlwaysCountingSensorStrength(fleet);
    }

    @Override
    public boolean withAdvanceInCampaign() {
        return false;
    }

    // withAdvanceInCampaign() returns false, so the engine never calls this
    @Override
    @SuppressWarnings("java:S1186")
    public void advanceInCampaign(CampaignFleetAPI fleet) {
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        combatPlanFor(ship).advance(ship, amount);
    }

    private ShipCombatPlan combatPlanFor(ShipAPI ship) {
        Map<String, Object> customData = ship.getCustomData();
        if (customData != null && customData.get(COMBAT_PLAN_KEY) instanceof ShipCombatPlan combatPlan) {
            return combatPlan;
        }
        ShipCombatPlan combatPlan = buildCombatPlan(treeFor(ship));
        ship.setCustomData(COMBAT_PLAN_KEY, combatPlan);
        return combatPlan;
    }

    private static ShipCombatPlan buildCombatPlan(ResolvedTree resolvedTree) {
        ShipCombatPlan combatPlan = new ShipCombatPlan();
        if (resolvedTree == null) return combatPlan;

        for (ResolvedTree.Entry entry : resolvedTree.entries()) {
            if (entry instanceof ResolvedTree.VanillaEntry vanillaEntry) {
                combatPlan.addVanillaEffect(vanillaEntry.effect());
            } else if (entry instanceof ResolvedTree.EffectEntry effectEntry && effectEntry.effect().advancesInCombat()) {
                combatPlan.addCombatUpdate(effectEntry);
            }
        }
        for (ResolvedTree.TemporaryNode temporaryNode : resolvedTree.temporaryNodes()) {
            combatPlan.addTemporaryNode(temporaryNode.durationSeconds(), temporaryNode.effects());
        }
        return combatPlan;
    }

    private static ResolvedTree treeFor(ShipAPI ship) {
        ShipVariantAPI variant = ship.getVariant();
        ShipSkillData shipData = SkillDataResolver.resolve(ship.getMutableStats().getFleetMember(), variant);
        return ResolvedTree.of(shipData, ship.getHullSize(), bonusScale(variant));
    }

    private static float bonusScale(ShipVariantAPI variant) {
        return SkillDataResolver.isNpcTree(variant) ? NpcBonusScale.current() : 1f;
    }
}
