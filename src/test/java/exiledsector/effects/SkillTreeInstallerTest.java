package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI;
import exiledsector.skills.SkillTree;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SkillTreeInstallerTest {

    private MockedStatic<Global> globalMock;
    private SectorAPI sector;
    private FleetDataAPI fleetData;

    @BeforeEach
    void setUp() {
        Map<String, Object> persistentData = new HashMap<>();
        sector = mock(SectorAPI.class);
        when(sector.getPersistentData()).thenReturn(persistentData);

        CampaignFleetAPI playerFleet = mock(CampaignFleetAPI.class);
        fleetData = mock(FleetDataAPI.class);
        when(playerFleet.getFleetData()).thenReturn(fleetData);
        MutableFleetStatsAPI fleetStats = mock(MutableFleetStatsAPI.class);
        when(playerFleet.getStats()).thenReturn(fleetStats);
        when(fleetStats.getDetectedRangeMod()).thenReturn(mock(StatBonus.class));
        when(playerFleet.isTransponderOn()).thenReturn(true);

        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
        when(sector.getPlayerFleet()).thenReturn(playerFleet);
        when(fleetData.getMembersListCopy()).thenReturn(List.of());

        SkillTree.clearNodes();
        ShipTreeSync.takePlayerFleetSyncRequest();
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
        SkillTree.clearNodes();
    }

    @Test
    void isNeverDone() {
        assertFalse(new SkillTreeInstaller().isDone());
    }

    @Test
    void doesNotRunWhilePaused() {
        assertFalse(new SkillTreeInstaller().runWhilePaused());
    }

    @Test
    void syncsThePlayerFleetOnTheFirstFrameAfterLoad() {
        new SkillTreeInstaller().advance(0.01f);

        verify(fleetData, times(1)).getMembersListCopy();
    }

    @Test
    void requestsOneFleetSyncAfterLoadSoStatsAreRebuiltWithTheLoadedTrees() {
        FleetMemberAPI member = ShipTreeSyncTest.mockMember("ship-a", true);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(member));
        SkillTreeInstaller installer = new SkillTreeInstaller();

        installer.advance(0.01f);
        installer.advance(1f);

        verify(fleetData, times(1)).setSyncNeeded();
    }

    @Test
    void doesNotPollTheFleetAgainUntilItSyncs() {
        SkillTreeInstaller installer = new SkillTreeInstaller();
        installer.advance(0.01f);

        installer.advance(5f);
        installer.advance(5f);
        verify(fleetData, times(1)).getMembersListCopy();

        ShipTreeSync.requestPlayerFleetSync();
        installer.advance(0.01f);
        installer.advance(0.01f);

        verify(fleetData, times(2)).getMembersListCopy();
    }

    private static CampaignFleetAPI syncingFleet(boolean isPlayerFleet) {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        FleetDataAPI syncingFleetData = mock(FleetDataAPI.class);
        when(syncingFleetData.getMembersListCopy()).thenReturn(List.of());
        when(fleet.getFleetData()).thenReturn(syncingFleetData);
        MutableFleetStatsAPI fleetStats = mock(MutableFleetStatsAPI.class);
        when(fleetStats.getSensorStrengthMod()).thenReturn(mock(StatBonus.class));
        when(fleet.getStats()).thenReturn(fleetStats);
        when(fleet.isPlayerFleet()).thenReturn(isPlayerFleet);
        return fleet;
    }

    @Test
    void thePlayerFleetsSyncAsksForOnePass() {
        new SkillTreeHullMod().onFleetSync(syncingFleet(true));

        assertTrue(ShipTreeSync.takePlayerFleetSyncRequest());
    }

    @Test
    void anotherFleetsSyncNeverAsksForAPlayerFleetPass() {
        new SkillTreeHullMod().onFleetSync(syncingFleet(false));

        assertFalse(ShipTreeSync.takePlayerFleetSyncRequest());
    }

    @Test
    void installsItsHullModOnANewShipOnceThePlayerFleetSyncs() {
        FleetMemberAPI veteran = ShipTreeSyncTest.mockMember("veteran", true);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(veteran));
        SkillTreeInstaller installer = new SkillTreeInstaller();
        installer.advance(0.01f);

        FleetMemberAPI newcomer = ShipTreeSyncTest.mockMember("newcomer", false);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(veteran, newcomer));
        ShipTreeSync.requestPlayerFleetSync();
        installer.advance(0.01f);

        verify(newcomer.getVariant()).addPermaMod(SkillTreeHullMod.ID);
        verify(newcomer).setStatUpdateNeeded(true);
        verify(fleetData, times(2)).setSyncNeeded();
    }

    @Test
    void aModThatKeepsUndoingOurHullModOrderIsAnsweredAtMostOnceASecond() {
        FleetMemberAPI contested = ShipTreeSyncTest.mockMember("contested", true);
        when(contested.getVariant().getHullMods()).thenReturn(List.of(SkillTreeHullMod.ID, "other_mods_hullmod"));
        when(fleetData.getMembersListCopy()).thenReturn(List.of(contested));
        SkillTreeInstaller installer = new SkillTreeInstaller();
        installer.advance(0.01f);

        ShipTreeSync.requestPlayerFleetSync();
        installer.advance(0.5f);
        verify(fleetData, times(1)).getMembersListCopy();

        installer.advance(0.5f);
        verify(fleetData, times(2)).getMembersListCopy();
    }

    @Test
    void doesNothingWhenThereIsNoPlayerFleet() {
        when(sector.getPlayerFleet()).thenReturn(null);

        new SkillTreeInstaller().advance(0.01f);

        verify(fleetData, never()).getMembersListCopy();
    }
}
