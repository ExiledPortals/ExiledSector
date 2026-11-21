package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.skills.skilleffect.ShipDestructionWatch.DestroyedShip;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShipDestructionWatchTest {

    private static final int PLAYER = 0;
    private static final int ENEMY = 1;

    private final List<ShipAPI> battleShips = new ArrayList<>();
    @RegisterExtension
    final CombatEngineExtension combat = new CombatEngineExtension();
    private CombatEngineAPI engine;
    private DestructionWatchHarness destructionWatch;

    private static final class RecordingSubscriber implements ShipDestructionWatch.Subscriber {

        private final ShipAPI watchingShip;
        private final List<List<DestroyedShip>> reports = new ArrayList<>();

        RecordingSubscriber(ShipAPI watchingShip) {
            this.watchingShip = watchingShip;
            ShipDestructionWatch.subscribe(this);
        }

        @Override
        public ShipAPI watchingShip() {
            return watchingShip;
        }

        @Override
        public void onShipsDestroyed(List<DestroyedShip> destroyedShips) {
            reports.add(destroyedShips);
        }

        List<ShipAPI> reportedWrecks() {
            return reports.stream().flatMap(List::stream).map(DestroyedShip::wreck).toList();
        }
    }

    @BeforeEach
    void setUp() {
        engine = combat.engine();
        destructionWatch = new DestructionWatchHarness(combat.global(), engine, battleShips);
    }

    private ShipAPI ship(int owner) {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.isAlive()).thenReturn(true);
        when(ship.getHitpoints()).thenReturn(1000f);
        battleShips.add(ship);
        return ship;
    }

    private static void destroy(ShipAPI ship) {
        when(ship.isAlive()).thenReturn(false);
        when(ship.isHulk()).thenReturn(true);
        when(ship.getHitpoints()).thenReturn(0f);
        when(ship.getOwner()).thenReturn(Misc.OWNER_NEUTRAL);
    }

    @Test
    void aShipSeenAliveThatIsNowAWreckIsReportedOnceWithItsSideWhileAlive() {
        RecordingSubscriber subscriber = new RecordingSubscriber(ship(PLAYER));
        ShipAPI enemy = ship(ENEMY);

        destructionWatch.tick();
        destroy(enemy);
        destructionWatch.tick();
        destructionWatch.tick();

        assertEquals(1, subscriber.reports.size());
        DestroyedShip destroyed = subscriber.reports.get(0).get(0);
        assertEquals(enemy, destroyed.wreck());
        assertEquals(ENEMY, destroyed.ownerWhileAlive());
        assertTrue(destroyed.wasHostileTo(subscriber.watchingShip()));
        assertFalse(new DestroyedShip(enemy, PLAYER).wasHostileTo(subscriber.watchingShip()));
        assertFalse(new DestroyedShip(enemy, Misc.OWNER_NEUTRAL).wasHostileTo(subscriber.watchingShip()));
    }

    @Test
    void aShipAtZeroHullCountsBeforeItTurnsIntoAHulk() {
        RecordingSubscriber subscriber = new RecordingSubscriber(ship(PLAYER));
        ShipAPI enemy = ship(ENEMY);

        destructionWatch.tick();
        when(enemy.isAlive()).thenReturn(false);
        when(enemy.getHitpoints()).thenReturn(0f);
        destructionWatch.tick();

        assertEquals(List.of(enemy), subscriber.reportedWrecks());
    }

    @Test
    void shipsNeverSeenAliveRetreatingRemovedOrAfterimagesAreNotReported() {
        RecordingSubscriber subscriber = new RecordingSubscriber(ship(PLAYER));
        ShipAPI alreadyDead = ship(ENEMY);
        destroy(alreadyDead);
        ShipAPI retreating = ship(ENEMY);
        ShipAPI removed = ship(ENEMY);
        ShipAPI afterimage = ship(ENEMY);
        ShipVariantAPI afterimageVariant = mock(ShipVariantAPI.class);
        when(afterimageVariant.hasTag(CombatQueries.AFTERIMAGE_TAG)).thenReturn(true);
        when(afterimage.getVariant()).thenReturn(afterimageVariant);

        destructionWatch.tick();
        when(retreating.isAlive()).thenReturn(false);
        battleShips.remove(removed);
        destroy(removed);
        destroy(afterimage);
        destructionWatch.tick();

        assertEquals(List.of(), subscriber.reportedWrecks());
    }

    @Test
    void aShipSpawnedSinceTheLastCheckCountsFromTheNextOne() {
        RecordingSubscriber subscriber = new RecordingSubscriber(ship(PLAYER));
        destructionWatch.tick();
        ShipAPI lateArrival = ship(ENEMY);
        destroy(lateArrival);
        destructionWatch.tick();

        ShipAPI reinforcement = ship(ENEMY);
        destructionWatch.tick();
        destroy(reinforcement);
        destructionWatch.tick();

        assertEquals(List.of(reinforcement), subscriber.reportedWrecks());
    }

    @Test
    void onlySubscribersWhoseShipIsStillAliveInBattleHearOfIt() {
        ShipAPI survivor = ship(PLAYER);
        ShipAPI casualty = ship(PLAYER);
        ShipAPI retreated = ship(PLAYER);
        RecordingSubscriber survivorSubscriber = new RecordingSubscriber(survivor);
        RecordingSubscriber casualtySubscriber = new RecordingSubscriber(casualty);
        RecordingSubscriber retreatedSubscriber = new RecordingSubscriber(retreated);
        ShipAPI enemy = ship(ENEMY);

        destructionWatch.tick();
        destroy(casualty);
        battleShips.remove(retreated);
        destroy(enemy);
        destructionWatch.tick();

        assertEquals(List.of(casualty, enemy), survivorSubscriber.reportedWrecks());
        assertEquals(List.of(), casualtySubscriber.reportedWrecks());
        assertEquals(List.of(), retreatedSubscriber.reportedWrecks());
    }

    @Test
    void theWatchChecksEveryQuarterSecondAndNotWhilePaused() {
        new RecordingSubscriber(ship(PLAYER));
        ShipDestructionWatch watch = (ShipDestructionWatch) engine.getCustomData().get(ShipDestructionWatch.CUSTOM_DATA_KEY);

        watch.advance(ShipDestructionWatch.CHECK_INTERVAL_SECONDS / 2f, List.of());
        verify(engine, never()).getShips();
        watch.advance(ShipDestructionWatch.CHECK_INTERVAL_SECONDS / 2f, List.of());
        verify(engine, times(1)).getShips();

        when(engine.isPaused()).thenReturn(true);
        watch.advance(ShipDestructionWatch.CHECK_INTERVAL_SECONDS, List.of());
        verify(engine, times(1)).getShips();
    }

    @Test
    void oneWatchServesTheWholeBattleAndNoneStartsOutsideCombat() {
        new RecordingSubscriber(ship(PLAYER));
        new RecordingSubscriber(ship(ENEMY));
        verify(engine, times(1)).addPlugin(any(ShipDestructionWatch.class));

        engine.getCustomData().clear();
        combat.global().when(Global::getCurrentState).thenReturn(GameState.CAMPAIGN);
        new RecordingSubscriber(ship(PLAYER));
        assertFalse(engine.getCustomData().containsKey(ShipDestructionWatch.CUSTOM_DATA_KEY));
    }
}
