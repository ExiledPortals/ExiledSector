package exiledsector.socketables;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.security.AnyTypePermission;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocketableSaveTest {

    @AfterEach
    void tearDown() {
        SocketableDefinitions.clear();
    }

    private static XStream xstream() {
        XStream xstream = new XStream(new DomDriver());
        XStream.setupDefaultSecurity(xstream);
        xstream.addPermission(AnyTypePermission.ANY);
        SocketableSaveAliases.register(xstream);
        return xstream;
    }

    @Test
    void theStoreSurvivesASaveAndLoadUnderStableNamesRatherThanJavaClassPaths() throws Exception {
        SocketableDefinition military = SocketableFixtures.registerMilitary();
        SocketableStore store = new SocketableStore();
        Socketable saved = store.add(military, 99L);

        String xml = xstream().toXML(store);
        SocketableStore loaded = (SocketableStore) xstream().fromXML(xml);

        assertTrue(xml.contains("<exiledSector.SocketableStore>") && xml.contains("<exiledSector.Socketable>") && xml.contains("<exiledSector.RolledEffect>"), xml);
        assertFalse(xml.contains("exiledsector.socketables"), xml);
        Socketable restored = loaded.owned().get(0);
        assertEquals(SocketType.SUBROUTINE, restored.kind());
        assertEquals(saved.id(), restored.id());
        assertEquals(saved.effects(), restored.effects());
        assertEquals(99L, restored.seed());
        assertEquals(saved.id().replace("1", "2"), loaded.add(military, 1L).id());
    }

    @Test
    void savesFromWhenSocketablesCouldBeFavouritedStillLoad() throws Exception {
        SocketableDefinition military = SocketableFixtures.registerMilitary();
        SocketableStore store = new SocketableStore();
        Socketable saved = store.add(military, 1L);
        XStream gameLike = xstream();
        gameLike.ignoreUnknownElements();

        String xml = xstream().toXML(store).replace("</effects>", "</effects>\n<favourite>true</favourite>");
        SocketableStore loaded = (SocketableStore) gameLike.fromXML(xml);

        assertTrue(xml.contains("<favourite>true</favourite>"), xml);
        assertEquals(saved.effects(), loaded.owned().get(0).effects());
    }

    @Test
    void socketablesFromBeforeSocketTypesLoadUnderTheirLegacyNames() throws Exception {
        SocketableFixtures.registerMilitary();
        SocketableStore store = new SocketableStore();
        Socketable saved = store.add(SocketableDefinitions.get(SocketableFixtures.MILITARY), 3L);
        String currentXml = xstream().toXML(store);

        for (String legacyAlias : SocketableSaveAliases.LEGACY_SOCKETABLE_ALIASES) {
            String legacyXml = currentXml.replace("exiledSector.Socketable>", legacyAlias + ">");
            SocketableStore loaded = (SocketableStore) xstream().fromXML(legacyXml);

            assertTrue(legacyXml.contains("<" + legacyAlias + ">"), legacyXml);
            Socketable restored = loaded.owned().get(0);
            assertEquals(Socketable.class, restored.getClass());
            assertEquals(saved.effects(), restored.effects());
            assertEquals(SocketType.SUBROUTINE, restored.kind());
            assertTrue(xstream().toXML(loaded).contains("<exiledSector.Socketable>"));
        }
    }

    @Test
    void cargoItemDataRoundTripsAndRejectsGarbage() {
        SocketableItemData item = new SocketableItemData("domain_subroutine_military", -5L);

        assertEquals(item, SocketableItemData.of(item.toSpecialItem()));
        assertEquals(null, SocketableItemData.parse("no_seed"));
        assertEquals(null, SocketableItemData.parse("id|not_a_number"));
        assertEquals(null, SocketableItemData.parse(null));
    }
}
