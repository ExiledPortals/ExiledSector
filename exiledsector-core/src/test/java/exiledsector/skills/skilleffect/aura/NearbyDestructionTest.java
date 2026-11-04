package exiledsector.skills.skilleffect.aura;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.CollisionGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.FighterLaunchBayAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import exiledsector.MockSector;
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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NearbyDestructionTest {

    private static final int PLAYER = 0;
    private static final int ENEMY = 1;
    private static final int NEUTRAL = 100;

    private final List<Object> gridShips = new ArrayList<>();
    private MockSector mockSector;

    @BeforeEach
    void setUp() {
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        CollisionGridAPI shipGrid = mock(CollisionGridAPI.class);
        when(engine.getShipGrid()).thenReturn(shipGrid);
        when(shipGrid.getCheckIterator(any(), anyFloat(), anyFloat())).thenAnswer(invocation -> new ArrayList<>(gridShips).iterator());
        mockSector = new MockSector();
        mockSector.global.when(Global::getCombatEngine).thenReturn(engine);
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
        gridShips.addAll(List.of(carrier, near, alsoNear, far, ally));
        WreckReclaimListener listener = new WreckReclaimListener(carrier);

        listener.advance(0.3f);
        List.of(near, alsoNear, far, ally).forEach(NearbyDestructionTest::destroy);
        listener.advance(0.3f);

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
        gridShips.add(plated);
        gridShips.addAll(enemies);
        WreckPlatingListener listener = new WreckPlatingListener(plated);

        listener.advance(0.3f);
        destroy(enemies.get(0));
        destroy(enemies.get(3));
        listener.advance(0.3f);
        assertArrayEquals(new float[]{5f, 55f}, cells[0], 0.001f);
        assertArrayEquals(new float[]{100f, 5f}, cells[1], 0.001f);

        destroy(enemies.get(1));
        destroy(enemies.get(2));
        listener.advance(0.3f);

        float repaired = cells[0][0] + cells[0][1] + cells[1][0] + cells[1][1] - 150f;
        assertEquals(40f, repaired, 0.001f);
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
