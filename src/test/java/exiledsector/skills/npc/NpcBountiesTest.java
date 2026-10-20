package exiledsector.skills.npc;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.intel.PersonBountyIntel;
import com.fs.starfarer.api.impl.campaign.missions.cb.BaseCustomBounty;
import com.fs.starfarer.api.impl.campaign.missions.cb.CustomBountyCreator.CustomBountyData;
import com.fs.starfarer.api.impl.campaign.missions.hub.MissionFleetAutoDespawn;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NpcBountiesTest {

    private static final float TOLERANCE = 0.0001f;

    private static final class OfferedBounty extends BaseCustomBounty {

        OfferedBounty(CustomBountyData low, CustomBountyData normal, CustomBountyData high) {
            dataLow = low;
            dataNormal = normal;
            dataHigh = high;
        }
    }

    private static final class AcceptedBounty extends BaseCustomBounty {

        AcceptedBounty(CustomBountyData accepted) {
            data = accepted;
        }
    }

    private MockedStatic<Global> globalMock;

    @BeforeEach
    void setUp() {
        FactoryAPI factory = mock(FactoryAPI.class);
        when(factory.createCargo(anyBoolean())).thenReturn(mock(CargoAPI.class));
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getFactory).thenReturn(factory);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    private static CampaignFleetAPI fleet(List<FleetEventListener> listeners, List<EveryFrameScript> scripts) {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        when(fleet.getEventListeners()).thenReturn(listeners);
        when(fleet.getScripts()).thenReturn(scripts);
        when(fleet.getMemoryWithoutUpdate()).thenReturn(mock(MemoryAPI.class));
        return fleet;
    }

    private static CustomBountyData bountyData(CampaignFleetAPI fleet, int difficulty) {
        CustomBountyData bountyData = new CustomBountyData();
        bountyData.fleet = fleet;
        bountyData.difficulty = difficulty;
        return bountyData;
    }

    @Test
    void personBountyLevelsReachFullDifficultyAtTen() {
        assertEquals(0f, NpcBounties.personBountyDifficulty(0), TOLERANCE);
        assertEquals(0.5f, NpcBounties.personBountyDifficulty(5), TOLERANCE);
        assertEquals(1f, NpcBounties.personBountyDifficulty(14), TOLERANCE);
        assertEquals(0f, NpcBounties.customBountyDifficulty(0), TOLERANCE);
        assertEquals(0.7f, NpcBounties.customBountyDifficulty(7), TOLERANCE);
        assertEquals(1f, NpcBounties.customBountyDifficulty(10), TOLERANCE);
    }

    @Test
    void aPersonBountyIsReadFromItsFleetListener() {
        PersonBountyIntel personBounty = mock(PersonBountyIntel.class);
        when(personBounty.getLevel()).thenReturn(4);

        assertEquals(0.4f, NpcBounties.find(fleet(List.of(personBounty), List.of())).difficulty(), TOLERANCE);
    }

    @Test
    void anAcceptedCustomBountyIsReadFromItsFleetListener() {
        List<FleetEventListener> listeners = new ArrayList<>();
        CampaignFleetAPI fleet = fleet(listeners, List.of());
        listeners.add(new AcceptedBounty(bountyData(fleet, 8)));

        assertEquals(0.8f, NpcBounties.find(fleet).difficulty(), TOLERANCE);
    }

    @Test
    void anOfferedCustomBountyIsFoundThroughItsDespawnScriptAndPicksThisFleetsDifficulty() {
        List<EveryFrameScript> scripts = new ArrayList<>();
        CampaignFleetAPI fleet = fleet(List.of(), scripts);
        CampaignFleetAPI otherOption = fleet(List.of(), List.of());
        OfferedBounty offered = new OfferedBounty(bountyData(otherOption, 2), bountyData(fleet, 6), bountyData(otherOption, 9));
        scripts.add(new MissionFleetAutoDespawn(offered, fleet));

        assertEquals(0.6f, NpcBounties.find(fleet).difficulty(), TOLERANCE);
    }

    @Test
    void magicLibBountiesCountAsTheHardestAndOtherFleetsAreNotBounties() {
        CampaignFleetAPI tagged = fleet(null, null);
        when(tagged.hasTag(NpcBounties.MAGICLIB_TARGET_TAG)).thenReturn(true);
        CampaignFleetAPI accepted = fleet(List.of(), List.of());
        when(accepted.getMemoryWithoutUpdate().getBoolean(NpcBounties.MAGICLIB_TARGET_MEMORY_KEY)).thenReturn(true);
        CampaignFleetAPI unrelatedDespawn = fleet(List.of(), List.of(new MissionFleetAutoDespawn(null, null)));

        assertEquals(1f, NpcBounties.find(tagged).difficulty(), TOLERANCE);
        assertEquals(1f, NpcBounties.find(accepted).difficulty(), TOLERANCE);
        assertNull(NpcBounties.find(unrelatedDespawn));
        assertNull(NpcBounties.find(fleet(List.of(), List.of())));
        assertNull(NpcBounties.find(null));
    }
}
