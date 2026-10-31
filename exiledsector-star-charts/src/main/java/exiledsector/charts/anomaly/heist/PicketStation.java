package exiledsector.charts.anomaly.heist;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import org.lwjgl.util.vector.Vector2f;

public final class PicketStation {

    private final int index;
    private final float angle;
    private final float radius;
    private String fleetId;
    private SectorEntityToken point;

    PicketStation(int index, float angle, float radius) {
        this.index = index;
        this.angle = angle;
        this.radius = radius;
    }

    int index() {
        return index;
    }

    String fleetId() {
        return fleetId;
    }

    void setFleetId(String fleetId) {
        this.fleetId = fleetId;
    }

    SectorEntityToken point() {
        return point;
    }

    void setPoint(SectorEntityToken point) {
        this.point = point;
    }

    Vector2f around(Vector2f centre) {
        double radians = Math.toRadians(angle);
        return new Vector2f(centre.x + (float) Math.cos(radians) * radius, centre.y + (float) Math.sin(radians) * radius);
    }

    void follow(Vector2f centre) {
        if (point != null && centre != null) {
            Vector2f at = around(centre);
            point.setLocation(at.x, at.y);
        }
    }
}
