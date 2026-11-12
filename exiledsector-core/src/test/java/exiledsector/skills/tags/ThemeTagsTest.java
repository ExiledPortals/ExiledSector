package exiledsector.skills.tags;

import exiledsector.i18n.LocaleChain;
import exiledsector.i18n.RealCatalogue;
import org.json.JSONArray;
import org.json.JSONException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThemeTagsTest {

    @AfterEach
    void restoreTheShippedVocabulary() throws IOException, JSONException {
        RealThemeTags.load();
    }

    private static void register(String rowsJson) throws JSONException {
        ThemeTags.register(new JSONArray(rowsJson));
    }

    @Test
    void theShippedVocabularyLoadsEveryRow() throws IOException, JSONException {
        assertEquals(RealThemeTags.rows().length(), ThemeTags.ids().size());
        assertEquals("weapon", ThemeTags.ids().get(0));
    }

    @Test
    void aTagCountsAsEveryAncestor() {
        assertEquals("weapon", ThemeTags.parent("ballistic"));
        assertEquals(List.of("front_shield", "shield", "defence"), ThemeTags.withAncestors("front_shield"));
        assertEquals(Set.of("ballistic", "weapon", "low_hull", "conditional"), ThemeTags.allWithAncestors(List.of("ballistic", "low_hull")));
    }

    @Test
    void aTagListedBesideItsOwnAncestorNamesThatAncestor() {
        assertEquals(List.of("shield", "defence"), ThemeTags.ancestorsListedBeside("front_shield", List.of("defence", "front_shield", "shield", "flux")));
        assertEquals(List.of(), ThemeTags.ancestorsListedBeside("ballistic", List.of("ballistic", "energy")));
        assertEquals(List.of(), ThemeTags.ancestorsListedBeside("weapon", List.of("weapon", "ballistic")));
    }

    @Test
    void damageTypesShareTheDamageTypeParentOutsideWeapons() {
        for (String damageType : List.of("kinetic", "high_explosive", "fragmentation", "energy_damage", "emp")) {
            assertEquals(List.of(damageType, "damage_type"), ThemeTags.withAncestors(damageType), damageType);
        }
    }

    @Test
    void beamAndNonBeamEnergyAreBothEnergyAndTheTechLevelsAreAffinities() {
        assertEquals(List.of("beam", "energy", "weapon"), ThemeTags.withAncestors("beam"));
        assertEquals(List.of("non_beam_energy", "energy", "weapon"), ThemeTags.withAncestors("non_beam_energy"));
        for (String techLevel : List.of("low_tech", "midline", "high_tech")) {
            assertEquals("affinity", ThemeTags.parent(techLevel), techLevel);
        }
    }

    @Test
    void sensorsAreLogisticsAndFleetSupportAndElectronicWarfareAreFleet() {
        assertEquals(List.of("sensors", "logistics"), ThemeTags.withAncestors("sensors"));
        assertEquals(List.of("fleet_support", "fleet"), ThemeTags.withAncestors("fleet_support"));
        assertEquals(List.of("electronic_warfare", "fleet"), ThemeTags.withAncestors("electronic_warfare"));
        assertNull(ThemeTags.parent("strikecraft"));
    }

    @Test
    void scopesSayWhatMayCarryATag() {
        assertEquals(Set.of(ThemeTagScope.SHIP, ThemeTagScope.STAR_CHART), ThemeTags.scopes("weapon"));
        assertEquals(Set.of(ThemeTagScope.SHIP), ThemeTags.scopes("cargo"));
        assertEquals(Set.of(ThemeTagScope.STAR_CHART), ThemeTags.scopes("enemy"));
        assertTrue(ThemeTags.ids(ThemeTagScope.SHIP).contains("salvage"));
        assertFalse(ThemeTags.ids(ThemeTagScope.SHIP).contains("boss"));
        assertTrue(ThemeTags.allows("boss", ThemeTagScope.STAR_CHART));
        assertFalse(ThemeTags.allows("boss", ThemeTagScope.SHIP));
    }

    @Test
    void tagsOutsideTheVocabularyHaveNoParentOrScope() {
        assertFalse(ThemeTags.isThemeTag("core"));
        assertFalse(ThemeTags.isThemeTag(null));
        assertNull(ThemeTags.parent("core"));
        assertTrue(ThemeTags.scopes("core").isEmpty());
        assertEquals(List.of("core"), ThemeTags.withAncestors("core"));
    }

    @Test
    void everyTagHasADisplayNameInEveryCatalogue() {
        List<String> missing = new ArrayList<>();
        for (String locale : List.of(LocaleChain.ENGLISH, LocaleChain.SIMPLIFIED_CHINESE)) {
            Map<String, String> strings = RealCatalogue.entries(locale);
            for (String tag : ThemeTags.ids()) {
                if (!strings.containsKey("tag." + tag)) {
                    missing.add(locale + ": tag." + tag);
                }
            }
        }

        assertTrue(missing.isEmpty(), String.join("\n", missing));
    }

    @Test
    void displayNamesComeFromTheCatalogue() {
        assertEquals("Strikecraft", ThemeTags.displayName("strikecraft"));
    }

    @Test
    void rowsWithAnUnknownOrMissingScopeAreSkipped() throws JSONException {
        register("[{\"tag\": \"weapon\", \"parent\": \"\", \"scopes\": \"ship\"},"
                + " {\"tag\": \"odd\", \"parent\": \"\", \"scopes\": \"ship;planet\"},"
                + " {\"tag\": \"bare\", \"parent\": \"\", \"scopes\": \"\"},"
                + " {\"tag\": \"\", \"parent\": \"\", \"scopes\": \"ship\"}]");

        assertEquals(List.of("weapon"), ThemeTags.ids());
    }

    @Test
    void anUnknownParentIsDropped() throws JSONException {
        register("[{\"tag\": \"ballistic\", \"parent\": \"guns\", \"scopes\": \"ship\"}]");

        assertTrue(ThemeTags.isThemeTag("ballistic"));
        assertNull(ThemeTags.parent("ballistic"));
    }

    @Test
    void aParentLoopIsBrokenSoAncestorsStayFinite() throws JSONException {
        register("[{\"tag\": \"first\", \"parent\": \"second\", \"scopes\": \"ship\"},"
                + " {\"tag\": \"second\", \"parent\": \"first\", \"scopes\": \"ship\"}]");

        assertEquals(List.of("second", "first"), ThemeTags.withAncestors("second"));
        assertEquals(List.of("first"), ThemeTags.withAncestors("first"));
    }
}
