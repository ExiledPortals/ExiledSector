package exiledsector.charts.anomaly.convoy;

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
import exiledsector.charts.effect.ChartEffect;
import exiledsector.charts.atlas.AtlasTraits;
import exiledsector.charts.content.ChartAnomalyFleets;
import exiledsector.i18n.Translation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class SupplyConvoy implements LayoutShaper, ContentPlacer, RunParticipant, LootShaper, AtlasRule {

    public static final String ANOMALY_ID = "supply_convoy";
    public static final String CARGO_KEY = ChartEffect.CHART_CONVOY_CARGO_PERCENT.anomalyTuningKey();
    public static final String ESCORT_KEY = ChartEffect.CHART_CONVOY_ESCORT_PERCENT.anomalyTuningKey();
    public static final String EXTRA_KEY = ChartEffect.CHART_EXTRA_CONVOYS.anomalyTuningKey();
    public static final String CONVOY_MEMORY_KEY = "$exiledSector_chartConvoy";
    public static final SupplyConvoy INSTANCE = new SupplyConvoy();
    static final int MIN_PLANETS = 3;
    static final long ROUTE_SALT = 0x434f4e564f5952L;

    private SupplyConvoy() {
    }

    @Override
    public String id() {
        return ANOMALY_ID;
    }

    static RunFactionDress factionDress(String lookalikeFactionId) {
        return new RunFactionDress(lookalikeFactionId, Translation.gameText("chart.convoy.faction.name"),
                Translation.gameText("chart.convoy.faction.nameWithArticle"), Translation.gameText("chart.convoy.faction.isOrAre"), RepLevel.NEUTRAL, true);
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
        int count = ConvoyRules.convoyCount(site.context().anomalyPercent(EXTRA_KEY));
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
}
