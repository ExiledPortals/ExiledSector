package exiledsector.skills.skilleffect.aura;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import exiledsector.MockSector;
import exiledsector.skills.skilleffect.DestructionWatchHarness;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TerribleBalanceTest {

    private static final int PLAYER = 0;
    private static final int ENEMY = 1;
    private static final int NEUTRAL = 100;

    private final List<Object> battleShips = new ArrayList<>();
    private MockSector mockSector;
    private DestructionWatchHarness destructionWatch;

    @BeforeEach
    void setUp() {
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        mockSector = new MockSector();
        mockSector.global.when(Global::getCombatEngine).thenReturn(engine);
        destructionWatch = new DestructionWatchHarness(mockSector.global, engine, battleShips);
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
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

    private static ShipAPI balancedShip(float percentPerStack) {
        ShipAPI ship = ship(0f, PLAYER);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getMutableStats()).thenReturn(stats);
        when(stats.getDynamic().getValue(TerribleBalanceListener.PERCENT_PER_STACK_KEY, 0f)).thenReturn(percentPerStack);
        return ship;
    }

    private static void destroy(ShipAPI ship) {
        when(ship.isAlive()).thenReturn(false);
        when(ship.isHulk()).thenReturn(true);
        when(ship.getHitpoints()).thenReturn(0f);
        when(ship.getOwner()).thenReturn(NEUTRAL);
    }

    @Test
    void onlyAlliedShipsLostInRangeAddStacksAndTheBonusScalesWithThem() {
        ShipAPI balanced = balancedShip(5f);
        ShipAPI nearAlly = ship(800f, PLAYER);
        ShipAPI farAlly = ship(1600f, PLAYER);
        ShipAPI nearEnemy = ship(400f, ENEMY);
        ShipAPI allyFighter = ship(200f, PLAYER);
        when(allyFighter.isFighter()).thenReturn(true);
        battleShips.addAll(List.of(balanced, nearAlly, farAlly, nearEnemy, allyFighter));
        TerribleBalanceListener listener = new TerribleBalanceListener(balanced);

        destructionWatch.tick();
        List.of(nearAlly, farAlly, nearEnemy, allyFighter).forEach(TerribleBalanceTest::destroy);
        destructionWatch.tick();

        assertEquals(1, listener.stacks());
        verify(balanced.getMutableStats().getBallisticWeaponDamageMult()).modifyPercent("exiledSector_terribleBalance", 5f);
        verify(balanced.getMutableStats().getMissileRoFMult()).modifyPercent("exiledSector_terribleBalance", 5f);
    }

    @Test
    void stacksStopAtTheMaximum() {
        ShipAPI balanced = balancedShip(5f);
        List<ShipAPI> allies = new ArrayList<>();
        for (int i = 0; i < TerribleBalanceListener.MAX_STACKS + 2; i++) {
            allies.add(ship(100f * (i + 1), PLAYER));
        }
        battleShips.add(balanced);
        battleShips.addAll(allies);
        TerribleBalanceListener listener = new TerribleBalanceListener(balanced);

        destructionWatch.tick();
        allies.forEach(TerribleBalanceTest::destroy);
        destructionWatch.tick();

        assertEquals(TerribleBalanceListener.MAX_STACKS, listener.stacks());
    }
}
