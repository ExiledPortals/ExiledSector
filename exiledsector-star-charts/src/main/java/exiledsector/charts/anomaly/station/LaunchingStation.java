package exiledsector.charts.anomaly.station;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import exiledsector.i18n.Translation;
import exiledsector.charts.ChartIds;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.anomaly.AnomalyRun;
import exiledsector.charts.anomaly.ContentPlacer;
import exiledsector.charts.anomaly.OrbitSearch;
import exiledsector.charts.anomaly.RunParticipant;
import exiledsector.charts.content.ChartAnomalyFleets;
import exiledsector.charts.content.ChartSites;
import exiledsector.charts.generation.ChartTier;

import java.util.Random;

abstract class LaunchingStation implements ContentPlacer, RunParticipant {

    static final long PLACEMENT_SALT = 0x535441544e4cL;
    static final String DAMAGED_STATION_KEY = "$damagedStation";

    abstract String factionId();

    abstract String variantFor(int tier);

    abstract String nameKey(int tier);

    abstract EveryFrameScript manager(CampaignFleetAPI station, String runId, int tier, long seed);

    abstract boolean damaged(int tier);

    final String stationTag() {
        return ChartIds.PREFIX + id();
    }

    @Override
    public void generate(AnomalyContentSite site) {
        place(site);
    }

    CampaignFleetAPI place(AnomalyContentSite site) {
        Random random = site.random(PLACEMENT_SALT);
        ChartSites.Orbit orbit = orbit(site, random);
        if (orbit == null) {
            throw new IllegalStateException("no clear orbit for the " + id());
        }
        ChartTier tier = ChartAnomalyFleets.tier(site.tier(), site.context());
        CampaignFleetAPI station = ChartAnomalyFleets.station(factionId(), variantFor(site.tier()), tier, random);
        if (station == null) {
            throw new IllegalStateException("the " + id() + " station " + variantFor(site.tier()) + " could not be built");
        }
        ChartAnomalyFleets.prepare(station, site.runId(), site.tier(), random.nextLong(), true, site.context());
        station.setName(Translation.gameText(nameKey(site.tier())));
        station.addTag(stationTag());
        station.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_JUMP, true);
        if (damaged(site.tier())) {
            station.getMemoryWithoutUpdate().set(DAMAGED_STATION_KEY, true);
        }
        site.addFleet(station, orbit.position());
        station.setAI(null);
        station.setCircularOrbitWithSpin(orbit.focus(), orbit.angle(), orbit.radius(), orbit.days(), StationRules.STATION_SPIN,
                StationRules.STATION_SPIN);
        site.addScript(manager(station, site.runId(), site.tier(), random.nextLong()));
        return station;
    }

    static ChartSites.Orbit orbit(AnomalyContentSite site, Random random) {
        return site.clearOrbit(OrbitSearch.bands(site.centre(), StationRules.ORBIT_MIN, StationRules.ORBIT_BAND_WIDTH, StationRules.ORBIT_BANDS)
                .awayFromEntry(StationRules.MIN_ENTRY_GAP), random);
    }

    CampaignFleetAPI stationIn(LocationAPI location) {
        if (location == null) {
            return null;
        }
        for (CampaignFleetAPI fleet : location.getFleets()) {
            if (fleet.hasTag(stationTag()) && fleet.isAlive()) {
                return fleet;
            }
        }
        return null;
    }

    @Override
    public String intelText(AnomalyRun run) {
        return stationIn(run.system()) != null ? intelLine() : null;
    }
}
