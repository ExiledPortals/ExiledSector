package exiledsector.compat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import exiledlib.compat.CompatTarget;
import lunalib.lunaSettings.LunaSettings;
import org.apache.log4j.Logger;
import org.magiclib.util.MagicSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class UnknownSkiesCompat {

    public static final String MOD_ID = "US";
    public static final String SETTINGS_ID = "unknownSkies";
    public static final List<String> TESTED_VERSIONS = List.of("3.0.3");
    public static final CompatTarget TARGET = new CompatTarget("Unknown Skies", List.of(MOD_ID), TESTED_VERSIONS, UnknownSkiesCompat::missingFeatures);
    static final String STAR_MODE_SETTING = "US_starMode";
    static final String VANILLA_MODE = "Vanilla";
    static final String CLASSIC_MODE = "Classic";
    static final String NEBULA_SUFFIX = "n.jpg";
    static final List<String> VARIANT_SUFFIXES = List.of("", "_2", "_3");
    static final Map<String, String> STAR_SPRITES = Map.of("star_blue_giant", "US_star_blue_giant", "star_blue_supergiant", "US_star_blue_giant",
            "star_yellow", "US_star_yellow", "star_orange", "US_star_orange", "star_orange_giant", "US_star_orange", "star_red_giant",
            "US_star_red_giant", "star_red_supergiant", "US_star_red_giant", "star_white", "US_star_white", "star_browndwarf", "US_star_browndwarf");
    private static final Logger LOG = Logger.getLogger(UnknownSkiesCompat.class);
    private static volatile Map<String, String> backgroundAges;

    private UnknownSkiesCompat() {
    }

    public static boolean isModEnabled() {
        return TARGET.isEnabled();
    }

    public static List<String> backgrounds(StarAge age, boolean nebula) {
        if (!isModEnabled() || age == null) {
            return List.of();
        }
        List<String> fitting = new ArrayList<>();
        backgroundAges().forEach((path, ages) -> {
            if (fits(ages, age) && (!nebula || path.endsWith(NEBULA_SUFFIX))) {
                fitting.add(path);
            }
        });
        fitting.sort(String::compareTo);
        return fitting;
    }

    static boolean fits(String ages, StarAge age) {
        return "ALL".equals(ages) || ages.contains(age.name());
    }

    private static Map<String, String> backgroundAges() {
        Map<String, String> cached = backgroundAges;
        if (cached == null) {
            try {
                cached = Map.copyOf(MagicSettings.getStringMap(SETTINGS_ID, "backgrounds"));
            } catch (RuntimeException | LinkageError e) {
                LOG.warn("[ExiledSector] Reading Unknown Skies backgrounds failed; Star Chart systems use their own", e);
                cached = Map.of();
            }
            backgroundAges = cached;
        }
        return cached;
    }

    public static void varyStar(PlanetAPI star, Random random) {
        if (!isModEnabled() || star == null || star.getSpec() == null) {
            return;
        }
        String base = STAR_SPRITES.get(star.getTypeId());
        String mode = starMode();
        if (base == null || mode == null || VANILLA_MODE.equals(mode)) {
            return;
        }
        List<String> variants = new ArrayList<>();
        for (String suffix : CLASSIC_MODE.equals(mode) ? VARIANT_SUFFIXES.subList(0, 1) : VARIANT_SUFFIXES) {
            String sprite = spriteName(base + suffix);
            if (sprite != null) {
                variants.add(sprite);
            }
        }
        int pick = random.nextInt(variants.size() + 1);
        if (pick < variants.size()) {
            star.getSpec().setTexture(variants.get(pick));
            star.applySpecChanges();
        }
    }

    private static String starMode() {
        try {
            return LunaSettings.getString(MOD_ID, STAR_MODE_SETTING);
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    private static String spriteName(String id) {
        try {
            return Global.getSettings().getSpriteName("stars", id);
        } catch (RuntimeException e) {
            return null;
        }
    }

    static void resetForTests() {
        backgroundAges = null;
    }

    static List<String> missingFeatures() {
        List<String> missing = new ArrayList<>();
        if (backgroundAges().isEmpty()) {
            missing.add("the unknownSkies backgrounds map in modSettings.json, used for Star Chart system backgrounds");
        }
        if (spriteName("US_star_yellow") == null) {
            missing.add("the US_star_* star sprites, used to vary Star Chart stars");
        }
        return missing;
    }
}
