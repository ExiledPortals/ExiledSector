package exiledsector.charts.anomaly.debris;

import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.ai.CampaignFleetAIAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.FIDConfig;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import exiledlib.MockSector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DormantFleetsTest {

    private MockSector mockSector;
    private FactoryAPI factory;
    private StarSystemAPI system;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        factory = mock(FactoryAPI.class);
        mockSector.global.when(Global::getFactory).thenReturn(factory);
        system = mock(StarSystemAPI.class);
        when(system.createToken(anyFloat(), anyFloat())).thenReturn(mock(SectorEntityToken.class));
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
    }

    private static MemoryAPI memory() {
        Map<String, Object> stored = new HashMap<>();
        MemoryAPI memory = mock(MemoryAPI.class);
        doAnswer(invocation -> stored.put(invocation.getArgument(0), invocation.getArgument(1))).when(memory).set(anyString(), any());
        doAnswer(invocation -> stored.remove((String) invocation.getArgument(0))).when(memory).unset(anyString());
        when(memory.getBoolean(anyString())).thenAnswer(invocation -> Boolean.TRUE.equals(stored.get((String) invocation.getArgument(0))));
        when(memory.contains(anyString())).thenAnswer(invocation -> stored.containsKey((String) invocation.getArgument(0)));
        return memory;
    }

    private CampaignFleetAPI dormantFleet() {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        MemoryAPI memory = memory();
        memory.set(DormantFleets.DORMANT_KEY, true);
        memory.set(MemFlags.MEMORY_KEY_MAKE_ALLOW_DISENGAGE, true);
        memory.set(MemFlags.MEMORY_KEY_ALLOW_LONG_PURSUIT, true);
        when(fleet.getMemoryWithoutUpdate()).thenReturn(memory);
        when(fleet.hasTag(DormantFleets.FLEET_TAG)).thenReturn(true);
        when(fleet.getLocation()).thenReturn(new Vector2f(100f, 200f));
        when(fleet.getContainingLocation()).thenReturn(system);
        StatBonus detected = new StatBonus();
        when(fleet.getDetectedRangeMod()).thenReturn(detected);
        DormantFleets.hide(fleet);
        return fleet;
    }

    @Test
    void wakingOneFleetMakesOnlyItHostileWithoutReputationAndKeepsItAtItsField() {
        CampaignFleetAPI attacked = dormantFleet();
        CampaignFleetAPI other = dormantFleet();
        CampaignFleetAIAPI ai = mock(CampaignFleetAIAPI.class);
        when(factory.createFleetAI(attacked)).thenReturn(ai);

        assertTrue(DormantFleets.wake(attacked));

        MemoryAPI memory = attacked.getMemoryWithoutUpdate();
        assertFalse(DormantFleets.isDormant(attacked));
        assertTrue(memory.getBoolean(MemFlags.MEMORY_KEY_MAKE_HOSTILE));
        assertTrue(memory.getBoolean(MemFlags.MEMORY_KEY_NO_REP_IMPACT));
        assertFalse(memory.contains(MemFlags.MEMORY_KEY_ALLOW_LONG_PURSUIT));
        assertFalse(DormantFleets.isHidden(attacked));
        verify(attacked).setAI(ai);
        verify(attacked).addAssignment(eq(FleetAssignment.ORBIT_AGGRESSIVE), any(SectorEntityToken.class), anyFloat());
        verify(system).createToken(100f, 200f);
        assertTrue(DormantFleets.isDormant(other));
        assertTrue(DormantFleets.isHidden(other));
        verify(other, never()).setAI(any());
        assertFalse(DormantFleets.wake(attacked));
    }

    @Test
    void theDormantFactionIsResetToNeutralWithThePlayerAndEveryOtherFaction() {
        FactionAPI dormant = mock(FactionAPI.class);
        when(dormant.getId()).thenReturn(DormantFleets.FACTION_ID);
        FactionAPI pirates = mock(FactionAPI.class);
        when(pirates.getId()).thenReturn("pirates");
        FactionAPI threat = mock(FactionAPI.class);
        when(threat.getId()).thenReturn("threat");

        DormantFleets.dressFaction(dormant, List.of(dormant, pirates, threat));

        verify(dormant).setRelationship("player", 0f);
        verify(dormant).setRelationship("pirates", 0f);
        verify(dormant).setRelationship("threat", 0f);
        verify(dormant, never()).setRelationship(DormantFleets.FACTION_ID, 0f);
    }

    @Test
    void anEncounterNeverPullsInOtherFleetsOrTouchesReputation() {
        FIDConfig first = DormantEncounters.config(true);
        FIDConfig later = DormantEncounters.config(false);

        assertTrue(first.straightToEngage);
        assertFalse(later.straightToEngage);
        assertFalse(first.pullInEnemies);
        assertFalse(first.pullInStations);
        assertFalse(first.impactsEnemyReputation);
        assertFalse(first.showWarningDialogWhenNotHostile);
    }
}
