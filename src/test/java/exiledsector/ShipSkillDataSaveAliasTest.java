package exiledsector;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.security.AnyTypePermission;
import exiledsector.skills.ShipSkillData;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShipSkillDataSaveAliasTest {

    private static XStream xstream(boolean withAliases) {
        XStream xstream = new XStream(new DomDriver());
        XStream.setupDefaultSecurity(xstream);
        xstream.addPermission(AnyTypePermission.ANY);
        if (withAliases) {
            new ExiledSectorModPlugin().configureXStream(xstream);
        }
        return xstream;
    }

    private static Map<String, ShipSkillData> records() {
        ShipSkillData data = new ShipSkillData();
        data.incrementLevel();
        data.addFreeAllocationCredit();
        Map<String, ShipSkillData> records = new HashMap<>();
        records.put("ship-a", data);
        return records;
    }

    @Test
    void shipRecordsAreSavedUnderAStableAliasInsteadOfTheirClassName() {
        String xml = xstream(true).toXML(records());

        assertTrue(xml.contains(ExiledSectorModPlugin.SHIP_SKILL_DATA_ALIAS), xml);
        assertFalse(xml.contains(ShipSkillData.class.getName()), xml);
    }

    @Test
    void savesWrittenBeforeTheAliasStillLoad() {
        String olderXml = xstream(false).toXML(records());

        @SuppressWarnings("unchecked") // the XML above was written from a Map<String, ShipSkillData>
        Map<String, ShipSkillData> loaded = (Map<String, ShipSkillData>) xstream(true).fromXML(olderXml);

        assertTrue(olderXml.contains(ShipSkillData.class.getName()), olderXml);
        assertEquals(1, loaded.get("ship-a").getLevel());
        assertEquals(1, loaded.get("ship-a").getBankedFreeAllocations());
    }
}
