package exiledsector;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.PlanetSpecAPI;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PlanetSpecs {

    private record Lookup(SettingsAPI settings, Map<String, PlanetSpecAPI> specsByType) {
    }

    private static volatile Lookup lookup;

    private PlanetSpecs() {
    }

    public static PlanetSpecAPI byType(String planetType) {
        SettingsAPI settings = Global.getSettings();
        if (settings == null || planetType == null) {
            return null;
        }
        Lookup current = lookup;
        if (current == null || current.settings() != settings) {
            List<PlanetSpecAPI> specs = settings.getAllPlanetSpecs();
            Map<String, PlanetSpecAPI> specsByType = new HashMap<>();
            for (PlanetSpecAPI spec : specs == null ? List.<PlanetSpecAPI>of() : specs) {
                specsByType.put(spec.getPlanetType(), spec);
            }
            current = new Lookup(settings, specsByType);
            if (!specsByType.isEmpty()) {
                lookup = current;
            }
        }
        return current.specsByType().get(planetType);
    }
}
