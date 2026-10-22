package exiledsector.items;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.security.AnyTypePermission;
import exiledsector.modifiers.ModifierSlot;
import exiledlib.skills.skilleffect.EffectAliases;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemSaveTest {

    @AfterEach
    void tearDown() {
        ItemDefinitions.clear();
    }

    private static XStream xstream() {
        XStream xstream = new XStream(new DomDriver());
        XStream.setupDefaultSecurity(xstream);
        xstream.addPermission(AnyTypePermission.ANY);
        ItemSaveAliases.register(xstream);
        return xstream;
    }

    @Test
    void itemsSavedAsLooseEffectsLoadAsModifiersWithSlotsFromTheirDefinition() throws Exception {
        ItemFixtures.registerMilitary();
        String legacyXml = "<exiledSector.SocketableStore><owned><exiledSector.Socketable>"
                + "<id>socketable_1</id><definitionId>" + ItemFixtures.MILITARY + "</definitionId><seed>5</seed><effects>"
                + "<exiledSector.RolledEffect><effectName>FLUX_CAPACITY_MULT</effectName><magnitude>5.0</magnitude></exiledSector.RolledEffect>"
                + "<exiledSector.RolledEffect><effectName>HULL_MULT</effectName><magnitude>6.0</magnitude></exiledSector.RolledEffect>"
                + "</effects></exiledSector.Socketable></owned><nextId>2</nextId></exiledSector.SocketableStore>";

        ItemStore loaded = (ItemStore) xstream().fromXML(legacyXml);
        Item restored = loaded.owned().get(0);

        assertEquals(List.of(RolledModifier.single(ModifierSlot.PREFIX, new RolledEffect("FLUX_CAPACITY_MULT", 5f)),
                RolledModifier.single(ModifierSlot.SUFFIX, new RolledEffect("HULL_MULT", 6f))), restored.explicits());
        String resaved = xstream().toXML(loaded);
        assertTrue(resaved.contains("<explicitModifiers>") && resaved.contains("<exiledSector.RolledModifier>"), resaved);
        assertFalse(resaved.contains("<effects>"), resaved);
    }

    @Test
    void anOldItemLoadedWhileItsDefinitionIsMissingKeepsItsSlotsForWhenTheDefinitionReturns() throws Exception {
        String legacyXml = "<exiledSector.SocketableStore><owned><exiledSector.Socketable>"
                + "<id>socketable_1</id><definitionId>" + ItemFixtures.MILITARY + "</definitionId><seed>5</seed><effects>"
                + "<exiledSector.RolledEffect><effectName>FLUX_CAPACITY_MULT</effectName><magnitude>5.0</magnitude></exiledSector.RolledEffect>"
                + "</effects></exiledSector.Socketable></owned><nextId>2</nextId></exiledSector.SocketableStore>";
        Item restored = ((ItemStore) xstream().fromXML(legacyXml)).owned().get(0);

        assertEquals(ModifierSlot.SUFFIX, restored.explicits().get(0).slot(), "without a definition the slot cannot be known yet");
        assertEquals(UnresolvedItem.class, restored.getClass());
        ItemFixtures.registerMilitary();
        assertEquals(ModifierSlot.PREFIX, restored.explicits().get(0).slot());
        String resaved = xstream().toXML(restored);
        assertTrue(resaved.startsWith("<exiledSector.Socketable>"), resaved);
        assertEquals(HullItem.class, xstream().fromXML(resaved).getClass());
    }

    @Test
    void aSavedModifierWhoseEffectWasRenamedLoadsUnderTheCurrentName() throws Exception {
        ItemDefinition military = ItemFixtures.registerMilitary();
        ItemStore store = new ItemStore();
        store.add(military, 1L);
        String xml = xstream().toXML(store).replace("HULL_MULT", "OLD_HULL_MULT");
        EffectAliases.register(new JSONArray().put(new JSONObject().put("alias", "OLD_HULL_MULT").put("effect", "HULL_MULT")));
        try {
            Item restored = ((ItemStore) xstream().fromXML(xml)).owned().get(0);

            assertTrue(restored.explicits().stream().noneMatch(modifier -> modifier.modifierId().startsWith("OLD_")), restored.explicits().toString());
            assertTrue(restored.explicits().stream().flatMap(modifier -> modifier.lines().stream()).noneMatch(line -> line.effectName().startsWith("OLD_")));
        } finally {
            EffectAliases.clear();
        }
    }

    @Test
    void theStoreSurvivesASaveAndLoadUnderStableNamesRatherThanJavaClassPaths() throws Exception {
        ItemDefinition military = ItemFixtures.registerMilitary();
        ItemStore store = new ItemStore();
        Item saved = store.add(military, 99L);

        String xml = xstream().toXML(store);
        ItemStore loaded = (ItemStore) xstream().fromXML(xml);

        assertTrue(xml.contains("<exiledSector.SocketableStore>") && xml.contains("<exiledSector.HullItem>") && xml.contains("<exiledSector.RolledEffect>"), xml);
        assertFalse(xml.contains("exiledsector.socketables"), xml);
        Item restored = loaded.owned().get(0);
        assertEquals(ItemKind.BRIDGE, restored.kind());
        assertEquals(saved.id(), restored.id());
        assertEquals(saved.effects(), restored.effects());
        assertEquals(99L, restored.seed());
        assertEquals(saved.id().replace("1", "2"), loaded.add(military, 1L).id());
    }

    @Test
    void savesFromWhenHullItemsCouldBeFavouritedStillLoad() throws Exception {
        ItemDefinition military = ItemFixtures.registerMilitary();
        ItemStore store = new ItemStore();
        Item saved = store.add(military, 1L);
        XStream gameLike = xstream();
        gameLike.ignoreUnknownElements();

        String xml = xstream().toXML(store).replace("</explicitModifiers>", "</explicitModifiers>\n<favourite>true</favourite>");
        ItemStore loaded = (ItemStore) gameLike.fromXML(xml);

        assertTrue(xml.contains("<favourite>true</favourite>"), xml);
        assertEquals(saved.effects(), loaded.owned().get(0).effects());
    }

    @Test
    void itemsSavedUnderLegacyNamesLoadAsTheTypeTheirDefinitionGives() throws Exception {
        ItemFixtures.registerMilitary();
        ItemStore store = new ItemStore();
        Item saved = store.add(ItemDefinitions.get(ItemFixtures.MILITARY), 3L);
        String currentXml = xstream().toXML(store);

        List<String> legacyAliases = new java.util.ArrayList<>(ItemSaveAliases.LEGACY_ITEM_ALIASES);
        legacyAliases.add(ItemSaveAliases.UNRESOLVED_ITEM_ALIAS);
        for (String legacyAlias : legacyAliases) {
            String legacyXml = currentXml.replace("exiledSector.HullItem>", legacyAlias + ">");
            ItemStore loaded = (ItemStore) xstream().fromXML(legacyXml);

            assertTrue(legacyXml.contains("<" + legacyAlias + ">"), legacyXml);
            Item restored = loaded.owned().get(0);
            assertEquals(HullItem.class, restored.getClass());
            assertEquals(saved.effects(), restored.effects());
            assertEquals(ItemKind.BRIDGE, restored.kind());
            assertTrue(xstream().toXML(loaded).contains("<exiledSector.HullItem>"));
        }
    }

    @Test
    void savedEffectNamesFollowARenameThroughTheAliasList() throws Exception {
        String rolledXml = xstream().toXML(new RolledEffect("HULL_MULT", 5f)).replace("HULL_MULT", "OLD_HULL_MULT");
        String nameXml = xstream().toXML(new FrozenName("HULL_MULT", "HULL_MULT", null, null)).replace("HULL_MULT", "OLD_HULL_MULT");
        EffectAliases.register(new JSONArray().put(new JSONObject().put("alias", "OLD_HULL_MULT").put("effect", "HULL_MULT")));
        try {
            RolledEffect loadedEffect = (RolledEffect) xstream().fromXML(rolledXml);
            FrozenName loadedName = (FrozenName) xstream().fromXML(nameXml);

            assertTrue(rolledXml.contains("OLD_HULL_MULT"), rolledXml);
            assertEquals(new RolledEffect("HULL_MULT", 5f), loadedEffect);
            assertEquals("HULL_MULT", loadedName.prefixModifierId());
            assertEquals("HULL_MULT", loadedName.suffixModifierId());
        } finally {
            EffectAliases.clear();
        }
    }

    @Test
    void cargoItemDataRoundTripsAndRejectsGarbage() {
        ItemData item = new ItemData("retired_item", -5L);

        assertEquals(item, ItemData.of(item.toSpecialItem()));
        assertEquals(null, ItemData.parse("no_seed"));
        assertEquals(null, ItemData.parse("id|not_a_number"));
        assertEquals(null, ItemData.parse(null));
    }
}
