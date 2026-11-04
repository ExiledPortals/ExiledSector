package exiledsector.charts.anomaly.abyssstorm;

import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.anomaly.AtlasRule;
import exiledsector.charts.anomaly.ContentPlacer;
import exiledsector.charts.anomaly.RunParticipant;
import exiledsector.charts.atlas.AtlasTraits;
import exiledsector.charts.layout.abyss.ChartAbyss;
import exiledsector.charts.layout.abyss.ChartPhotospherePlugin;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

public final class AbyssalStorm implements ContentPlacer, AtlasRule, RunParticipant {

    public static final String ANOMALY_ID = "abyssal_storm";
    public static final AbyssalStorm INSTANCE = new AbyssalStorm();
    static final String BIOME_ID = "abyssal_rift";

    private AbyssalStorm() {
    }

    @Override
    public String id() {
        return ANOMALY_ID;
    }

    @Override
    public boolean fitsAt(AtlasTraits traits) {
        return BIOME_ID.equals(traits.regionId());
    }

    @Override
    public void generate(AnomalyContentSite site) {
        site.addTerrain(AbyssalStormTerrainPlugin.TERRAIN_ID, new AbyssalStormTerrainPlugin.Params(stormColour(site)), new Vector2f());
    }

    static Color stormColour(AnomalyContentSite site) {
        for (CustomCampaignEntityAPI photosphere : ChartAbyss.photospheres(site.system())) {
            if (photosphere.getCustomPlugin() instanceof ChartPhotospherePlugin plugin && plugin.colour() != null) {
                return plugin.colour();
            }
        }
        return Color.WHITE;
    }
}
