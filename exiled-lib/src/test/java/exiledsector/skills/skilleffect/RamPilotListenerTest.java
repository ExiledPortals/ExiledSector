package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BoundsAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RamPilotListenerTest {

    private final List<ShipAPI> ships = new ArrayList<>();
    private MockedStatic<Global> globalMock;
    private CombatEngineAPI engine;

    @BeforeEach
    void setUp() {
        engine = mock(CombatEngineAPI.class);
        when(engine.getShips()).thenReturn(ships);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getCombatEngine).thenReturn(engine);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    private ShipAPI deployed(int owner, HullSize size, float x, float y) {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.getHullSize()).thenReturn(size);
        when(ship.getLocation()).thenReturn(new Vector2f(x, y));
        when(ship.getVelocity()).thenReturn(new Vector2f());
        when(ship.isAlive()).thenReturn(true);
        when(ship.getCollisionRadius()).thenReturn(50f);
        ships.add(ship);
        return ship;
    }

    private ShipAPI enemy(HullSize size, float x, float y) {
        return deployed(1, size, x, y);
    }

    private ShipAPI rammer() {
        ShipAPI rammer = deployed(0, HullSize.FRIGATE, 0f, 0f);
        when(rammer.getMaxSpeed()).thenReturn(200f);
        when(rammer.getTurnAcceleration()).thenReturn(40f);
        return rammer;
    }

    private static void costs(ShipAPI ship, float deploymentPoints) {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getDeploymentPointsCost()).thenReturn(deploymentPoints);
        when(ship.getFleetMember()).thenReturn(member);
    }

    private static BoundsAPI square(float centerX, float centerY, float halfSize) {
        Vector2f bottomLeft = new Vector2f(centerX - halfSize, centerY - halfSize);
        Vector2f bottomRight = new Vector2f(centerX + halfSize, centerY - halfSize);
        Vector2f topRight = new Vector2f(centerX + halfSize, centerY + halfSize);
        Vector2f topLeft = new Vector2f(centerX - halfSize, centerY + halfSize);
        List<BoundsAPI.SegmentAPI> segments = List.of(segment(bottomLeft, bottomRight), segment(bottomRight, topRight),
                segment(topRight, topLeft), segment(topLeft, bottomLeft));
        BoundsAPI bounds = mock(BoundsAPI.class);
        when(bounds.getSegments()).thenReturn(segments);
        return bounds;
    }

    private static BoundsAPI.SegmentAPI segment(Vector2f start, Vector2f end) {
        BoundsAPI.SegmentAPI segment = mock(BoundsAPI.SegmentAPI.class);
        when(segment.getP1()).thenReturn(start);
        when(segment.getP2()).thenReturn(end);
        return segment;
    }

    @Test
    void theNodeAttachesTheRamPilot() {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        when(ship.getMutableStats()).thenReturn(stats);
        when(stats.getDynamic()).thenReturn(mock(DynamicStatsAPI.class));

        CombatSkillEffect.RAM_BIGGEST_ENEMY.applyAfterShipCreation(ship, "mod_id", 0f);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(ship).addListener(captor.capture());
        assertInstanceOf(RamPilotListener.class, captor.getValue());
    }

    @Test
    void theBiggestHullSizeWinsOverACloserSmallerShip() {
        ShipAPI rammer = rammer();
        enemy(HullSize.CRUISER, 100f, 0f);
        ShipAPI capital = enemy(HullSize.CAPITAL_SHIP, 5000f, 0f);

        assertSame(capital, CombatQueries.biggestEnemyShip(rammer));
    }

    @Test
    void equalHullSizesAreSplitByDeploymentCostThenDistance() {
        ShipAPI rammer = rammer();
        ShipAPI cheapNear = enemy(HullSize.CRUISER, 100f, 0f);
        costs(cheapNear, 12f);
        ShipAPI costlyFar = enemy(HullSize.CRUISER, 4000f, 0f);
        costs(costlyFar, 20f);
        assertSame(costlyFar, CombatQueries.biggestEnemyShip(rammer));

        costs(cheapNear, 20f);
        assertSame(cheapNear, CombatQueries.biggestEnemyShip(rammer));
    }

    @Test
    void friendliesFightersDronesPodsAndWrecksAreNeverTargeted() {
        ShipAPI rammer = rammer();
        deployed(0, HullSize.CAPITAL_SHIP, 100f, 0f);
        when(enemy(HullSize.CAPITAL_SHIP, 200f, 0f).isHulk()).thenReturn(true);
        when(enemy(HullSize.FIGHTER, 300f, 0f).isFighter()).thenReturn(true);
        when(enemy(HullSize.CRUISER, 300f, 0f).isDrone()).thenReturn(true);
        when(enemy(HullSize.CRUISER, 300f, 0f).isShuttlePod()).thenReturn(true);
        when(enemy(HullSize.CRUISER, 300f, 0f).isAlive()).thenReturn(false);
        ShipAPI frigate = enemy(HullSize.FRIGATE, 900f, 0f);

        assertSame(frigate, CombatQueries.biggestEnemyShip(rammer));
    }

    @Test
    void stationsAreOnlyTargetedOnceNoOtherShipIsLeft() {
        ShipAPI rammer = rammer();
        ShipAPI station = enemy(HullSize.CAPITAL_SHIP, 300f, 0f);
        when(station.isStation()).thenReturn(true);
        when(enemy(HullSize.CRUISER, 200f, 0f).isStationModule()).thenReturn(true);
        ShipAPI frigate = enemy(HullSize.FRIGATE, 900f, 0f);
        assertSame(frigate, CombatQueries.biggestEnemyShip(rammer));

        when(frigate.isAlive()).thenReturn(false);
        assertSame(station, CombatQueries.biggestEnemyShip(rammer));
    }

    @Test
    void anAiShipFliesStraightAtItsTargetAndFocusesTheAiOnIt() {
        ShipAPI rammer = rammer();
        ShipwideAIFlags flags = mock(ShipwideAIFlags.class);
        when(rammer.getAIFlags()).thenReturn(flags);
        ShipAPI target = enemy(HullSize.CRUISER, 2000f, 0f);

        new RamPilotListener(rammer).advance(0.1f);

        verify(rammer).setShipTarget(target);
        verify(flags).setFlag(AIFlags.MANEUVER_TARGET, 0.5f, target);
        verify(flags).setFlag(AIFlags.DO_NOT_BACK_OFF, 0.5f);
        verify(rammer).giveCommand(ShipCommand.ACCELERATE, null, 0);
        verify(rammer, never()).giveCommand(Mockito.eq(ShipCommand.TURN_LEFT), any(), anyInt());
        verify(rammer, never()).giveCommand(Mockito.eq(ShipCommand.TURN_RIGHT), any(), anyInt());
        for (ShipCommand blocked : List.of(ShipCommand.TURN_LEFT, ShipCommand.TURN_RIGHT, ShipCommand.DECELERATE,
                ShipCommand.ACCELERATE_BACKWARDS, ShipCommand.STRAFE_LEFT, ShipCommand.STRAFE_RIGHT)) {
            verify(rammer).blockCommandForOneFrame(blocked);
        }
        verify(rammer, never()).blockCommandForOneFrame(ShipCommand.ACCELERATE);
    }

    @Test
    void aTargetBehindTheShipMakesItTurnAndBrakeInsteadOfThrustingAway() {
        ShipAPI rammer = rammer();
        enemy(HullSize.CRUISER, -2000f, 10f);

        new RamPilotListener(rammer).advance(0.1f);

        verify(rammer).giveCommand(ShipCommand.DECELERATE, null, 0);
        verify(rammer).giveCommand(ShipCommand.TURN_LEFT, null, 0);
        verify(rammer).blockCommandForOneFrame(ShipCommand.ACCELERATE);
        verify(rammer).blockCommandForOneFrame(ShipCommand.TURN_RIGHT);
        verify(rammer, never()).blockCommandForOneFrame(ShipCommand.TURN_LEFT);
    }

    @Test
    void thePlayerFlyingTheirOwnShipIsNeverOverridden() {
        ShipAPI rammer = rammer();
        enemy(HullSize.CRUISER, 2000f, 0f);
        when(engine.getPlayerShip()).thenReturn(rammer);
        when(engine.isUIAutopilotOn()).thenReturn(true);

        new RamPilotListener(rammer).advance(0.1f);

        verify(rammer, never()).setShipTarget(any());
        verify(rammer, never()).giveCommand(any(), any(), anyInt());
        verify(rammer, never()).blockCommandForOneFrame(any());
    }

    @Test
    void thePlayersShipRamsWhileOnAutopilot() {
        ShipAPI rammer = rammer();
        enemy(HullSize.CRUISER, 2000f, 0f);
        when(engine.getPlayerShip()).thenReturn(rammer);
        when(engine.isUIAutopilotOn()).thenReturn(false);
        when(rammer.getShipAI()).thenReturn(mock(ShipAIPlugin.class));

        new RamPilotListener(rammer).advance(0.1f);

        verify(rammer).giveCommand(ShipCommand.ACCELERATE, null, 0);
    }

    @Test
    void shipsStillOnTheirTravelDriveAreLeftToDeploy() {
        ShipAPI rammer = rammer();
        ShipSystemAPI travelDrive = mock(ShipSystemAPI.class);
        when(travelDrive.isOn()).thenReturn(true);
        when(rammer.getTravelDrive()).thenReturn(travelDrive);
        enemy(HullSize.CRUISER, 2000f, 0f);

        new RamPilotListener(rammer).advance(0.1f);

        verify(rammer, never()).giveCommand(any(), any(), anyInt());
    }

    @Test
    void withNoEnemiesTheFieldIsOnlyRescannedOnTheRetargetInterval() {
        ShipAPI rammer = rammer();
        RamPilotListener listener = new RamPilotListener(rammer);

        for (int frame = 0; frame < 10; frame++) {
            listener.advance(0.01f);
        }

        verify(engine, Mockito.times(1)).getShips();
    }

    @Test
    void retreatingShipsAndStationsAreLeftAlone() {
        ShipAPI retreating = rammer();
        when(retreating.isRetreating()).thenReturn(true);
        ShipAPI station = rammer();
        when(station.isStation()).thenReturn(true);
        enemy(HullSize.CRUISER, 2000f, 0f);

        new RamPilotListener(retreating).advance(0.1f);
        new RamPilotListener(station).advance(0.1f);

        verify(retreating, never()).giveCommand(any(), any(), anyInt());
        verify(station, never()).giveCommand(any(), any(), anyInt());
    }

    @Test
    void aDestroyedTargetIsReplacedOnTheNextFrame() {
        ShipAPI rammer = rammer();
        ShipAPI capital = enemy(HullSize.CAPITAL_SHIP, 2000f, 0f);
        ShipAPI cruiser = enemy(HullSize.CRUISER, 2000f, 500f);
        RamPilotListener listener = new RamPilotListener(rammer);

        listener.advance(0.01f);
        when(capital.isAlive()).thenReturn(false);
        listener.advance(0.01f);

        verify(rammer).setShipTarget(capital);
        verify(rammer).setShipTarget(cruiser);
    }

    @Test
    void shieldsAreLoweredOnlyOnceTheTargetIsClose() {
        ShipAPI rammer = rammer();
        ShieldAPI shield = mock(ShieldAPI.class);
        when(shield.isOn()).thenReturn(true);
        when(rammer.getShield()).thenReturn(shield);
        ShipAPI target = enemy(HullSize.CRUISER, 2000f, 0f);
        RamPilotListener listener = new RamPilotListener(rammer);

        listener.advance(0.01f);
        verify(rammer, never()).giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);

        when(target.getLocation()).thenReturn(new Vector2f(350f, 0f));
        listener.advance(0.01f);
        verify(rammer).giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
        verify(rammer, never()).blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);

        when(shield.isOn()).thenReturn(false);
        listener.advance(0.01f);
        verify(rammer).blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);
    }

    @Test
    void aPhasedShipOnlyUnphasesWhenItAlreadyOverlapsTheTargetsHull() {
        ShipAPI rammer = rammer();
        when(rammer.getPhaseCloak()).thenReturn(mock(ShipSystemAPI.class));
        when(rammer.isPhased()).thenReturn(true);
        BoundsAPI rammerBounds = square(0f, 0f, 20f);
        when(rammer.getExactBounds()).thenReturn(rammerBounds);
        ShipAPI target = enemy(HullSize.CRUISER, 100f, 0f);
        BoundsAPI targetBounds = square(100f, 0f, 40f);
        when(target.getExactBounds()).thenReturn(targetBounds);
        RamPilotListener listener = new RamPilotListener(rammer);

        listener.advance(0.01f);
        verify(rammer).blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);
        verify(rammer, never()).giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);

        when(rammer.getLocation()).thenReturn(new Vector2f(90f, 0f));
        BoundsAPI insideBounds = square(90f, 0f, 20f);
        when(rammer.getExactBounds()).thenReturn(insideBounds);
        listener.advance(0.01f);
        verify(rammer).giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
    }

    @Test
    void boundsOverlapCountsCrossingEdgesAndOneShipSittingInsideAnother() {
        ShipAPI first = deployed(0, HullSize.FRIGATE, 0f, 0f);
        ShipAPI second = deployed(1, HullSize.CAPITAL_SHIP, 0f, 0f);
        BoundsAPI small = square(0f, 0f, 10f);
        when(first.getExactBounds()).thenReturn(small);
        BoundsAPI large = square(0f, 0f, 100f);
        when(second.getExactBounds()).thenReturn(large);
        assertTrue(CombatQueries.boundsOverlap(first, second));

        BoundsAPI crossing = square(95f, 0f, 10f);
        when(first.getExactBounds()).thenReturn(crossing);
        when(first.getLocation()).thenReturn(new Vector2f(95f, 0f));
        assertTrue(CombatQueries.boundsOverlap(first, second));

        BoundsAPI apart = square(300f, 0f, 10f);
        when(first.getExactBounds()).thenReturn(apart);
        when(first.getLocation()).thenReturn(new Vector2f(300f, 0f));
        assertFalse(CombatQueries.boundsOverlap(first, second));
    }

    @Test
    void theInterceptPointLeadsAMovingTargetByUpToThreeSeconds() {
        Vector2f aim = RamPilotListener.interceptPoint(new Vector2f(), new Vector2f(), 200f, new Vector2f(1000f, 0f), new Vector2f(0f, 100f));

        assertEquals(1000f, aim.x, 0.001f);
        assertEquals(300f, aim.y, 0.001f);
    }

    @Test
    void theAngleToAimTakesTheShortWayRound() {
        assertEquals(90f, RamPilotListener.angleToAim(new Vector2f(), 0f, new Vector2f(0f, 10f)), 0.001f);
        assertEquals(-90f, RamPilotListener.angleToAim(new Vector2f(), 0f, new Vector2f(0f, -10f)), 0.001f);
        assertEquals(20f, RamPilotListener.angleToAim(new Vector2f(), 170f, new Vector2f(-10f, (float) -Math.tan(Math.toRadians(10)) * 10f)), 0.01f);
    }

    @Test
    void theShipBrakesItsTurnBeforeOvershootingAndHoldsInsideTheDeadBand() {
        assertEquals(ShipCommand.TURN_LEFT, RamPilotListener.turnCommand(45f, 0f, 40f));
        assertEquals(ShipCommand.TURN_RIGHT, RamPilotListener.turnCommand(-45f, 0f, 40f));
        assertEquals(ShipCommand.TURN_RIGHT, RamPilotListener.turnCommand(10f, 40f, 40f));
        assertNull(RamPilotListener.turnCommand(0.5f, 0f, 40f));
    }
}
