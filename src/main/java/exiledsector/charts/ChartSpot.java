package exiledsector.charts;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import org.lwjgl.util.vector.Vector2f;

public record ChartSpot(Object vanillaLocation, Vector2f position, SectorEntityToken focus, float weight) {

    public float orbitRadius() {
        if (focus == null || focus.getLocation() == null || position == null) {
            return 0f;
        }
        return Vector2f.sub(position, focus.getLocation(), new Vector2f()).length();
    }
}
