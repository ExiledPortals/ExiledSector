package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI.CoreUITradeMode;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.HullModEffect;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.InstalledHullMods;
import exiledsector.ui.VanillaText;
import exiledsector.ui.refit.PhantomHullModRefitHider;

import java.awt.Color;

public final class PhantomHullModEffect extends BaseHullMod {

    private HullModEffect originalEffect = new BaseHullMod();
    private boolean wrapsOriginal;
    private String hullModId;
    private String installedTag;

    @Override
    public void init(HullModSpecAPI spec) {
        super.init(spec);
        hullModId = spec.getId();
        installedTag = InstalledHullMods.tag(hullModId);
        HullModEffect createdOriginal = PhantomHullMods.createOriginal(hullModId);
        if (createdOriginal != null) {
            originalEffect = createdOriginal;
            wrapsOriginal = true;
        }
        originalEffect.init(spec);
    }

    boolean wrapsOriginal() {
        return wrapsOriginal;
    }

    HullModEffect original() {
        return originalEffect;
    }

    private boolean isPhantom(ShipVariantAPI variant) {
        return variant != null && installedTag != null && variant.hasTag(installedTag);
    }

    private boolean isPhantom(ShipAPI ship) {
        return ship != null && isPhantom(ship.getVariant());
    }

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        if (!isPhantom(stats.getVariant())) {
            originalEffect.applyEffectsBeforeShipCreation(hullSize, stats, id);
        }
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (!isPhantom(ship)) {
            originalEffect.applyEffectsAfterShipCreation(ship, id);
        }
    }

    @Override
    public void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship, String id) {
        if (!isPhantom(ship)) {
            originalEffect.applyEffectsToFighterSpawnedByShip(fighter, ship, id);
        }
    }

    @Override
    public void applyEffectsAfterShipAddedToCombatEngine(ShipAPI ship, String id) {
        if (!isPhantom(ship)) {
            originalEffect.applyEffectsAfterShipAddedToCombatEngine(ship, id);
        }
    }

    @Override
    public void advanceInCampaign(FleetMemberAPI member, float amount) {
        if (member == null || !isPhantom(member.getVariant())) {
            originalEffect.advanceInCampaign(member, amount);
        }
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (!isPhantom(ship)) {
            originalEffect.advanceInCombat(ship, amount);
        }
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return isPhantom(ship) || originalEffect.isApplicableToShip(ship);
    }

    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        return isPhantom(ship) ? null : originalEffect.getUnapplicableReason(ship);
    }

    @Override
    public boolean canBeAddedOrRemovedNow(ShipAPI ship, MarketAPI marketOrNull, CoreUITradeMode mode) {
        return !isPhantom(ship) && originalEffect.canBeAddedOrRemovedNow(ship, marketOrNull, mode);
    }

    @Override
    public String getCanNotBeInstalledNowReason(ShipAPI ship, MarketAPI marketOrNull, CoreUITradeMode mode) {
        if (isPhantom(ship)) {
            return I18n.forGameText(() -> Translation.text("hullmod.phantom.locked"));
        }
        return originalEffect.getCanNotBeInstalledNowReason(ship, marketOrNull, mode);
    }

    @Override
    public boolean shouldAddDescriptionToTooltip(HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
        return !isPhantom(ship) && originalEffect.shouldAddDescriptionToTooltip(hullSize, ship, isForModSpec);
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        if (!isPhantom(ship)) {
            originalEffect.addPostDescriptionSection(tooltip, hullSize, ship, width, isForModSpec);
            return;
        }
        String nodeName = PhantomHullMods.providingNodeName(ship, hullModId);
        Color highlightColor = Global.getSettings().getColor("hColor");
        I18n.forGameText(() -> {
            StyledText phantomText = nodeName == null
                    ? Translation.styled("hullmod.phantom.generic")
                    : Translation.msg("hullmod.phantom.providedBy").arg("node", nodeName).styled();
            VanillaText.addPara(tooltip, phantomText, 10, Misc.getTextColor(), style -> highlightColor);
        });
    }

    @Override
    public boolean hasSModEffectSection(HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
        return !isPhantom(ship) && originalEffect.hasSModEffectSection(hullSize, ship, isForModSpec);
    }

    @Override
    public void addSModSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec,
                               boolean isForBuildInList) {
        if (!isPhantom(ship)) {
            originalEffect.addSModSection(tooltip, hullSize, ship, width, isForModSpec, isForBuildInList);
        }
    }

    @Override
    public void addSModEffectSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec,
                                     boolean isForBuildInList) {
        if (!isPhantom(ship)) {
            originalEffect.addSModEffectSection(tooltip, hullSize, ship, width, isForModSpec, isForBuildInList);
        }
    }

    @Override
    public void addRequiredItemSection(TooltipMakerAPI tooltip, FleetMemberAPI member, ShipVariantAPI currentVariant,
                                       MarketAPI dockedAt, float width, boolean isForModSpec) {
        if (!isPhantom(currentVariant)) {
            originalEffect.addRequiredItemSection(tooltip, member, currentVariant, dockedAt, width, isForModSpec);
        }
    }

    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
        return originalEffect.getDescriptionParam(index, hullSize);
    }

    @Override
    public String getDescriptionParam(int index, HullSize hullSize, ShipAPI ship) {
        return originalEffect.getDescriptionParam(index, hullSize, ship);
    }

    @Override
    public String getSModDescriptionParam(int index, HullSize hullSize) {
        return originalEffect.getSModDescriptionParam(index, hullSize);
    }

    @Override
    public String getSModDescriptionParam(int index, HullSize hullSize, ShipAPI ship) {
        return originalEffect.getSModDescriptionParam(index, hullSize, ship);
    }

    @Override
    public boolean affectsOPCosts() {
        return originalEffect.affectsOPCosts();
    }

    @Override
    public Color getBorderColor() {
        return originalEffect.getBorderColor();
    }

    @Override
    public Color getNameColor() {
        return originalEffect.getNameColor();
    }

    @Override
    public int getDisplaySortOrder() {
        return originalEffect.getDisplaySortOrder();
    }

    @Override
    public int getDisplayCategoryIndex() {
        PhantomHullModRefitHider.requestRefresh();
        return originalEffect.getDisplayCategoryIndex();
    }

    @Override
    public boolean hasSModEffect() {
        return originalEffect.hasSModEffect();
    }

    @Override
    public float getTooltipWidth() {
        return originalEffect.getTooltipWidth();
    }

    @Override
    public boolean isSModEffectAPenalty() {
        return originalEffect.isSModEffectAPenalty();
    }

    @Override
    public boolean showInRefitScreenModPickerFor(ShipAPI ship) {
        return originalEffect.showInRefitScreenModPickerFor(ship);
    }

    @Override
    public CargoStackAPI getRequiredItem() {
        return originalEffect.getRequiredItem();
    }
}
