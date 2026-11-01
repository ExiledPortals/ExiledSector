package exiledsector.ui.decoration;

import com.fs.starfarer.api.campaign.PlanetSpecAPI;
import exiledsector.PlanetSpecs;
import exiledsector.skills.SkillTree;
import exiledsector.skills.decoration.Star;
import exiledsector.ui.util.ColorUtil;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class TreeFactionColours {

    private TreeFactionColours() {
    }

    public static void publish() {
        FactionStarColours.register(colours(SkillTree.getDecorations().stars(), TreeFleetFactions.regions(), TreeFleetFactions::factionsIn,
                TreeFactionColours::coronaColour));
    }

    private static Color coronaColour(String starType) {
        PlanetSpecAPI spec = PlanetSpecs.byType(starType);
        return spec != null && spec.isStar() ? spec.getCoronaColor() : null;
    }

    static Map<String, Color> colours(List<Star> stars, List<String> regions, Function<String, List<String>> factionsIn,
                                      Function<String, Color> starTypeColour) {
        Map<String, Color> colours = new HashMap<>();
        for (String region : regions) {
            Star star = stars.stream().filter(candidate -> region.equals(candidate.getRegion())).findFirst().orElse(null);
            Color colour = star == null ? null : ColorUtil.parseHexColor(star.getColor(), starTypeColour.apply(star.getStarType()));
            if (colour == null) {
                continue;
            }
            Color opaque = new Color(colour.getRed(), colour.getGreen(), colour.getBlue());
            for (String factionId : factionsIn.apply(region)) {
                colours.putIfAbsent(factionId, opaque);
            }
        }
        return colours;
    }
}
