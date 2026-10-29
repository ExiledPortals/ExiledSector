package exiledsector.charts.layout;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

final class ChartBodyNames {

    static final int DESIGNATION_RANGE = 0x10000;
    static final String STAR_LETTERS = "ABC";
    static final String MOON_LETTERS = "abcdefghijklmnopqrstuvwxyz";

    private ChartBodyNames() {
    }

    static String designation(Random namingRandom) {
        return String.format(Locale.ROOT, "%04X", namingRandom.nextInt(DESIGNATION_RANGE));
    }

    static void assign(StarSystemAPI system, String designation) {
        Map<PlanetAPI, String> names = new IdentityHashMap<>();
        nameStars(system, designation, names);
        List<PlanetAPI> bodies = new ArrayList<>();
        for (PlanetAPI planet : system.getPlanets()) {
            if (!planet.isStar()) {
                bodies.add(planet);
            }
        }
        bodies.sort(Comparator.comparingDouble(ChartBodyNames::orbitDistance).thenComparing(ChartBodyNames::idOrEmpty));
        int planetIndex = 0;
        for (PlanetAPI body : bodies) {
            if (!(parentPlanet(body) instanceof PlanetAPI)) {
                planetIndex++;
                names.put(body, planetName(designation, planetIndex));
            }
        }
        boolean namedAny = true;
        while (namedAny) {
            namedAny = false;
            Map<PlanetAPI, Integer> moonsSoFar = new IdentityHashMap<>();
            for (PlanetAPI body : bodies) {
                if (names.containsKey(body) || !(parentPlanet(body) instanceof PlanetAPI parent) || !names.containsKey(parent)) {
                    continue;
                }
                int moonIndex = moonsSoFar.merge(parent, 1, Integer::sum) - 1;
                names.put(body, moonName(names.get(parent), moonIndex));
                namedAny = true;
            }
        }
        for (PlanetAPI body : bodies) {
            if (!names.containsKey(body)) {
                planetIndex++;
                names.put(body, planetName(designation, planetIndex));
            }
        }
        names.forEach(ChartBodyNames::rename);
    }

    private static void nameStars(StarSystemAPI system, String designation, Map<PlanetAPI, String> names) {
        int starIndex = 0;
        for (PlanetAPI star : new PlanetAPI[]{system.getStar(), system.getSecondary(), system.getTertiary()}) {
            if (star != null && !names.containsKey(star) && starIndex < STAR_LETTERS.length()) {
                names.put(star, starName(designation, STAR_LETTERS.charAt(starIndex)));
                starIndex++;
            }
        }
        for (PlanetAPI planet : system.getPlanets()) {
            if (planet.isStar() && !names.containsKey(planet)) {
                names.put(planet, starName(designation, STAR_LETTERS.charAt(Math.min(starIndex, STAR_LETTERS.length() - 1))));
                starIndex++;
            }
        }
    }

    private static SectorEntityToken parentPlanet(PlanetAPI body) {
        SectorEntityToken focus = body.getOrbitFocus();
        return focus instanceof PlanetAPI planet && !planet.isStar() && planet != body ? focus : null;
    }

    static float orbitDistance(SectorEntityToken entity) {
        if (entity.getOrbitFocus() != null && entity.getCircularOrbitRadius() > 0f) {
            return entity.getCircularOrbitRadius();
        }
        Vector2f location = entity.getLocation();
        return location == null ? 0f : location.length();
    }

    private static String idOrEmpty(SectorEntityToken entity) {
        return entity.getId() == null ? "" : entity.getId();
    }

    private static void rename(PlanetAPI body, String name) {
        body.setName(name);
        if (body.getMarket() != null) {
            body.getMarket().setName(name);
        }
    }

    static String starName(String designation, char letter) {
        return I18n.forGameText(() -> Translation.msg("chart.layout.name.star").arg("designation", designation).arg("letter", String.valueOf(letter)).text());
    }

    static String planetName(String designation, int index) {
        return I18n.forGameText(() -> Translation.msg("chart.layout.name.planet").arg("designation", designation).arg("index", index).text());
    }

    static String moonName(String planetName, int moonIndex) {
        String letter = String.valueOf(MOON_LETTERS.charAt(Math.min(moonIndex, MOON_LETTERS.length() - 1)));
        return I18n.forGameText(() -> Translation.msg("chart.layout.name.moon").arg("planet", planetName).arg("letter", letter).text());
    }
}
