package exiledsector.skills.skilleffect.aura;

import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RepairCoordinatorFieldTest {

    private static final float MAX_ARMOR_IN_CELL = 100f;

    private static ArmorGridAPI grid(float[][] cells) {
        ArmorGridAPI grid = mock(ArmorGridAPI.class);
        when(grid.getGrid()).thenReturn(cells);
        when(grid.getMaxArmorInCell()).thenReturn(MAX_ARMOR_IN_CELL);
        when(grid.getArmorValue(anyInt(), anyInt())).thenAnswer(invocation -> cells[(int) invocation.getArgument(0)][(int) invocation.getArgument(1)]);
        doAnswer(invocation -> {
            cells[(int) invocation.getArgument(0)][(int) invocation.getArgument(1)] = invocation.getArgument(2);
            return null;
        }).when(grid).setArmorValue(anyInt(), anyInt(), anyFloat());
        return grid;
    }

    private static ShipAPI ship(int owner, float x, ArmorGridAPI grid) {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.isAlive()).thenReturn(true);
        when(ship.getLocation()).thenReturn(new Vector2f(x, 0f));
        when(ship.getArmorGrid()).thenReturn(grid);
        when(ship.getFluxTracker()).thenReturn(mock(FluxTrackerAPI.class));
        return ship;
    }

    private static RepairCoordinatorListener coordinator(ShipAPI ship, float percentPerSecond) {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS);
        when(stats.getDynamic().getValue(RepairCoordinatorListener.ARMOR_REPAIR_PERCENT_KEY, 0f)).thenReturn(percentPerSecond);
        when(ship.getMutableStats()).thenReturn(stats);
        return new RepairCoordinatorListener(ship);
    }

    private static GravityDragField.ShipsNear among(ShipAPI... ships) {
        return (source, radius, filter) -> List.of(ships).stream().filter(filter).toList();
    }

    private static float[][] damagedCells() {
        return new float[][]{{0f, 50f}, {100f, 100f}};
    }

    @Test
    void restoringArmorFillsEachCellInProportionToWhatItIsMissing() {
        float[][] cells = damagedCells();

        float restored = RepairCoordinatorField.restoreArmor(grid(cells), 75f);

        assertEquals(75f, restored, 0.001f);
        assertEquals(50f, cells[0][0], 0.001f);
        assertEquals(75f, cells[0][1], 0.001f);
        assertEquals(100f, cells[1][0], 0.001f);
    }

    @Test
    void restoringNeverOverfillsTheArmor() {
        float[][] cells = damagedCells();

        float restored = RepairCoordinatorField.restoreArmor(grid(cells), 1000f);

        assertEquals(150f, restored, 0.001f);
        assertEquals(100f, cells[0][0], 0.001f);
        assertEquals(100f, cells[0][1], 0.001f);
    }

    @Test
    void theCoordinatorRepairsTheClosestDamagedAllyAndNotItselfOrEnemies() {
        ShipAPI source = ship(0, 0f, grid(damagedCells()));
        float[][] nearCells = damagedCells();
        float[][] farCells = damagedCells();
        float[][] enemyCells = damagedCells();
        ShipAPI nearAlly = ship(0, 200f, grid(nearCells));
        ShipAPI farAlly = ship(0, 600f, grid(farCells));
        ShipAPI enemy = ship(1, 100f, grid(enemyCells));
        Map<ShipAPI, Float> repaired = new IdentityHashMap<>();

        RepairCoordinatorField.repair(List.of(coordinator(source, 10f)), among(source, farAlly, nearAlly, enemy), repaired, 1f);

        assertEquals(40f, repaired.get(nearAlly), 0.001f);
        assertEquals(90f, nearCells[0][0] + nearCells[0][1], 0.001f);
        assertEquals(50f, farCells[0][1], 0.001f);
        assertEquals(50f, enemyCells[0][1], 0.001f);
        assertEquals(1, repaired.size());
    }

    private static float[][] fullCells() {
        return new float[][]{{100f, 100f}, {100f, 100f}};
    }

    private static float[][] strippedCells() {
        return new float[][]{{0f, 0f}, {0f, 0f}};
    }

    private static float sum(float[][] cells) {
        return cells[0][0] + cells[0][1] + cells[1][0] + cells[1][1];
    }

    @Test
    void oneCoordinatorRestoresAtMostItsShareOfAnAllyThenMovesOnToTheNextClosest() {
        float[][] nearCells = strippedCells();
        float[][] farCells = strippedCells();
        ShipAPI nearAlly = ship(0, 200f, grid(nearCells));
        ShipAPI farAlly = ship(0, 600f, grid(farCells));
        ShipAPI source = ship(0, 0f, grid(fullCells()));
        List<RepairCoordinatorListener> sources = List.of(coordinator(source, 50f));
        Map<ShipAPI, Float> repaired = new IdentityHashMap<>();

        for (int tick = 0; tick < 4; tick++) {
            RepairCoordinatorField.repair(sources, among(source, nearAlly, farAlly), repaired, 1f);
        }

        float perCoordinatorShare = 400f * RepairCoordinatorField.MAX_REPAIR_PERCENT_PER_COORDINATOR / 100f;
        assertEquals(perCoordinatorShare, sum(nearCells), 0.001f);
        assertEquals(perCoordinatorShare, sum(farCells), 0.001f);
    }

    @Test
    void everyCoordinatorTogetherRestoresAtMostTheSharedCeilingOfAnAlly() {
        float[][] allyCells = strippedCells();
        ShipAPI ally = ship(0, 200f, grid(allyCells));
        List<ShipAPI> coordinatorShips = List.of(ship(0, 0f, grid(fullCells())), ship(0, 100f, grid(fullCells())),
                ship(0, 300f, grid(fullCells())), ship(0, 400f, grid(fullCells())));
        List<RepairCoordinatorListener> sources = coordinatorShips.stream().map(ship -> coordinator(ship, 50f)).toList();
        ShipAPI[] everyone = Stream.concat(coordinatorShips.stream(), Stream.of(ally)).toArray(ShipAPI[]::new);
        Map<ShipAPI, Float> repaired = new IdentityHashMap<>();

        for (int tick = 0; tick < 5; tick++) {
            RepairCoordinatorField.repair(sources, among(everyone), repaired, 1f);
        }

        float ceiling = 400f * RepairCoordinatorField.MAX_REPAIR_PERCENT_OF_ARMOR / 100f;
        assertEquals(ceiling, repaired.get(ally), 0.001f);
        assertEquals(ceiling, sum(allyCells), 0.001f);
    }

    @Test
    void anOverloadedOrVentingCoordinatorRepairsNothing() {
        float[][] allyCells = damagedCells();
        ShipAPI ally = ship(0, 200f, grid(allyCells));
        ShipAPI source = ship(0, 0f, grid(damagedCells()));
        when(source.getFluxTracker().isOverloadedOrVenting()).thenReturn(true);
        Map<ShipAPI, Float> repaired = new IdentityHashMap<>();

        RepairCoordinatorField.repair(List.of(coordinator(source, 10f)), among(ally, source), repaired, 1f);

        assertEquals(0, repaired.size());
        assertEquals(50f, allyCells[0][1], 0.001f);
    }

    @Test
    void theDronesAreSentToTheAllysMostDamagedCellAndRecalledWhenTheWorkIsDone() {
        ArmorGridAPI allyGrid = grid(damagedCells());
        Vector2f worstCellLocation = new Vector2f(210f, 5f);
        when(allyGrid.getLocation(0, 0)).thenReturn(worstCellLocation);
        ShipAPI ally = ship(0, 200f, allyGrid);
        ShipAPI source = ship(0, 0f, grid(fullCells()));
        RepairCoordinatorListener coordinator = coordinator(source, 1f);
        Map<ShipAPI, Float> repaired = new IdentityHashMap<>();

        RepairCoordinatorField.repair(List.of(coordinator), among(ally, source), repaired, 1f);
        assertEquals(worstCellLocation, coordinator.workPoint());

        when(ally.isAlive()).thenReturn(false);
        RepairCoordinatorField.repair(List.of(coordinator), among(ally, source), repaired, 1f);
        assertNull(coordinator.workPoint());
    }
}
