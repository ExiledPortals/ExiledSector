package exiledsector.skills.npc;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import exiledsector.charts.ChartIds;
import exiledsector.skills.progression.ShipLevelConfig;
import lunalib.lunaSettings.LunaSettings;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NpcLevelCapsTest {

    private static final String VANILLA_ROWS = "[{\"id\": \"pirate\", \"category\": \"pirate\", \"cap\": \"15\"},"
            + "{\"id\": \"faction\", \"category\": \"faction\", \"cap\": \"25\"},"
            + "{\"id\": \"ai\", \"category\": \"ai\", \"cap\": \"35\"},"
            + "{\"id\": \"remnant\", \"category\": \"ai\", \"cap\": \"\"}]";

    private MockedStatic<LunaSettings> lunaSettingsMock;

    @BeforeEach
    void setUp() {
        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class, invocation -> null);
    }

    @AfterEach
    void tearDown() {
        lunaSettingsMock.close();
        NpcLevelCaps.clear();
    }

    @Test
    void thePirateFlagAndAnAiCoreCommanderPickTheCategoryAndEverythingElseIsAFaction() throws Exception {
        NpcLevelCaps.register(new JSONArray(VANILLA_ROWS));

        assertEquals(NpcLevelCategory.PIRATE, NpcLevelCaps.categoryFor("modded_raiders", true, false));
        assertEquals(NpcLevelCategory.AI, NpcLevelCaps.categoryFor("modded_raiders", true, true));
        assertEquals(NpcLevelCategory.AI, NpcLevelCaps.categoryFor("remnant", false, false));
        assertEquals(NpcLevelCategory.FACTION, NpcLevelCaps.categoryFor(Factions.PLAYER, false, false));
        assertEquals(NpcLevelCategory.FACTION, NpcLevelCaps.categoryFor(null, false, false));
        assertEquals(15, NpcLevelCaps.capFor("modded_raiders", true, false));
        assertEquals(25, NpcLevelCaps.capFor("hegemony", false, false));
        assertEquals(35, NpcLevelCaps.capFor("remnant", false, false));
    }

    @Test
    void csvRowsOverrideCategoryCapsAndSingleFactions() throws Exception {
        NpcLevelCaps.register(new JSONArray("[{\"id\": \"pirate\", \"category\": \"pirate\", \"cap\": \"12\"},"
                + "{\"id\": \"wrongly_flagged\", \"category\": \"faction\", \"cap\": \"\"},"
                + "{\"id\": \"elite_guard\", \"category\": \"\", \"cap\": \"30\"},"
                + "{\"id\": \"elite_raiders\", \"category\": \"pirate\", \"cap\": \"20\"},"
                + "{\"id\": \"typo\", \"category\": \"pirat\", \"cap\": \"5\"},"
                + "{\"id\": \"bad_cap\", \"category\": \"\", \"cap\": \"lots\"},"
                + "{\"id\": \"empty\", \"category\": \"\", \"cap\": \"\"}]"));

        assertEquals(12, NpcLevelCaps.capOf(NpcLevelCategory.PIRATE));
        assertEquals(12, NpcLevelCaps.capFor("raiders", true, false));
        assertEquals(NpcLevelCategory.FACTION, NpcLevelCaps.categoryFor("wrongly_flagged", true, true));
        assertEquals(25, NpcLevelCaps.capFor("wrongly_flagged", true, true));
        assertEquals(30, NpcLevelCaps.capFor("elite_guard", false, false));
        assertEquals(NpcLevelCategory.PIRATE, NpcLevelCaps.categoryFor("elite_raiders", false, false));
        assertEquals(20, NpcLevelCaps.capFor("elite_raiders", false, false));
        assertEquals(12, NpcLevelCaps.capFor("typo", true, false));
        assertEquals(25, NpcLevelCaps.capFor("bad_cap", false, false));
        assertEquals(25, NpcLevelCaps.capFor("empty", false, false));
    }

    @Test
    void capsNeverExceedTheMaxLevelSetting() throws Exception {
        NpcLevelCaps.register(new JSONArray(VANILLA_ROWS));
        lunaSettingsMock.when(() -> LunaSettings.getInt("exiledSector", ShipLevelConfig.MAX_LEVEL_FIELD_ID)).thenReturn(20);

        assertEquals(15, NpcLevelCaps.capOf(NpcLevelCategory.PIRATE));
        assertEquals(20, NpcLevelCaps.capOf(NpcLevelCategory.FACTION));
        assertEquals(20, NpcLevelCaps.capFor("remnant", false, false));
    }

    @Test
    void capsNeverExceedTheMaxAllocatedNodes() throws Exception {
        NpcLevelCaps.register(new JSONArray(VANILLA_ROWS));
        lunaSettingsMock.when(() -> LunaSettings.getInt("exiledSector", ShipLevelConfig.MAX_ALLOCATED_NODES_FIELD_ID)).thenReturn(31);

        assertEquals(30, NpcLevelCaps.capOf(NpcLevelCategory.AI));
    }

    @Test
    void fleetsAreReadFromTheirFactionFlagAndCommander() {
        assertEquals(15, NpcLevelCaps.capFor(fleet("pirates", true, false)));
        assertEquals(35, NpcLevelCaps.capFor(fleet("modded_ai", false, true)));
        assertEquals(25, NpcLevelCaps.capFor(fleet("hegemony", false, false)));
        assertEquals(25, NpcLevelCaps.capFor((CampaignFleetAPI) null));
    }

    @Test
    void membersUseTheirFleetsCapAndFallBackToTheFactionCap() {
        CampaignFleetAPI pirateFleet = fleet("pirates", true, false);
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        FleetDataAPI fleetData = mock(FleetDataAPI.class);
        when(fleetData.getFleet()).thenReturn(pirateFleet);
        when(member.getFleetData()).thenReturn(fleetData);

        assertEquals(15, NpcLevelCaps.capFor(member));
        assertEquals(25, NpcLevelCaps.capFor(mock(FleetMemberAPI.class)));
        assertEquals(25, NpcLevelCaps.capFor((FleetMemberAPI) null));
    }

    @Test
    void aCapTagOnTheVariantWinsOverTheLiveFleetAndCanBeReplacedOrRemoved() {
        List<String> tags = new ArrayList<>();
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.getTags()).thenAnswer(invocation -> new ArrayList<>(tags));
        doAnswer(invocation -> tags.add(invocation.getArgument(0))).when(variant).addTag(anyString());
        doAnswer(invocation -> tags.remove((String) invocation.getArgument(0))).when(variant).removeTag(anyString());

        assertEquals(25, NpcLevelCaps.capFor(null, variant));
        NpcLevelCaps.tag(variant, 35);
        NpcLevelCaps.tag(variant, 15);
        NpcLevelCaps.tag(variant, 15);

        assertEquals(List.of(NpcLevelCaps.TAG_PREFIX + "15"), tags);
        assertEquals(15, NpcLevelCaps.capFor(null, variant));
        NpcLevelCaps.tag(variant, NpcLevelCaps.UNCAPPED);
        assertEquals(NpcLevelCaps.UNCAPPED, NpcLevelCaps.capFor(null, variant));
        NpcLevelCaps.removeTag(variant);
        assertEquals(List.of(), tags);
    }

    @Test
    void chartFleetsAreNotCapped() {
        CampaignFleetAPI chartFleet = fleet("pirates", true, false);
        when(chartFleet.getMemoryWithoutUpdate().get(ChartIds.TIER_MEMORY_KEY)).thenReturn(3);

        assertEquals(NpcLevelCaps.UNCAPPED, NpcLevelCaps.capFor(chartFleet));
    }

    private static CampaignFleetAPI fleet(String factionId, boolean pirateBehavior, boolean aiCommander) {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        FactionAPI faction = mock(FactionAPI.class);
        when(faction.getId()).thenReturn(factionId);
        when(faction.getCustomBoolean(Factions.CUSTOM_PIRATE_BEHAVIOR)).thenReturn(pirateBehavior);
        when(fleet.getFaction()).thenReturn(faction);
        PersonAPI commander = mock(PersonAPI.class);
        when(commander.isAICore()).thenReturn(aiCommander);
        when(fleet.getCommander()).thenReturn(commander);
        when(fleet.getMemoryWithoutUpdate()).thenReturn(mock(MemoryAPI.class));
        return fleet;
    }
}
