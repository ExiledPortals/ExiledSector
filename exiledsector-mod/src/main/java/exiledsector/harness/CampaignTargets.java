package exiledsector.harness;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.anomaly.excavation.ExcavationSite;
import exiledsector.charts.anomaly.excavation.ExcavationStates;
import exiledsector.charts.anomaly.listeningpost.ListeningPost;
import exiledsector.charts.run.ChartRun;
import exiledsector.i18n.Translation;
import org.lwjgl.util.vector.Vector2f;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class CampaignTargets {

    static final String ENTITY_PREFIX = "entity:";
    static final String FLEET_PREFIX = "fleet:";
    static final String BOLT_HOLE = "boltHole";
    static final String PICKET_GAP = "picketGap";
    static final String PICKET_GAP_OUTSIDE = "picketGap:outside";
    static final String DESCRIPTION = ENTITY_PREFIX + "<custom entity type, tag or id>, " + FLEET_PREFIX + "<fleet name>, " + BOLT_HOLE + ", "
            + PICKET_GAP + " or " + PICKET_GAP_OUTSIDE;
    static final float OUTSIDE_GAP_DISTANCE = 400f;
    private static final String PICKET_NAME_KEY = "chart.anomaly.listening_post.fleet.picket.name";

    private CampaignTargets() {
    }

    static boolean isKnown(String target) {
        if (target == null) {
            return false;
        }
        return BOLT_HOLE.equals(target) || PICKET_GAP.equals(target) || PICKET_GAP_OUTSIDE.equals(target)
                || Stream.of(ENTITY_PREFIX, FLEET_PREFIX).anyMatch(prefix -> target.startsWith(prefix) && !target.substring(prefix.length()).isBlank());
    }

    static SectorEntityToken entity(String target) {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        LocationAPI location = playerFleet.getContainingLocation();
        if (target.startsWith(ENTITY_PREFIX)) {
            String wanted = target.substring(ENTITY_PREFIX.length()).trim();
            return nearest(playerFleet, location.getAllEntities().stream().filter(entity -> entity != playerFleet && matches(entity, wanted)));
        }
        if (target.startsWith(FLEET_PREFIX)) {
            String wanted = target.substring(FLEET_PREFIX.length()).trim();
            return nearest(playerFleet, location.getFleets().stream().filter(fleet -> fleet != playerFleet && wanted.equalsIgnoreCase(fleet.getName())));
        }
        if (BOLT_HOLE.equals(target)) {
            ChartRun run = HarnessCampaign.runAroundPlayer(Global.getSector());
            if (run == null) {
                return null;
            }
            Set<String> sitePlanetIds = ExcavationStates.standing(run).stream().map(ExcavationSite::planetId).collect(Collectors.toSet());
            return nearest(playerFleet, location.getPlanets().stream().filter(planet -> sitePlanetIds.contains(planet.getId())));
        }
        if (PICKET_GAP.equals(target) || PICKET_GAP_OUTSIDE.equals(target)) {
            return nearest(playerFleet, location.getAllEntities().stream().filter(entity -> entity.hasTag(ListeningPost.POST_TAG)));
        }
        return null;
    }

    static Vector2f worldPoint(String target) {
        SectorEntityToken entity = entity(target);
        if (entity == null) {
            return null;
        }
        if (PICKET_GAP.equals(target) || PICKET_GAP_OUTSIDE.equals(target)) {
            return picketGap(entity, PICKET_GAP_OUTSIDE.equals(target) ? OUTSIDE_GAP_DISTANCE : 0f);
        }
        return new Vector2f(entity.getLocation());
    }

    static Vector2f screenPoint(String target) {
        Vector2f world = worldPoint(target);
        ViewportAPI viewport = Global.getSector().getViewport();
        return world == null || viewport == null ? null
                : new Vector2f(viewport.convertWorldXtoScreenX(world.x), viewport.convertWorldYtoScreenY(world.y));
    }

    static String unknownTargetMessage(String target) {
        return "no " + target + " is in the player fleet's location";
    }

    private static boolean matches(SectorEntityToken entity, String wanted) {
        return wanted.equals(entity.getId()) || entity.hasTag(wanted)
                || (entity.getCustomEntitySpec() != null && wanted.equals(entity.getCustomEntitySpec().getId()));
    }

    private static SectorEntityToken nearest(CampaignFleetAPI playerFleet, Stream<? extends SectorEntityToken> candidates) {
        return candidates.min(Comparator.comparingDouble(entity -> Misc.getDistance(playerFleet, entity))).orElse(null);
    }

    private static Vector2f picketGap(SectorEntityToken post, float beyondRing) {
        String picketName = Translation.gameText(PICKET_NAME_KEY);
        Vector2f postAt = post.getLocation();
        List<CampaignFleetAPI> pickets = post.getContainingLocation().getFleets().stream()
                .filter(fleet -> picketName.equalsIgnoreCase(fleet.getName()))
                .toList();
        if (pickets.isEmpty()) {
            return new Vector2f(postAt.x + beyondRing, postAt.y);
        }
        List<Float> angles = pickets.stream().map(picket -> Misc.getAngleInDegrees(postAt, picket.getLocation())).sorted().toList();
        float ringRadius = (float) pickets.stream().mapToDouble(picket -> Misc.getDistance(postAt, picket.getLocation())).average().orElse(0d);
        float gapMiddle = angles.get(0) + 180f;
        float widestGap = angles.size() == 1 ? 360f : 0f;
        for (int index = 0; index < angles.size() && angles.size() > 1; index++) {
            float from = angles.get(index);
            float to = index + 1 < angles.size() ? angles.get(index + 1) : angles.get(0) + 360f;
            if (to - from > widestGap) {
                widestGap = to - from;
                gapMiddle = (from + to) / 2f;
            }
        }
        Vector2f direction = Misc.getUnitVectorAtDegreeAngle(gapMiddle);
        float distance = ringRadius + beyondRing;
        return new Vector2f(postAt.x + direction.x * distance, postAt.y + direction.y * distance);
    }
}
