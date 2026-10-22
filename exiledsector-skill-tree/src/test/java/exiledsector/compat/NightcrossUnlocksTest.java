package exiledsector.compat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
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

    private static final List<UnlockCondition> PASCAL_GATE = List.of(UnlockCondition.modGate(UnlockConditionType.NIGHTCROSS_PASCAL_VISITED));
    private static final List<UnlockCondition> LUNAR_FLEET_GATE = List.of(UnlockCondition.modGate(UnlockConditionType.NIGHTCROSS_LUNAR_FLEET_DEFEATED));
    private static final List<UnlockCondition> PROTOTYPE_GATE = List.of(UnlockCondition.modGate(UnlockConditionType.NIGHTCROSS_ANCIENT_PROTOTYPE_FOUND));

    private MockedStatic<LunaSettings> lunaSettingsMock;
    private MockedStatic<Global> globalMock;
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
        playerFleet = mock(CampaignFleetAPI.class);
        FleetDataAPI fleetData = mock(FleetDataAPI.class);
        when(playerFleet.getFleetData()).thenReturn(fleetData);
        when(fleetData.getMembersListCopy()).thenReturn(List.of());
        sector = mock(SectorAPI.class);
        when(sector.getMemoryWithoutUpdate()).thenReturn(sectorMemory);
        when(sector.getPlayerMemoryWithoutUpdate()).thenReturn(playerMemory);
        when(sector.getStarSystem(NightcrossUnlocks.PASCAL_SYSTEM_NAME)).thenReturn(pascal);
        when(sector.getPlayerFleet()).thenReturn(playerFleet);

        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSettings).thenReturn(settings);
        globalMock.when(Global::getSector).thenReturn(sector);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
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
    void theNightcrossAreaOpensOnceThePlayerHasEnteredPascal() {
        assertTrue(SkillTypeUnlockStatus.isLocked(PASCAL_GATE, null));

        when(pascal.isEnteredByPlayer()).thenReturn(true);

        assertFalse(SkillTypeUnlockStatus.isLocked(PASCAL_GATE, null));
        when(pascal.isEnteredByPlayer()).thenReturn(false);
        assertTrue(NightcrossUnlocks.isPascalVisited(sector), "the visit is remembered");
    }

    @Test
    void aSectorWithoutPascalLeavesTheNightcrossAreaOpen() {
        when(sector.getStarSystem(NightcrossUnlocks.PASCAL_SYSTEM_NAME)).thenReturn(null);

        assertFalse(SkillTypeUnlockStatus.isLocked(PASCAL_GATE, null));
    }

    @Test
    void pascalMissingAtLoadDoesNotRecordAVisitSoTheGateClosesOnceNightcrossCreatesIt() {
        when(sector.getStarSystem(NightcrossUnlocks.PASCAL_SYSTEM_NAME)).thenReturn(null);
        NightcrossUnlocks.checkSaveOnLoad(sector);
        assertFalse(sectorMemory.getBoolean(NightcrossUnlocks.PASCAL_VISITED_FLAG));

        when(sector.getStarSystem(NightcrossUnlocks.PASCAL_SYSTEM_NAME)).thenReturn(pascal);

        assertTrue(SkillTypeUnlockStatus.isLocked(PASCAL_GATE, null));
    }

    @Test
    void theStargazerAreaOpensWhenEitherLunarFleetFlagIsSet() {
        assertTrue(SkillTypeUnlockStatus.isLocked(LUNAR_FLEET_GATE, null));

        playerMemory.set(NightcrossUnlocks.LUNAR_FLEET_DEFEATED_PLAYER_FLAG, true);
        assertFalse(SkillTypeUnlockStatus.isLocked(LUNAR_FLEET_GATE, null));

        playerMemory.set(NightcrossUnlocks.LUNAR_FLEET_DEFEATED_PLAYER_FLAG, false);
        sectorMemory.set(NightcrossUnlocks.LUNAR_FLEET_DEFEATED_SECTOR_FLAG, true);
        assertFalse(SkillTypeUnlockStatus.isLocked(LUNAR_FLEET_GATE, null));
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
    void beatingAPrototypesDefendersCountsAsFindingOneButBeatingTheLunarFleetDoesNot() {
        sectorMemory.set(NightcrossUnlocks.LUNAR_FLEET_DEFEATED_SECTOR_FLAG, true);
        playerMemory.set(NightcrossUnlocks.LUNAR_FLEET_DEFEATED_PLAYER_FLAG, true);
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
        assertFalse(SkillTypeUnlockStatus.isLocked(LUNAR_FLEET_GATE, null));
        assertFalse(SkillTypeUnlockStatus.isLocked(PROTOTYPE_GATE, null));
    }
}
