package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.CollisionGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CrewStealTest {

    private static final int PLAYER = 0;
    private static final int ENEMY = 1;
    private static final int NEUTRAL = 100;

    private final List<Object> gridShips = new ArrayList<>();
    private MockedStatic<Global> globalMock;
    private CombatEngineAPI engine;

    @BeforeEach
    void setUp() {
        engine = mock(CombatEngineAPI.class);
        when(engine.getCustomData()).thenReturn(new HashMap<>());
        when(engine.isInCampaign()).thenReturn(true);
        CollisionGridAPI shipGrid = mock(CollisionGridAPI.class);
        when(engine.getShipGrid()).thenReturn(shipGrid);
        when(shipGrid.getCheckIterator(any(), anyFloat(), anyFloat())).thenAnswer(invocation -> new ArrayList<>(gridShips).iterator());
        CargoAPI cargo = mock(CargoAPI.class);
        when(cargo.getCrew()).thenReturn(2);
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        when(fleet.getCargo()).thenReturn(cargo);
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getPlayerFleet()).thenReturn(fleet);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getCombatEngine).thenReturn(engine);
        globalMock.when(Global::getSector).thenReturn(sector);
        FleetCrewLedger.drain();
    }

    @AfterEach
    void tearDown() {
        FleetCrewLedger.drain();
        globalMock.close();
    }

    private static ShipAPI ship(float x, int owner) {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        when(stats.getDynamic()).thenReturn(mock(DynamicStatsAPI.class));
        when(ship.getMutableStats()).thenReturn(stats);
        when(ship.getLocation()).thenReturn(new Vector2f(x, 0f));
        when(ship.getOwner()).thenReturn(owner);
        when(ship.isAlive()).thenReturn(true);
        when(ship.getHitpoints()).thenReturn(1000f);
        return ship;
    }

    private static ShipAPI thief(float x, float percent) {
        ShipAPI thief = ship(x, PLAYER);
        DynamicStatsAPI dynamic = thief.getMutableStats().getDynamic();
        when(dynamic.getValue(CrewStealListener.RANGE_KEY, 0f)).thenReturn(1500f);
        when(dynamic.getValue(CrewStealListener.SKELETON_CREW_PERCENT_KEY, 0f)).thenReturn(percent);
        return thief;
    }

    private static ShipAPI enemy(float x, float skeletonCrew) {
        ShipAPI enemy = ship(x, ENEMY);
        ShipHullSpecAPI hull = mock(ShipHullSpecAPI.class);
        when(hull.getMinCrew()).thenReturn(skeletonCrew);
        when(enemy.getHullSpec()).thenReturn(hull);
        StatBonus minCrew = mock(StatBonus.class);
        when(minCrew.computeEffective(anyFloat())).thenAnswer(invocation -> invocation.getArgument(0));
        when(enemy.getMutableStats().getMinCrewMod()).thenReturn(minCrew);
        return enemy;
    }

    private static void destroy(ShipAPI ship) {
        when(ship.isAlive()).thenReturn(false);
        when(ship.isHulk()).thenReturn(true);
        when(ship.getHitpoints()).thenReturn(0f);
        when(ship.getOwner()).thenReturn(NEUTRAL);
    }

    private void fight(List<CrewStealListener> thieves, Runnable between) {
        thieves.forEach(listener -> listener.advance(0.3f));
        between.run();
        thieves.forEach(listener -> listener.advance(0.3f));
    }

    @Test
    void anEnemyDestroyedInRangeYieldsTenPercentOfItsSkeletonCrewRoundedUp() {
        ShipAPI thief = thief(0f, 10f);
        ShipAPI enemy = enemy(800f, 15f);
        gridShips.addAll(List.of(thief, enemy));

        fight(List.of(new CrewStealListener(thief)), () -> destroy(enemy));

        assertEquals(new FleetCrewLedger.CrewChange(2, 0), FleetCrewLedger.drain());
    }

    @Test
    void stolenCrewRefillsTheLiveMunitionsPoolMidCombatAndTheBattleSettlesOnTheNet() {
        ShipAPI thief = thief(0f, 10f);
        ShipAPI enemy = enemy(800f, 30f);
        gridShips.addAll(List.of(thief, enemy));
        FleetCrewLedger ledger = FleetCrewLedger.forCurrentCombat();
        ledger.sacrifice();
        ledger.sacrifice();
        assertFalse(ledger.hasCrew());

        fight(List.of(new CrewStealListener(thief)), () -> destroy(enemy));

        assertTrue(ledger.hasCrew());
        ledger.sacrifice();
        ledger.sacrifice();
        ledger.sacrifice();
        assertFalse(ledger.hasCrew());
        FleetCrewLedger.CrewChange change = FleetCrewLedger.drain();
        assertEquals(new FleetCrewLedger.CrewChange(3, 5), change);
        assertEquals(-2, change.net());
    }

    @Test
    void aWreckSeenByTwoThievesCountsOnceAtTheHighestShare() {
        ShipAPI small = thief(0f, 10f);
        ShipAPI large = thief(200f, 20f);
        ShipAPI enemy = enemy(800f, 30f);
        gridShips.addAll(List.of(small, large, enemy));

        fight(List.of(new CrewStealListener(small), new CrewStealListener(large)), () -> destroy(enemy));

        assertEquals(6, FleetCrewLedger.drain().stolen());
    }

    @Test
    void retreatingShipsFightersDronesModulesAndAlliesYieldNothing() {
        ShipAPI thief = thief(0f, 10f);
        ShipAPI retreating = enemy(300f, 30f);
        ShipAPI fighter = enemy(400f, 30f);
        when(fighter.isFighter()).thenReturn(true);
        ShipAPI drone = enemy(500f, 30f);
        when(drone.isDrone()).thenReturn(true);
        ShipAPI module = enemy(600f, 30f);
        when(module.isStationModule()).thenReturn(true);
        ShipAPI friend = enemy(700f, 30f);
        when(friend.getOwner()).thenReturn(PLAYER);
        gridShips.addAll(List.of(thief, retreating, fighter, drone, module, friend));

        fight(List.of(new CrewStealListener(thief)), () -> {
            when(retreating.isAlive()).thenReturn(false);
            destroy(fighter);
            destroy(drone);
            destroy(module);
            destroy(friend);
        });

        assertEquals(0, FleetCrewLedger.drain().stolen());
    }

    @Test
    void npcAndAlliedShipsNeverStealForThePlayersFleet() {
        ShipAPI npc = thief(0f, 10f);
        when(npc.getOwner()).thenReturn(ENEMY);
        ShipAPI ally = thief(100f, 10f);
        when(ally.isAlly()).thenReturn(true);
        ShipAPI victim = enemy(800f, 30f);
        when(victim.getOwner()).thenReturn(PLAYER);
        ShipAPI enemy = enemy(900f, 30f);
        gridShips.addAll(List.of(npc, ally, victim, enemy));

        fight(List.of(new CrewStealListener(npc), new CrewStealListener(ally)), () -> {
            destroy(victim);
            destroy(enemy);
        });

        assertEquals(0, FleetCrewLedger.drain().stolen());
    }

    @Test
    void simulatorBattlesNeverReachTheFleet() {
        when(engine.isSimulation()).thenReturn(true);
        ShipAPI thief = thief(0f, 10f);
        ShipAPI enemy = enemy(800f, 30f);
        gridShips.addAll(List.of(thief, enemy));

        fight(List.of(new CrewStealListener(thief)), () -> destroy(enemy));

        assertEquals(new FleetCrewLedger.CrewChange(0, 0), FleetCrewLedger.drain());
    }

    @Test
    void withoutAPlayerFleetTheCrewPoolNeverRunsOutEvenAfterSacrificesAndCredits() {
        when(Global.getSector().getPlayerFleet()).thenReturn(null);
        FleetCrewLedger ledger = FleetCrewLedger.forCurrentCombat();

        ledger.sacrifice();
        ledger.credit(new Object(), 30f);
        ledger.sacrifice();

        assertTrue(ledger.hasCrew());
        assertEquals(new FleetCrewLedger.CrewChange(0, 0), FleetCrewLedger.drain());
    }

    @Test
    void roundingUpIgnoresFloatingPointNoise() {
        assertEquals(0, FleetCrewLedger.roundUp(0f));
        assertEquals(1, FleetCrewLedger.roundUp(0.2f));
        assertEquals(5, FleetCrewLedger.roundUp(5.00001f));
        assertEquals(6, FleetCrewLedger.roundUp(5.1f));
    }
}
