package exiledsector.charts.anomaly.convoy;

import com.fs.starfarer.api.campaign.BattleCreationPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.anomaly.AnomalyRun;
import exiledsector.charts.anomaly.AtlasRule;
import exiledsector.charts.anomaly.ContentPlacer;
import exiledsector.charts.anomaly.LayoutShaper;
import exiledsector.charts.anomaly.LootShaper;
import exiledsector.charts.anomaly.RunFactionDress;
import exiledsector.charts.anomaly.RunParticipant;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.atlas.AtlasTraits;
import exiledsector.charts.content.ChartAnomalyFleets;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class SupplyConvoy implements LayoutShaper, ContentPlacer, RunParticipant, LootShaper, AtlasRule {

    public static final String ANOMALY_ID = "supply_convoy";
    public static final String ESCORT_KEY = ChartModifier.CHART_CONVOY_ESCORT_PERCENT.anomalyTuningKey();
    public static final String EXTRA_KEY = ChartModifier.CHART_EXTRA_CONVOYS.anomalyTuningKey();
    public static final String EXTRA_CHANCE_KEY = ChartModifier.CHART_EXTRA_CONVOY_CHANCE_PERCENT.anomalyTuningKey();
    public static final String DOUBLE_FEEDSTOCK_KEY = ChartModifier.CHART_CONVOY_DOUBLE_FEEDSTOCK_CHANCE_PERCENT.anomalyTuningKey();
    public static final String BREACH_FEEDSTOCK_KEY = ChartModifier.CHART_CONVOY_BREACH_FEEDSTOCK_PERCENT.anomalyTuningKey();
    public static final String OFFICER_FEEDSTOCK_KEY = ChartModifier.CHART_CONVOY_OFFICER_FEEDSTOCK.anomalyTuningKey();
    public static final String EXCAVATION_DELIVERY_KEY = ChartModifier.CHART_CONVOY_EXCAVATION_DELIVERY.anomalyTuningKey();
    public static final String EXCAVATION_RAID_DIFFICULTY_KEY = ChartModifier.CHART_CONVOY_EXCAVATION_RAID_DIFFICULTY_PERCENT.anomalyTuningKey();
    public static final String EXCAVATION_LOOT_KEY = ChartModifier.CHART_CONVOY_EXCAVATION_LOOT_PERCENT.anomalyTuningKey();
    public static final String HOSTILE_KEY = ChartModifier.CHART_HOSTILE_CONVOYS.anomalyTuningKey();
    public static final String FEEDSTOCK_KEY = ChartModifier.CHART_CONVOY_FEEDSTOCK_PERCENT.anomalyTuningKey();
    public static final String MIXED_MANIFEST_KEY = ChartModifier.CHART_CONVOY_MIXED_MANIFEST.anomalyTuningKey();
    public static final String FLEET_SIZE_KEY = ChartModifier.CHART_CONVOY_FLEET_SIZE_PERCENT.anomalyTuningKey();
    public static final String CONVOY_MEMORY_KEY = "$exiledSector_chartConvoy";
    public static final SupplyConvoy INSTANCE = new SupplyConvoy();
    static final int MIN_PLANETS = 3;
    static final long ROUTE_SALT = 0x434f4e564f5952L;
    static final long EXTRA_CHANCE_SALT = 0x434f4e564f5943L;

    private SupplyConvoy() {
    }

    @Override
    public String id() {
        return ANOMALY_ID;
    }

    @Override
    public void loadData() {
        ConvoyFeedstock.load();
    }

    static RunFactionDress factionDress(String lookalikeFactionId) {
        return new RunFactionDress(lookalikeFactionId, null, null, null, RepLevel.NEUTRAL, true);
    }

    @Override
    public Set<String> runFactionIds() {
        return Set.of(ConvoyRules.FACTION_ID);
    }

    @Override
    public List<String> lootScopes() {
        return List.of(ConvoyRules.LOOT_SCOPE);
    }

    @Override
    public int minPlanets() {
        return MIN_PLANETS;
    }

    @Override
    public boolean fitsAt(AtlasTraits traits) {
        return traits.specialId() == null && traits.infestationId() == null;
    }

    @Override
    public void generate(AnomalyContentSite site) {
        List<SectorEntityToken> planets = new ArrayList<>(site.worlds());
        if (planets.isEmpty()) {
            throw new IllegalStateException("no planet for the convoy to deliver to");
        }
        List<SectorEntityToken> stations = site.pois().stream().filter(poi -> poi.hasTag(Tags.STATION)).toList();
        String convoyFactionId = ChartAnomalyFleets.dress(ConvoyRules.FACTION_ID, factionDress(site.factionId()));
        ConvoyScript script = new ConvoyScript(site.runId(), site.tier(), site.system());
        Set<SectorEntityToken> takenDestinations = new HashSet<>(planets.stream().filter(site::isClaimed).toList());
        int count = ConvoyRules.convoyCount(site.context().anomalyPercent(EXTRA_KEY), site.context().anomalyPercent(EXTRA_CHANCE_KEY),
                site.random(EXTRA_CHANCE_SALT));
        for (int index = 0; index < count; index++) {
            Random random = site.random(ROUTE_SALT + index);
            ConvoyRoute.Plan plan = ConvoyRoute.plan(planets, stations, takenDestinations, random);
            if (plan == null) {
                continue;
            }
            ConvoyState convoy = new ConvoyState(index, plan.waypoints().stream().map(SectorEntityToken::getId).toList(), plan.destination().getId(),
                    plan.entry().x, plan.entry().y, plan.exit().x, plan.exit().y, random.nextLong());
            if (ConvoyFleets.spawn(site, convoy, convoyFactionId, script, random)) {
                takenDestinations.add(plan.destination());
                site.claim(plan.destination());
                script.add(convoy);
            }
        }
        if (script.convoys().isEmpty()) {
            throw new IllegalStateException("no convoy could be formed");
        }
        site.addScript(script);
    }

    @Override
    public String intelText(AnomalyRun run) {
        ConvoyScript script = ConvoyScript.in(run.system());
        return script != null && script.isActive() ? intelLine() : null;
    }

    @Override
    public InteractionDialogPlugin dialogFor(AnomalyRun run, SectorEntityToken target) {
        if (ConvoyEncounterDialog.wantsToHandle(target)) {
            return new ConvoyEncounterDialog();
        }
        return ConvoyCacheDialog.wantsToHandle(target) ? new ConvoyCacheDialog() : null;
    }

    @Override
    public BattleCreationPlugin battleFor(SectorEntityToken opponent) {
        List<CampaignFleetAPI> sunstruck = ConvoySunstruck.fleetsToWearDown(opponent);
        return sunstruck.isEmpty() ? null : new SunstruckBattleCreationPlugin(sunstruck);
    }
}
