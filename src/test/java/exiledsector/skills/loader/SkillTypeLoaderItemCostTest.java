package exiledsector.skills.loader;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.SpecialItemSpecAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import exiledsector.skills.SkillType;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SkillTypeLoaderItemCostTest {

    private MockedStatic<Global> globalMock;
    private SettingsAPI settings;

    @BeforeEach
    void setUp() {
        settings = mock(SettingsAPI.class);
        when(settings.getCommoditySpec("lobster")).thenReturn(mock(CommoditySpecAPI.class));
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSettings).thenReturn(settings);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    private static SkillType typeCosting(String itemId) throws Exception {
        JSONObject types = new JSONObject("{\"skillTypes\": [{\"id\": \"paid\", \"name\": \"Paid\", \"icon\": \"a.png\", \"tier\": \"NOTABLE\","
                + " \"itemCost\": {\"itemId\": \"" + itemId + "\", \"quantity\": 50}}]}");
        Map<String, SkillType> parsed = SkillTypeLoader.parseSkillTypes(types);
        return parsed.get("paid");
    }

    @Test
    void anItemCostInACommodityTheGameHasIsKept() throws Exception {
        SkillType type = typeCosting("lobster");

        assertEquals("lobster", type.getItemCost().itemId());
        assertEquals(50f, type.getItemCost().quantity());
    }

    @Test
    void anItemCostInASpecialItemTheGameHasIsKept() throws Exception {
        when(settings.getSpecialItemSpec("fragment_fabricator")).thenReturn(mock(SpecialItemSpecAPI.class));

        assertEquals("fragment_fabricator", typeCosting("fragment_fabricator").getItemCost().itemId());
    }

    @Test
    void anItemCostInACommodityFromAMissingModIsWaivedSoTheNodeCanStillBeTaken() throws Exception {
        assertNull(typeCosting("rat_abyssal_matter").getItemCost());
    }
}
