package exiledlib.skills.skilleffect.aura;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuraFieldTest {

    private MockedStatic<Global> globalMock;
    private CombatEngineAPI engine;

    private static final class RecordingSource implements AuraField.Source {
        private final ShipAPI ship;

        private RecordingSource(ShipAPI ship) {
            this.ship = ship;
        }

        @Override
        public ShipAPI source() {
            return ship;
        }
    }

    private static final class RecordingField extends AuraField<RecordingSource> {
        private final List<Float> updates = new ArrayList<>();
        private int frames;

        private RecordingField(boolean updatesOnFirstFrame) {
            super(updatesOnFirstFrame);
        }

        @Override
        void everyFrame(CombatEngineAPI engine) {
            frames++;
        }

        @Override
        void update(float elapsedSeconds) {
            updates.add(elapsedSeconds);
        }
    }

    @BeforeEach
    void setUp() {
        engine = mock(CombatEngineAPI.class);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getCombatEngine).thenReturn(engine);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    private static ShipAPI liveShip() {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.isAlive()).thenReturn(true);
        return ship;
    }

    @Test
    void aFieldThatUpdatesOnTheFirstFrameRunsAtOnceAndThenEveryQuarterSecond() {
        RecordingField field = new RecordingField(true);

        field.advance(0.1f, List.of());
        field.advance(0.1f, List.of());
        field.advance(0.2f, List.of());

        assertEquals(List.of(0.35f, 0.3f), roundedUpdates(field));
        assertEquals(3, field.frames);
    }

    @Test
    void aRepairStyleFieldWaitsAQuarterSecondAndPassesTheSecondsSinceItsLastUpdate() {
        RecordingField field = new RecordingField(false);

        field.advance(0.2f, List.of());
        field.advance(0.1f, List.of());

        assertEquals(List.of(0.3f), roundedUpdates(field));
    }

    @Test
    void aPausedBattleSkipsEveryFrameWorkAndTheTimer() {
        RecordingField field = new RecordingField(true);
        when(engine.isPaused()).thenReturn(true);

        field.advance(1f, List.of());

        assertEquals(0, field.frames);
        assertEquals(List.of(), field.updates);
    }

    @Test
    void sourcesThatDiedOrLeftTheBattleArePrunedBeforeTheUpdate() {
        RecordingField field = new RecordingField(true);
        ShipAPI alive = liveShip();
        ShipAPI dead = mock(ShipAPI.class);
        ShipAPI gone = liveShip();
        when(engine.isEntityInPlay(alive)).thenReturn(true);
        when(engine.isEntityInPlay(dead)).thenReturn(true);
        RecordingSource kept = new RecordingSource(alive);
        field.sources.addAll(List.of(kept, new RecordingSource(dead), new RecordingSource(gone)));

        field.advance(0.25f, List.of());

        assertEquals(List.of(kept), field.sources);
    }

    @Test
    void appliedValuesRemoveLeaversAndReapplyOnlyWhatTheirChangeRuleAllows() {
        ShipAPI first = mock(ShipAPI.class);
        ShipAPI second = mock(ShipAPI.class);
        Map<ShipAPI, List<Float>> applied = new HashMap<>();
        List<ShipAPI> removed = new ArrayList<>();
        AppliedAuraValues exact = new AppliedAuraValues(AppliedAuraValues.Change.EXACT);
        AppliedAuraValues loose = new AppliedAuraValues(AppliedAuraValues.Change.beyond(0.01f));
        AppliedAuraValues always = new AppliedAuraValues(AppliedAuraValues.Change.ALWAYS);
        AppliedAuraValues.Apply recordApplied = (target, value) -> applied.computeIfAbsent(target, ignored -> new ArrayList<>()).add(value);
        for (AppliedAuraValues values : List.of(exact, loose, always)) {
            values.update(Map.of(first, 1f, second, 2f), removed::add, recordApplied);
            values.update(Map.of(first, 1.005f), removed::add, recordApplied);
        }

        assertEquals(List.of(second, second, second), removed);
        assertEquals(List.of(1f, 1.005f, 1f, 1f, 1.005f), applied.get(first));
    }

    private static List<Float> roundedUpdates(RecordingField field) {
        return field.updates.stream().map(seconds -> Math.round(seconds * 100f) / 100f).toList();
    }
}
