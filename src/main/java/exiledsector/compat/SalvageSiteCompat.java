package exiledsector.compat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;
import exiledsector.ModCsv;
import exiledsector.ModSettings;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class SalvageSiteCompat {

    public static final String DROPS_FIELD_ID = "exiledSector_socketableOtherModDrops";
    public static final boolean DEFAULT_DROPS = true;
    static final String FOLDER = "data/config/exiledSector/compat/salvage/";

    public record Source(String name, String modId, List<String> testedVersions) {

        public String file() {
            return FOLDER + modId + ".csv";
        }

        public CompatTarget target() {
            return new CompatTarget(name + " salvage sites", List.of(modId), testedVersions, () -> missingSites(this));
        }
    }

    public static final List<Source> SOURCES = List.of(
            new Source("Random Assortment of Things", "assortment_of_things", List.of("3.3.1")),
            new Source("Industrial Evolution", "IndEvo", List.of("4.1.b")),
            new Source("Knights of Ludd", "knights_of_ludd", List.of("1.4.0")),
            new Source("Secrets of the Frontier", "secretsofthefrontier", List.of("0.15.1")),
            new Source("Arthr's Ships n Shit", "arthrships", List.of("0.100-dev")),
            new Source("What We Left Behind", "niko_morePlanetaryConditions", List.of("4.5.3")),
            new Source("Ship Mastery System", "shipmasterysystem", List.of("2.0.8")),
            new Source("Unthemed Weapons Collection", "unthemedweapons", List.of("0.7.4")),
            new Source("DIY Planets", "diyplanets", List.of("1.0.30")));

    private SalvageSiteCompat() {
    }

    public static boolean dropsEnabled() {
        return ModSettings.booleanOr(DROPS_FIELD_ID, DEFAULT_DROPS);
    }

    public static List<CompatTarget> targets() {
        return SOURCES.stream().map(Source::target).toList();
    }

    public static List<Source> enabledSources() {
        SettingsAPI settings = Global.getSettings();
        ModManagerAPI mods = settings == null ? null : settings.getModManager();
        if (mods == null) {
            return List.of();
        }
        return SOURCES.stream().filter(source -> mods.isModEnabled(source.modId())).toList();
    }

    public static JSONArray rows(Source source) throws IOException, JSONException {
        return ModCsv.rows("site", source.file());
    }

    private static List<String> missingSites(Source source) {
        List<String> missing = new ArrayList<>();
        try {
            ModCsv.forEach(rows(source), (index, row) -> {
                String site = ModCsv.text(row, "site");
                if (!site.isEmpty() && !hasEntitySpec(site)) {
                    missing.add("the salvage site " + site);
                }
            });
        } catch (IOException | JSONException e) {
            missing.add("a readable " + source.file() + " (" + e.getMessage() + ")");
        }
        return missing;
    }

    // java:S1181: an unknown custom entity id can surface as any RuntimeException from the game, and it only means the site is missing
    @SuppressWarnings("java:S1181")
    private static boolean hasEntitySpec(String site) {
        try {
            return Global.getSettings().getCustomEntitySpec(site) != null;
        } catch (Throwable e) {
            return false;
        }
    }
}
