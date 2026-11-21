package exiledsector.skills.skilleffect.aura;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.FighterLaunchBayAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.MockSector;
import exiledsector.skills.skilleffect.CombatTestShips;
import exiledsector.skills.skilleffect.DestructionWatchHarness;
import exiledsector.skills.skilleffect.weapon.PirateFrenzy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NearbyDestructionTest {

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
        ShipAPI ship = CombatTestShips.ship().at(new Vector2f(x, 0f)).owner(owner).alive().build();
        when(ship.getHitpoints()).thenReturn(1000f);
        return ship;
    }

    private static void destroy(ShipAPI ship) {
        when(ship.isAlive()).thenReturn(false);
        when(ship.isHulk()).thenReturn(true);
        when(ship.getHitpoints()).thenReturn(0f);
        when(ship.getOwner()).thenReturn(NEUTRAL);
    }

    private static FighterLaunchBayAPI bay(float rate) {
        FighterLaunchBayAPI bay = mock(FighterLaunchBayAPI.class);
        float[] currentRate = {rate};
        when(bay.getCurrRate()).thenAnswer(call -> currentRate[0]);
        doAnswer(call -> {
            currentRate[0] = call.getArgument(0);
            return null;
        }).when(bay).setCurrRate(anyFloat());
        return bay;
    }

    private static ShipAPI carrier(float percent, List<FighterLaunchBayAPI> bays) {
        ShipAPI carrier = ship(0f, PLAYER);
        when(carrier.getMutableStats().getDynamic().getValue(WreckReclaimListener.RESTORE_PERCENT_KEY, 0f)).thenReturn(percent);
        when(carrier.hasLaunchBays()).thenReturn(!bays.isEmpty());
        when(carrier.getLaunchBaysCopy()).thenReturn(bays);
        return carrier;
    }

    @Test
    void eachEnemyShipDestroyedInRangeRestoresReplacementRateUpToFull() {
        FighterLaunchBayAPI wornBay = bay(0.5f);
        FighterLaunchBayAPI nearlyFullBay = bay(0.95f);
        ShipAPI carrier = carrier(10f, List.of(wornBay, nearlyFullBay));
        ShipAPI near = ship(800f, ENEMY);
        ShipAPI alsoNear = ship(-1200f, ENEMY);
        ShipAPI far = ship(2400f, ENEMY);
        ShipAPI ally = ship(300f, PLAYER);
        battleShips.addAll(List.of(carrier, near, alsoNear, far, ally));
        new WreckReclaimListener(carrier);

        destructionWatch.tick();
        List.of(near, alsoNear, far, ally).forEach(NearbyDestructionTest::destroy);
        destructionWatch.tick();

        assertEquals(0.7f, wornBay.getCurrRate(), 0.0001f);
        assertEquals(1f, nearlyFullBay.getCurrRate(), 0.0001f);
    }

    @Test
    void enemyShipsDestroyedInRangeRepairArmorUntilTheDeploymentCap() {
        ShipAPI plated = ship(0f, PLAYER);
        when(plated.getMutableStats().getDynamic().getValue(WreckPlatingListener.REPAIR_CAP_PERCENT_KEY, 0f)).thenReturn(10f);
        float[][] cells = {{0f, 50f}, {100f, 0f}};
        ArmorGridAPI armorGrid = grid(100f, cells);
        when(plated.getArmorGrid()).thenReturn(armorGrid);
        List<ShipAPI> enemies = List.of(ship(500f, ENEMY), ship(-500f, ENEMY), ship(0f, ENEMY), ship(1500f, ENEMY));
        battleShips.add(plated);
        battleShips.addAll(enemies);
        new WreckPlatingListener(plated);

        destructionWatch.tick();
        destroy(enemies.get(0));
        destroy(enemies.get(3));
        destructionWatch.tick();
        assertArrayEquals(new float[]{5f, 55f}, cells[0], 0.001f);
        assertArrayEquals(new float[]{100f, 5f}, cells[1], 0.001f);

        destroy(enemies.get(1));
        destroy(enemies.get(2));
        destructionWatch.tick();

        float repaired = cells[0][0] + cells[0][1] + cells[1][0] + cells[1][1] - 150f;
        assertEquals(40f, repaired, 0.001f);
    }

    private static ShipAPI frenziedShip(float range) {
        ShipAPI raider = ship(0f, PLAYER);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS);
        when(raider.getMutableStats()).thenReturn(stats);
        when(raider.getId()).thenReturn("raider");
        when(stats.getDynamic().getValue(FrenzyOnNearbyDestructionListener.RANGE_KEY, 0f)).thenReturn(range);
        List<Object> listeners = new ArrayList<>();
        doAnswer(call -> listeners.add(call.getArgument(0))).when(raider).addListener(any());
        when(raider.getListeners(any())).thenAnswer(call -> listeners.stream().filter(((Class<?>) call.getArgument(0))::isInstance).toList());
        return raider;
    }

    @Test
    void eachEnemyShipDestroyedInRangeGrantsAPirateFrenzyStack() {
        ShipAPI raider = frenziedShip(1000f);
        ShipAPI near = ship(800f, ENEMY);
        ShipAPI alsoNear = ship(-900f, ENEMY);
        ShipAPI far = ship(1600f, ENEMY);
        ShipAPI ally = ship(300f, PLAYER);
        ShipAPI fighter = ship(200f, ENEMY);
        when(fighter.isFighter()).thenReturn(true);
        battleShips.addAll(List.of(raider, near, alsoNear, far, ally, fighter));
        new FrenzyOnNearbyDestructionListener(raider);

        destructionWatch.tick();
        List.of(near, alsoNear, far, ally, fighter).forEach(NearbyDestructionTest::destroy);
        destructionWatch.tick();

        assertEquals(2, PirateFrenzy.of(raider).stacks());
    }

    private static ArmorGridAPI grid(float maxArmorInCell, float[][] cells) {
        ArmorGridAPI grid = mock(ArmorGridAPI.class);
        when(grid.getMaxArmorInCell()).thenReturn(maxArmorInCell);
        when(grid.getGrid()).thenReturn(cells);
        when(grid.getArmorValue(anyInt(), anyInt())).thenAnswer(call -> cells[(int) call.getArgument(0)][(int) call.getArgument(1)]);
        doAnswer(call -> {
            cells[(int) call.getArgument(0)][(int) call.getArgument(1)] = call.getArgument(2);
            return null;
        }).when(grid).setArmorValue(anyInt(), anyInt(), anyFloat());
        return grid;
    }
}
