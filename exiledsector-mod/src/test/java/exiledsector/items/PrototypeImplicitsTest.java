package exiledsector.items;

import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.skilleffect.EffectAliases;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static exiledsector.ProjectFiles.csvRows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrototypeImplicitsTest {

    private static final String EMITTER = "shield_generator_emitter";

    @BeforeEach
    void loadRealData() throws Exception {
        EffectAliases.register(csvRows(EffectAliases.dataPath()));
        ItemModifiers.register(csvRows(ItemModifiers.dataPath()));
        ItemDefinitions.register(csvRows(SubroutineDefinitions.dataPath()), csvRows(HullItem.definitionsPath()));
        ItemImplicits.register(csvRows(ItemImplicits.dataPath()));
        PrototypeImplicits.register(csvRows(PrototypeImplicits.dataPath()));
    }

    @AfterEach
    void clear() {
        ItemImplicits.clear();
        ItemDefinitions.clear();
        ItemModifiers.clear();
    }

    private static ItemData prototype(String baseId, long seed) {
        return PrototypeImplicits.create(ItemDefinitions.get(baseId), seed, new Random(seed));
    }

    @Test
    void everyShippedBaseIsARepeatableHullItemWithItsOwnPool() {
        assertFalse(PrototypeImplicits.bases().isEmpty());
        for (String baseId : PrototypeImplicits.bases()) {
            ItemDefinition base = ItemDefinitions.get(baseId);
            assertNotNull(base, baseId);
            assertEquals(ItemType.HULL_ITEM, base.type(), baseId);
            assertFalse(base.unique(), baseId);
            assertFalse(PrototypeImplicits.forBase(baseId).isEmpty(), baseId);
        }
    }

    @Test
    void aPrototypeReplacesItsBaseImplicitsWithOneFromItsBasePool() {
        Item item = prototype(EMITTER, 11L).preview();
        RolledModifier added = PrototypeImplicits.prototypeImplicit(item);

        assertFalse(ItemImplicits.forType(item.kind()).isEmpty());
        assertNotNull(added);
        assertEquals(List.of(added), item.implicits());
        assertTrue(PrototypeImplicits.forBase(EMITTER).stream().anyMatch(implicit -> implicit.id().equals(added.modifierId())));
        assertFalse(item.implicitLines(added).isEmpty());
    }

    @Test
    void thePrototypeImplicitAppliesItsEffect() {
        Item item = prototype(EMITTER, 11L).preview();
        RolledModifier added = PrototypeImplicits.prototypeImplicit(item);
        String effectName = added.lines().get(0).effectName();

        assertTrue(item.skillEffects().stream().map(SkillTypeEffect::effect).anyMatch(effect -> effect.name().equals(effectName)), effectName);
    }

    @Test
    void thePrototypeImplicitSurvivesSavingAndAChangeToTheKindImplicits() throws Exception {
        ItemData saved = prototype(EMITTER, 11L);
        RolledModifier added = PrototypeImplicits.prototypeImplicit(saved.preview());

        assertEquals(added, PrototypeImplicits.prototypeImplicit(ItemData.parse(saved.encode()).preview()));

        ItemImplicits.register(new JSONArray().put(new JSONObject().put("section", "shield_generator").put("implicits", "HULL_PERCENT:50:60")));
        assertEquals(List.of(added), ItemData.parse(saved.encode()).preview().implicits());
    }

    @Test
    void aSavedPrototypeThatStillHasItsBaseImplicitsKeepsOnlyThePrototypeImplicit() throws Exception {
        Item item = prototype(EMITTER, 11L).preview();
        RolledModifier added = PrototypeImplicits.prototypeImplicit(item);
        List<RolledModifier> oldImplicits = new ArrayList<>(ItemImplicits.roll(item.kind(), item.seed()));
        oldImplicits.add(added);
        item.replaceImplicits(oldImplicits);

        assertEquals(List.of(added), ItemData.parse(ItemData.of(item).encode()).preview().implicits());
    }

    @Test
    void basesWithoutAPoolGiveNoPrototype() {
        ItemDefinition unlisted = ItemDefinitions.all().stream()
                .filter(definition -> definition.type() == ItemType.HULL_ITEM && PrototypeImplicits.forBase(definition.id()).isEmpty())
                .findFirst().orElseThrow();

        assertNull(PrototypeImplicits.create(unlisted, 1L, new Random(1L)));
        assertNull(PrototypeImplicits.create(null, 1L, new Random(1L)));
    }

    @Test
    void aBaseOrImplicitListedTwiceIsSkipped() throws Exception {
        PrototypeImplicits.register(new JSONArray()
                .put(new JSONObject().put("base", "armor_plate").put("implicits", "@KINETIC_DAMAGE_PERCENT_PROTOTYPE_IMPLICIT"))
                .put(new JSONObject().put("base", "armor_plate").put("implicits", "@ENERGY_DAMAGE_PERCENT_PROTOTYPE_IMPLICIT"))
                .put(new JSONObject().put("base", EMITTER).put("implicits", "@KINETIC_DAMAGE_PERCENT_PROTOTYPE_IMPLICIT")));

        assertEquals(List.of("armor_plate"), PrototypeImplicits.bases());
        assertTrue(PrototypeImplicits.isPrototypeImplicit("KINETIC_DAMAGE_PERCENT_PROTOTYPE_IMPLICIT"));
        assertFalse(PrototypeImplicits.isPrototypeImplicit("ENERGY_DAMAGE_PERCENT_PROTOTYPE_IMPLICIT"));
    }

}
