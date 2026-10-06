package exiledsector.effects;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI.CoreUITradeMode;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.HullModFleetEffect;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
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
        ShipSkillData data = SkillDataResolver.resolve(stats.getFleetMember(), stats.getVariant());
        if (data == null) return;

        ResolvedTree tree = ResolvedTree.of(data, hullSize);
        for (ResolvedTree.Entry entry : tree.entries()) {
            if (entry instanceof ResolvedTree.VanillaEntry vanilla) {
                vanilla.effect().applyEffectsBeforeShipCreation(hullSize, stats, vanilla.hullModId());
            } else if (entry instanceof ResolvedTree.EffectEntry effect && !effect.effect().appliesAfterOtherEffects()) {
                effect.effect().apply(stats, effect.modId(), effect.magnitude());
            }
        }
        for (ResolvedTree.Entry entry : tree.entries()) {
            if (entry instanceof ResolvedTree.EffectEntry effect && effect.effect().appliesAfterOtherEffects()) {
                effect.effect().apply(stats, effect.modId(), effect.magnitude());
            }
        }
        if (isOpCostPass(stats)) return;
        boolean npcTree = SkillDataResolver.isNpcTree(stats.getVariant());
        if (!npcTree) {
            SkillDataResolver.syncShipTag(stats.getFleetMember(), stats.getVariant());
            OpReserveHullMods.sync(stats.getFleetMember(), stats.getVariant());
        }
        PhantomInstallSync.sync(tree.phantomHullModIds(), stats.getVariant());
        if (npcTree) {
            HullModConflictResolver.removeHullModsThatTriedToStripAPhantom(tree.allocated(), stats.getVariant(), false);
        } else {
            HullModConflictResolver.removeConflicts(tree.allocated(), stats.getVariant());
        }
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ResolvedTree tree = treeFor(ship);
        if (tree == null) return;

        for (ResolvedTree.Entry entry : tree.entries()) {
            if (entry instanceof ResolvedTree.VanillaEntry vanilla) {
                vanilla.effect().applyEffectsAfterShipCreation(ship, vanilla.hullModId());
            } else if (entry instanceof ResolvedTree.EffectEntry effect) {
                effect.effect().applyAfterShipCreation(ship, effect.modId(), effect.magnitude());
            }
        }
        HullModConflictResolver.removeHullModsThatTriedToStripAPhantom(tree.allocated(), ship.getVariant(),
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
        ShipTreeLookup.ShipTree tree = ShipTreeLookup.forShip(member, ship.getVariant());
        if (tree == null) {
            I18n.forGameText(() -> VanillaText.addPara(tooltip, Translation.styled("hullmod.exiledSector_core.noTree"), TOOLTIP_PAD,
                    Misc.getGrayColor()));
            return;
        }
        ShipTreeSummaryRenderer.render(tooltip, tree, hullSize, TOOLTIP_PAD, MAX_TOOLTIP_BONUS_LINES);
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
        ResolvedTree tree = treeFor(ship);
        if (tree == null) return;

        for (ResolvedTree.Entry entry : tree.entries()) {
            if (entry instanceof ResolvedTree.VanillaEntry vanilla) {
                vanilla.effect().applyEffectsToFighterSpawnedByShip(fighter, ship, vanilla.hullModId());
            } else if (entry instanceof ResolvedTree.EffectEntry effect) {
                effect.effect().applyToFighterSpawnedByShip(fighter, ship, effect.modId(), effect.magnitude());
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
        if (customData != null && customData.get(COMBAT_PLAN_KEY) instanceof ShipCombatPlan plan) {
            return plan;
        }
        ShipCombatPlan plan = buildCombatPlan(treeFor(ship));
        ship.setCustomData(COMBAT_PLAN_KEY, plan);
        return plan;
    }

    private static ShipCombatPlan buildCombatPlan(ResolvedTree tree) {
        ShipCombatPlan plan = new ShipCombatPlan();
        if (tree == null) return plan;

        for (ResolvedTree.Entry entry : tree.entries()) {
            if (entry instanceof ResolvedTree.VanillaEntry vanilla) {
                plan.addVanillaEffect(vanilla.effect());
            } else if (entry instanceof ResolvedTree.EffectEntry effect && effect.effect().advancesInCombat()) {
                plan.addCombatUpdate(effect);
            }
        }
        for (ResolvedTree.TemporaryNode node : tree.temporaryNodes()) {
            plan.addTemporaryNode(node.durationSeconds(), node.effects());
        }
        return plan;
    }

    private static ResolvedTree treeFor(ShipAPI ship) {
        ShipSkillData data = SkillDataResolver.resolve(ship.getMutableStats().getFleetMember(), ship.getVariant());
        return ResolvedTree.of(data, ship.getHullSize());
    }
}
