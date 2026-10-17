package exiledsector.hullitems;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.security.AnyTypePermission;
import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierSlot;
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

class HullItemModifierReferencesTest {

    private static final String DRIVE = "engine_room_drive";

    @BeforeEach
    void register() throws Exception {
        HullItemFixtures.registerModifierNames(
                "TOP_SPEED_PERCENT,prefix,TOP_SPEED_PERCENT:-10:-5,Hasty",
                "TOP_SPEED_PERCENT_ENGINE_ROOM,prefix,TOP_SPEED_PERCENT:5:15,Swift",
                "FUEL_USE_MULT_ENGINE_ROOM,suffix,FUEL_USE_MULT:-15:-10,Economy",
                "TOP_SPEED_PERCENT_IMPLICIT,implicit,TOP_SPEED_PERCENT:5:10,",
                "HYBRID_FLUX_CAPACITY_FLUX_DISSIPATION,implicit,FLUX_CAPACITY_PERCENT:5:10; FLUX_DISSIPATION_PERCENT:5:10,");
        HullItemDefinitions.register(new JSONArray()
                .put(HullItemFixtures.row(DRIVE, "engine_room", "@TOP_SPEED_PERCENT_ENGINE_ROOM:3", "@FUEL_USE_MULT_ENGINE_ROOM")
                        .put("name", "Drive"))
                .put(HullItemFixtures.row("emitter", "shield_generator", "@TOP_SPEED_PERCENT_ENGINE_ROOM", "").put("name", "Emitter")));
        HullItemImplicits.register(new JSONArray()
                .put(new JSONObject().put("type", "engine_room").put("implicits", "@TOP_SPEED_PERCENT_IMPLICIT"))
                .put(new JSONObject().put("type", "shield_generator").put("implicits", "@HYBRID_FLUX_CAPACITY_FLUX_DISSIPATION")));
    }

    @AfterEach
    void clear() {
        HullItemImplicits.clear();
        HullItemDefinitions.clear();
        HullItemModifiers.clear();
    }

    private static XStream xstream() {
        XStream xstream = new XStream(new DomDriver());
        XStream.setupDefaultSecurity(xstream);
        xstream.addPermission(AnyTypePermission.ANY);
        HullItemSaveAliases.register(xstream);
        return xstream;
    }

    @Test
    void aPoolEntryUsesTheReferencedModifierAndItsOwnWeight() {
        HullItemDefinition drive = HullItemDefinitions.get(DRIVE);
        PoolEntry speed = drive.prefixes().get(0);

        assertSame(HullItemModifiers.get("TOP_SPEED_PERCENT_ENGINE_ROOM"), speed.modifier());
        assertEquals(3f, speed.weight());
        assertEquals(15f, speed.max());
        assertSame(speed, drive.poolEntry("TOP_SPEED_PERCENT"));
    }

    @Test
    void aReferenceToAMissingModifierOrTheWrongSlotIsSkipped() throws Exception {
        HullItemDefinitions.register(new JSONArray().put(HullItemFixtures.row("broken", "engine_room",
                "@NO_SUCH_MODIFIER; @FUEL_USE_MULT_ENGINE_ROOM; @TOP_SPEED_PERCENT_ENGINE_ROOM", "")));

        assertEquals(List.of("TOP_SPEED_PERCENT_ENGINE_ROOM"),
                HullItemDefinitions.get("broken").prefixes().stream().map(entry -> entry.modifier().id()).toList());
    }

