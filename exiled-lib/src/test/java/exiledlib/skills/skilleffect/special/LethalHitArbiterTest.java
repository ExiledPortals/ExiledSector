package exiledlib.skills.skilleffect.special;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LethalHitArbiterTest {

    private final List<Object> attached = new ArrayList<>();
    private final List<String> calls = new ArrayList<>();
    private ShipAPI ship;

    @BeforeEach
    void setUp() {
        ship = mock(ShipAPI.class);
        when(ship.getHitpoints()).thenReturn(100f);
        when(ship.getListeners(any())).thenAnswer(invocation -> {
            Class<?> type = invocation.getArgument(0);
            return attached.stream().filter(type::isInstance).toList();
        });
        doAnswer(invocation -> attached.add(invocation.getArgument(0))).when(ship).addListener(any());
    }

    private LethalHitArbiter.Saver saver(String name, boolean saves) {
        return (damagedShip, damageAmount) -> {
            calls.add(name);
            return saves;
        };
    }

    private boolean hit(float damage) {
        return ((HullDamageAboutToBeTakenListener) attached.get(0)).notifyAboutToTakeHullDamage(null, ship, new Vector2f(), damage);
    }

    @Test
    void reactorsFireThenSaversTryInPriorityOrderUntilOneSaves() {
        LethalHitArbiter.addSaver(ship, LethalHitArbiter.PHASE_ANCHOR_DIVE_PRIORITY, saver("dive", true));
        LethalHitArbiter.addSaver(ship, LethalHitArbiter.RISE_FROM_THE_ASHES_PRIORITY, saver("rise", true));
        LethalHitArbiter.addReactor(ship, (damagedShip, damageAmount) -> calls.add("explode"));

        assertTrue(hit(150f));

        assertEquals(1, attached.size());
        assertEquals(List.of("explode", "rise"), calls);
    }

    @Test
    void aSaverThatDeclinesPassesTheHitToTheNext() {
        LethalHitArbiter.addSaver(ship, LethalHitArbiter.RISE_FROM_THE_ASHES_PRIORITY, saver("rise", false));
        LethalHitArbiter.addSaver(ship, LethalHitArbiter.PHASE_ANCHOR_DIVE_PRIORITY, saver("dive", false));

        assertFalse(hit(150f));

        assertEquals(List.of("rise", "dive"), calls);
    }

    @Test
    void aHitTheShipSurvivesReachesNoSaverOrReactor() {
        LethalHitArbiter.addSaver(ship, LethalHitArbiter.RISE_FROM_THE_ASHES_PRIORITY, saver("rise", true));
        LethalHitArbiter.addReactor(ship, (damagedShip, damageAmount) -> calls.add("explode"));

        assertFalse(hit(50f));

        assertTrue(calls.isEmpty());
    }

    @Test
    void reactorsFireOnALethalHitWhileASaverBlocksHullDamage() {
        LethalHitArbiter.addSaver(ship, LethalHitArbiter.PHASE_ANCHOR_DIVE_PRIORITY, new LethalHitArbiter.Saver() {
            @Override
            public boolean blocksHullDamage() {
                return true;
            }

            @Override
            public boolean saveFromLethalHit(ShipAPI damagedShip, float damageAmount) {
                calls.add("dive");
                return true;
            }
        });
        LethalHitArbiter.addReactor(ship, (damagedShip, damageAmount) -> calls.add("explode"));

        assertTrue(hit(10f));
        assertTrue(calls.isEmpty());

        assertTrue(hit(150f));
        assertEquals(List.of("explode"), calls);
    }
}
