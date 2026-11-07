package exiledsector.skills.skilleffect.defense;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.fleet.CrewCompositionAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CrewLossHullDamageListenerTest {

    private static final float EPSILON = 1e-4f;
    private static final float MAX_HULL = 1000f;

    private final MutableStat hullDamageTakenMult = new MutableStat(1f);
    private final MutableStat crewLossMult = new MutableStat(1f);
    private ShipAPI ship;

    @BeforeEach
    void setUp() {
        ship = shipWithCrew(400f);
    }

    private ShipAPI shipWithCrew(float crew) {
        ShipAPI deployed = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        CrewCompositionAPI composition = mock(CrewCompositionAPI.class);
        when(deployed.getMutableStats()).thenReturn(stats);
        when(stats.getDynamic()).thenReturn(dynamic);
        when(dynamic.getValue(CrewLossHullDamageListener.PERCENT_PER_STEP_KEY, 0f)).thenReturn(1f);
        when(stats.getHullDamageTakenMult()).thenReturn(hullDamageTakenMult);
        when(stats.getCrewLossMult()).thenReturn(crewLossMult);
        when(deployed.getMaxHitpoints()).thenReturn(MAX_HULL);
        when(deployed.getFleetMember()).thenReturn(member);
        when(member.getCrewComposition()).thenReturn(composition);
        when(composition.getCrew()).thenReturn(crew);
        return deployed;
    }

    private static ApplyDamageResultAPI hullHit(float damage) {
        ApplyDamageResultAPI result = mock(ApplyDamageResultAPI.class);
        when(result.getDamageToHull()).thenReturn(damage);
        return result;
    }

    @Test
    void crewLostFollowsVanillasPostBattleFormulaAtItsAverageRoll() {
        assertEquals(400f * 0.25f * CrewLossHullDamageListener.AVERAGE_LOSS_ROLL, CrewLossHullDamageListener.crewLost(400f, 0.5f, 1f), EPSILON);
        assertEquals(200f * 0.25f * CrewLossHullDamageListener.AVERAGE_LOSS_ROLL, CrewLossHullDamageListener.crewLost(400f, 0.5f, 0.5f), EPSILON);
        assertEquals(400f, CrewLossHullDamageListener.crewLost(400f, 2f, 1f), EPSILON);
    }

    @Test
    void theReductionGrowsForEveryWholeStepOfCrewLostUpToTheCap() {
        assertEquals(0f, CrewLossHullDamageListener.reductionPercent(1f, 9.9f), EPSILON);
        assertEquals(3f, CrewLossHullDamageListener.reductionPercent(1f, 35f), EPSILON);
        assertEquals(CrewLossHullDamageListener.MAX_REDUCTION_PERCENT, CrewLossHullDamageListener.reductionPercent(1f, 500f), EPSILON);
        assertEquals(0f, CrewLossHullDamageListener.reductionPercent(0f, 500f), EPSILON);
    }

    @Test
    void hullHitsThisDeploymentAddUpAndReduceLaterHullDamage() {
        CrewLossHullDamageListener listener = new CrewLossHullDamageListener(ship);

        listener.reportDamageApplied(null, ship, hullHit(100f));
        assertEquals(1f, hullDamageTakenMult.getModifiedValue(), EPSILON);

        listener.reportDamageApplied(null, ship, hullHit(300f));

        assertEquals(0.96f, hullDamageTakenMult.getModifiedValue(), EPSILON);
    }

    @Test
    void damageToAnotherShipOrToArmorAloneChangesNothing() {
        CrewLossHullDamageListener listener = new CrewLossHullDamageListener(ship);

        listener.reportDamageApplied(null, mock(ShipAPI.class), hullHit(900f));
        listener.reportDamageApplied(null, ship, hullHit(0f));

        assertEquals(1f, hullDamageTakenMult.getModifiedValue(), EPSILON);
    }

    @Test
    void aStationModuleCountsItsParentsCrew() {
        ShipAPI module = shipWithCrew(0f);
        ShipAPI parent = shipWithCrew(400f);
        when(module.getParentStation()).thenReturn(parent);
        CrewLossHullDamageListener listener = new CrewLossHullDamageListener(module);

        listener.reportDamageApplied(null, module, hullHit(400f));

        assertEquals(0.96f, hullDamageTakenMult.getModifiedValue(), EPSILON);
    }

    @Test
    void crewIsCountedAtTheFirstHullHitNotAtDeployment() {
        ShipAPI module = shipWithCrew(0f);
        CrewLossHullDamageListener listener = new CrewLossHullDamageListener(module);
        when(module.getParentStation()).thenReturn(ship);

        listener.reportDamageApplied(null, module, hullHit(400f));

        assertEquals(0.96f, hullDamageTakenMult.getModifiedValue(), EPSILON);
    }

    @Test
    void aShipWithoutCrewGainsNothing() {
        ShipAPI uncrewed = shipWithCrew(0f);
        CrewLossHullDamageListener listener = new CrewLossHullDamageListener(uncrewed);

        listener.reportDamageApplied(null, uncrewed, hullHit(900f));

        assertEquals(1f, hullDamageTakenMult.getModifiedValue(), EPSILON);
    }
}
