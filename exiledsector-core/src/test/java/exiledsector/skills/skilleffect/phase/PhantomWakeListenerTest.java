package exiledsector.skills.skilleffect.phase;

import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CollisionGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import exiledsector.skills.skilleffect.CombatQueries;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PhantomWakeListenerTest {

    private static final int PLAYER_SIDE = 0;
    private static final float LIFETIME_SECONDS = 3f;

    private MockedStatic<Global> globalMock;
    private CombatEngineAPI engine;
    private CombatFleetManagerAPI fleetManager;
    private ShipVariantAPI afterimageVariant;
    private FleetMemberAPI temporaryMember;
    private ShipAPI ownerShip;
    private ShipAPI afterimage;
    private final Vector2f ownerLocation = new Vector2f(100f, 200f);

    @BeforeEach
    void setUp() {
        engine = mock(CombatEngineAPI.class);
        fleetManager = mock(CombatFleetManagerAPI.class);
        when(engine.getFleetManager(PLAYER_SIDE)).thenReturn(fleetManager);
        SettingsAPI settings = mock(SettingsAPI.class);
        FactoryAPI factory = mock(FactoryAPI.class);
        afterimageVariant = mock(ShipVariantAPI.class);
        temporaryMember = mock(FleetMemberAPI.class, RETURNS_DEEP_STUBS);
        when(settings.createEmptyVariant(anyString(), any(ShipHullSpecAPI.class))).thenReturn(afterimageVariant);
        when(factory.createFleetMember(FleetMemberType.SHIP, afterimageVariant)).thenReturn(temporaryMember);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getCombatEngine).thenReturn(engine);
        globalMock.when(Global::getSettings).thenReturn(settings);
        globalMock.when(Global::getFactory).thenReturn(factory);

        ownerShip = mock(ShipAPI.class);
        ShipHullSpecAPI hullSpec = mock(ShipHullSpecAPI.class);
        MutableShipStatsAPI ownerStats = mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS);
        when(ownerShip.getHullSpec()).thenReturn(hullSpec);
        when(ownerShip.getId()).thenReturn("harbinger");
        when(ownerShip.isAlive()).thenReturn(true);
        when(ownerShip.getOwner()).thenReturn(PLAYER_SIDE);
        when(ownerShip.getLocation()).thenReturn(ownerLocation);
        when(ownerShip.getFacing()).thenReturn(45f);
        when(ownerShip.getMutableStats()).thenReturn(ownerStats);
        when(ownerStats.getDynamic().getValue(PhantomWakeListener.SECONDS_KEY, 0f)).thenReturn(LIFETIME_SECONDS);

        afterimage = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(afterimage.getLocation()).thenReturn(new Vector2f());
        when(afterimage.getVelocity()).thenReturn(new Vector2f());
        when(engine.isEntityInPlay(afterimage)).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    private void spawnsAs(ShipAPI spawned) {
        when(fleetManager.spawnFleetMember(eq(temporaryMember), any(Vector2f.class), anyFloat(), eq(0f))).thenReturn(spawned);
    }

    private static void enterPhase(PhantomWakeListener listener, ShipAPI ship) {
        when(ship.isPhased()).thenReturn(false);
        listener.advance(0.1f);
        when(ship.isPhased()).thenReturn(true);
        listener.advance(0.1f);
    }

    @Test
    void enteringPhaseLeavesAMarkedInertAfterimageThatCostsNoDeploymentPoints() {
        spawnsAs(afterimage);

        enterPhase(new PhantomWakeListener(ownerShip), ownerShip);

        verify(afterimageVariant).addTag(CombatQueries.AFTERIMAGE_TAG);
        verify(temporaryMember).setAlly(true);
        verify(engine).addPlugin(any(PhaseAfterimage.class));
        verify(afterimage).setInvalidTransferCommandTarget(true);
        verify(afterimage).setHoldFire(true);
        StatBonus deploymentPoints = temporaryMember.getStats().getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD);
        verify(deploymentPoints).modifyMult(PhaseAfterimage.STATS_MOD_ID, 0f);
        assertEquals(ownerLocation, afterimage.getLocation());
        InOrder messages = inOrder(fleetManager);
        messages.verify(fleetManager).setSuppressDeploymentMessages(true);
        messages.verify(fleetManager).spawnFleetMember(eq(temporaryMember), any(Vector2f.class), anyFloat(), eq(0f));
        messages.verify(fleetManager).setSuppressDeploymentMessages(false);
    }

    @Test
    void aSpawnThatFailsOutrightLeavesNothingAndRestoresDeploymentMessages() {
        when(fleetManager.spawnFleetMember(eq(temporaryMember), any(Vector2f.class), anyFloat(), eq(0f)))
                .thenThrow(new IllegalStateException("broken"));

        enterPhase(new PhantomWakeListener(ownerShip), ownerShip);

        verify(engine, never()).addPlugin(any());
        verify(fleetManager).setSuppressDeploymentMessages(false);
    }

    @Test
    void anAfterimageThatThrowsWhileSettlingInIsKept() {
        when(fleetManager.spawnFleetMember(eq(temporaryMember), any(Vector2f.class), anyFloat(), eq(0f)))
                .thenThrow(new IllegalStateException("settling"));
        when(fleetManager.getShipFor(temporaryMember)).thenReturn(afterimage);

        enterPhase(new PhantomWakeListener(ownerShip), ownerShip);

        verify(engine).addPlugin(any(PhaseAfterimage.class));
        verify(afterimage).setHoldFire(true);
    }

    @Test
    void enteringPhaseAgainMovesTheLivingAfterimageInsteadOfBuildingAnother() {
        spawnsAs(afterimage);
        PhantomWakeListener listener = new PhantomWakeListener(ownerShip);
        enterPhase(listener, ownerShip);
        ownerLocation.set(500f, 600f);

        enterPhase(listener, ownerShip);

        verify(fleetManager, times(1)).spawnFleetMember(any(), any(), anyFloat(), anyFloat());
        verify(engine, never()).removeEntity(afterimage);
        assertEquals(new Vector2f(500f, 600f), afterimage.getLocation());
    }

    @Test
    void aFadedAfterimageIsRemovedAndTheNextEntryBuildsAFreshOne() {
        spawnsAs(afterimage);
        PhantomWakeListener listener = new PhantomWakeListener(ownerShip);
        enterPhase(listener, ownerShip);
        ArgumentCaptor<PhaseAfterimage> plugin = ArgumentCaptor.forClass(PhaseAfterimage.class);
        verify(engine).addPlugin(plugin.capture());

        plugin.getValue().advance(LIFETIME_SECONDS, List.of());

        verify(engine).removePlugin(plugin.getValue());
        verify(fleetManager).removeDeployed(afterimage, false);
        verify(engine).removeEntity(afterimage);

        when(engine.isEntityInPlay(afterimage)).thenReturn(false);
        enterPhase(listener, ownerShip);
        verify(fleetManager, times(2)).spawnFleetMember(any(), any(), anyFloat(), anyFloat());
    }

    @Test
    void anAfterimageFadesWhileItLasts() {
        spawnsAs(afterimage);
        PhaseAfterimage plugin = PhaseAfterimage.leave(ownerShip, LIFETIME_SECONDS, 0f, engine);

        plugin.advance(LIFETIME_SECONDS / 2f, List.of());

        verify(afterimage).setAlphaMult(PhaseAfterimage.START_ALPHA / 2f);
        verify(engine, never()).removePlugin(plugin);
    }

    private void stubShipsNearTheAfterimage(ShipAPI... ships) {
        CollisionGridAPI grid = mock(CollisionGridAPI.class);
        when(engine.getShipGrid()).thenReturn(grid);
        when(grid.getCheckIterator(any(), anyFloat(), anyFloat())).thenAnswer(invocation -> List.<Object>of(ships).iterator());
    }

    private static ShipAPI shipAt(int owner, Vector2f location) {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.isAlive()).thenReturn(true);
        when(ship.getCollisionClass()).thenReturn(CollisionClass.SHIP);
        when(ship.getLocation()).thenReturn(location);
        return ship;
    }

    @Test
    void aFadingAfterimageArcsItsEmpIntoEveryNearbyEnemyShip() {
        spawnsAs(afterimage);
        ShipAPI enemy = shipAt(1, new Vector2f(100f, 0f));
        ShipAPI enemyFighter = shipAt(1, new Vector2f(50f, 0f));
        when(enemyFighter.isFighter()).thenReturn(true);
        ShipAPI ally = shipAt(PLAYER_SIDE, new Vector2f(80f, 0f));
        ShipAPI farEnemy = shipAt(1, new Vector2f(PhaseAfterimage.FADE_BURST_RANGE + 100f, 0f));
        stubShipsNearTheAfterimage(enemy, enemyFighter, ally, farEnemy);
        PhaseAfterimage plugin = PhaseAfterimage.leave(ownerShip, LIFETIME_SECONDS, 500f, engine);

        plugin.advance(LIFETIME_SECONDS, List.of());

        verify(engine).spawnEmpArc(eq(ownerShip), any(Vector2f.class), isNull(), eq(enemy), eq(DamageType.ENERGY), eq(0f), eq(500f),
                anyFloat(), any(), anyFloat(), any(), any());
        verify(engine, times(1)).spawnEmpArc(any(), any(), any(), any(), any(), anyFloat(), anyFloat(), anyFloat(), any(), anyFloat(), any(),
                any());
        verify(engine).removeEntity(afterimage);
    }

    @Test
    void anAfterimageMovedByANewEntryOrAlreadyGoneNeverBursts() {
        spawnsAs(afterimage);
        stubShipsNearTheAfterimage(shipAt(1, new Vector2f(100f, 0f)));
        PhaseAfterimage plugin = PhaseAfterimage.leave(ownerShip, LIFETIME_SECONDS, 500f, engine);

        plugin.advance(LIFETIME_SECONDS / 2f, List.of());
        plugin.restartAt(ownerShip, LIFETIME_SECONDS, 500f, engine);
        when(engine.isEntityInPlay(afterimage)).thenReturn(false);
        plugin.advance(LIFETIME_SECONDS / 2f, List.of());

        verify(engine, never()).spawnEmpArc(any(), any(), any(), any(), any(), anyFloat(), anyFloat(), anyFloat(), any(), anyFloat(), any(),
                any());
    }

    @Test
    void shipsWithModulesOrBuiltInWingsLeaveNone() {
        ShipAPI stationCore = mock(ShipAPI.class);
        when(stationCore.getHullSpec()).thenReturn(mock(ShipHullSpecAPI.class));
        when(stationCore.getChildModulesCopy()).thenReturn(List.of(mock(ShipAPI.class)));

        assertNull(PhaseAfterimage.leave(stationCore, LIFETIME_SECONDS, 0f, engine));
        verify(engine, never()).addPlugin(any());
    }
}
