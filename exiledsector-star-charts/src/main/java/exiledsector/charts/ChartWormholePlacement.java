package exiledsector.charts;

import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.campaign.CampaignTerrainPlugin;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.terrain.BaseRingTerrain;
import com.fs.starfarer.api.impl.campaign.terrain.FlareManager.FlareManagerDelegate;
import com.fs.starfarer.api.impl.campaign.terrain.PulsarBeamTerrainPlugin;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

final class ChartWormholePlacement {

    static final float MIN_RADIUS = 1800f;
    static final float STAR_CLEARANCE = 900f;
    static final float PLANET_CLEARANCE = 600f;
    static final float TERRAIN_CLEARANCE = 500f;
    static final float STEP_PAST_BAND = 1f;
    static final float ARRIVAL_SEARCH_STEP = 50f;
    static final int MAX_ORBIT_DEPTH = 6;
    static final double AVOID_SPREAD = Math.PI / 2d;

    private ChartWormholePlacement() {
    }

    static Vector2f choose(StarSystemAPI system, Random placementRandom) {
        return choose(system, placementRandom, null);
    }

    static Vector2f choose(StarSystemAPI system, Random placementRandom, Vector2f avoid) {
        return place(blockedBands(system), placementRandom, avoid);
    }

    static float outermost(StarSystemAPI system, Collection<? extends SectorEntityToken> ignored) {
        float outermost = 0f;
        for (PlanetAPI body : system.getPlanets()) {
            if (!ignored.contains(body)) {
                float clearance = body.getRadius() + (body.isStar() ? STAR_CLEARANCE : PLANET_CLEARANCE);
                outermost = Math.max(outermost, centreBand(body, 0).widenedBy(clearance).outer());
            }
        }
        for (CampaignTerrainAPI terrain : system.getTerrainCopy()) {
            Band band = ringTerrainBand(terrain);
            if (band != null && !ignored.contains(terrain.getOrbitFocus())) {
                outermost = Math.max(outermost, band.outer());
            }
        }
        return outermost;
    }

    static Vector2f place(List<Band> blocked, Random placementRandom, Vector2f avoid) {
        double angle = placementRandom.nextDouble() * Math.PI * 2d;
        if (avoid != null && avoid.lengthSquared() > 0f) {
            angle = Math.atan2(avoid.y, avoid.x) + Math.PI + (angle / (Math.PI * 2d) - 0.5d) * AVOID_SPREAD;
        }
        float outermost = 0f;
        for (Band band : blocked) {
            outermost = Math.max(outermost, band.outer());
        }
        float radius = clearRadius(blocked, MIN_RADIUS);
        while (!isClear(blocked, arrivalDistance(angle, radius)) && radius <= outermost + ChartSystemFactory.ENTRY_OFFSET) {
            radius = clearRadius(blocked, radius + ARRIVAL_SEARCH_STEP);
        }
        return polar(angle, radius);
    }

    static float arrivalDistance(double angle, float radius) {
        Vector2f wormhole = polar(angle, radius);
        return new Vector2f(wormhole.x + ChartSystemFactory.ENTRY_OFFSET, wormhole.y).length();
    }

    private static Vector2f polar(double angle, float radius) {
        return new Vector2f((float) (Math.cos(angle) * radius), (float) (Math.sin(angle) * radius));
    }

    static boolean isClear(List<Band> blocked, float distance) {
        for (Band band : blocked) {
            if (band.contains(distance)) {
                return false;
            }
        }
        return true;
    }

    static float clearRadius(List<Band> blocked, float startRadius) {
        float radius = startRadius;
        boolean moved = true;
        while (moved) {
            moved = false;
            for (Band band : blocked) {
                if (band.contains(radius)) {
                    radius = band.outer() + STEP_PAST_BAND;
                    moved = true;
                }
            }
        }
        return radius;
    }

    static List<Band> blockedBands(StarSystemAPI system) {
        List<Band> blocked = new ArrayList<>();
        for (PlanetAPI body : system.getPlanets()) {
            float clearance = body.getRadius() + (body.isStar() ? STAR_CLEARANCE : PLANET_CLEARANCE);
            blocked.add(centreBand(body, 0).widenedBy(clearance));
        }
        for (CampaignTerrainAPI terrain : system.getTerrainCopy()) {
            Band band = ringTerrainBand(terrain);
            if (band != null) {
                blocked.add(band);
            }
        }
        return blocked;
    }

    private static Band ringTerrainBand(CampaignTerrainAPI terrain) {
        CampaignTerrainPlugin plugin = terrain.getPlugin();
        if (!(plugin instanceof BaseRingTerrain ring) || ring.params == null || plugin instanceof PulsarBeamTerrainPlugin) {
            return null;
        }
        float outerEdge = ring.params.middleRadius + ring.params.bandWidthInEngine * 0.5f;
        float innerEdge = Math.max(0f, ring.params.middleRadius - ring.params.bandWidthInEngine * 0.5f);
        float flareReach = plugin instanceof FlareManagerDelegate flares ? flareReach(flares, outerEdge) : 0f;
        return centreBand(terrain, 0).sweptAt(innerEdge, outerEdge + flareReach).widenedBy(TERRAIN_CLEARANCE);
    }

    static float flareReach(FlareManagerDelegate flares, float outerEdge) {
        float largeFlare = (flares.getFlareExtraLengthMultMax() - 1f) * outerEdge + flares.getFlareExtraLengthFlatMax();
        float smallFlare = (flares.getFlareSmallExtraLengthMultMax() - 1f) * outerEdge + flares.getFlareSmallExtraLengthFlatMax();
        return Math.max(0f, Math.max(largeFlare, smallFlare));
    }

    static Band centreBand(SectorEntityToken entity, int depth) {
        SectorEntityToken focus = entity.getOrbitFocus();
        if (focus == null || focus == entity || depth >= MAX_ORBIT_DEPTH) {
            Vector2f location = entity.getLocation();
            float distance = location == null ? 0f : location.length();
            return new Band(distance, distance);
        }
        float orbitRadius = entity.getCircularOrbitRadius();
        if (orbitRadius <= 0f && entity.getLocation() != null && focus.getLocation() != null) {
            orbitRadius = Vector2f.sub(entity.getLocation(), focus.getLocation(), new Vector2f()).length();
        }
        return centreBand(focus, depth + 1).sweptAt(orbitRadius, orbitRadius);
    }

    record Band(float inner, float outer) {

        Band widenedBy(float amount) {
            return new Band(Math.max(0f, inner - amount), outer + amount);
        }

        Band sweptAt(float innerDistance, float outerDistance) {
            float nearest = Math.max(0f, Math.max(inner - outerDistance, innerDistance - outer));
            return new Band(nearest, outer + outerDistance);
        }

        boolean contains(float radius) {
            return radius >= inner && radius <= outer;
        }
    }
}
