package exiledsector.charts.phasing;

import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.OrbitGap;
import com.fs.starfarer.api.impl.campaign.terrain.BaseRingTerrain;
import com.fs.starfarer.api.impl.campaign.terrain.BaseRingTerrain.RingParams;
import com.fs.starfarer.api.impl.campaign.terrain.StarCoronaTerrainPlugin;

import java.util.ArrayList;
import java.util.List;

public final class PhaseDestination {

    static final float MIN_PAD = 400f;
    static final float CLEARANCE = 250f;
    static final float FALLBACK_CLEARANCE = 600f;
    static final float ORBIT_SPEED = 22f;

    private PhaseDestination() {
    }

    public static SectorEntityToken focusOf(StarSystemAPI system) {
        PlanetAPI star = system.getStar();
        if (star != null && star.getOrbitFocus() == null) {
            return star;
        }
        return system.getCenter() != null ? system.getCenter() : star;
    }

    public static float orbitRadius(StarSystemAPI system, SectorEntityToken focus, float planetRadius) {
        float minPad = Math.max(MIN_PAD, coronaReach(system, focus) + planetRadius + CLEARANCE);
        float outermost = outermostReach(system, focus);
        float widestStart = 0f;
        float widestEnd = 0f;
        for (OrbitGap gap : BaseThemeGenerator.findGaps(focus, minPad, outermost, planetRadius * 2f + CLEARANCE * 2f)) {
            if (gap.end - gap.start > widestEnd - widestStart) {
                widestStart = gap.start;
                widestEnd = gap.end;
            }
        }
        if (widestEnd > widestStart) {
            return (widestStart + widestEnd) * 0.5f;
        }
        return Math.max(outermost, focus.getRadius() + minPad) + planetRadius + FALLBACK_CLEARANCE;
    }

    public static float orbitDays(float orbitRadius) {
        return Math.max(1f, orbitRadius / ORBIT_SPEED);
    }

    static float coronaReach(StarSystemAPI system, SectorEntityToken focus) {
        float reach = 0f;
        for (CampaignTerrainAPI terrain : terrainAround(system, focus)) {
            if (terrain.getPlugin() instanceof StarCoronaTerrainPlugin corona && ((BaseRingTerrain) corona).params != null) {
                RingParams ring = ((BaseRingTerrain) corona).params;
                reach = Math.max(reach, ring.middleRadius + ring.bandWidthInEngine * 0.5f - focus.getRadius());
            }
        }
        return reach;
    }

    static float outermostReach(StarSystemAPI system, SectorEntityToken focus) {
        float reach = BaseThemeGenerator.getOrbitalRadius(focus);
        List<SectorEntityToken> orbiting = new ArrayList<>(system.getJumpPoints());
        orbiting.addAll(system.getEntities(CustomCampaignEntityAPI.class));
        for (SectorEntityToken entity : orbiting) {
            if (entity.getOrbitFocus() == focus) {
                reach = Math.max(reach, entity.getCircularOrbitRadius() + entity.getRadius());
            }
        }
        return reach;
    }

    private static List<CampaignTerrainAPI> terrainAround(StarSystemAPI system, SectorEntityToken focus) {
        List<CampaignTerrainAPI> around = new ArrayList<>();
        for (CampaignTerrainAPI terrain : system.getTerrainCopy()) {
            if (terrain.getOrbitFocus() == focus) {
                around.add(terrain);
            }
        }
        return around;
    }
}