    @Test
    void anItemSavedBeforeModifierIdsKeepsItsRollUnderThePoolsCurrentId() throws Exception {
        HullItem saved = new HullItem("item_1", DRIVE, 4L, List.of(
                RolledModifier.single(ModifierSlot.PREFIX, new RolledEffect("TOP_SPEED_PERCENT", 12f)),
                RolledModifier.single(ModifierSlot.SUFFIX, new RolledEffect("FUEL_USE_MULT", -11f))));
        String xml = xstream().toXML(saved);

        HullItem restored = (HullItem) xstream().fromXML(xml);

        assertEquals(List.of(new RolledModifier("TOP_SPEED_PERCENT_ENGINE_ROOM", ModifierSlot.PREFIX, List.of(new RolledEffect("TOP_SPEED_PERCENT", 12f))),
                new RolledModifier("FUEL_USE_MULT_ENGINE_ROOM", ModifierSlot.SUFFIX, List.of(new RolledEffect("FUEL_USE_MULT", -11f)))),
                restored.explicits());
        assertEquals(15f, restored.rolls().stream().filter(roll -> roll.slot() == ModifierSlot.PREFIX).findFirst().orElseThrow().line().max());
    }

    @Test
    void aFrozenNameSavedWithEffectNamesStillFindsItsWords() {
        HullItem saved = new HullItem("item_1", DRIVE, 4L, List.of(
                RolledModifier.single(ModifierSlot.PREFIX, new RolledEffect("TOP_SPEED_PERCENT", 12f))));
        saved.freezeName(new FrozenName("TOP_SPEED_PERCENT", "FUEL_USE_MULT", null, null));

        assertTrue(saved.name().startsWith("Swift ") && saved.name().endsWith(" Economy"), saved.name());
    }

    @Test
    void anEffectThatLeftThePoolStillNamesFrozenItemsThroughANamedModifierOfItsSlot() {
        HullItem saved = new HullItem("item_1", "emitter", 4L, List.of(
                new RolledModifier("TOP_SPEED_PERCENT_ENGINE_ROOM", ModifierSlot.PREFIX, List.of(new RolledEffect("TOP_SPEED_PERCENT", 12f)))));
        saved.freezeName(new FrozenName(null, "FUEL_USE_MULT", null, null));

        assertTrue(saved.name().endsWith(" Economy"), saved.name());
    }

    @Test
    void aFrozenNameWhoseModifierIdWasRenamedTakesTheWordOfTheCurrentFirstPrefix() {
        HullItem saved = new HullItem("item_1", DRIVE, 4L, List.of(
                new RolledModifier("TOP_SPEED_PERCENT_ENGINE_ROOM", ModifierSlot.PREFIX, List.of(new RolledEffect("TOP_SPEED_PERCENT", 12f))),
                new RolledModifier("FUEL_USE_MULT_ENGINE_ROOM", ModifierSlot.SUFFIX, List.of(new RolledEffect("FUEL_USE_MULT", -11f)))));
        saved.freezeName(new FrozenName("OLD_SPEED_ID", "FUEL_USE_MULT_ENGINE_ROOM", null, null));

        assertTrue(saved.name().startsWith("Swift ") && saved.name().endsWith(" Economy"), saved.name());
    }

    @Test
    void rollChancesAreKeptPerModifier() {
        assertEquals(1.0, HullItemRollOdds.weightShares(HullItemDefinitions.get(DRIVE)).get("TOP_SPEED_PERCENT_ENGINE_ROOM"));
    }

    @Test
    void savedImplicitsSurviveWhileTheirTypeHasNoLoadedImplicits() throws Exception {
        HullItem rolled = new HullItem("item_1", DRIVE, 9L, List.of());
        List<RolledModifier> implicits = rolled.implicits();
        String xml = xstream().toXML(rolled);
        HullItemImplicits.register(new JSONArray());

        HullItem restored = (HullItem) xstream().fromXML(xml);

        assertEquals(implicits, restored.implicits());
        register();
        assertEquals(implicits, restored.implicits());
    }

