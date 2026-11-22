package exiledsector.skills.tags;

import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillType;
import exiledsector.skills.layout.SkillNodeDecoration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ModCreditTest {

    @Test
    void aTypeOnlyInOneModsVolumesCreditsThatMod() {
        SkillType exoship = type("exoship");
        SkillType grace = type("borrowed_grace");

        Map<String, ModCredit> credits = ModCredit.byType(List.of(node("a", exoship, "exotech"), node("b", grace, "abyss"),
                node("c", grace, "abyss")));

        assertEquals(ModCredit.RANDOM_ASSORTMENT, credits.get("exoship"));
        assertEquals(ModCredit.RANDOM_ASSORTMENT, credits.get("borrowed_grace"));
    }

    @Test
    void aTypeAlsoUsedOutsideModVolumesHasNoCredit() {
        SkillType armor = type("armor");

        Map<String, ModCredit> credits = ModCredit.byType(List.of(node("a", armor, "tania"), node("b", armor, null)));

        assertFalse(credits.containsKey("armor"));
    }

    @Test
    void aTypeAlsoUsedOutsideModVolumesHasNoCreditWhicheverNodeComesFirst() {
        SkillType armor = type("armor");

        Map<String, ModCredit> credits = ModCredit.byType(List.of(node("a", armor, null), node("b", armor, "tania"), node("c", armor, "tania")));

        assertFalse(credits.containsKey("armor"));
    }

    @Test
    void aTypeSharedByTwoModsHasNoCredit() {
        SkillType fluxCost = type("energy_weapon_flux_cost");

        Map<String, ModCredit> credits = ModCredit.byType(List.of(node("a", fluxCost, "kesteven"), node("b", fluxCost, "exotech")));

        assertFalse(credits.containsKey("energy_weapon_flux_cost"));
    }

    @Test
    void smallNodesHaveNoCredit() {
        SkillType increasedHull = type("increased_hull", SkillTier.SMALL);

        Map<String, ModCredit> credits = ModCredit.byType(List.of(node("a", increasedHull, "tania")));

        assertFalse(credits.containsKey("increased_hull"));
    }

    @Test
    void choiceOptionsShareTheirNodesCredit() {
        SkillType choice = new SkillType.Builder("choice", "Choice", "a.png", SkillTier.NOTABLE).choiceOptionIds(List.of("left", "right")).build();

        Map<String, ModCredit> credits = ModCredit.byType(List.of(node("a", choice, "stargazer")));

        assertEquals(ModCredit.NIGHTCROSS, credits.get("left"));
        assertEquals(ModCredit.NIGHTCROSS, credits.get("right"));
    }

    @Test
    void everyModVolumeHasACreditAndOtherVolumesHaveNone() {
        VolumeToggles.TOGGLED_VOLUMES.forEach(volumes -> volumes.forEach(volume -> assertNotNull(ModCredit.forVolume(volume), volume)));
        assertEquals(ModCredit.TANIA, ModCredit.forVolume("tania"));
        assertEquals(ModCredit.LOST_SECTOR, ModCredit.forVolume("kesteven"));
        assertNull(ModCredit.forVolume("luddic"));
        assertNull(ModCredit.forVolume(null));
    }

    @Test
    void aTypeInstallingAModsHullModCreditsThatModWhereverItSits() {
        SkillType prefabricator = new SkillType.Builder("missile_prefabricator", "P", "a.png", SkillTier.NOTABLE)
                .phantomHullModIds(List.of("MHMods_prefabricator")).build();
        SkillType hullfoam = new SkillType.Builder("hullfoam_dispenser", "H", "a.png", SkillTier.NOTABLE)
                .phantomHullModIds(List.of("mhmods_hullfoam")).build();

        Map<String, ModCredit> credits = ModCredit.byType(List.of(node("a", prefabricator, "core"), node("b", hullfoam, null),
                node("c", hullfoam, "tania")));

        assertEquals(ModCredit.MORE_HULLMODS, credits.get("missile_prefabricator"));
        assertEquals(ModCredit.MORE_HULLMODS, credits.get("hullfoam_dispenser"));
    }

    @Test
    void vanillaHullModsCreditNoMod() {
        SkillType hardened = new SkillType.Builder("hardened_shields", "H", "a.png", SkillTier.NOTABLE).vanillaHullModId("hardenedshieldemitter").build();

        assertNull(ModCredit.forHullMods(hardened));
    }

    private static SkillType type(String id) {
        return type(id, SkillTier.NOTABLE);
    }

    private static SkillType type(String id, SkillTier tier) {
        return new SkillType.Builder(id, id, "a.png", tier).build();
    }

    private static SkillNode node(String id, SkillType type, String volume) {
        return new SkillNode(id, type, List.of(), 0f, 0f, SkillNodeDecoration.NONE, volume == null ? List.of() : List.of(volume));
    }
}
