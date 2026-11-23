package exiledsector.ui.decoration;

import exiledsector.skills.ResearchStationNodes;
import exiledsector.skills.SkillNode;
import exiledsector.skills.decoration.Orbit;

import java.util.Random;

public final class ResearchStationOrbit {

    public static final float ORBIT_SECONDS = 180f;

    private final SkillNode stationNode;
    private final Orbit orbit;
    private float elapsedSeconds;

    public ResearchStationOrbit(SkillNode stationNode, float elapsedSeconds) {
        this.stationNode = stationNode;
        this.orbit = stationNode == null ? null : orbitOf(stationNode);
        this.elapsedSeconds = elapsedSeconds;
    }

    public static ResearchStationOrbit atRandomPoint(Random random) {
        return new ResearchStationOrbit(ResearchStationNodes.station(), random.nextFloat() * ORBIT_SECONDS);
    }

    private static Orbit orbitOf(SkillNode stationNode) {
        float radius = (float) Math.hypot(stationNode.getX(), stationNode.getY());
        float startAngle = (float) Math.toDegrees(Math.atan2(stationNode.getY(), stationNode.getX()));
        return new Orbit(radius, ORBIT_SECONDS, false, startAngle);
    }

    public SkillNode stationNode() {
        return stationNode;
    }

    public void advance(float amount) {
        elapsedSeconds += amount;
    }

    public float angleDeg() {
        return orbit == null ? 0f : orbit.angleDegAt(elapsedSeconds);
    }

    public float x() {
        return orbit == null ? 0f : orbit.radius() * (float) Math.cos(Math.toRadians(angleDeg()));
    }

    public float y() {
        return orbit == null ? 0f : orbit.radius() * (float) Math.sin(Math.toRadians(angleDeg()));
    }
}
