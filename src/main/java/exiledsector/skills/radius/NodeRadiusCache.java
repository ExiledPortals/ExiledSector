package exiledsector.skills.radius;

import exiledsector.skills.ExoshipNodes;
import exiledsector.skills.SkillNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class NodeRadiusCache {

    private static final RadiusSize[] SIZES = RadiusSize.values();

    private final Map<RadiusSize, Float> builtDistances;
    private final Map<RadiusSize, Map<String, Set<String>>> nodeIdsWithinBySize;

    private NodeRadiusCache(Collection<SkillNode> nodes, Map<RadiusSize, Float> distancesBySize) {
        builtDistances = distancesBySize;
        List<SkillNode> centres = new ArrayList<>();
        for (SkillNode node : nodes) {
            if (!isExcluded(node)) {
                centres.add(node);
            }
        }
        float[] squaredLimits = new float[SIZES.length];
        Map<RadiusSize, Map<String, Set<String>>> building = new EnumMap<>(RadiusSize.class);
        for (RadiusSize size : SIZES) {
            float distance = distancesBySize.getOrDefault(size, 0f);
            squaredLimits[size.ordinal()] = distance > 0f ? distance * distance : -1f;
            Map<String, Set<String>> nodeIdsWithin = new HashMap<>();
            for (SkillNode centre : centres) {
                nodeIdsWithin.put(centre.getId(), new LinkedHashSet<>());
            }
            building.put(size, nodeIdsWithin);
        }
        for (int first = 0; first < centres.size(); first++) {
            SkillNode firstNode = centres.get(first);
            for (int second = first + 1; second < centres.size(); second++) {
                SkillNode secondNode = centres.get(second);
                float deltaX = firstNode.getOffsetX() - secondNode.getOffsetX();
                float deltaY = firstNode.getOffsetY() - secondNode.getOffsetY();
                float squaredDistance = deltaX * deltaX + deltaY * deltaY;
                for (RadiusSize size : SIZES) {
                    if (squaredDistance <= squaredLimits[size.ordinal()]) {
                        Map<String, Set<String>> nodeIdsWithin = building.get(size);
                        nodeIdsWithin.get(firstNode.getId()).add(secondNode.getId());
                        nodeIdsWithin.get(secondNode.getId()).add(firstNode.getId());
                    }
                }
            }
        }
        Map<RadiusSize, Map<String, Set<String>>> frozen = new EnumMap<>(RadiusSize.class);
        building.forEach((size, nodeIdsWithin) -> {
            Map<String, Set<String>> frozenSets = new HashMap<>();
            nodeIdsWithin.forEach((centreId, nodeIds) -> frozenSets.put(centreId, Collections.unmodifiableSet(nodeIds)));
            frozen.put(size, frozenSets);
        });
        nodeIdsWithinBySize = frozen;
    }

    public static NodeRadiusCache of(Collection<SkillNode> nodes, Map<RadiusSize, Float> distancesBySize) {
        return new NodeRadiusCache(nodes, distancesBySize);
    }

    public static boolean isExcluded(SkillNode node) {
        return ExoshipNodes.isCarried(node) || ExoshipNodes.isExoship(node);
    }

    public boolean isBuiltWith(Map<RadiusSize, Float> distancesBySize) {
        return builtDistances == distancesBySize;
    }

    public boolean isCentre(String nodeId) {
        return nodeIdsWithinBySize.get(RadiusSize.SMALL).containsKey(nodeId);
    }

    public Set<String> nodeIdsWithin(String centreId, RadiusSize size) {
        return nodeIdsWithinBySize.get(size).getOrDefault(centreId, Set.of());
    }
}
