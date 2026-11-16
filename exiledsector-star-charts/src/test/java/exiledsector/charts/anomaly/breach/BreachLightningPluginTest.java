package exiledsector.charts.anomaly.breach;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.MockSector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BreachLightningPluginTest {

    private MockSector mockSector;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
    }

    private static ShipAPI ship(int owner) {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.isAlive()).thenReturn(true);
        when(ship.getLocation()).thenReturn(new Vector2f());
        return ship;
    }

    @Test
    void lightningStrikesShipsOnBothSidesButNeverWrecksFightersOrPhasedShips() {
        ShipAPI player = ship(BreachLightningPlugin.PLAYER_OWNER);
        ShipAPI enemy = ship(BreachLightningPlugin.ENEMY_OWNER);
        ShipAPI neutral = ship(100);
        ShipAPI fighter = ship(BreachLightningPlugin.ENEMY_OWNER);
        when(fighter.isFighter()).thenReturn(true);
        ShipAPI phased = ship(BreachLightningPlugin.PLAYER_OWNER);
        when(phased.isPhased()).thenReturn(true);
        ShipAPI wreck = ship(BreachLightningPlugin.ENEMY_OWNER);
        when(wreck.isHulk()).thenReturn(true);

        assertTrue(BreachLightningPlugin.canBeStruck(player));
        assertTrue(BreachLightningPlugin.canBeStruck(enemy));
        for (ShipAPI spared : List.of(neutral, fighter, phased, wreck)) {
            assertFalse(BreachLightningPlugin.canBeStruck(spared));
        }
        assertFalse(BreachLightningPlugin.canBeStruck(null));

        Set<ShipAPI> struck = new HashSet<>();
        Random random = new Random(3L);
        for (int strike = 0; strike < 40; strike++) {
            struck.add(BreachLightningPlugin.pickTarget(List.of(player, enemy, neutral, fighter, phased, wreck), random));
        }
        assertEquals(Set.of(player, enemy), struck);
        assertNull(BreachLightningPlugin.pickTarget(List.of(neutral), random));
    }

    @Test
    void withNoBreachAroundThePlayerTheLightningStaysOff() {
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        when(engine.isInCampaign()).thenReturn(true);
        BreachLightningPlugin plugin = new BreachLightningPlugin(new Random(1L));

        plugin.init(engine);
        plugin.advance(BreachRules.LIGHTNING_INTERVAL_SECONDS * 3f, List.of());

        assertFalse(plugin.isActive());
        verify(engine, never()).spawnEmpArcPierceShields(any(), any(), any(), any(), any(), anyFloat(), anyFloat(), anyFloat(), anyString(), anyFloat(),
                any(), any());
    }
}
