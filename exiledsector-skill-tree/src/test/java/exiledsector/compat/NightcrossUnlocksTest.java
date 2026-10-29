package exiledsector.compat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.MockSector;
import exiledsector.loot.BattleEngagement;
import exiledsector.skills.unlock.AnchorFinds;
import exiledsector.skills.unlock.RelockedGates;
import exiledsector.skills.unlock.SkillTypeUnlockStatus;
import exiledsector.skills.unlock.UnlockCondition;
import exiledsector.skills.unlock.UnlockConditionType;
import lunalib.lunaSettings.LunaSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NightcrossUnlocksTest {

    private static final List<UnlockCondition> PASCAL_GATE = List.of(UnlockCondition.anchorFound("nightcross"));
    private static final List<UnlockCondition> EXISTENTIAL_THREAT_GATE = List.of(UnlockCondition.modGate(UnlockConditionType.NIGHTCROSS_EXISTENTIAL_THREAT_DEFEATED));
    private static final List<UnlockCondition> PROTOTYPE_GATE = List.of(UnlockCondition.modGate(UnlockConditionType.NIGHTCROSS_ANCIENT_PROTOTYPE_FOUND));

    private MockedStatic<LunaSettings> lunaSettingsMock;
    private MockSector mockSector;
    private ModManagerAPI modManager;
    private SectorAPI sector;
    private MemoryAPI sectorMemory;
    private MemoryAPI playerMemory;
    private StarSystemAPI pascal;
    private CampaignFleetAPI playerFleet;

    private static MemoryAPI memory() {
        Map<String, Object> values = new HashMap<>();
        MemoryAPI memory = mock(MemoryAPI.class);
        when(memory.getBoolean(anyString())).thenAnswer(invocation -> Boolean.TRUE.equals(values.get(invocation.getArgument(0))));
        doAnswer(invocation -> values.put(invocation.getArgument(0), invocation.getArgument(1))).when(memory).set(anyString(), any());
        return memory;
    }

    @BeforeEach
    void setUp() {
        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class);
        lunaSettingsMock.when(() -> LunaSettings.getBoolean(anyString(), anyString())).thenReturn(null);
        SettingsAPI settings = mock(SettingsAPI.class);
        modManager = mock(ModManagerAPI.class);
        when(settings.getModManager()).thenReturn(modManager);
        when(modManager.isModEnabled(NightcrossCompat.MOD_ID)).thenReturn(true);

        sectorMemory = memory();
        playerMemory = memory();
        pascal = mock(StarSystemAPI.class);
        mockSector = new MockSector();
        sector = mockSector.sector;
        playerFleet = mockSector.playerFleet();
        FleetDataAPI fleetData = mock(FleetDataAPI.class);
        when(playerFleet.getFleetData()).thenReturn(fleetData);
        when(fleetData.getMembersListCopy()).thenReturn(List.of());
        when(sector.getMemoryWithoutUpdate()).thenReturn(sectorMemory);
        when(sector.getPlayerMemoryWithoutUpdate()).thenReturn(playerMemory);
        when(sector.getStarSystem(NightcrossUnlocks.PASCAL_SYSTEM_NAME)).thenReturn(pascal);

        mockSector.global.when(Global::getSettings).thenReturn(settings);
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
        lunaSettingsMock.close();
    }

    private SectorEntityToken wreck(String id, boolean stillHidden) {
        SectorEntityToken wreck = mock(SectorEntityToken.class);
        when(wreck.getId()).thenReturn(id);
        when(wreck.isDiscoverable()).thenReturn(stillHidden);
        when(sector.getEntityById(id)).thenReturn(wreck);
        return wreck;
    }

    @Test
    void theNightcrossAreaOpensOnceANightcrossAnchorIsFoundNotOnEnteringPascal() {
        when(pascal.isEnteredByPlayer()).thenReturn(true);
        assertTrue(SkillTypeUnlockStatus.isLocked(PASCAL_GATE, null));

        AnchorFinds.record(sector, "nightcross", "test");

        assertFalse(SkillTypeUnlockStatus.isLocked(PASCAL_GATE, null));
    }

    @Test
    void aSaveThatEnteredPascalBeforeAnchorsCountsAsHavingFoundOne() {
        assertFalse(NightcrossUnlocks.pascalVisitedBeforeAnchors(sector));

        when(pascal.isEnteredByPlayer()).thenReturn(true);
        assertTrue(NightcrossUnlocks.pascalVisitedBeforeAnchors(sector));

        when(pascal.isEnteredByPlayer()).thenReturn(false);
        sectorMemory.set(NightcrossUnlocks.PASCAL_VISITED_FLAG, true);
        assertTrue(NightcrossUnlocks.pascalVisitedBeforeAnchors(sector));
    }

    @Test
    void aRandomSectorWithoutPascalCountsAsVisitedLikeBefore() {
        when(sector.getStarSystem(NightcrossUnlocks.PASCAL_SYSTEM_NAME)).thenReturn(null);

        assertTrue(NightcrossUnlocks.pascalVisitedBeforeAnchors(sector));
    }

    @Test
    void theStargazerAreaOpensWhenEitherExistentialThreatFlagIsSet() {
        assertTrue(SkillTypeUnlockStatus.isLocked(EXISTENTIAL_THREAT_GATE, null));

        playerMemory.set(NightcrossUnlocks.EXISTENTIAL_THREAT_DEFEATED_PLAYER_FLAG, true);
        assertFalse(SkillTypeUnlockStatus.isLocked(EXISTENTIAL_THREAT_GATE, null));

        playerMemory.set(NightcrossUnlocks.EXISTENTIAL_THREAT_DEFEATED_PLAYER_FLAG, false);
        sectorMemory.set(NightcrossUnlocks.EXISTENTIAL_THREAT_DEFEATED_SECTOR_FLAG, true);
        assertFalse(SkillTypeUnlockStatus.isLocked(EXISTENTIAL_THREAT_GATE, null));
    }

    @Test
    void candlesGraceStaysLockedWhileThePrototypesAreStillHidden() {
        wreck("na_mare_x_wreck", true);

        assertTrue(SkillTypeUnlockStatus.isLocked(PROTOTYPE_GATE, null));
        verify(sector, never()).getEntityById(anyString());
    }

    @Test
    void discoveringAnyPrototypeWreckUnlocksCandlesGraceForGood() {
        SectorEntityToken wreck = wreck("na_sop_x_wreck", true);

        NightcrossUnlocks.recordDiscovery(sector, wreck);
        when(sector.getEntityById("na_sop_x_wreck")).thenReturn(null);

        assertFalse(SkillTypeUnlockStatus.isLocked(PROTOTYPE_GATE, null));
    }

    @Test
    void aWreckTheSensorsAlreadyFoundCountsOnLoadEvenWithoutTheDiscoveryEvent() {
        wreck("na_mare_x_wreck", false);
        assertTrue(SkillTypeUnlockStatus.isLocked(PROTOTYPE_GATE, null), "the gate check itself only reads flags");

        NightcrossUnlocks.checkSaveOnLoad(sector);

        assertFalse(SkillTypeUnlockStatus.isLocked(PROTOTYPE_GATE, null));
    }

    @Test
    void beatingAPrototypesDefendersCountsAsFindingOneButBeatingTheExistentialThreatDoesNot() {
        sectorMemory.set(NightcrossUnlocks.EXISTENTIAL_THREAT_DEFEATED_SECTOR_FLAG, true);
        playerMemory.set(NightcrossUnlocks.EXISTENTIAL_THREAT_DEFEATED_PLAYER_FLAG, true);
        assertTrue(SkillTypeUnlockStatus.isLocked(PROTOTYPE_GATE, null));

        sectorMemory.set(NightcrossUnlocks.PROTOTYPE_DEFENDERS_DEFEATED_FLAG, true);
        assertFalse(SkillTypeUnlockStatus.isLocked(PROTOTYPE_GATE, null));
    }

    @Test
    void aSaveWhosePlayerAlreadyFliesAPrototypeIsRecognisedOnLoad() {
        ShipHullSpecAPI hullSpec = mock(ShipHullSpecAPI.class);
        when(hullSpec.getHullId()).thenReturn("na_macula_proto");
        FleetMemberAPI prototype = mock(FleetMemberAPI.class);
        when(prototype.getHullSpec()).thenReturn(hullSpec);
        when(playerFleet.getFleetData().getMembersListCopy()).thenReturn(List.of(prototype));

        NightcrossUnlocks.checkSaveOnLoad(sector);

        assertFalse(SkillTypeUnlockStatus.isLocked(PROTOTYPE_GATE, null));
    }

    @Test
    void discoveringSomethingElseUnlocksNothing() {
        SectorEntityToken other = mock(SectorEntityToken.class);
        when(other.getId()).thenReturn("some_other_wreck");

        NightcrossUnlocks.recordDiscovery(sector, other);

        assertTrue(SkillTypeUnlockStatus.isLocked(PROTOTYPE_GATE, null));
    }

    @Test
    void withoutNightcrossInstalledEveryNightcrossGateIsOpen() {
        when(modManager.isModEnabled(NightcrossCompat.MOD_ID)).thenReturn(false);

        assertFalse(SkillTypeUnlockStatus.isLocked(PASCAL_GATE, null));
        assertFalse(SkillTypeUnlockStatus.isLocked(EXISTENTIAL_THREAT_GATE, null));
        assertFalse(SkillTypeUnlockStatus.isLocked(PROTOTYPE_GATE, null));
    }

    @Test
    void discoveringAPrototypeWreckReopensARelockedGate() {
        sectorMemory.set(NightcrossUnlocks.PROTOTYPE_FOUND_FLAG, true);
        RelockedGates.relock(sector, List.of(UnlockConditionType.NIGHTCROSS_ANCIENT_PROTOTYPE_FOUND));

        NightcrossUnlocks.recordDiscovery(sector, wreck("na_sop_x_wreck", true));

        assertFalse(RelockedGates.isRelocked(sector, UnlockConditionType.NIGHTCROSS_ANCIENT_PROTOTYPE_FOUND));
    }

    @Test
    void beatingTheWholeExistentialThreatReopensARelockedGate() {
        sectorMemory.set(NightcrossUnlocks.EXISTENTIAL_THREAT_DEFEATED_SECTOR_FLAG, true);
        RelockedGates.relock(sector, List.of(UnlockConditionType.NIGHTCROSS_EXISTENTIAL_THREAT_DEFEATED));
        FleetMemberAPI flagship = mock(FleetMemberAPI.class);
        FleetMemberAPI escort = mock(FleetMemberAPI.class);
        CampaignFleetAPI existentialThreat = mock(CampaignFleetAPI.class);
        MemoryAPI threatMemory = memory();
        threatMemory.set(NightcrossUnlocks.EXISTENTIAL_THREAT_MEMORY_KEY, true);
        when(existentialThreat.getMemoryWithoutUpdate()).thenReturn(threatMemory);
        FleetDataAPI threatData = mock(FleetDataAPI.class);
        when(threatData.getMembersListCopy()).thenReturn(List.of(flagship, escort));
        when(existentialThreat.getFleetData()).thenReturn(threatData);
        BattleAPI battle = mock(BattleAPI.class);
        when(battle.getSourceFleet(any())).thenReturn(existentialThreat);

        NightcrossUnlocks.recordEngagement(sector, new BattleEngagement(battle, 1f, 100f, List.of(flagship)));
        assertTrue(RelockedGates.isRelocked(sector, UnlockConditionType.NIGHTCROSS_EXISTENTIAL_THREAT_DEFEATED));

        NightcrossUnlocks.recordEngagement(sector, new BattleEngagement(battle, 1f, 100f, List.of(flagship, escort)));
        assertFalse(RelockedGates.isRelocked(sector, UnlockConditionType.NIGHTCROSS_EXISTENTIAL_THREAT_DEFEATED));
    }
}
