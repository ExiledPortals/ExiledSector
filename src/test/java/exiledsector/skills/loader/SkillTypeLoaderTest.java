package exiledsector.skills.loader;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillType;
import exiledsector.skills.skilleffect.DefenseSkillEffect;
import exiledsector.skills.unlock.BlueprintCategory;
import exiledsector.skills.unlock.UnlockCondition;
import exiledsector.skills.unlock.UnlockConditionType;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SkillTypeLoaderTest {

    @Test
    void parsesAllFieldsIncludingEffectsList() throws Exception {
        JSONObject root = new JSONObject("{ \"skillTypes\": [ {"
                + "\"id\": \"hull\","
                + "\"name\": \"Reinforced Hull\","
                + "\"icon\": \"graphics/hullmods/reinforced_bulkheads.png\","
                + "\"effects\": [ { \"effect\": \"HULL_PERCENT\", \"magnitude\": 10 } ]"
                + "} ] }");

        Map<String, SkillType> types = SkillTypeLoader.parseSkillTypes(root);

        SkillType hull = types.get("hull");
        assertEquals("Reinforced Hull", hull.getDisplayName());
        assertEquals("graphics/hullmods/reinforced_bulkheads.png", hull.getIconPath());
        assertEquals(1, hull.getEffects().size());
        assertEquals(DefenseSkillEffect.HULL_PERCENT, hull.getEffects().get(0).effect());
        assertEquals(10f, hull.getEffects().get(0).magnitude());
    }

    @Test
    void parsesMultipleEffectsInOrder() throws Exception {
        JSONObject root = new JSONObject("{ \"skillTypes\": [ {"
                + "\"id\": \"heavyarmor\","
                + "\"name\": \"Heavy Armor\","
                + "\"icon\": \"graphics/icons/notable_hullmods/heavy_armor.png\","
                + "\"effects\": [ { \"effect\": \"ARMOR_PERCENT\", \"magnitude\": 15 }, { \"effect\": \"HULL_PERCENT\", \"magnitude\": 5 } ]"
                + "} ] }");

        SkillType heavyArmor = SkillTypeLoader.parseSkillTypes(root).get("heavyarmor");

        assertEquals(2, heavyArmor.getEffects().size());
        assertEquals(DefenseSkillEffect.ARMOR_PERCENT, heavyArmor.getEffects().get(0).effect());
        assertEquals(DefenseSkillEffect.HULL_PERCENT, heavyArmor.getEffects().get(1).effect());
    }

    @Test
    void missingEffectsFieldMeansCosmeticPlaceholder() throws Exception {
        JSONObject root = new JSONObject("{ \"skillTypes\": [ {"
                + "\"id\": \"capacitors\","
                + "\"name\": \"Capacitors\","
                + "\"icon\": \"graphics/hullmods/flux_coil_adjunct.png\""
                + "} ] }");

        SkillType capacitors = SkillTypeLoader.parseSkillTypes(root).get("capacitors");

        assertTrue(capacitors.getEffects().isEmpty());
    }

    @Test
    void missingTierFieldDefaultsToSmall() throws Exception {
        JSONObject root = new JSONObject("{ \"skillTypes\": [ {"
                + "\"id\": \"capacitors\","
                + "\"name\": \"Capacitors\","
                + "\"icon\": \"graphics/hullmods/flux_coil_adjunct.png\""
                + "} ] }");

        SkillType capacitors = SkillTypeLoader.parseSkillTypes(root).get("capacitors");

        assertEquals(SkillTier.SMALL, capacitors.getTier());
        assertNull(capacitors.getVanillaHullModId());
    }

    @Test
    void parsesTierVanillaHullModAndTodoFields() throws Exception {
        JSONObject root = new JSONObject("{ \"skillTypes\": [ {"
                + "\"id\": \"escort_package\","
                + "\"name\": \"Escort Package\","
                + "\"icon\": \"graphics/icons/notable_hullmods/escort_package.png\","
                + "\"tier\": \"NOTABLE\","
                + "\"vanillaHullMod\": \"escort_package\","
                + "\"todo\": \"Needs a real mechanic\""
                + "} ] }");

        SkillType escortPackage = SkillTypeLoader.parseSkillTypes(root).get("escort_package");

        assertEquals(SkillTier.NOTABLE, escortPackage.getTier());
        assertEquals("escort_package", escortPackage.getVanillaHullModId());
        assertEquals("Needs a real mechanic", escortPackage.getTodo());
        assertTrue(escortPackage.getEffects().isEmpty());
    }

    @Test
    void parsesDescriptionOverride() throws Exception {
        JSONObject root = new JSONObject("{ \"skillTypes\": [ {"
                + "\"id\": \"hull\","
                + "\"name\": \"Hull\","
                + "\"icon\": \"a.png\","
                + "\"description\": \"Custom flavor text.\""
                + "} ] }");

        SkillType hull = SkillTypeLoader.parseSkillTypes(root).get("hull");

        assertEquals("Custom flavor text.", hull.getDescriptionOverride());
    }

    @Test
    void missingOptionalOptionsFieldMeansNotOptional() throws Exception {
        JSONObject root = new JSONObject("{ \"skillTypes\": [ {"
                + "\"id\": \"hull\","
                + "\"name\": \"Hull\","
                + "\"icon\": \"a.png\""
                + "} ] }");

        SkillType hull = SkillTypeLoader.parseSkillTypes(root).get("hull");

        assertTrue(hull.getOptionalOptionIds().isEmpty());
        assertFalse(hull.isOptional());
    }

    @Test
    void parsesOptionalOptionsIntoAnOrderedList() throws Exception {
        JSONObject root = new JSONObject("{ \"skillTypes\": [ {"
                + "\"id\": \"slot\","
                + "\"name\": \"Optional Skill\","
                + "\"icon\": \"a.png\","
                + "\"optionalOptions\": [\"hull\", \"armor\"]"
                + "} ] }");

        SkillType slot = SkillTypeLoader.parseSkillTypes(root).get("slot");

        assertTrue(slot.isOptional());
        assertEquals(2, slot.getOptionalOptionIds().size());
        assertEquals("hull", slot.getOptionalOptionIds().get(0));
        assertEquals("armor", slot.getOptionalOptionIds().get(1));
    }

    @Test
    void missingExclusiveHullModsFieldMeansNoExclusions() throws Exception {
        JSONObject root = new JSONObject("{ \"skillTypes\": [ {"
                + "\"id\": \"hull\","
                + "\"name\": \"Hull\","
                + "\"icon\": \"a.png\""
                + "} ] }");

        SkillType hull = SkillTypeLoader.parseSkillTypes(root).get("hull");

        assertTrue(hull.getExclusiveHullModIds().isEmpty());
    }

    @Test
    void parsesExclusiveHullModsIntoAnOrderedList() throws Exception {
        JSONObject root = new JSONObject("{ \"skillTypes\": [ {"
                + "\"id\": \"heavyarmor\","
                + "\"name\": \"Heavy Armor\","
                + "\"icon\": \"a.png\","
                + "\"exclusiveHullMods\": [\"armoredcladding\", \"heavyarmor\"]"
                + "} ] }");

        SkillType heavyArmor = SkillTypeLoader.parseSkillTypes(root).get("heavyarmor");

        assertEquals(2, heavyArmor.getExclusiveHullModIds().size());
        assertEquals("armoredcladding", heavyArmor.getExclusiveHullModIds().get(0));
        assertEquals("heavyarmor", heavyArmor.getExclusiveHullModIds().get(1));
    }

    @Test
    void parsesRequiredHullSizesAndIgnoresNamesThatAreNotShipHullSizes() throws Exception {
        JSONObject root = new JSONObject("{ \"skillTypes\": [ {"
                + "\"id\": \"escort\","
                + "\"name\": \"Escort\","
                + "\"icon\": \"a.png\","
                + "\"requiredHullSizes\": [\"CRUISER\", \"DESTROYER\", \"FIGHTER\", \"BATTLESHIP\"]"
                + "} ] }");

        SkillType escort = SkillTypeLoader.parseSkillTypes(root).get("escort");

        assertEquals(Set.of(HullSize.DESTROYER, HullSize.CRUISER), escort.getRequiredHullSizes());
        assertTrue(escort.allowsHullSize(HullSize.DESTROYER));
        assertFalse(escort.allowsHullSize(HullSize.FRIGATE));
        assertFalse(escort.allowsHullSize(HullSize.CAPITAL_SHIP));
    }

    @Test
    void aTypeWithoutRequiredHullSizesAllowsEveryHullSize() throws Exception {
        JSONObject root = new JSONObject("{ \"skillTypes\": [ { \"id\": \"hull\", \"name\": \"Hull\", \"icon\": \"a.png\" } ] }");

        SkillType hull = SkillTypeLoader.parseSkillTypes(root).get("hull");

        assertTrue(hull.getRequiredHullSizes().isEmpty());
        for (HullSize hullSize : List.of(HullSize.FRIGATE, HullSize.DESTROYER, HullSize.CRUISER, HullSize.CAPITAL_SHIP)) {
            assertTrue(hull.allowsHullSize(hullSize));
        }
    }

    @Test
    void vanillaHullModIsImplicitlyExclusiveWithItself() throws Exception {
        JSONObject root = new JSONObject("{ \"skillTypes\": [ {"
                + "\"id\": \"escort_package\","
                + "\"name\": \"Escort Package\","
                + "\"icon\": \"a.png\","
                + "\"vanillaHullMod\": \"escort_package\""
                + "} ] }");

        SkillType escortPackage = SkillTypeLoader.parseSkillTypes(root).get("escort_package");

        assertEquals(1, escortPackage.getExclusiveHullModIds().size());
        assertEquals("escort_package", escortPackage.getExclusiveHullModIds().get(0));
    }

    @Test
    void missingExclusiveSkillTypesFieldMeansNoExclusions() throws Exception {
        JSONObject root = new JSONObject("{ \"skillTypes\": [ {"
                + "\"id\": \"hull\","
                + "\"name\": \"Hull\","
                + "\"icon\": \"a.png\""
                + "} ] }");

        SkillType hull = SkillTypeLoader.parseSkillTypes(root).get("hull");

        assertTrue(hull.getExclusiveSkillTypeIds().isEmpty());
    }

    @Test
    void parsesExclusiveSkillTypesIntoAnOrderedList() throws Exception {
        JSONObject root = new JSONObject("{ \"skillTypes\": [ {"
                + "\"id\": \"frontemitter\","
                + "\"name\": \"Shield Conversion - Front\","
                + "\"icon\": \"a.png\","
                + "\"exclusiveSkillTypes\": [\"adaptiveshields\", \"shield_shunt\"]"
                + "} ] }");

        SkillType frontEmitter = SkillTypeLoader.parseSkillTypes(root).get("frontemitter");

        assertEquals(2, frontEmitter.getExclusiveSkillTypeIds().size());
        assertEquals("adaptiveshields", frontEmitter.getExclusiveSkillTypeIds().get(0));
        assertEquals("shield_shunt", frontEmitter.getExclusiveSkillTypeIds().get(1));
    }

    @Test
    void missingUnlockConditionsFieldMeansNeverLocked() throws Exception {
        JSONObject root = new JSONObject("{ \"skillTypes\": [ {"
                + "\"id\": \"hull\","
                + "\"name\": \"Hull\","
                + "\"icon\": \"a.png\""
                + "} ] }");

        SkillType hull = SkillTypeLoader.parseSkillTypes(root).get("hull");

        assertTrue(hull.getUnlockConditions().isEmpty());
    }

    @Test
    void parsesABlueprintUnlockCondition() throws Exception {
        JSONObject root = new JSONObject("{ \"skillTypes\": [ {"
                + "\"id\": \"escort_package\","
                + "\"name\": \"Escort Package\","
                + "\"icon\": \"a.png\","
                + "\"vanillaHullMod\": \"escort_package\","
                + "\"unlockConditions\": [ { \"type\": \"blueprint\", \"category\": \"hullmod\", \"id\": \"escort_package\" } ]"
                + "} ] }");

        SkillType escortPackage = SkillTypeLoader.parseSkillTypes(root).get("escort_package");

        List<UnlockCondition> conditions = escortPackage.getUnlockConditions();
        assertEquals(1, conditions.size());
        assertEquals(UnlockConditionType.BLUEPRINT, conditions.get(0).getType());
        assertEquals(BlueprintCategory.HULLMOD, conditions.get(0).getBlueprintCategory());
        assertEquals("escort_package", conditions.get(0).getKey());
    }

    @Test
    void parsesACharacterStatUnlockCondition() throws Exception {
        JSONObject root = new JSONObject("{ \"skillTypes\": [ {"
                + "\"id\": \"neural_interface\","
                + "\"name\": \"Neural Interface\","
                + "\"icon\": \"a.png\","
                + "\"unlockConditions\": [ { \"type\": \"characterStat\", \"statId\": \"has_neural_link\" } ]"
                + "} ] }");

        SkillType type = SkillTypeLoader.parseSkillTypes(root).get("neural_interface");

        UnlockCondition condition = type.getUnlockConditions().get(0);
        assertEquals(UnlockConditionType.CHARACTER_STAT, condition.getType());
        assertEquals("has_neural_link", condition.getKey());
    }

    @Test
    void parsesAMinShipLevelUnlockCondition() throws Exception {
        JSONObject root = new JSONObject("{ \"skillTypes\": [ {"
                + "\"id\": \"powerful_notable\","
                + "\"name\": \"Powerful Notable\","
                + "\"icon\": \"a.png\","
                + "\"unlockConditions\": [ { \"type\": \"minShipLevel\", \"level\": 15 } ]"
                + "} ] }");

        SkillType type = SkillTypeLoader.parseSkillTypes(root).get("powerful_notable");

        UnlockCondition condition = type.getUnlockConditions().get(0);
        assertEquals(UnlockConditionType.MIN_SHIP_LEVEL, condition.getType());
        assertEquals(15, condition.getMinLevel());
    }

    @Test
    void parsesAMemoryFlagUnlockCondition() throws Exception {
        JSONObject root = new JSONObject("{ \"skillTypes\": [ {"
                + "\"id\": \"wormhole\","
                + "\"name\": \"Wormhole\","
                + "\"icon\": \"a.png\","
                + "\"unlockConditions\": [ { \"type\": \"memoryFlag\", \"key\": \"$playerCanUseGates\" } ]"
                + "} ] }");

        SkillType type = SkillTypeLoader.parseSkillTypes(root).get("wormhole");

        UnlockCondition condition = type.getUnlockConditions().get(0);
        assertEquals(UnlockConditionType.MEMORY_FLAG, condition.getType());
        assertEquals("$playerCanUseGates", condition.getKey());
    }

    @Test
    void parsesMultipleUnlockConditionsInOrder() throws Exception {
        JSONObject root = new JSONObject("{ \"skillTypes\": [ {"
                + "\"id\": \"neural_interface\","
                + "\"name\": \"Neural Interface\","
                + "\"icon\": \"a.png\","
                + "\"unlockConditions\": ["
                + "  { \"type\": \"blueprint\", \"category\": \"hullmod\", \"id\": \"neural_interface\" },"
                + "  { \"type\": \"characterStat\", \"statId\": \"has_neural_link\" }"
                + "]"
                + "} ] }");

        SkillType type = SkillTypeLoader.parseSkillTypes(root).get("neural_interface");

        List<UnlockCondition> conditions = type.getUnlockConditions();
        assertEquals(2, conditions.size());
        assertEquals(UnlockConditionType.BLUEPRINT, conditions.get(0).getType());
        assertEquals(UnlockConditionType.CHARACTER_STAT, conditions.get(1).getType());
    }

    @Test
    void missingTagsFieldMeansNoTags() throws Exception {
        JSONObject root = new JSONObject("{ \"skillTypes\": [ {"
                + "\"id\": \"hull\","
                + "\"name\": \"Hull\","
                + "\"icon\": \"a.png\""
                + "} ] }");

        SkillType hull = SkillTypeLoader.parseSkillTypes(root).get("hull");

        assertTrue(hull.getTags().isEmpty());
    }

    @Test
    void parsesTagsIntoAnOrderedList() throws Exception {
        JSONObject root = new JSONObject("{ \"skillTypes\": [ {"
                + "\"id\": \"frontemitter\","
                + "\"name\": \"Shield Conversion - Front\","
                + "\"icon\": \"a.png\","
                + "\"tags\": [\"shield\", \"req_shields\"]"
                + "} ] }");

        SkillType frontEmitter = SkillTypeLoader.parseSkillTypes(root).get("frontemitter");

        assertEquals(List.of("shield", "req_shields"), frontEmitter.getTags());
    }

    @Test
    void parsesMultipleTypesKeyedById() throws Exception {
        JSONObject root = new JSONObject("{ \"skillTypes\": ["
                + "{\"id\": \"a\", \"name\": \"A\", \"icon\": \"a.png\"},"
                + "{\"id\": \"b\", \"name\": \"B\", \"icon\": \"b.png\"}"
                + "] }");

        Map<String, SkillType> types = SkillTypeLoader.parseSkillTypes(root);

        assertEquals(2, types.size());
        assertEquals("A", types.get("a").getDisplayName());
        assertEquals("B", types.get("b").getDisplayName());
    }

    @Test
    void aDuplicateIdOverwritesTheEarlierDefinitionInsteadOfBeingRejected() throws Exception {
        JSONObject root = new JSONObject("{ \"skillTypes\": ["
                + "{\"id\": \"a\", \"name\": \"First\", \"icon\": \"a.png\"},"
                + "{\"id\": \"a\", \"name\": \"Second\", \"icon\": \"a.png\"}"
                + "] }");

        Map<String, SkillType> types = SkillTypeLoader.parseSkillTypes(root);

        assertEquals(1, types.size());
        assertEquals("Second", types.get("a").getDisplayName());
    }

    private static Float temporarySecondsFor(String extraFields) throws Exception {
        JSONObject root = new JSONObject("{ \"skillTypes\": [ {"
                + "\"id\": \"burst\", \"name\": \"Burst\", \"icon\": \"a.png\","
                + "\"temporaryAfterDeploymentSeconds\": 30,"
                + extraFields
                + "} ] }");
        return SkillTypeLoader.parseSkillTypes(root).get("burst").getTemporaryAfterDeploymentSeconds();
    }

    @Test
    void temporaryGatingIsKeptWhenEveryEffectSupportsIt() throws Exception {
        assertEquals(30f, temporarySecondsFor("\"effects\": [ { \"effect\": \"HULL_PERCENT\", \"magnitude\": 10 } ]"));
    }

    @Test
    void temporaryGatingIsDroppedWhenTheTypeInstallsHullMods() throws Exception {
        assertNull(temporarySecondsFor("\"effects\": [ { \"effect\": \"HULL_PERCENT\", \"magnitude\": 10 } ],"
                + "\"installedHullMods\": [ \"heavyarmor\" ]"));
    }

    @Test
    void temporaryGatingIsDroppedWhenAnEffectCannotBeGated() throws Exception {
        assertNull(temporarySecondsFor("\"effects\": [ { \"effect\": \"HULL_PERCENT\", \"magnitude\": 10 },"
                + "{ \"effect\": \"PD_IGNORES_DECOY_FLARES\", \"magnitude\": 1 } ]"));
    }

    @Test
    void temporaryGatingIsDroppedWhenAHullSizeEffectCannotBeGated() throws Exception {
        assertNull(temporarySecondsFor("\"hullSizeEffects\": [ { \"effect\": \"PD_IGNORES_DECOY_FLARES\","
                + "\"frigate\": 1, \"destroyer\": 1, \"cruiser\": 1, \"capitalShip\": 1 } ]"));
    }

    @Test
    void anUnreadableDataFileLoadsNoTypesInsteadOfCrashing() throws Exception {
        SettingsAPI settings = mock(SettingsAPI.class);
        when(settings.loadJSON("data/skilltrees/skill_types.json")).thenThrow(new IOException("missing"));
        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            global.when(Global::getSettings).thenReturn(settings);

            SkillTypeLoader.LoadedTypes loaded = SkillTypeLoader.loadAll();

            assertTrue(loaded.types().isEmpty());
            assertEquals(0, loaded.declaredCount());
        }
    }

    @Test
    void typesWithABadFieldAreSkippedAndTheRestStillLoad() throws Exception {
        JSONObject root = new JSONObject("{ \"skillTypes\": ["
                + "{\"id\": \"good_a\", \"name\": \"Good A\", \"icon\": \"a.png\"},"
                + "{\"id\": \"bad_effect\", \"name\": \"Bad\", \"icon\": \"a.png\","
                + " \"effects\": [ { \"effect\": \"NOT_AN_EFFECT\", \"magnitude\": 1 } ]},"
                + "{\"id\": \"bad_tier\", \"name\": \"Bad\", \"icon\": \"a.png\", \"tier\": \"ENORMOUS\"},"
                + "{\"id\": \"bad_unlock\", \"name\": \"Bad\", \"icon\": \"a.png\","
                + " \"unlockConditions\": [ { \"type\": \"teleport\" } ]},"
                + "{\"id\": \"no_name\", \"icon\": \"a.png\"},"
                + "\"not_an_object\","
                + "{\"id\": \"good_b\", \"name\": \"Good B\", \"icon\": \"a.png\"}"
                + "] }");

        assertEquals(List.of("good_a", "good_b"), List.copyOf(SkillTypeLoader.parseSkillTypes(root).keySet()));
    }
}
