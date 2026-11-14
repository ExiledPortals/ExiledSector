package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class DestructionWatchHarness {

    private final Map<String, Object> engineCustomData = new HashMap<>();

    public DestructionWatchHarness(MockedStatic<Global> global, CombatEngineAPI engine, List<?> battleShips) {
        global.when(Global::getSettings).thenReturn(mock(SettingsAPI.class));
        global.when(Global::getCurrentState).thenReturn(GameState.COMBAT);
        when(engine.getCustomData()).thenReturn(engineCustomData);
        when(engine.getShips()).thenAnswer(invocation -> shipsIn(battleShips));
    }

    public void tick() {
        if (engineCustomData.get(ShipDestructionWatch.CUSTOM_DATA_KEY) instanceof ShipDestructionWatch watch) {
            watch.advance(ShipDestructionWatch.CHECK_INTERVAL_SECONDS, List.of());
        }
    }

    private static List<ShipAPI> shipsIn(List<?> battleShips) {
        List<ShipAPI> ships = new ArrayList<>();
        for (Object battleShip : battleShips) {
            ships.add((ShipAPI) battleShip);
        }
        return ships;
    }
}
