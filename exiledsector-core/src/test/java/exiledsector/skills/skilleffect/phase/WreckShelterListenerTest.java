package exiledsector.skills.skilleffect.phase;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import exiledsector.MockSector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WreckShelterListenerTest {

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

    private static ShipAPI ship(float x) {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        when(stats.getDynamic()).thenReturn(mock(DynamicStatsAPI.class));
        when(stats.getPhaseCloakUpkeepCostBonus()).thenReturn(mock(StatBonus.class));
        when(ship.getMutableStats()).thenReturn(stats);
        when(ship.getId()).thenReturn("ship" + x);
        when(ship.getLocation()).thenReturn(new Vector2f(x, 0f));
        when(ship.isAlive()).thenReturn(true);
        return ship;
    }

    private static ShipAPI wreck(float x) {
        ShipAPI wreck = ship(x);
        when(wreck.isAlive()).thenReturn(false);
        when(wreck.isHulk()).thenReturn(true);
        return wreck;
    }

    private static ShipAPI phaseShip(float range) {
        ShipAPI phaseShip = ship(0f);
        when(phaseShip.getPhaseCloak()).thenReturn(mock(ShipSystemAPI.class));
        when(phaseShip.getMutableStats().getDynamic().getValue(WreckShelterListener.RANGE_KEY, 0f)).thenReturn(range);
        return phaseShip;
    }

    @Test
    void phaseUpkeepIsFreeOnlyWhileAWreckIsInRange() {
        ShipAPI phaseShip = phaseShip(500f);
        ShipAPI wreck = wreck(400f);
        gridShips.addAll(List.of(phaseShip, wreck));
        StatBonus upkeep = phaseShip.getMutableStats().getPhaseCloakUpkeepCostBonus();
        WreckShelterListener listener = new WreckShelterListener(phaseShip);

        listener.advance(0.3f);
        listener.advance(0.3f);
        verify(upkeep, times(1)).modifyMult("exiledSector_noPhaseUpkeepNearWreck_ship0.0", 0f);

        when(wreck.getLocation()).thenReturn(new Vector2f(600f, 0f));
        listener.advance(0.3f);
        verify(upkeep).unmodifyMult("exiledSector_noPhaseUpkeepNearWreck_ship0.0");
    }

    @Test
    void theCappedShelterLastsFiveSecondsPerPhaseAndResetsOnUnphasing() {
        ShipAPI phaseShip = phaseShip(0f);
        when(phaseShip.getMutableStats().getDynamic().getValue(WreckShelterListener.CAPPED_RANGE_KEY, 0f)).thenReturn(500f);
        when(phaseShip.isPhased()).thenReturn(true);
        gridShips.addAll(List.of(phaseShip, wreck(400f)));
        StatBonus upkeep = phaseShip.getMutableStats().getPhaseCloakUpkeepCostBonus();
        WreckShelterListener listener = new WreckShelterListener(phaseShip);
        int checksInCap = Math.round(WreckShelterListener.CAPPED_SECONDS / 0.25f);

        for (int check = 0; check < checksInCap; check++) {
            listener.advance(0.25f);
        }
        verify(upkeep, times(1)).modifyMult("exiledSector_noPhaseUpkeepNearWreck_ship0.0", 0f);
        verify(upkeep, never()).unmodifyMult(anyString());

        listener.advance(0.25f);
        verify(upkeep).unmodifyMult("exiledSector_noPhaseUpkeepNearWreck_ship0.0");

        when(phaseShip.isPhased()).thenReturn(false);
        listener.advance(0.25f);
        verify(upkeep, times(2)).modifyMult("exiledSector_noPhaseUpkeepNearWreck_ship0.0", 0f);
    }

    @Test
    void livingShipsAndStrikecraftWrecksGiveNoShelter() {
        ShipAPI phaseShip = phaseShip(500f);
        ShipAPI fighterWreck = wreck(100f);
        when(fighterWreck.isFighter()).thenReturn(true);
        gridShips.addAll(List.of(phaseShip, ship(200f), fighterWreck));

        new WreckShelterListener(phaseShip).advance(0.3f);

        verify(phaseShip.getMutableStats().getPhaseCloakUpkeepCostBonus(), never()).modifyMult(anyString(), anyFloat());
    }
}
