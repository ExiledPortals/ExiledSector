package exiledsector.charts.anomaly.excavation;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleCreationPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.charts.effect.ChartEffect;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.anomaly.AnomalyRun;
import exiledsector.charts.anomaly.ContentPlacer;
import exiledsector.charts.anomaly.LayoutShaper;
import exiledsector.charts.anomaly.LootShaper;
import exiledsector.charts.anomaly.RunParticipant;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.run.ChartRun;
import exiledsector.items.ItemDrops;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class ExcavationSites implements LayoutShaper, ContentPlacer, RunParticipant, LootShaper {

    public static final String ANOMALY_ID = "excavation_sites";
    public static final String EXTRA_KEY = ChartEffect.CHART_EXTRA_EXCAVATION_SITES.anomalyTuningKey();
    public static final String LOOT_KEY = ChartEffect.CHART_EXCAVATION_LOOT_QUANTITY_PERCENT.anomalyTuningKey();
    public static final ExcavationSites INSTANCE = new ExcavationSites();
    static final long PLACEMENT_SALT = 0x45584341564154L;

    private ExcavationSites() {
    }

    @Override
    public String id() {
        return ANOMALY_ID;
    }

    @Override
    public List<String> lootScopes() {
        return List.of(ExcavationRules.LOOT_SCOPE);
    }

    @Override
    public int minPlanets() {
        return ExcavationRules.MIN_PLANETS;
    }

    @Override
    public void generate(AnomalyContentSite site) {
        if (place(site, site.run(), Global.getSector()).isEmpty()) {
            throw new IllegalStateException("no planet for an excavation site");
        }
    }

    static List<ExcavationSite> place(AnomalyContentSite site, ChartRun run, SectorAPI sector) {
        Random random = site.random(PLACEMENT_SALT);
        List<PlanetAPI> planets = candidates(site);
        Collections.shuffle(planets, random);
        int count = Math.min(planets.size(), ExcavationRules.siteCount(random, Math.round(site.context().anomalyPercent(EXTRA_KEY))));
        List<ExcavationSite> placed = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            ExcavationSite placedSite = new ExcavationSite(index, planets.get(index).getId(), ExcavationRules.access(random),
                    ExcavationRules.lean(sector, random), random.nextLong(), false);
            ExcavationStates.record(run, placedSite);
            site.claim(planets.get(index));
            placed.add(placedSite);
        }
        return placed;
    }

    static List<PlanetAPI> candidates(AnomalyContentSite site) {
        List<PlanetAPI> solid = new ArrayList<>();
        List<PlanetAPI> any = new ArrayList<>();
        for (PlanetAPI planet : site.system().getPlanets()) {
            if (planet.isStar() || !ExcavationSite.encodable(planet.getId()) || site.isClaimed(planet)) {
                continue;
            }
            any.add(planet);
            if (!planet.isGasGiant()) {
                solid.add(planet);
            }
        }
        return solid.isEmpty() ? any : solid;
    }

    @Override
    public InteractionDialogPlugin dialogFor(AnomalyRun run, SectorEntityToken target) {
        return ExcavationSiteDialog.wantsToHandle(run.run(), target) ? new ExcavationSiteDialog() : null;
    }

    @Override
    public BattleCreationPlugin battleFor(SectorEntityToken opponent) {
        return ShaftEncounters.isShaftBattle(opponent) ? new ShaftBattleCreationPlugin() : null;
    }

    @Override
    public ItemDrops.SiteLoot fleetItems(SectorAPI sector, CampaignFleetAPI fleet, ChartRun run, ChartGenContext context, float share, Random random) {
        return ExcavationLoot.fleetItems(sector, fleet, run, context, share, random);
    }

    @Override
    public String intelText(AnomalyRun run) {
        List<StyledText> planets = new ArrayList<>();
        for (ExcavationSite site : ExcavationStates.standing(run.run())) {
            SectorEntityToken planet = run.system().getEntityById(site.planetId());
            if (planet != null) {
                planets.add(StyledText.styled(planet.getName(), Style.HIGHLIGHT));
            }
        }
        if (planets.isEmpty()) {
            return null;
        }
        return I18n.forGameText(() -> Translation.msg("chart.excavation.intel").count(planets.size()).arg("planets", Translation.list(planets)).text());
    }
}
