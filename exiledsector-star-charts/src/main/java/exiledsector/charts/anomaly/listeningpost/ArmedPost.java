package exiledsector.charts.anomaly.listeningpost;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import exiledsector.charts.ChartIds;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.content.ChartAnomalyFleets;
import exiledsector.charts.generation.ChartTier;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.i18n.Translation;
import org.lwjgl.util.vector.Vector2f;

import java.util.Random;

final class ArmedPost {

    static final String ARMED_POST_TAG = ChartIds.PREFIX + "listening_post_armed";
    static final String[] FALLBACK_VARIANTS = {"station1_midline_Standard", "station2_midline_Standard", "station3_midline_Standard"};
    static final int TIERS_PER_FALLBACK_VARIANT = 5;
    static final float STATION_SPIN = 1f;

    private ArmedPost() {
    }

    static String variantFor(String factionId, int tier, Random random) {
        String variantId = ChartAnomalyFleets.stationVariant(factionId, tier, random);
        return variantId != null ? variantId : fallbackVariant(tier);
    }

    static String fallbackVariant(int tier) {
        int index = (ChartTiers.clamp(tier) - 1) / TIERS_PER_FALLBACK_VARIANT;
        return FALLBACK_VARIANTS[Math.max(0, Math.min(FALLBACK_VARIANTS.length - 1, index))];
    }

    static CampaignFleetAPI spawn(AnomalyContentSite site, SectorEntityToken post, FleetEventListener listener, Random random) {
        ChartTier tier = ChartAnomalyFleets.tier(site.tier(), site.context());
        CampaignFleetAPI station = ChartAnomalyFleets.station(site.factionId(), variantFor(site.factionId(), site.tier(), random), tier, random);
        if (station == null) {
            return null;
        }
        ChartAnomalyFleets.prepare(station, site.runId(), site.tier(), random.nextLong(), true, site.context());
        station.setName(Translation.gameText("chart.anomaly.listening_post.fleet.station.name"));
        station.addTag(ARMED_POST_TAG);
        station.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_JUMP, true);
        float angle = random.nextFloat() * 360f;
        float radius = post.getRadius() + ListeningPostRules.ARMED_POST_GAP;
        Vector2f at = new Vector2f(post.getLocation().x + radius, post.getLocation().y);
        site.addFleet(station, at);
        station.setAI(null);
        station.setCircularOrbitWithSpin(post, angle, radius, ListeningPostRules.ARMED_POST_ORBIT_DAYS, STATION_SPIN, STATION_SPIN);
        station.addEventListener(listener);
        return station;
    }

    static boolean isArmedPost(SectorEntityToken entity) {
        return entity instanceof CampaignFleetAPI fleet && fleet.hasTag(ARMED_POST_TAG);
    }
}
