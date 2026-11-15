package exiledsector.charts.phasing;

import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.campaign.CampaignTerrainPlugin;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.terrain.BaseRingTerrain;
import com.fs.starfarer.api.impl.campaign.terrain.BaseRingTerrain.RingParams;
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.PulsarBeamTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.RadioChatterTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.StarCoronaTerrainPlugin;
import exiledsector.charts.wormhole.ChartWormholePlacement;
import exiledsector.charts.wormhole.ChartWormholePlacement.Band;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.Comparator;
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
        float innermostOrbit = focus.getRadius() + Math.max(MIN_PAD, coronaReach(system, focus) + planetRadius + CLEARANCE);
        float neededGap = planetRadius * 2f + CLEARANCE * 2f;
        List<Band> occupied = occupiedBands(system, focus);
        occupied.sort(Comparator.comparingDouble(Band::inner));
        float widestStart = 0f;
        float widestEnd = 0f;
        float freeFrom = innermostOrbit;
        for (Band band : occupied) {
            if (band.inner() - freeFrom > widestEnd - widestStart) {
                widestStart = freeFrom;
                widestEnd = band.inner();
            }
            freeFrom = Math.max(freeFrom, band.outer());
        }
        if (widestEnd - widestStart >= neededGap) {
            return (widestStart + widestEnd) * 0.5f;
        }
        return freeFrom + planetRadius + FALLBACK_CLEARANCE;
    }

    public static float orbitDays(float orbitRadius) {
        return Math.max(1f, orbitRadius / ORBIT_SPEED);
    }

    static float coronaReach(StarSystemAPI system, SectorEntityToken focus) {
        float reach = 0f;
        for (CampaignTerrainAPI terrain : system.getTerrainCopy()) {
            if (terrain.getOrbitFocus() == focus && terrain.getPlugin() instanceof StarCoronaTerrainPlugin corona
                    && ((BaseRingTerrain) corona).params != null) {
                RingParams ring = ((BaseRingTerrain) corona).params;
                reach = Math.max(reach, ring.middleRadius + ring.bandWidthInEngine * 0.5f - focus.getRadius());
            }
        }
        return reach;
    }

    static List<Band> occupiedBands(StarSystemAPI system, SectorEntityToken focus) {
        Vector2f focusLocation = focus.getLocation() != null ? focus.getLocation() : new Vector2f();
        List<SectorEntityToken> bodies = new ArrayList<>(system.getPlanets());
        bodies.addAll(system.getJumpPoints());
        bodies.addAll(system.getEntities(CustomCampaignEntityAPI.class));
        List<Band> occupied = new ArrayList<>();
        for (SectorEntityToken body : bodies) {
            if (body != focus) {
                occupied.add(ChartWormholePlacement.centreBand(body, focusLocation, 0).sweptAt(0f, body.getRadius()));
            }
        }
        for (CampaignTerrainAPI terrain : system.getTerrainCopy()) {
            if (terrain.getPlugin() instanceof BaseRingTerrain ring && ring.params != null && isObstacle(terrain, focus)) {
                float halfWidth = ring.params.bandWidthInEngine * 0.5f;
                Band ringCentre = ChartWormholePlacement.centreBand(terrain, focusLocation, 0);
                occupied.add(ringCentre.sweptAt(Math.max(0f, ring.params.middleRadius - halfWidth), ring.params.middleRadius + halfWidth));
            }
        }
        return occupied;
    }

    private static boolean isObstacle(CampaignTerrainAPI terrain, SectorEntityToken focus) {
        CampaignTerrainPlugin plugin = terrain.getPlugin();
        if (plugin instanceof PulsarBeamTerrainPlugin || plugin instanceof RadioChatterTerrainPlugin) {
            return false;
        }
        boolean aroundFocus = terrain.getOrbitFocus() == focus;
        return !(aroundFocus && (plugin instanceof StarCoronaTerrainPlugin || plugin instanceof MagneticFieldTerrainPlugin));
    }
}
