package exiledsector.ui.decoration;

import exiledsector.skills.layout.Star;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TreeFactionColoursTest {

    @Test
    void factionsTakeTheColourOfTheirRegionsStar() {
        List<Star> stars = List.of(new Star("tritach_star", 0f, 0f, 400f, "star_blue_giant", "#00479e", List.of("tritachyon")),
                new Star("pirate_star", 0f, 0f, 300f, "star_red_dwarf", null, List.of("pirate")));
        Map<String, List<String>> factions = Map.of("tritachyon", List.of("tritachyon"), "pirate", List.of("pirates", "pirate_raiders"),
                "hegemony", List.of("hegemony"));
        Map<String, Color> colours = TreeFactionColours.colours(stars, List.of("tritachyon", "pirate", "hegemony"), factions::get,
                type -> "star_red_dwarf".equals(type) ? new Color(255, 80, 60, 128) : null);

        assertEquals(new Color(0x00, 0x47, 0x9e), colours.get("tritachyon"));
        assertEquals(new Color(255, 80, 60), colours.get("pirates"));
        assertEquals(colours.get("pirates"), colours.get("pirate_raiders"));
        assertEquals(255, colours.get("pirates").getAlpha());
        assertFalse(colours.containsKey("hegemony"));
    }
}
