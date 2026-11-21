package exiledsector.items;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.security.AnyTypePermission;
import exiledsector.items.hull.HullItem;
import exiledsector.items.modifier.RolledLine;
import exiledsector.items.modifier.RolledModifier;
import exiledsector.items.name.FrozenName;
import exiledsector.skills.skilleffect.EffectAliases;
import exiledsector.modifiers.ModifierSlot;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

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
    void anImmutableModifierKeepsItsSavedLockedFieldName() {
        RolledModifier immutable = RolledModifier.single(ModifierSlot.PREFIX, new RolledLine("HULL_MULT", 6f)).asImmutable();

        String xml = xstream().toXML(immutable);
        RolledModifier loaded = (RolledModifier) xstream().fromXML(xml);

        assertTrue(xml.contains("<locked>true</locked>"), xml);
        assertTrue(loaded.immutable());
    }

    @Test
    void anItemLoadedWhileItsDefinitionIsMissingKeepsItsModifiersForWhenTheDefinitionReturns() throws Exception {
        ItemStore store = new ItemStore();
        Item saved = store.add(HullItemRows.registerMilitary(), 5L);
        String xml = xstream().toXML(store);
        ItemDefinitions.clear();

        Item restored = ((ItemStore) xstream().fromXML(xml)).owned().get(0);

        assertEquals(UnresolvedItem.class, restored.getClass());
        assertEquals(saved.explicits(), restored.explicits());
        HullItemRows.registerMilitary();
        String resaved = xstream().toXML(restored);
        assertTrue(resaved.startsWith("<exiledSector.Socketable>"), resaved);
        assertEquals(HullItem.class, xstream().fromXML(resaved).getClass());
    }

    @Test
    void aSavedModifierWhoseEffectWasRenamedLoadsUnderTheCurrentName() throws Exception {
        ItemDefinition military = HullItemRows.registerMilitary();
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
        ItemDefinition military = HullItemRows.registerMilitary();
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
        ItemDefinition military = HullItemRows.registerMilitary();
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
    void anItemSavedWhileItsDefinitionWasMissingLoadsAsTheTypeItsDefinitionGives() throws Exception {
        HullItemRows.registerMilitary();
        ItemStore store = new ItemStore();
        Item saved = store.add(ItemDefinitions.get(HullItemRows.MILITARY), 3L);
        String unresolvedXml = xstream().toXML(store).replace("exiledSector.HullItem>", ItemSaveAliases.UNRESOLVED_ITEM_ALIAS + ">");

        ItemStore loaded = (ItemStore) xstream().fromXML(unresolvedXml);

        assertTrue(unresolvedXml.contains("<" + ItemSaveAliases.UNRESOLVED_ITEM_ALIAS + ">"), unresolvedXml);
        Item restored = loaded.owned().get(0);
        assertEquals(HullItem.class, restored.getClass());
        assertEquals(saved.effects(), restored.effects());
        assertEquals(ItemKind.BRIDGE, restored.kind());
        assertTrue(xstream().toXML(loaded).contains("<exiledSector.HullItem>"));
    }

    @Test
    void savedEffectNamesFollowARenameThroughTheAliasList() throws Exception {
        String rolledXml = xstream().toXML(new RolledLine("HULL_MULT", 5f)).replace("HULL_MULT", "OLD_HULL_MULT");
        String nameXml = xstream().toXML(new FrozenName("HULL_MULT", "HULL_MULT", null, null)).replace("HULL_MULT", "OLD_HULL_MULT");
        EffectAliases.register(new JSONArray().put(new JSONObject().put("alias", "OLD_HULL_MULT").put("effect", "HULL_MULT")));
        try {
            RolledLine loadedEffect = (RolledLine) xstream().fromXML(rolledXml);
            FrozenName loadedName = (FrozenName) xstream().fromXML(nameXml);

            assertTrue(rolledXml.contains("OLD_HULL_MULT"), rolledXml);
            assertEquals(new RolledLine("HULL_MULT", 5f), loadedEffect);
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
