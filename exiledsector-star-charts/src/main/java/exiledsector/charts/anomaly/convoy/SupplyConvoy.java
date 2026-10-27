package exiledsector.charts.anomaly.convoy;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.anomaly.ChartAnomaly;
import exiledsector.charts.atlas.AtlasTraits;
import exiledsector.charts.content.ChartAnomalyFleets;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class SupplyConvoy implements ChartAnomaly {

    public static final String ID = "supply_convoy";
    public static final String CARGO_KEY = ID + ".cargo";
    public static final String ESCORT_KEY = ID + ".escort";
    public static final String EXTRA_KEY = ID + ".extra";
    public static final String CONVOY_MEMORY_KEY = "$exiledSector_chartConvoy";
    public static final String ROLE_MEMORY_KEY = "$exiledSector_chartConvoyRole";
    public static final SupplyConvoy INSTANCE = new SupplyConvoy();
    static final int MIN_PLANETS = 3;
    static final long ROUTE_SALT = 0x434f4e564f5952L;

    private SupplyConvoy() {
    }

    @Override
    public String id() {
        return ID;
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
        StarSystemAPI system = site.system();
        List<SectorEntityToken> planets = new ArrayList<>();
        for (PlanetAPI planet : system.getPlanets()) {
            if (!planet.isStar()) {
                planets.add(planet);
            }
        }
        if (planets.isEmpty()) {
            throw new IllegalStateException("no planet for the convoy to deliver to");
        }
        List<SectorEntityToken> stations = site.pois().stream().filter(poi -> poi.hasTag(Tags.STATION)).toList();
        String convoyFactionId = ChartAnomalyFleets.dressConvoy(site.factionId());
        ConvoyScript script = new ConvoyScript(site.runId(), site.tier(), system);
        Set<SectorEntityToken> destinations = new HashSet<>();
        int count = ConvoyRules.convoyCount(site.context().anomalyPercent(EXTRA_KEY));
        for (int index = 0; index < count; index++) {
            Random random = site.random(ROUTE_SALT + index);
            ConvoyRoute.Plan plan = ConvoyRoute.plan(planets, stations, destinations, random);
            if (plan == null) {
                continue;
            }
            ConvoyState convoy = new ConvoyState(index, plan.waypoints().stream().map(SectorEntityToken::getId).toList(), plan.destination().getId(),
                    plan.entry().x, plan.entry().y, plan.exit().x, plan.exit().y, random.nextLong());
            if (ConvoyFleets.spawn(site, convoy, convoyFactionId, random)) {
                destinations.add(plan.destination());
                script.add(convoy);
            }
        }
        if (script.convoys().isEmpty()) {
            throw new IllegalStateException("no convoy could be formed");
        }
        system.addScript(script);
    }

    @Override
    public boolean isActiveIn(LocationAPI location) {
        ConvoyScript script = ConvoyScript.in(location);
        return script != null && script.isActive();
    }
}
