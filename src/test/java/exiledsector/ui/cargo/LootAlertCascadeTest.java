package exiledsector.ui.cargo;

import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LootAlertCascadeTest {

    private static final long MS = TimeUnit.MILLISECONDS.toNanos(1);
    private static final String RARE_ID = "rare";
    private static final String UNIQUE_ID = "unique";
    private static final String PLAIN_ID = "plain";

    private final Map<String, LootAlert> alertsById = Map.of(RARE_ID, LootAlert.STANDARD, UNIQUE_ID, LootAlert.VALUABLE);
    private final Function<CargoStackAPI, LootAlert> classifier = stack -> alertsById.get(stack.getSpecialDataIfSpecial().getId());
    private final List<String> played = new ArrayList<>();

    private static CargoStackAPI stack(String itemId, String data) {
        CargoStackAPI stack = mock(CargoStackAPI.class);
        when(stack.getSpecialDataIfSpecial()).thenReturn(new SpecialItemData(itemId, data));
        return stack;
    }

    private static CargoStackAPI commodity() {
        return mock(CargoStackAPI.class);
    }

    @Test
    void slowRevealPlaysEachAlertAsItsStackAppears() {
        LootAlertCascade cascade = LootAlertCascade.of(List.of(commodity(), stack(RARE_ID, "a"), stack(UNIQUE_ID, "b")), classifier, 0);
        List<CargoStackAPI> shown = new ArrayList<>();

        cascade.advance(shown, 100 * MS, played::add);
        assertTrue(played.isEmpty());

        shown.add(commodity());
        cascade.advance(shown, 2200 * MS, played::add);
        shown.add(stack(RARE_ID, "a"));
        cascade.advance(shown, 2500 * MS, played::add);
        assertEquals(List.of(LootAlert.STANDARD.soundId()), played);

        shown.add(stack(UNIQUE_ID, "b"));
        cascade.advance(shown, 2800 * MS, played::add);
        assertEquals(List.of(LootAlert.STANDARD.soundId(), LootAlert.VALUABLE.soundId()), played);
        assertTrue(cascade.finished(2800 * MS));
    }

    @Test
    void skippedRevealCascadesAlertsTwoHundredMillisecondsApart() {
        LootAlertCascade cascade = LootAlertCascade.of(List.of(stack(RARE_ID, "a"), stack(UNIQUE_ID, "b"), stack(RARE_ID, "c")), classifier, 0);
        List<CargoStackAPI> shown = List.of(stack(RARE_ID, "a"), stack(UNIQUE_ID, "b"), stack(RARE_ID, "c"));

        cascade.advance(shown, 1000 * MS, played::add);
        assertEquals(1, played.size());
        cascade.advance(shown, 1100 * MS, played::add);
        cascade.advance(shown, 1199 * MS, played::add);
        assertEquals(1, played.size());
        cascade.advance(shown, 1200 * MS, played::add);
        assertEquals(2, played.size());
        assertFalse(cascade.finished(1200 * MS));
        cascade.advance(shown, 1400 * MS, played::add);

        assertEquals(List.of(LootAlert.STANDARD.soundId(), LootAlert.VALUABLE.soundId(), LootAlert.STANDARD.soundId()), played);
        assertTrue(cascade.finished(1400 * MS));
    }

    @Test
    void everyInstanceInAStackPlaysItsOwnAlert() {
        CargoStackAPI threeTuning = stack(UNIQUE_ID, "a");
        when(threeTuning.getSize()).thenReturn(3f);
        LootAlertCascade cascade = LootAlertCascade.of(List.of(threeTuning), classifier, 0);
        List<CargoStackAPI> shown = List.of(stack(UNIQUE_ID, "a"));

        for (long step = 0; step < 5; step++) {
            cascade.advance(shown, 1000 * MS + step * LootAlertCascade.SPACING_NANOS, played::add);
        }

        assertEquals(List.of(LootAlert.VALUABLE.soundId(), LootAlert.VALUABLE.soundId(), LootAlert.VALUABLE.soundId()), played);
    }

    @Test
    void stacksAddedAfterTheLootWasShownStaySilent() {
        LootAlertCascade cascade = LootAlertCascade.of(List.of(stack(RARE_ID, "a")), classifier, 0);

        cascade.advance(List.of(stack(RARE_ID, "other"), stack(UNIQUE_ID, "b")), 2500 * MS, played::add);

        assertTrue(played.isEmpty());
        assertFalse(cascade.finished(2500 * MS));
        assertTrue(cascade.finished(2500 * MS + LootAlertCascade.IDLE_TIMEOUT_NANOS + 1));
    }

    @Test
    void longCommodityRevealsKeepTheWatcherWaitingForLaterSpecials() {
        LootAlertCascade cascade = LootAlertCascade.of(List.of(stack(UNIQUE_ID, "b")), classifier, 0);
        List<CargoStackAPI> shown = new ArrayList<>();
        long nowNanos = 0;

        for (int commodityStack = 0; commodityStack < 4; commodityStack++) {
            nowNanos += LootAlertCascade.IDLE_TIMEOUT_NANOS / 2;
            shown.add(commodity());
            cascade.advance(shown, nowNanos, played::add);
            assertFalse(cascade.finished(nowNanos));
        }
        shown.add(stack(UNIQUE_ID, "b"));
        cascade.advance(shown, nowNanos + MS, played::add);

        assertEquals(List.of(LootAlert.VALUABLE.soundId()), played);
    }

    @Test
    void lootWithoutAlertsHasNothingToWatch() {
        LootAlertCascade cascade = LootAlertCascade.of(List.of(commodity(), stack(PLAIN_ID, "a")), classifier, 0);

        assertFalse(cascade.hasAlerts());
    }
}
