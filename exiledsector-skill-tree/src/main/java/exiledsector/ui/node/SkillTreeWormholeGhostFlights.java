package exiledsector.ui.node;

import exiledsector.skills.ExoshipNodes;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTreeLinks;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillTreeTopology;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.decoration.GhostFlight;
import exiledsector.ui.decoration.GhostFlightPool;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

final class SkillTreeWormholeGhostFlights {

    static final float MIN_SECONDS_BETWEEN_FLIGHTS = 10f;
    static final float MAX_SECONDS_BETWEEN_FLIGHTS = 25f;
    private static final float FLIGHT_SPEED = 1400f;
    private static final float GHOST_SIZE_RATIO = 0.35f;

    private final Random random;
    private final Map<String, float[]> secondsUntilNextFlight = new HashMap<>();
    private final GhostFlightPool flights = new GhostFlightPool();
    private List<SkillTreeTopology.WormholePair> pairs = List.of();
    private ShipSkillData pairsShip;
    private SkillTreeTopology pairsTopology;
    private int pairsRevision = -1;

    SkillTreeWormholeGhostFlights(Random random) {
        this.random = random;
    }

    void advance(float amount, ShipSkillData skillData) {
        flights.advance(amount);
        for (SkillTreeTopology.WormholePair pair : pairsFor(skillData)) {
            if (skillData.isAllocated(pair.first().getId()) && skillData.isAllocated(pair.second().getId())
                    && !ExoshipNodes.isCarried(pair.first()) && !ExoshipNodes.isCarried(pair.second())) {
                scheduleFlights(pair, amount);
            }
        }
    }

    private List<SkillTreeTopology.WormholePair> pairsFor(ShipSkillData skillData) {
        SkillTreeTopology topology = SkillTree.topology();
        if (skillData != pairsShip || skillData.revision() != pairsRevision || topology != pairsTopology) {
            pairs = ShipTreeLinks.wormholePairs(skillData, topology);
            pairsShip = skillData;
            pairsRevision = skillData.revision();
            pairsTopology = topology;
        }
        return pairs;
    }

    int activeFlightCount() {
        return flights.size();
    }

    private void scheduleFlights(SkillTreeTopology.WormholePair pair, float amount) {
        String pairKey = pair.first().getId();
        float[] remainingSeconds = secondsUntilNextFlight.get(pairKey);
        if (remainingSeconds == null) {
            remainingSeconds = new float[]{random.nextFloat() * MAX_SECONDS_BETWEEN_FLIGHTS};
            secondsUntilNextFlight.put(pairKey, remainingSeconds);
        }
        remainingSeconds[0] -= amount;
        if (remainingSeconds[0] <= 0f) {
            boolean forwards = random.nextBoolean();
            flights.add(forwards ? launch(pair.first(), pair.second()) : launch(pair.second(), pair.first()));
            remainingSeconds[0] = MIN_SECONDS_BETWEEN_FLIGHTS
                    + random.nextFloat() * (MAX_SECONDS_BETWEEN_FLIGHTS - MIN_SECONDS_BETWEEN_FLIGHTS);
        }
    }

    void launchFrom(SkillNode from, SkillNode to) {
        flights.add(launch(from, to));
    }

    private GhostFlight launch(SkillNode from, SkillNode to) {
        return GhostFlight.between(from.getOffsetX(), from.getOffsetY(), to.getOffsetX(), to.getOffsetY(), FLIGHT_SPEED, random);
    }

    void draw(TreeViewport viewport, float alphaMult) {
        flights.draw(NodeMetrics.NODE_SIZE * GHOST_SIZE_RATIO * viewport.zoom(), viewport, alphaMult);
    }
}
