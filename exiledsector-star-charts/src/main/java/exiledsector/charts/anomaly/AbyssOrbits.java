package exiledsector.charts.anomaly;

import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import exiledsector.charts.content.ChartSites;
import exiledsector.charts.layout.abyss.ChartAbyss;

import java.util.List;
import java.util.Random;

public final class AbyssOrbits {

    private AbyssOrbits() {
    }

    public static ChartSites.Orbit nearPhotosphere(AnomalyContentSite site, float minGap, float maxGap, Random random) {
        List<CustomCampaignEntityAPI> photospheres = ChartAbyss.photospheres(site.system());
        SectorEntityToken focus = photospheres.isEmpty() ? site.centre() : photospheres.get(random.nextInt(photospheres.size()));
        float coreRadius = focus == null ? 0f : focus.getRadius();
        ChartSites.Orbit orbit = site.clearOrbit(focus, coreRadius + minGap, coreRadius + maxGap, random);
        return orbit != null ? orbit : AnomalyContentSite.orbit(focus, random.nextFloat() * 360f, coreRadius + minGap);
    }
}
