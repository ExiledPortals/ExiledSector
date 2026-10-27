package exiledsector.charts.anomaly.convoy;

import exiledsector.charts.ChartSaveAliasesView;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConvoyPersistenceTest {

    private static List<String> savedFields(Class<?> type) {
        List<String> saved = new ArrayList<>();
        for (Field field : type.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) && !Modifier.isTransient(field.getModifiers())) {
                saved.add(field.getName() + ": " + field.getType().getSimpleName());
            }
        }
        return saved;
    }

    @Test
    void theConvoyIntelSavesOnlyItsRunAndConvoySoItNeverHoldsTheSystem() {
        assertEquals(List.of("runId: String", "convoyIndex: int"), savedFields(ConvoyIntel.class));
    }

    @Test
    void everyPersistedConvoyClassHasAStableAlias() {
        for (Class<?> type : List.of(ConvoyScript.class, ConvoyState.class, ConvoyState.Phase.class, ConvoyRole.class, ConvoyStep.class,
                ConvoyStep.Kind.class, ConvoyIntel.class)) {
            assertTrue(ChartSaveAliasesView.aliased(type), type.getName());
        }
    }
}
