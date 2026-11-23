package exiledsector.skills;

import exiledsector.skills.tags.SkillTags;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ResearchStationNodes {

    public static final String STATION_TYPE_ID = "ai_subsystem_integration";

    private static SkillTreeTopology indexedTopology;
    private static SkillNode indexedStation;
    private static List<SkillNode> indexedSubsystems = List.of();

    private ResearchStationNodes() {
    }

    public static SkillNode station() {
        index();
        return indexedStation;
    }

    public static List<SkillNode> subsystems() {
        index();
        return indexedSubsystems;
    }

    public static boolean isStation(SkillNode node) {
        return node != null && node.getType() != null && STATION_TYPE_ID.equals(node.getType().getId());
    }

    public static boolean isSubsystem(SkillNode node) {
        return node != null && node.getType() != null && node.getType().getTags().contains(SkillTags.STATION_SUBSYSTEM);
    }

    public static boolean isStationNode(SkillNode node) {
        return isStation(node) || isSubsystem(node);
    }

    public static boolean isStationNodeId(String nodeId) {
        return isStationNode(SkillTree.get(nodeId));
    }

    public static boolean isActive(ShipSkillData data, boolean automated) {
        SkillNode station = station();
        return data != null && station != null && data.startingRootId != null && (automated || data.isIntegrated(station.getId()));
    }

    public static boolean countsAsIntegration(ShipSkillData data) {
        SkillNode station = station();
        return data != null && station != null && data.isIntegrated(station.getId());
    }

    public static SkillNode selectedSubsystem(ShipSkillData data) {
        if (data == null) {
            return null;
        }
        for (SkillNode subsystem : subsystems()) {
            if (data.isAllocated(subsystem.getId())) {
                return subsystem;
            }
        }
        return null;
    }

    public static boolean sync(ShipSkillData data, boolean automated) {
        SkillNode station = station();
        if (data == null || station == null) {
            return false;
        }
        boolean active = isActive(data, automated);
        boolean changed = false;
        if (active && !data.isAllocated(station.getId())) {
            ShipTrees.allocateGranted(data, station);
            changed = true;
        } else if (!active && data.isAllocated(station.getId())) {
            ShipTrees.deallocate(data, station);
            changed = true;
        }
        boolean keepOne = active;
        for (SkillNode subsystem : subsystems()) {
            if (!data.isAllocated(subsystem.getId())) {
                continue;
            }
            if (keepOne) {
                keepOne = false;
            } else {
                ShipTrees.deallocate(data, subsystem);
                changed = true;
            }
        }
        return changed;
    }

    public static void select(ShipSkillData data, SkillNode subsystem) {
        release(data);
        ShipTrees.allocateGranted(data, subsystem);
    }

    static boolean holdsOnlyFreeStationNodesBeside(ShipSkillData data, String rootId) {
        for (String nodeId : data.allocatedNodeIds) {
            if (!nodeId.equals(rootId) && (!isStationNodeId(nodeId) || countsAsIntegration(data))) {
                return false;
            }
        }
        return true;
    }

    static void releaseStation(ShipSkillData data) {
        release(data);
        SkillNode station = station();
        if (station != null && data.isAllocated(station.getId())) {
            ShipTrees.deallocate(data, station);
        }
    }

    public static boolean release(ShipSkillData data) {
        boolean released = false;
        for (SkillNode subsystem : subsystems()) {
            if (data.isAllocated(subsystem.getId())) {
                ShipTrees.deallocate(data, subsystem);
                released = true;
            }
        }
        return released;
    }

    private static void index() {
        SkillTreeTopology topology = SkillTree.topology();
        if (topology == indexedTopology) {
            return;
        }
        SkillNode station = null;
        List<SkillNode> subsystems = new ArrayList<>();
        for (SkillNode node : SkillTree.getAllNodes().values()) {
            if (isStation(node)) {
                station = node;
            } else if (isSubsystem(node)) {
                subsystems.add(node);
            }
        }
        if (station != null) {
            SkillNode centre = station;
            subsystems.sort(Comparator.comparingDouble(node -> ringAngle(centre, node)));
        }
        indexedStation = station;
        indexedSubsystems = List.copyOf(subsystems);
        indexedTopology = topology;
    }

    private static double ringAngle(SkillNode station, SkillNode node) {
        double angle = Math.atan2(node.getY() - station.getY(), node.getX() - station.getX()) + Math.PI / 2;
        return angle < 0 ? angle + 2 * Math.PI : angle;
    }
}