    @Test
    void shieldGeneratorImplicitsSavedAsTwoSinglesFoldIntoTheHybridKeepingTheirValues() throws Exception {
        HullItem saved = new HullItem("item_1", "emitter", 9L, List.of());
        String xml = xstream().toXML(saved).replace("<explicitModifiers/>",
                "<explicitModifiers/><implicitModifiers>"
                        + "<exiledSector.RolledModifier><modifierId>FLUX_CAPACITY_PERCENT</modifierId><slot>IMPLICIT</slot><lines>"
                        + "<exiledSector.RolledEffect><effectName>FLUX_CAPACITY_PERCENT</effectName><magnitude>7.0</magnitude></exiledSector.RolledEffect>"
                        + "</lines></exiledSector.RolledModifier>"
                        + "<exiledSector.RolledModifier><modifierId>FLUX_DISSIPATION_PERCENT</modifierId><slot>IMPLICIT</slot><lines>"
                        + "<exiledSector.RolledEffect><effectName>FLUX_DISSIPATION_PERCENT</effectName><magnitude>9.0</magnitude></exiledSector.RolledEffect>"
                        + "</lines></exiledSector.RolledModifier></implicitModifiers>");

        HullItem restored = (HullItem) xstream().fromXML(xml);

        assertTrue(xml.contains("<implicitModifiers>"), xml);
        assertEquals(List.of(new RolledModifier("HYBRID_FLUX_CAPACITY_FLUX_DISSIPATION", ModifierSlot.IMPLICIT,
                List.of(new RolledEffect("FLUX_CAPACITY_PERCENT", 7f), new RolledEffect("FLUX_DISSIPATION_PERCENT", 9f)))), restored.implicits());
    }

    @Test
    void anImplicitThatNoLongerMatchesItsTypeIsRerolledFromTheSeed() {
        HullItem saved = new HullItem("item_1", DRIVE, 9L, List.of());
        List<RolledModifier> stale = List.of(RolledModifier.single(ModifierSlot.IMPLICIT, new RolledEffect("ARMOR_PERCENT", 7f)));

        List<RolledModifier> resolved = HullItemImplicits.withCurrentIds(saved.kind(), stale, 9L);

        assertEquals(HullItemImplicits.roll(saved.kind(), 9L), resolved);
        assertEquals("TOP_SPEED_PERCENT_IMPLICIT", resolved.get(0).modifierId());
    }

    @Test
    void cargoCodesFromBeforeModifierIdsDecodeToThePoolsCurrentIds() {
        List<RolledModifier> decoded = HullItemCodec.decodeModifiers("P=TOP_SPEED_PERCENT:12;FUEL_USE_MULT:-11", DRIVE);

        assertEquals(List.of("TOP_SPEED_PERCENT_ENGINE_ROOM", "FUEL_USE_MULT_ENGINE_ROOM"), decoded.stream().map(RolledModifier::modifierId).toList());
        String reencoded = HullItemCodec.modifiers(decoded);
        assertEquals("P=TOP_SPEED_PERCENT_ENGINE_ROOM=TOP_SPEED_PERCENT:12;S=FUEL_USE_MULT_ENGINE_ROOM=FUEL_USE_MULT:-11", reencoded);
        assertEquals(decoded, HullItemCodec.decodeModifiers(reencoded, DRIVE));
    }

    @Test
    void aCargoCodeWithAMalformedModifierIdIsRejected() {
        assertNull(HullItemCodec.decodeModifiers("P=bad id=TOP_SPEED_PERCENT:12", DRIVE));
        assertNull(HullItemCodec.decodeModifiers("P==TOP_SPEED_PERCENT:12", DRIVE));
    }

    @Test
    void theCatalogueSkipsBrokenRowsAndKeepsTheRest() throws Exception {
        HullItemModifiers.register(new JSONArray()
                .put(HullItemFixtures.modifierRow("HULL_MULT", "prefix", "HULL_MULT:4:6", "Reinforced"))
                .put(HullItemFixtures.modifierRow("HULL_MULT", "suffix", "HULL_MULT:1:2", "Again"))
                .put(HullItemFixtures.modifierRow("NOT_REAL", "prefix", "NOT_REAL:1:2", "Nope"))
                .put(HullItemFixtures.modifierRow("ARMOR_PERCENT", "sideways", "ARMOR_PERCENT:1:2", "Plated")));

        assertEquals(List.of("HULL_MULT"), HullItemModifiers.all().stream().map(Modifier::id).toList());
        assertEquals(ModifierSlot.PREFIX, HullItemModifiers.get("HULL_MULT").slot());
    }
}
