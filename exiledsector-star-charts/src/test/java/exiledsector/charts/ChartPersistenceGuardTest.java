package exiledsector.charts;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.thoughtworks.xstream.XStream;
import exiledsector.charts.expansion.BreachCorePlugin;
import exiledsector.charts.expansion.BreachSupplyBuff;
import exiledsector.charts.expansion.BreachTerrainPlugin;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ChartPersistenceGuardTest {

    private static final List<Class<?>> RUN_RECORD_CLASSES = List.of(ChartRun.class, ChartRunState.class);
    private static final Set<Class<?>> FORBIDDEN_TYPES = Set.of(StarSystemAPI.class, SectorEntityToken.class, CampaignFleetAPI.class,
            LocationAPI.class);

    @Test
    void theRunRecordStoresOnlyStringsNumbersAndItsStateNeverEngineObjects() {
        List<String> offending = new ArrayList<>();
        for (Class<?> recordClass : RUN_RECORD_CLASSES) {
            for (Field field : recordClass.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) {
                    continue;
                }
                Class<?> type = field.getType();
                boolean forbidden = FORBIDDEN_TYPES.stream().anyMatch(engineType -> engineType.isAssignableFrom(type) || type.isAssignableFrom(engineType));
                boolean allowed = type == String.class || type.isPrimitive() || type == ChartRunState.class
                        || recordClass.isEnum() && type == recordClass;
                if (forbidden || !allowed) {
                    offending.add(recordClass.getSimpleName() + "." + field.getName() + ": " + type.getName());
                }
            }
        }
        assertEquals(List.of(), offending);
    }

    @Test
    void everyPersistedChartClassHasAStableAlias() {
        assertTrue(ChartSaveAliases.ALIASES.containsValue(ChartRun.class));
        assertTrue(ChartSaveAliases.ALIASES.containsValue(ChartRunState.class));
        assertTrue(ChartSaveAliases.ALIASES.containsValue(ChartWormholePlugin.class));
        assertTrue(ChartSaveAliases.ALIASES.containsValue(ChartObjectiveIntel.class));
        assertTrue(ChartSaveAliases.ALIASES.containsValue(WormholeGateway.class));
        assertTrue(ChartSaveAliases.ALIASES.containsValue(BreachTerrainPlugin.class));
        assertTrue(ChartSaveAliases.ALIASES.containsValue(BreachCorePlugin.class));
        assertTrue(ChartSaveAliases.ALIASES.containsValue(BreachSupplyBuff.class));
        ChartSaveAliases.ALIASES.keySet().forEach(alias -> assertTrue(alias.startsWith("exiledSector."), alias));
    }

    @Test
    void registersEveryAliasWithXStream() {
        XStream xstream = mock(XStream.class);

        ChartSaveAliases.register(xstream);

        ChartSaveAliases.ALIASES.forEach((alias, type) -> verify(xstream).alias(alias, type));
    }

    @Test
    void theObjectiveIntelSavesOnlyItsRunIdSoItNeverHoldsTheSystem() {
        List<String> saved = new ArrayList<>();
        for (Field field : ChartObjectiveIntel.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) && !Modifier.isTransient(field.getModifiers())) {
                saved.add(field.getName() + ": " + field.getType().getSimpleName());
            }
        }
        assertEquals(List.of("runId: String"), saved);
    }

    @Test
    void theContentListenerIsNeverSaved() {
        for (Field field : ChartContentListener.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                assertTrue(field.getType().isPrimitive(), field.getName());
            }
        }
    }

    @Test
    void theBreachSavesOnlyNumbersAndTextAndKeepsItsSpritesOutOfTheSave() {
        for (Field field : BreachSupplyBuff.class.getDeclaredFields()) {
            assertTrue(field.getType().isPrimitive() || field.getType() == String.class, field.getName() + ": " + field.getType().getName());
        }
        for (Field field : BreachTerrainPlugin.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) {
                continue;
            }
            Class<?> type = field.getType().isArray() ? field.getType().getComponentType() : field.getType();
            assertTrue(type.isPrimitive() || type == String.class, field.getName() + ": " + field.getType().getName());
        }
        for (Field field : BreachCorePlugin.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                assertTrue(Modifier.isTransient(field.getModifiers()), field.getName() + " would be written to the save");
            }
        }
    }

    @Test
    void theWormholePluginKeepsItsSpritesAndAnimationOutOfTheSave() {
        for (Field field : ChartWormholePlugin.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                assertTrue(Modifier.isTransient(field.getModifiers()), field.getName() + " would be written to the save");
            }
        }
    }
}
