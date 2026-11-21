package exiledsector.charts.phasing;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.StarChartSettings;
import exiledsector.charts.wormhole.ColonyFreeAtlas;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.ui.VanillaText;

public class PhaseDisplacementGate extends BaseIndustry {

    public static final String INDUSTRY_ID = "exiledSector_phase_displacement_gate";
    static final float TOOLTIP_PAD = 10f;

    private transient float uncheckedDays;

    public static MarketAPI playerGateMarket(SectorAPI sector) {
        if (sector == null || sector.getEconomy() == null) {
            return null;
        }
        for (MarketAPI candidate : sector.getEconomy().getMarketsCopy()) {
            if (isPlayers(candidate) && candidate.hasIndustry(INDUSTRY_ID)) {
                return candidate;
            }
        }
        return null;
    }

    static boolean isPlayers(MarketAPI candidate) {
        return candidate.isPlayerOwned() || Factions.PLAYER.equals(candidate.getFactionId());
    }

    @Override
    public void apply() {
        super.apply(true);
        boolean pulling = PhaseGateState.hasJobEntry(market);
        demand(Commodities.RARE_METALS, pulling ? PhasePull.TRANSPLUTONICS_DEMAND : 0);
        demand(Commodities.VOLATILES, pulling ? PhasePull.VOLATILES_DEMAND : 0);
    }

    @Override
    public void unapply() {
        super.unapply();
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);
        SectorAPI sector = Global.getSector();
        if (market == null || sector == null || sector.getClock() == null) {
            return;
        }
        uncheckedDays += sector.getClock().convertToDays(amount);
        if (uncheckedDays < PhasePull.CHECK_DAYS) {
            return;
        }
        float days = uncheckedDays;
        uncheckedDays = 0f;
        if (PhaseGateState.hasJobEntry(market)) {
            PhasePull.advance(sector, this, days);
        }
    }

    @Override
    public void notifyBeingRemoved(MarketAPI.MarketInteractionMode mode, boolean forUpgrade) {
        super.notifyBeingRemoved(mode, forUpgrade);
        if (!forUpgrade) {
            PhasePull.abandon(Global.getSector(), market);
        }
    }

    @Override
    public boolean isAvailableToBuild() {
        return market != null && StarChartSettings.enabled() && !ColonyFreeAtlas.active() && super.isAvailableToBuild()
                && PhaseGateRules.blocker(market) == null;
    }

    @Override
    public boolean showWhenUnavailable() {
        return market != null && StarChartSettings.enabled() && !ColonyFreeAtlas.active() && PhaseGateRules.isUnlockedFor(market)
                && super.showWhenUnavailable();
    }

    @Override
    public String getUnavailableReason() {
        PhaseGateRules.Blocker blocker = market == null ? null : PhaseGateRules.blocker(market);
        if (blocker == null) {
            return super.getUnavailableReason();
        }
        return unavailableText(blocker);
    }

    static String unavailableText(PhaseGateRules.Blocker blocker) {
        String key = PhaseGateRules.blockerKey(blocker);
        return I18n.forGameText(() -> Translation.msg(key).arg("range", HypershuntPower.rangeLightYears()).text());
    }

    @Override
    public boolean canInstallAICores() {
        return false;
    }

    @Override
    public boolean canImprove() {
        return false;
    }

    @Override
    protected void addPostDescriptionSection(TooltipMakerAPI tooltip, IndustryTooltipMode mode) {
        VanillaText.plainPara(tooltip, I18n.forGameText(() -> Translation.msg("chart.phasing.tooltip.power")
                .arg("range", HypershuntPower.rangeLightYears()).text()), TOOLTIP_PAD, Misc.getTextColor());
        PhasedPlanetRecord job = market == null || mode == IndustryTooltipMode.ADD_INDUSTRY ? null : PhaseGateState.job(market);
        if (job == null) {
            return;
        }
        PhasePull.Pause pause = PhasePull.pauseFor(this);
        int daysLeft = (int) Math.ceil(Math.max(0f, PhasePull.PULL_DAYS - PhaseGateState.jobDays(market)));
        String status = pause == null
                ? I18n.forGameText(() -> Translation.msg("chart.phasing.tooltip.pulling").arg("planet", job.name()).arg("days", daysLeft).text())
                : I18n.forGameText(() -> Translation.msg("chart.phasing.tooltip.paused").arg("planet", job.name())
                .arg("reason", Translation.gameText(PhasePull.pauseKey(pause))).text());
        VanillaText.plainPara(tooltip, status, TOOLTIP_PAD, Misc.getHighlightColor());
    }
}
