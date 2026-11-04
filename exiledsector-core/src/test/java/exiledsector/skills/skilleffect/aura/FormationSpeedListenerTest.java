package exiledsector.skills.skilleffect.aura;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FormationSpeedListenerTest {

    private static final String MOD_ID = "exiledSector_formationSpeed_owner";

    private final List<Object> gridShips = new ArrayList<>();
    private MockedStatic<Global> globalMock;

    @BeforeEach
    void setUp() {
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        CollisionGridAPI shipGrid = mock(CollisionGridAPI.class);
        when(engine.getShipGrid()).thenReturn(shipGrid);
        when(shipGrid.getCheckIterator(any(), anyFloat(), anyFloat())).thenAnswer(invocation -> new ArrayList<>(gridShips).iterator());
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getCombatEngine).thenReturn(engine);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    private static ShipAPI ship(String id, int owner, float x) {
        ShipAPI ship = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getId()).thenReturn(id);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.isAlive()).thenReturn(true);
        when(ship.getLocation()).thenReturn(new Vector2f(x, 0f));
        return ship;
    }

    private ShipAPI owner() {
        ShipAPI owner = ship("owner", 0, 0f);
        when(owner.getMutableStats().getDynamic().getValue(FormationSpeedListener.SPEED_PERCENT_PER_ALLY_KEY, 0f)).thenReturn(2f);
        gridShips.add(owner);
        return owner;
    }

    @Test
    void everyAlliedShipInRangeAddsTopSpeedUpToTheCap() {
        assertEquals(6f, FormationSpeedListener.speedPercent(2f, 3));
        assertEquals(FormationSpeedListener.MAX_SPEED_PERCENT, FormationSpeedListener.speedPercent(2f, 8));
        assertEquals(0f, FormationSpeedListener.speedPercent(2f, 0));
    }

    @Test
    void onlyAlliedShipsWithinRangeCountNeverStrikecraftModulesOrEnemies() {
        ShipAPI owner = owner();
        gridShips.add(ship("ally", 0, 900f));
        gridShips.add(ship("farAlly", 0, 1200f));
        gridShips.add(ship("enemy", 1, 100f));
        ShipAPI fighter = ship("fighter", 0, 100f);
        when(fighter.isFighter()).thenReturn(true);
        gridShips.add(fighter);
        ShipAPI module = ship("module", 0, 100f);
        when(module.isStationModule()).thenReturn(true);
        gridShips.add(module);
        FormationSpeedListener listener = new FormationSpeedListener(owner);

        listener.advance(FormationSpeedListener.SCAN_INTERVAL_SECONDS);

        verify(owner.getMutableStats().getMaxSpeed()).modifyPercent(MOD_ID, 2f);
    }

    @Test
    void theScanWaitsForItsInterval() {
        ShipAPI owner = owner();
        gridShips.add(ship("ally", 0, 100f));
        FormationSpeedListener listener = new FormationSpeedListener(owner);
        listener.advance(FormationSpeedListener.SCAN_INTERVAL_SECONDS);

        gridShips.add(ship("secondAlly", 0, 200f));
        listener.advance(0.1f);

        verify(owner.getMutableStats().getMaxSpeed(), never()).modifyPercent(anyString(), Mockito.eq(4f));
    }
}
