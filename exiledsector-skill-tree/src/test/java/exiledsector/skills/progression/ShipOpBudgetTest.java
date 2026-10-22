package exiledsector.skills.progression;

import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShipOpBudgetTest {

    private FleetMemberAPI member;
    private ShipVariantAPI variant;
    private MutableCharacterStatsAPI commanderStats;
    private MutableCharacterStatsAPI captainStats;

    private static PersonAPI personWith(MutableCharacterStatsAPI stats) {
        PersonAPI person = mock(PersonAPI.class);
        when(person.getStats()).thenReturn(stats);
        return person;
    }

    @BeforeEach
    void setUp() {
        member = mock(FleetMemberAPI.class);
        variant = mock(ShipVariantAPI.class);
        ShipHullSpecAPI hullSpec = mock(ShipHullSpecAPI.class);
        commanderStats = mock(MutableCharacterStatsAPI.class);
        captainStats = mock(MutableCharacterStatsAPI.class);
        when(member.getHullSpec()).thenReturn(hullSpec);
        when(hullSpec.getOrdnancePoints(any())).thenReturn(100);
        when(hullSpec.getOrdnancePoints(commanderStats)).thenReturn(110);
        when(variant.computeOPCost(any())).thenReturn(80);
        when(variant.computeOPCost(commanderStats)).thenReturn(74);
        PersonAPI captain = personWith(captainStats);
        when(member.getCaptain()).thenReturn(captain);
    }

    @Test
    void theFleetCommandersSkillsSetTheShipsOrdnancePointsLikeTheRefitScreenDoes() {
        PersonAPI commander = personWith(commanderStats);
        when(member.getFleetCommanderForStats()).thenReturn(commander);

        ShipOpBudget budget = ShipOpBudget.of(member, variant);

        assertEquals(110, budget.totalOp);
        assertEquals(74, budget.usedOp);
    }

    @Test
    void theFleetCommanderIsUsedWhenThereIsNoCommanderForStats() {
        PersonAPI commander = personWith(commanderStats);
        when(member.getFleetCommander()).thenReturn(commander);

        assertEquals(110, ShipOpBudget.of(member, variant).totalOp);
    }

    @Test
    void aShipOutsideAnyFleetFallsBackToItsCaptain() {
        when(member.getHullSpec().getOrdnancePoints(captainStats)).thenReturn(105);

        assertEquals(105, ShipOpBudget.of(member, variant).totalOp);
    }

    @Test
    void aShipWithNoCommanderOrCaptainUsesTheBaseOrdnancePoints() {
        when(member.getCaptain()).thenReturn(null);

        ShipOpBudget budget = ShipOpBudget.of(member, variant);

        assertEquals(100, budget.totalOp);
        assertEquals(80, budget.usedOp);
    }
}
