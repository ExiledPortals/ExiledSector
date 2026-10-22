package exiledsector.items;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.security.AnyTypePermission;
import exiledlib.items.FrozenName;
import exiledlib.items.Item;
import exiledlib.items.ItemCodec;
import exiledlib.items.ItemData;
import exiledlib.items.ItemDefinition;
import exiledlib.items.ItemDefinitions;
import exiledlib.items.ItemIdAliases;
import exiledlib.items.ItemImplicits;
import exiledlib.items.ItemModifiers;
import exiledlib.items.ItemRollOdds;
import exiledlib.items.ItemType;
import exiledlib.items.PoolEntry;
import exiledlib.items.RolledEffect;
import exiledlib.items.RolledModifier;
import exiledlib.items.UnresolvedItem;
import exiledlib.modifiers.Modifier;
import exiledlib.modifiers.ModifierSlot;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemModifierReferencesTest {

    private static final String DRIVE = "engine_room_drive";

    @BeforeEach
    void register() throws Exception {
        ItemFixtures.registerModifierNames(
                "TOP_SPEED_PERCENT,prefix,TOP_SPEED_PERCENT:-10:-5,Hasty",
                "TOP_SPEED_PERCENT_ENGINE_ROOM,prefix,TOP_SPEED_PERCENT:5:15,Swift",
                "FUEL_USE_MULT_ENGINE_ROOM,suffix,FUEL_USE_MULT:-15:-10,Economy",
                "TOP_SPEED_PERCENT_IMPLICIT,implicit,TOP_SPEED_PERCENT:5:10,",
                "HYBRID_FLUX_CAPACITY_FLUX_DISSIPATION,implicit,FLUX_CAPACITY_PERCENT:5:10; FLUX_DISSIPATION_PERCENT:5:10,");
        ItemDefinitions.register(new JSONArray()
                .put(ItemFixtures.row(DRIVE, "engine_room", "@TOP_SPEED_PERCENT_ENGINE_ROOM:3", "@FUEL_USE_MULT_ENGINE_ROOM")
                        .put("name", "Drive"))
                .put(ItemFixtures.row("emitter", "shield_generator", "@TOP_SPEED_PERCENT_ENGINE_ROOM", "").put("name", "Emitter")));
        ItemImplicits.register(new JSONArray()
                .put(new JSONObject().put("section", "engine_room").put("implicits", "@TOP_SPEED_PERCENT_IMPLICIT"))
                .put(new JSONObject().put("section", "shield_generator").put("implicits", "@HYBRID_FLUX_CAPACITY_FLUX_DISSIPATION")));
    }

    @AfterEach
    void clear() {
        ItemImplicits.clear();
        ItemDefinitions.clear();
        ItemModifiers.clear();
    }

    private static XStream xstream() {
        XStream xstream = new XStream(new DomDriver());
        XStream.setupDefaultSecurity(xstream);
        xstream.addPermission(AnyTypePermission.ANY);
        ItemSaveAliases.register(xstream);
        return xstream;
    }

    @Test
    void aPoolEntryUsesTheReferencedModifierAndItsOwnWeight() {
        ItemDefinition drive = ItemDefinitions.get(DRIVE);
        PoolEntry speed = drive.prefixes().get(0);

        assertSame(ItemModifiers.get("TOP_SPEED_PERCENT_ENGINE_ROOM"), speed.modifier());
        assertEquals(3f, speed.weight());
        assertEquals(15f, speed.max());
        assertSame(speed, drive.poolEntry("TOP_SPEED_PERCENT"));
    }

    @Test
    void aReferenceToAMissingModifierOrTheWrongSlotIsSkipped() throws Exception {
        ItemDefinitions.register(new JSONArray().put(ItemFixtures.row("broken", "engine_room",
                "@NO_SUCH_MODIFIER; @FUEL_USE_MULT_ENGINE_ROOM; @TOP_SPEED_PERCENT_ENGINE_ROOM", "")));

        assertEquals(List.of("TOP_SPEED_PERCENT_ENGINE_ROOM"),
                ItemDefinitions.get("broken").prefixes().stream().map(entry -> entry.modifier().id()).toList());
    }

    @Test
    void aPoolEntryForAModifierScopedToAnotherItemTypeIsSkipped() throws Exception {
        ItemModifiers.register(new JSONArray()
                .put(ItemFixtures.modifierRow("VENT_RATE_PERCENT", "prefix", "star_chart", "VENT_RATE_PERCENT:1:2", "Venting"))
                .put(ItemFixtures.modifierRow("HULL_MULT", "prefix", "hull_item", "HULL_MULT:4:6", "Reinforced")));
        ItemDefinitions.register(new JSONArray()
                .put(ItemFixtures.row("chip", "bridge", "@VENT_RATE_PERCENT; @HULL_MULT")));

        assertEquals(List.of("HULL_MULT"), ItemDefinitions.get("chip").prefixes().stream().map(entry -> entry.modifier().id()).toList());
    }

    @Test
    void aModifierWithoutItemTypesIsSkipped() throws Exception {
        ItemModifiers.register(new JSONArray()
                .put(ItemFixtures.modifierRow("HULL_MULT", "prefix", "", "HULL_MULT:4:6", "Reinforced"))
                .put(ItemFixtures.modifierRow("ARMOR_PERCENT", "suffix", "hull_item", "ARMOR_PERCENT:1:2", "Plating")));

        assertNull(ItemModifiers.get("HULL_MULT"));
        assertEquals(java.util.Set.of(ItemType.HULL_ITEM), ItemModifiers.itemTypes("ARMOR_PERCENT"));
    }

    @Test
    void itemsSavedUnderARenamedDefinitionIdLoadUnderTheCurrentId() throws Exception {
        ItemIdAliases.register(new JSONArray().put(new JSONObject().put("alias", "framework_engine_room_drive").put("id", DRIVE)));
        try {
            String xml = xstream().toXML(Item.create("item_1", DRIVE, 4L, List.of())).replace(DRIVE, "framework_engine_room_drive");

            Item restored = (Item) xstream().fromXML(xml);

            assertTrue(xml.contains("framework_engine_room_drive"), xml);
            assertEquals(HullItem.class, restored.getClass());
            assertEquals(DRIVE, restored.definitionId());
            assertEquals(DRIVE, ItemData.parse("framework_engine_room_drive|5").definitionId());
            assertEquals(DRIVE, ItemDefinitions.get("framework_engine_room_drive").id());
        } finally {
            ItemIdAliases.clear();
        }
    }

    @Test
    void itemsAreTheSubclassOfTheirDefinitionsType() {
        assertEquals(HullItem.class, Item.create("item_1", DRIVE, 1L, List.of()).getClass());
        assertEquals(UnresolvedItem.class, Item.create("item_2", "not_loaded", 1L, List.of()).getClass());
        assertEquals(ShipSection.ENGINE_ROOM, ((HullItem) Item.create("item_3", DRIVE, 1L, List.of())).section());
    }

    @Test
    void anItemSavedBeforeModifierIdsKeepsItsRollUnderThePoolsCurrentId() throws Exception {
        Item saved = Item.create("item_1", DRIVE, 4L, List.of(
                RolledModifier.single(ModifierSlot.PREFIX, new RolledEffect("TOP_SPEED_PERCENT", 12f)),
                RolledModifier.single(ModifierSlot.SUFFIX, new RolledEffect("FUEL_USE_MULT", -11f))));
        String xml = xstream().toXML(saved);

        Item restored = (Item) xstream().fromXML(xml);

        assertEquals(List.of(new RolledModifier("TOP_SPEED_PERCENT_ENGINE_ROOM", ModifierSlot.PREFIX, List.of(new RolledEffect("TOP_SPEED_PERCENT", 12f))),
                new RolledModifier("FUEL_USE_MULT_ENGINE_ROOM", ModifierSlot.SUFFIX, List.of(new RolledEffect("FUEL_USE_MULT", -11f)))),
                restored.explicits());
        assertEquals(15f, restored.rolls().stream().filter(roll -> roll.slot() == ModifierSlot.PREFIX).findFirst().orElseThrow().line().max());
    }

    @Test
    void aFrozenNameSavedWithEffectNamesStillFindsItsWords() {
        Item saved = Item.create("item_1", DRIVE, 4L, List.of(
                RolledModifier.single(ModifierSlot.PREFIX, new RolledEffect("TOP_SPEED_PERCENT", 12f))));
        saved.freezeName(new FrozenName("TOP_SPEED_PERCENT", "FUEL_USE_MULT", null, null));

        assertTrue(saved.name().startsWith("Swift ") && saved.name().endsWith(" Economy"), saved.name());
    }

    @Test
    void anEffectThatLeftThePoolStillNamesFrozenItemsThroughANamedModifierOfItsSlot() {
        Item saved = Item.create("item_1", "emitter", 4L, List.of(
                new RolledModifier("TOP_SPEED_PERCENT_ENGINE_ROOM", ModifierSlot.PREFIX, List.of(new RolledEffect("TOP_SPEED_PERCENT", 12f)))));
        saved.freezeName(new FrozenName(null, "FUEL_USE_MULT", null, null));

        assertTrue(saved.name().endsWith(" Economy"), saved.name());
    }

    @Test
    void aFrozenNameWhoseModifierIdWasRenamedTakesTheWordOfTheCurrentFirstPrefix() {
        Item saved = Item.create("item_1", DRIVE, 4L, List.of(
                new RolledModifier("TOP_SPEED_PERCENT_ENGINE_ROOM", ModifierSlot.PREFIX, List.of(new RolledEffect("TOP_SPEED_PERCENT", 12f))),
                new RolledModifier("FUEL_USE_MULT_ENGINE_ROOM", ModifierSlot.SUFFIX, List.of(new RolledEffect("FUEL_USE_MULT", -11f)))));
        saved.freezeName(new FrozenName("OLD_SPEED_ID", "FUEL_USE_MULT_ENGINE_ROOM", null, null));

        assertTrue(saved.name().startsWith("Swift ") && saved.name().endsWith(" Economy"), saved.name());
    }

    @Test
    void rollChancesAreKeptPerModifier() {
        assertEquals(1.0, ItemRollOdds.weightShares(ItemDefinitions.get(DRIVE)).get("TOP_SPEED_PERCENT_ENGINE_ROOM"));
    }

    @Test
    void savedImplicitsSurviveWhileTheirTypeHasNoLoadedImplicits() throws Exception {
        Item rolled = Item.create("item_1", DRIVE, 9L, List.of());
        List<RolledModifier> implicits = rolled.implicits();
        String xml = xstream().toXML(rolled);
        ItemImplicits.register(new JSONArray());

        Item restored = (Item) xstream().fromXML(xml);

        assertEquals(implicits, restored.implicits());
        register();
        assertEquals(implicits, restored.implicits());
    }

    @Test
    void shieldGeneratorImplicitsSavedAsTwoSinglesFoldIntoTheHybridKeepingTheirValues() throws Exception {
        Item saved = Item.create("item_1", "emitter", 9L, List.of());
        String xml = xstream().toXML(saved).replace("<explicitModifiers/>",
                "<explicitModifiers/><implicitModifiers>"
                        + "<exiledSector.RolledModifier><modifierId>FLUX_CAPACITY_PERCENT</modifierId><slot>IMPLICIT</slot><lines>"
                        + "<exiledSector.RolledEffect><effectName>FLUX_CAPACITY_PERCENT</effectName><magnitude>7.0</magnitude></exiledSector.RolledEffect>"
                        + "</lines></exiledSector.RolledModifier>"
                        + "<exiledSector.RolledModifier><modifierId>FLUX_DISSIPATION_PERCENT</modifierId><slot>IMPLICIT</slot><lines>"
                        + "<exiledSector.RolledEffect><effectName>FLUX_DISSIPATION_PERCENT</effectName><magnitude>9.0</magnitude></exiledSector.RolledEffect>"
                        + "</lines></exiledSector.RolledModifier></implicitModifiers>");

        Item restored = (Item) xstream().fromXML(xml);

        assertTrue(xml.contains("<implicitModifiers>"), xml);
        assertEquals(List.of(new RolledModifier("HYBRID_FLUX_CAPACITY_FLUX_DISSIPATION", ModifierSlot.IMPLICIT,
                List.of(new RolledEffect("FLUX_CAPACITY_PERCENT", 7f), new RolledEffect("FLUX_DISSIPATION_PERCENT", 9f)))), restored.implicits());
    }

    @Test
    void anImplicitThatNoLongerMatchesItsTypeIsRerolledFromTheSeed() {
        Item saved = Item.create("item_1", DRIVE, 9L, List.of());
        List<RolledModifier> stale = List.of(RolledModifier.single(ModifierSlot.IMPLICIT, new RolledEffect("ARMOR_PERCENT", 7f)));

        List<RolledModifier> resolved = ItemImplicits.withCurrentIds(saved.kind(), stale, 9L);

        assertEquals(ItemImplicits.roll(saved.kind(), 9L), resolved);
        assertEquals("TOP_SPEED_PERCENT_IMPLICIT", resolved.get(0).modifierId());
    }

    @Test
    void cargoCodesFromBeforeModifierIdsDecodeToThePoolsCurrentIds() {
        List<RolledModifier> decoded = ItemCodec.decodeModifiers("P=TOP_SPEED_PERCENT:12;FUEL_USE_MULT:-11", DRIVE);

        assertEquals(List.of("TOP_SPEED_PERCENT_ENGINE_ROOM", "FUEL_USE_MULT_ENGINE_ROOM"), decoded.stream().map(RolledModifier::modifierId).toList());
        String reencoded = ItemCodec.modifiers(decoded);
        assertEquals("P=TOP_SPEED_PERCENT_ENGINE_ROOM=TOP_SPEED_PERCENT:12;S=FUEL_USE_MULT_ENGINE_ROOM=FUEL_USE_MULT:-11", reencoded);
        assertEquals(decoded, ItemCodec.decodeModifiers(reencoded, DRIVE));
    }

    @Test
    void aCargoCodeWithAMalformedModifierIdIsRejected() {
        assertNull(ItemCodec.decodeModifiers("P=bad id=TOP_SPEED_PERCENT:12", DRIVE));
        assertNull(ItemCodec.decodeModifiers("P==TOP_SPEED_PERCENT:12", DRIVE));
    }

    @Test
    void theCatalogueSkipsBrokenRowsAndKeepsTheRest() throws Exception {
        ItemModifiers.register(new JSONArray()
                .put(ItemFixtures.modifierRow("HULL_MULT", "prefix", "HULL_MULT:4:6", "Reinforced"))
                .put(ItemFixtures.modifierRow("HULL_MULT", "suffix", "HULL_MULT:1:2", "Again"))
                .put(ItemFixtures.modifierRow("NOT_REAL", "prefix", "NOT_REAL:1:2", "Nope"))
                .put(ItemFixtures.modifierRow("ARMOR_PERCENT", "sideways", "ARMOR_PERCENT:1:2", "Plated")));

        assertEquals(List.of("HULL_MULT"), ItemModifiers.all().stream().map(Modifier::id).toList());
        assertEquals(ModifierSlot.PREFIX, ItemModifiers.get("HULL_MULT").slot());
    }
}
