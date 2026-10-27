package exiledsector.charts.layout.abyss;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

class AbyssSaveFieldsTest {

    @Test
    void savedPluginFieldsHoldNoRecordsBecauseTheGamesXStreamCannotRebuildThem() {
        for (Class<?> saved : List.of(ChartPhotospherePlugin.class, AbyssalFogTerrainPlugin.class, AbyssalDarknessTerrainPlugin.class)) {
            for (Field field : saved.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) {
                    continue;
                }
                assertFalse(field.getType().isRecord(), saved.getSimpleName() + "." + field.getName());
            }
        }
    }
}
