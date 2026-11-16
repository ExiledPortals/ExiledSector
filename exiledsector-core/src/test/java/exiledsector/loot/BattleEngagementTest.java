package exiledsector.loot;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BattleEngagementTest {

    @Test
    void anUnfoughtEngagementCountsEveryShipOfTheFleetAsLostAtBaseDifficulty() {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        FleetMemberAPI destroyer = mock(FleetMemberAPI.class);
        FleetMemberAPI cruiser = mock(FleetMemberAPI.class);
        when(destroyer.getDeploymentPointsCost()).thenReturn(10f);
        when(cruiser.getDeploymentPointsCost()).thenReturn(18f);

        BattleEngagement engagement = BattleEngagement.unfought(fleet, List.of(destroyer, cruiser));

        assertEquals(28f, engagement.defeatedDeploymentPoints(), 0.001f);
        assertEquals(1f, engagement.difficultyMultiplier(), 0.001f);
        assertEquals(Map.of(fleet, List.of(destroyer, cruiser)), engagement.lostEnemiesBySourceFleet());
        assertSame(fleet, engagement.sourceFleetOf(cruiser));
    }

    @Test
    void shipsLostByAFleetFlyingARunOnlyCopyCountForTheRealFactionBehindIt() {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        FactionAPI copy = mock(FactionAPI.class);
        when(copy.getId()).thenReturn("exiledSector_skirmish_attacker");
        when(fleet.getFaction()).thenReturn(copy);
        MemoryAPI memory = mock(MemoryAPI.class);
        when(fleet.getMemoryWithoutUpdate()).thenReturn(memory);
        FleetMemberAPI frigate = mock(FleetMemberAPI.class);
        when(frigate.getDeploymentPointsCost()).thenReturn(5f);
        BattleEngagement engagement = BattleEngagement.unfought(fleet, List.of(frigate));

        assertEquals(Map.of("exiledSector_skirmish_attacker", 5f), engagement.deploymentPointsLostByFaction());

        when(memory.getString(FleetFactions.REAL_FACTION_MEMORY_KEY)).thenReturn("hegemony");
        assertEquals(Map.of("hegemony", 5f), engagement.deploymentPointsLostByFaction());
    }

    private static FleetMemberAPI member(String id, float deploymentPoints) {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn(id);
        when(member.getDeploymentPointsCost()).thenReturn(deploymentPoints);
        return member;
    }

    private static CampaignFleetAPI fleetOf(String factionId, MemoryAPI memory) {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        FactionAPI faction = mock(FactionAPI.class);
        when(faction.getId()).thenReturn(factionId);
        when(fleet.getFaction()).thenReturn(faction);
        when(fleet.getMemoryWithoutUpdate()).thenReturn(memory);
        return fleet;
    }

    private static MemoryAPI storingMemory() {
        Map<String, Object> stored = new HashMap<>();
        MemoryAPI memory = mock(MemoryAPI.class);
        doAnswer(invocation -> stored.put(invocation.getArgument(0), invocation.getArgument(1))).when(memory).set(anyString(), any());
        doAnswer(invocation -> stored.remove((String) invocation.getArgument(0))).when(memory).unset(anyString());
        when(memory.get(anyString())).thenAnswer(invocation -> stored.get((String) invocation.getArgument(0)));
        return memory;
    }

    @Test
    void aShipRecordedWithItsOwnFactionCountsForThatFactionAndTheRestForTheFleets() {
        CampaignFleetAPI fleet = fleetOf("exiledSector_dormant", storingMemory());
        FleetMemberAPI remnant = member("remnant_ship", 20f);
        FleetMemberAPI threat = member("threat_ship", 12f);
        FleetMemberAPI plain = member("plain_ship", 8f);
        FleetMemberAPI otherPlain = member("other_plain_ship", 4f);
        FleetFactions.recordMemberFactions(fleet, Map.of(remnant, "remnant", threat, "threat"));
        BattleEngagement engagement = BattleEngagement.unfought(fleet, List.of(remnant, threat, plain, otherPlain));

        assertEquals(Map.of("remnant", 20f, "threat", 12f, "exiledSector_dormant", 12f), engagement.deploymentPointsLostByFaction());
        assertEquals(Map.of("exiledSector_dormant", 44f), engagement.deploymentPointsLostByFleetFaction());
        assertEquals("remnant", FleetFactions.factionIdOf(remnant, fleet));
        assertEquals("exiledSector_dormant", FleetFactions.factionIdOf(plain, fleet));
    }

    @Test
    void recordingNoShipsClearsTheRecordAndOddRecordsFallBackToTheFleetsFaction() {
        MemoryAPI memory = storingMemory();
        CampaignFleetAPI fleet = fleetOf("exiledSector_dormant", memory);
        FleetMemberAPI ship = member("ship", 10f);
        FleetFactions.recordMemberFactions(fleet, Map.of(ship, "dweller"));
        assertEquals("dweller", FleetFactions.memberFactionIdOf(ship, fleet));

        FleetFactions.recordMemberFactions(fleet, Map.of());
        assertNull(memory.get(FleetFactions.MEMBER_FACTIONS_MEMORY_KEY));
        assertEquals("exiledSector_dormant", FleetFactions.factionIdOf(ship, fleet));

        memory.set(FleetFactions.MEMBER_FACTIONS_MEMORY_KEY, "not a map");
        assertEquals("exiledSector_dormant", FleetFactions.factionIdOf(ship, fleet));
        memory.set(FleetFactions.MEMBER_FACTIONS_MEMORY_KEY, Map.of("ship", ""));
        assertEquals("exiledSector_dormant", FleetFactions.factionIdOf(ship, fleet));
        assertNull(FleetFactions.memberFactionIdOf(null, fleet));
        assertNull(FleetFactions.memberFactionIdOf(ship, null));
    }

    @Test
    void aRunOnlyCopyFleetsRecordedShipsStillCountForTheirOwnFaction() {
        MemoryAPI memory = storingMemory();
        CampaignFleetAPI fleet = fleetOf("exiledSector_skirmish_attacker", memory);
        when(memory.getString(FleetFactions.REAL_FACTION_MEMORY_KEY)).thenReturn("hegemony");
        FleetMemberAPI borrowed = member("borrowed", 15f);
        FleetMemberAPI own = member("own", 5f);
        FleetFactions.recordMemberFactions(fleet, Map.of(borrowed, "remnant"));

        assertEquals(Map.of("remnant", 15f, "hegemony", 5f), BattleEngagement.unfought(fleet, List.of(borrowed, own)).deploymentPointsLostByFaction());
    }
}
