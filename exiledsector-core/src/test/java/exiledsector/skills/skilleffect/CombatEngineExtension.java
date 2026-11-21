package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.listeners.CombatListenerManagerAPI;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CombatEngineExtension implements BeforeEachCallback, AfterEachCallback {

    private final boolean withListenerManager;
    private MockedStatic<Global> global;
    private CombatEngineAPI engine;
    private CombatListenerManagerAPI listeners;

    public CombatEngineExtension() {
        this(false);
    }

    private CombatEngineExtension(boolean withListenerManager) {
        this.withListenerManager = withListenerManager;
    }

    public static CombatEngineExtension withListenerManager() {
        return new CombatEngineExtension(true);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        engine = mock(CombatEngineAPI.class);
        if (withListenerManager) {
            listeners = mock(CombatListenerManagerAPI.class);
            when(engine.getListenerManager()).thenReturn(listeners);
            when(listeners.getListeners(any())).thenReturn(List.of());
        }
        global = Mockito.mockStatic(Global.class);
        global.when(Global::getCombatEngine).thenReturn(engine);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        global.close();
    }

    public void shipGridOver(List<?> gridShips) {
        CollisionGridAPI shipGrid = gridOver(gridShips);
        when(engine.getShipGrid()).thenReturn(shipGrid);
    }

    public void asteroidGridOver(List<?> gridAsteroids) {
        CollisionGridAPI asteroidGrid = gridOver(gridAsteroids);
        when(engine.getAsteroidGrid()).thenReturn(asteroidGrid);
    }

    private static CollisionGridAPI gridOver(List<?> entities) {
        CollisionGridAPI grid = mock(CollisionGridAPI.class);
        when(grid.getCheckIterator(any(), anyFloat(), anyFloat())).thenAnswer(call -> new ArrayList<Object>(entities).iterator());
        return grid;
    }

    public MockedStatic<Global> global() {
        return global;
    }

    public CombatEngineAPI engine() {
        return engine;
    }

    public CombatListenerManagerAPI listeners() {
        return listeners;
    }
}
