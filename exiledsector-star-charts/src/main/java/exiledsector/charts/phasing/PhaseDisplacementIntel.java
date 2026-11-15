package exiledsector.charts.phasing;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.ui.VanillaText;

import java.awt.Color;
import java.util.Set;

public class PhaseDisplacementIntel extends BaseIntelPlugin {

    static final float PARAGRAPH_PAD = 10f;
    static final float BULLET_PAD = 3f;
    static final float DAYS_SHOWN_AFTER_ARRIVAL = 30f;

    private final String gateMarketId;
    private String arrivedPlanetId;
    private String arrivedPlanetName;

    public PhaseDisplacementIntel(String gateMarketId) {
        this.gateMarketId = gateMarketId;
    }

    static PhaseDisplacementIntel started(SectorAPI sector, MarketAPI gateMarket) {
        IntelManagerAPI manager = sector == null ? null : sector.getIntelManager();
        if (manager == null || gateMarket == null) {
            return null;
        }
        PhaseDisplacementIntel intel = activeFor(manager, gateMarket.getId());
        if (intel == null) {
            intel = new PhaseDisplacementIntel(gateMarket.getId());
            manager.addIntel(intel, false);
        }
        return intel;
    }

    static void arrived(SectorAPI sector, MarketAPI gateMarket, PlanetAPI planet) {
        PhaseDisplacementIntel intel = started(sector, gateMarket);
        if (intel == null || planet == null) {
            return;
        }
        intel.arrivedPlanetId = planet.getId();
        intel.arrivedPlanetName = planet.getName();
        intel.sendUpdateIfPlayerHasIntel(null, false);
        intel.endAfterDelay(DAYS_SHOWN_AFTER_ARRIVAL);
    }

    private static PhaseDisplacementIntel activeFor(IntelManagerAPI manager, String marketId) {
        for (IntelInfoPlugin existing : manager.getIntel(PhaseDisplacementIntel.class)) {
            if (existing instanceof PhaseDisplacementIntel intel && marketId.equals(intel.gateMarketId) && intel.arrivedPlanetId == null
                    && !intel.isEnding() && !intel.isEnded()) {
                return intel;
            }
        }
        return null;
    }

    private MarketAPI gateMarket() {
        return Global.getSector() == null || Global.getSector().getEconomy() == null ? null : Global.getSector().getEconomy().getMarket(gateMarketId);
    }

    @Override
    protected String getName() {
        String key = arrivedPlanetId == null ? "chart.phasing.intel.title" : "chart.phasing.intel.titleArrived";
        return Translation.gameText(key);
    }

    @Override
    public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
        info.addPara(VanillaText.escapeFormat(getName()), getTitleColor(mode), 0f);
        String status = statusLine();
        if (status != null) {
            info.addPara(VanillaText.escapeFormat(status), Misc.getGrayColor(), BULLET_PAD);
        }
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        MarketAPI market = gateMarket();
        Color text = Misc.getTextColor();
        String colony = market == null ? "" : market.getName();
        if (arrivedPlanetId != null) {
            String planetName = arrivedPlanetName;
            VanillaText.plainPara(info, I18n.forGameText(() -> Translation.msg("chart.phasing.intel.arrived").arg("planet", planetName)
                    .arg("colony", colony).text()), 0f, text);
            return;
        }
        PhasedPlanetRecord job = PhaseGateState.job(market);
        if (job == null) {
            VanillaText.plainPara(info, Translation.gameText("chart.phasing.intel.idle"), 0f, text);
            return;
        }
        VanillaText.plainPara(info, I18n.forGameText(() -> Translation.msg("chart.phasing.intel.pulling").arg("planet", job.name())
                .arg("colony", colony).text()), 0f, text);
        String status = statusLine();
        if (status != null) {
            VanillaText.plainPara(info, status, PARAGRAPH_PAD, Misc.getHighlightColor());
        }
    }

    String statusLine() {
        if (arrivedPlanetId != null) {
            return null;
        }
        MarketAPI market = gateMarket();
        PhasedPlanetRecord job = PhaseGateState.job(market);
        if (job == null) {
            return null;
        }
        Industry industry = market.getIndustry(PhaseDisplacementGate.INDUSTRY_ID);
        PhasePull.Pause pause = industry instanceof PhaseDisplacementGate gate ? PhasePull.pauseFor(gate) : PhasePull.Pause.NOT_FUNCTIONAL;
        if (pause != null) {
            return Translation.gameText(PhasePull.pauseKey(pause));
        }
        int daysLeft = (int) Math.ceil(Math.max(0f, PhasePull.PULL_DAYS - PhaseGateState.jobDays(market)));
        return I18n.forGameText(() -> Translation.msg("chart.phasing.intel.daysLeft").arg("days", daysLeft).text());
    }

    @Override
    public String getIcon() {
        return Global.getSettings().getIndustrySpec(PhaseDisplacementGate.INDUSTRY_ID).getImageName();
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        tags.add(Tags.INTEL_COLONIES);
        return tags;
    }

    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        if (arrivedPlanetId != null && Global.getSector().getEntityById(arrivedPlanetId) != null) {
            return Global.getSector().getEntityById(arrivedPlanetId);
        }
        MarketAPI market = gateMarket();
        return market == null ? null : market.getPrimaryEntity();
    }

    @Override
    public boolean shouldRemoveIntel() {
        return arrivedPlanetId == null && !PhaseGateState.hasJobEntry(gateMarket()) || super.shouldRemoveIntel();
    }
}
