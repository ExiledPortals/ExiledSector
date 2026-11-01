package exiledsector.ui.decoration;

import com.fs.starfarer.api.Global;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.ui.hyperspace.HyperspaceAnchor;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

public final class FactionFleetRenderer implements DecorationRenderer {

    static final int MIN_FLEETS_PER_VOLUME = 2;
    static final int MAX_FLEETS_PER_VOLUME = 3;
    static final float FLEET_RADIUS = 40f;
    static final float MIN_TRAVEL_SPEED = 90f;
    static final float MAX_TRAVEL_SPEED = 180f;
    static final float VOLUME_TRAVEL_CHANCE = 0.15f;
    static final float ENCOUNTER_CHANCE = 0.15f;
    static final float MIN_RESPAWN_SECONDS = 30f;
    static final float MAX_RESPAWN_SECONDS = 60f;

    private final LaneFleetTraffic.Factions factions = LaneFleetTraffic.sectorFactions();
    private final FleetTraffic traffic = new FleetTraffic(new Random(), MIN_RESPAWN_SECONDS, MAX_RESPAWN_SECONDS, this::spawn);
    private final Map<String, RoamVolume> volumesByRegion = new LinkedHashMap<>();

    private FactionFleetRenderer() {
    }

    public static FactionFleetRenderer forActiveTree(Predicate<SkillNode> nodeHidden) {
        FactionFleetRenderer renderer = new FactionFleetRenderer();
        if (Global.getSector() == null) {
            return renderer;
        }
        Collection<SkillNode> nodes = SkillTree.getAllNodes().values();
        Set<String> lockedRegions = HyperspaceAnchor.lockedRegions(nodes, SkillNode::getRegion, nodeHidden);
        for (String region : TreeFleetFactions.regions()) {
            RoamVolume volume = lockedRegions.contains(region) ? null : TreeRoamVolumes.region(region, SkillTree.getDecorations().stars(), nodes);
            if (volume != null) {
                renderer.volumesByRegion.put(region, volume);
            }
        }
        Random random = renderer.traffic.random();
        for (String region : renderer.volumesByRegion.keySet()) {
            int fleetCount = MIN_FLEETS_PER_VOLUME + random.nextInt(MAX_FLEETS_PER_VOLUME - MIN_FLEETS_PER_VOLUME + 1);
            for (int i = 0; i < fleetCount; i++) {
                FactionFleet fleet = renderer.spawn(region);
                if (fleet != null) {
                    fleet.showFullySpawned();
                }
            }
        }
        return renderer;
    }

    static RoamVolume nextVolume(RoamVolume current, RoamVolume home, List<RoamVolume> alliedVolumes, Random random) {
        if (alliedVolumes.isEmpty() || random.nextFloat() >= VOLUME_TRAVEL_CHANCE) {
            return current;
        }
        List<RoamVolume> destinations = new ArrayList<>(alliedVolumes);
        destinations.add(home);
        destinations.remove(current);
        return destinations.get(random.nextInt(destinations.size()));
    }

    private FactionFleet spawn(String region) {
        RoamVolume home = volumesByRegion.get(region);
        List<String> factionIds = TreeFleetFactions.factionsIn(region).stream().filter(factionId -> !factions.hulls(factionId).isEmpty()).toList();
        if (home == null || factionIds.isEmpty()) {
            return null;
        }
        Random random = traffic.random();
        String factionId = factionIds.get(random.nextInt(factionIds.size()));
        Color factionColor = factions.colour(factionId);
        float travelSpeed = MIN_TRAVEL_SPEED + random.nextFloat() * (MAX_TRAVEL_SPEED - MIN_TRAVEL_SPEED);
        FactionFleet fleet = new FactionFleet(factionId, factionColor == null ? Color.WHITE : factionColor, region, home,
                alliedVolumes(factionId, region), travelSpeed, random, this::arrive);
        return traffic.addFleet(fleet, factions.hulls(factionId), FLEET_RADIUS);
    }

    private List<RoamVolume> alliedVolumes(String factionId, String homeRegion) {
        return volumesByRegion.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(homeRegion)
                        && TreeFleetFactions.factionsIn(entry.getKey()).stream().anyMatch(other -> allied(factionId, other)))
                .map(Map.Entry::getValue).toList();
    }

    private boolean allied(String factionId, String otherFactionId) {
        return !factionId.equals(otherFactionId) && factions.friendly(factionId, otherFactionId);
    }

    private RoamVolume arrive(FactionFleet fleet, RoamVolume current) {
        RoamVolume fleetVolume = fleet.flight().roamVolume();
        if (traffic.random().nextFloat() < ENCOUNTER_CHANCE && traffic.startBattle(fleet,
                other -> other.flight().roamVolume() != fleetVolume && factions.hostile(fleet.factionId(), other.factionId()))) {
            return current;
        }
        return nextVolume(current, fleet.home(), fleet.alliedVolumes(), traffic.random());
    }

    @Override
    public void advance(float amount) {
        traffic.advance(amount);
    }

    @Override
    public void render(DecorationScene scene, DecorationDepth depth) {
        if (depth == DecorationDepth.FLEETS) {
            traffic.render(scene.viewport(), scene.sceneryAlpha() * scene.treeAlpha());
        }
    }
}
