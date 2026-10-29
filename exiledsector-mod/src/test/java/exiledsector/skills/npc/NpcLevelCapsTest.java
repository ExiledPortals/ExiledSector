package exiledsector.skills.npc;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.intel.PersonBountyIntel;
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
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    void fixedRangesComeFromTheFactionRowThenTheCategoryRowThenLunaLib() throws Exception {
        NpcLevelCaps.register(new JSONArray("[{\"id\": \"pirate\", \"category\": \"pirate\", \"cap\": \"15\", \"min\": \"5\", \"mode\": \"fixed\"},"
                + "{\"id\": \"elite_guard\", \"category\": \"\", \"cap\": \"30\", \"min\": \"20\", \"mode\": \"Fixed\"},"
                + "{\"id\": \"scaled_raiders\", \"category\": \"pirate\", \"mode\": \"player\"}]"));

        assertEquals(new NpcLevelLimit(15, 15, 0, 5), NpcLevelCaps.limitFor(fleet("pirates", true, false)));
        assertEquals(new NpcLevelLimit(30, 30, 0, 20), NpcLevelCaps.limitFor(fleet("elite_guard", false, false)));
        assertEquals(NpcLevelLimit.of(15), NpcLevelCaps.limitFor(fleet("scaled_raiders", false, false)));
        assertEquals(NpcLevelLimit.of(25), NpcLevelCaps.limitFor(fleet("hegemony", false, false)));

        lunaSettingsMock.when(() -> LunaSettings.getString("exiledSector", NpcTreeConfig.LEVEL_MODE_FIELD_ID))
                .thenReturn(NpcTreeConfig.FIXED_RANGE);
        assertEquals(new NpcLevelLimit(25, 25, 0, 13), NpcLevelCaps.limitFor(fleet("hegemony", false, false)));
        assertEquals(NpcLevelLimit.of(15), NpcLevelCaps.limitFor(fleet("scaled_raiders", false, false)));
        lunaSettingsMock.when(() -> LunaSettings.getInt("exiledSector", NpcTreeConfig.FIXED_MIN_PERCENT_FIELD_ID)).thenReturn(100);
        assertEquals(new NpcLevelLimit(25, 25, 0, 25), NpcLevelCaps.limitFor(fleet("hegemony", false, false)));
    }

    @Test
    void aMinimumAboveTheCapIsClampedAndABountyRaisesOnlyTheTopOfTheRange() throws Exception {
        NpcLevelCaps.register(new JSONArray("[{\"id\": \"pirate\", \"category\": \"pirate\", \"min\": \"5\", \"mode\": \"fixed\"},"
                + "{\"id\": \"backwards\", \"category\": \"\", \"cap\": \"10\", \"min\": \"40\", \"mode\": \"fixed\"}]"));
        CampaignFleetAPI pirateBounty = fleet("pirates", true, false);
        when(pirateBounty.hasTag(NpcBounties.MAGICLIB_TARGET_TAG)).thenReturn(true);

        assertEquals(new NpcLevelLimit(10, 10, 0, 10), NpcLevelCaps.limitFor(fleet("backwards", false, false)));
        assertEquals(new NpcLevelLimit(25, 28, 3, 5), NpcLevelCaps.limitFor(pirateBounty));
    }

    @Test
    void aFixedLimitRollsEveryLevelFromItsMinimumToItsCap() {
        NpcLevelLimit limit = NpcLevelLimit.of(12).fixedFrom(10);
        Set<Integer> rolled = new TreeSet<>();
        Random random = new Random(1);
        for (int roll = 0; roll < 200; roll++) {
            rolled.add(limit.rollFixed(random));
        }

        assertEquals(Set.of(10, 11, 12), rolled);
        assertEquals(new NpcLevelLimit(12, 12, 0, 0), NpcLevelLimit.of(12).fixedFrom(-3));
    }

    @Test
    void rowsWithAnUnknownModeKeepTreeOrMinAreIgnored() throws Exception {
        NpcLevelCaps.register(new JSONArray("[{\"id\": \"bad_mode\", \"category\": \"\", \"cap\": \"5\", \"mode\": \"static\"},"
                + "{\"id\": \"bad_keep\", \"category\": \"\", \"cap\": \"5\", \"keep_tree\": \"yes\"},"
                + "{\"id\": \"bad_min\", \"category\": \"\", \"cap\": \"5\", \"min\": \"some\"}]"));

        assertEquals(25, NpcLevelCaps.capFor("bad_mode", false, false));
        assertEquals(25, NpcLevelCaps.capFor("bad_keep", false, false));
        assertEquals(25, NpcLevelCaps.capFor("bad_min", false, false));
    }

    @Test
    void keepTreeComesFromTheFactionOrCategoryRowAndIsCarriedOnTheVariant() throws Exception {
        NpcLevelCaps.register(new JSONArray("[{\"id\": \"pirate\", \"category\": \"pirate\", \"keep_tree\": \"wipe\"},"
                + "{\"id\": \"salvagers\", \"category\": \"pirate\", \"keep_tree\": \"keep\"}]"));
        List<String> tags = new ArrayList<>();
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.getTags()).thenAnswer(invocation -> new ArrayList<>(tags));
        doAnswer(invocation -> tags.add(invocation.getArgument(0))).when(variant).addTag(anyString());
        doAnswer(invocation -> tags.remove((String) invocation.getArgument(0))).when(variant).removeTag(anyString());

        assertEquals(Boolean.FALSE, NpcLevelCaps.keepTreeSetting(fleet("pirates", true, false)));
        assertEquals(Boolean.TRUE, NpcLevelCaps.keepTreeSetting(fleet("salvagers", true, false)));
        assertNull(NpcLevelCaps.keepTreeSetting(fleet("hegemony", false, false)));

        assertTrue(NpcLevelCaps.keepsTreeOnRecovery(variant));
        NpcLevelCaps.tagRecovery(variant, false);
        assertFalse(NpcLevelCaps.keepsTreeOnRecovery(variant));
        NpcLevelCaps.tagRecovery(variant, true);
        assertEquals(List.of(NpcLevelCaps.RECOVERY_TAG_PREFIX + "keep"), tags);
        NpcLevelCaps.tagRecovery(variant, null);
        assertEquals(List.of(), tags);
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
    void bountyCapsRiseFromTheCategoryCapToTwentyFiveWithDifficultyAndTheFlagshipGetsThreeMore() {
        assertEquals(new NpcLevelLimit(15, 18, 3), NpcLevelCaps.bountyLimit(15, 0f));
        assertEquals(new NpcLevelLimit(20, 23, 3), NpcLevelCaps.bountyLimit(15, 0.5f));
        assertEquals(new NpcLevelLimit(25, 28, 3), NpcLevelCaps.bountyLimit(15, 1f));
        assertEquals(new NpcLevelLimit(25, 28, 3), NpcLevelCaps.bountyLimit(25, 0f));
        assertEquals(new NpcLevelLimit(35, 38, 3), NpcLevelCaps.bountyLimit(35, 1f));
    }

    @Test
    void bountyCapsStayWithinTheMaxLevel() {
        lunaSettingsMock.when(() -> LunaSettings.getInt("exiledSector", ShipLevelConfig.MAX_LEVEL_FIELD_ID)).thenReturn(20);

        assertEquals(new NpcLevelLimit(20, 20, 3), NpcLevelCaps.bountyLimit(15, 1f));
    }

    @Test
    void theFlagshipBonusAddsToTheRollAndItsOwnCap() {
        NpcLevelLimit bountyLimit = new NpcLevelLimit(15, 18, 3);

        assertEquals(10, bountyLimit.nodeCount(10, 59, false));
        assertEquals(13, bountyLimit.nodeCount(10, 59, true));
        assertEquals(15, bountyLimit.nodeCount(30, 59, false));
        assertEquals(18, bountyLimit.nodeCount(30, 59, true));
        assertEquals(16, bountyLimit.nodeCount(30, 16, true));
    }

    @Test
    void aBountyFleetIsCappedByItsDifficulty() {
        CampaignFleetAPI pirateBounty = fleet("pirates", true, false);
        when(pirateBounty.hasTag(NpcBounties.MAGICLIB_TARGET_TAG)).thenReturn(true);

        assertEquals(new NpcLevelLimit(25, 28, 3), NpcLevelCaps.limitFor(pirateBounty));
        assertEquals(NpcLevelLimit.of(15), NpcLevelCaps.limitFor(fleet("pirates", true, false)));
    }

    @Test
    void aPersonBountyCountsAsPiratesEvenWhileItsFleetIsStillNeutralAndItsFlagshipFallsBackToTheFlagshipCap() {
        CampaignFleetAPI neutralBounty = fleet("neutral", false, false);
        PersonBountyIntel personBounty = mock(PersonBountyIntel.class);
        when(personBounty.getLevel()).thenReturn(0);
        when(neutralBounty.getEventListeners()).thenReturn(List.of(personBounty));
        FleetMemberAPI flagship = mock(FleetMemberAPI.class);
        when(flagship.isFlagship()).thenReturn(true);
        FleetDataAPI fleetData = mock(FleetDataAPI.class);
        when(fleetData.getFleet()).thenReturn(neutralBounty);
        when(flagship.getFleetData()).thenReturn(fleetData);

        assertEquals(new NpcLevelLimit(15, 18, 3), NpcLevelCaps.limitFor(neutralBounty));
        assertEquals(18, NpcLevelCaps.capFor(flagship));
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
