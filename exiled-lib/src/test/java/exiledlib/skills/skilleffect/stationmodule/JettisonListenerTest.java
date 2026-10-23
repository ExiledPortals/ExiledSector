package exiledlib.skills.skilleffect.stationmodule;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JettisonListenerTest {

    private static ShipAPI ship(String id, Map<String, Float> magnitudes) {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getId()).thenReturn(id);
        when(ship.getMutableStats()).thenReturn(stats);
        magnitudes.forEach((key, value) -> when(stats.getDynamic().getValue(key, 0f)).thenReturn(value));
        when(ship.isAlive()).thenReturn(true);
        when(ship.getLocation()).thenReturn(new Vector2f());
        return ship;
    }

    private static ShipAPI module(boolean alive) {
        ShipAPI module = mock(ShipAPI.class);
        when(module.isAlive()).thenReturn(alive);
        when(module.getHitpoints()).thenReturn(alive ? 500f : 0f);
        return module;
    }

    private static void withModules(ShipAPI ship, List<ShipAPI> modules) {
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        Map<String, String> stationModules = new HashMap<>();
        for (int i = 0; i < modules.size(); i++) {
            stationModules.put("WS000" + i, "plate_" + i);
        }
        when(variant.getStationModules()).thenReturn(stationModules);
        when(ship.getVariant()).thenReturn(variant);
        when(ship.getChildModulesCopy()).thenReturn(modules);
    }

    @Test
    void jettisonCountsEachDestroyedModuleOnceItWasSeenAlive() {
        ShipAPI first = module(true);
        ShipAPI second = module(true);
        ShipAPI ship = ship("merite", Map.of(JettisonListener.TOP_SPEED_PERCENT_PER_MODULE_KEY, 10f));
        withModules(ship, List.of(first, second));
        JettisonListener listener = new JettisonListener(ship);

        listener.advance(0.3f);
        verify(ship.getMutableStats().getMaxSpeed(), never()).modifyPercent(Mockito.anyString(), anyFloat());

        when(first.isAlive()).thenReturn(false);
        listener.advance(0.3f);
        verify(ship.getMutableStats().getMaxSpeed()).modifyPercent("exiledSector_jettison_merite", 10f);

        when(second.isHulk()).thenReturn(true);
        listener.advance(0.3f);
        verify(ship.getMutableStats().getMaxSpeed()).modifyPercent("exiledSector_jettison_merite", 20f);
        assertEquals(2, JettisonListener.destroyedCount(List.of(first, second)));
    }

    @Test
    void jettisonCountsModulesAlreadyLostWhenTheShipDeploys() {
        ShipAPI survivor = module(true);
        ShipAPI ship = ship("argentavis", Map.of(JettisonListener.TOP_SPEED_PERCENT_PER_MODULE_KEY, 10f));
        withModules(ship, List.of(survivor, module(true)));
        when(ship.getChildModulesCopy()).thenReturn(List.of(survivor));

        new JettisonListener(ship).advance(0.3f);

        verify(ship.getMutableStats().getMaxSpeed()).modifyPercent("exiledSector_jettison_argentavis", 10f);
    }
}
