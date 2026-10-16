package exiledsector.persistence;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetiredReserveIdsTest {

    static final class SavedDemand {
        String commodityId;
        String hullModId;
    }

    private static XStream secured() {
        XStream xstream = new XStream(new DomDriver());
        XStream.setupDefaultSecurity(xstream);
        xstream.allowTypes(new Class[]{LinkedHashSet.class, String.class, SavedDemand.class});
        xstream.useAttributeFor(SavedDemand.class, "commodityId");
        xstream.useAttributeFor(SavedDemand.class, "hullModId");
        return xstream;
    }

    private static XStream xstreamWithRetiredIds() {
        XStream xstream = secured();
        RetiredReserveIds.register(xstream);
        return xstream;
    }

    @Test
    void stringsSavedAsAttributesStillLoadAndOldSlotIdsAmongThemAreRetired() {
        SavedDemand saved = new SavedDemand();
        saved.commodityId = "food";
        saved.hullModId = "exiledSector_opSpent_2500";
        String xml = secured().toXML(saved);

        SavedDemand loaded = (SavedDemand) xstreamWithRetiredIds().fromXML(xml);

        assertTrue(xml.contains("commodityId=\"food\""), xml);
        assertEquals("food", loaded.commodityId);
        assertEquals(RetiredReserveIds.RETIRED_ID, loaded.hullModId);
    }

    @Test
    void stringsSavedAsAttributesAreWrittenUnchanged() {
        SavedDemand saved = new SavedDemand();
        saved.commodityId = "food";
        saved.hullModId = "exiledSector_opSpent_2500";

        assertEquals(secured().toXML(saved), xstreamWithRetiredIds().toXML(saved));
    }

    @Test
    void everyOldSlotIdALoadedSaveListsBecomesTheOneRetiredId() {
        XStream xstream = xstreamWithRetiredIds();
        Set<String> savedHullMods = new LinkedHashSet<>(List.of("heavyarmor", "exiledSector_opSpent_4321", "exiledSector_opSpent_7"));

        Object loaded = xstream.fromXML(new XStream(new DomDriver()).toXML(savedHullMods));

        assertEquals(new LinkedHashSet<>(List.of("heavyarmor", RetiredReserveIds.RETIRED_ID)), loaded);
    }

    @Test
    void stringsThatOnlyLookLikeOldSlotIdsAreLeftAlone() {
        XStream xstream = xstreamWithRetiredIds();

        for (String unrelated : List.of("exiledSector_opSpentSlots", "exiledSector_opSpent_", "exiledSector_opSpent_7a", "exiledSector_opReserve_40")) {
            assertEquals(unrelated, xstream.fromXML(new XStream(new DomDriver()).toXML(unrelated)));
        }
    }

    @Test
    void savingWritesStringsUnchanged() {
        assertEquals(new XStream(new DomDriver()).toXML("exiledSector_opSpent_12"), xstreamWithRetiredIds().toXML("exiledSector_opSpent_12"));
    }

    @Test
    void registeringTwiceDoesNotStackTheRewrite() {
        XStream xstream = secured();
        RetiredReserveIds.register(xstream);
        RetiredReserveIds.register(xstream);

        assertEquals(RetiredReserveIds.RETIRED_ID, xstream.fromXML(new XStream(new DomDriver()).toXML("exiledSector_opSpent_99")));
    }

    @Test
    void onlyDigitsAfterThePrefixCountAsAnOldSlot() {
        assertTrue(RetiredReserveIds.isLegacySlotId("exiledSector_opSpent_0"));
        assertTrue(RetiredReserveIds.isLegacySlotId("exiledSector_opSpent_9999"));
        assertFalse(RetiredReserveIds.isLegacySlotId("exiledSector_opSpent_"));
        assertFalse(RetiredReserveIds.isLegacySlotId("exiledSector_opSpentNextSlot"));
    }
}
