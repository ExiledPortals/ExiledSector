package exiledsector.charts;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.BuffManagerAPI.Buff;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignTerrainPlugin;
import com.fs.starfarer.api.campaign.CustomCampaignEntityPlugin;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.characters.AbilityPlugin;
import com.thoughtworks.xstream.XStream;
import exiledsector.charts.anomaly.breach.BreachCorePlugin;
import exiledsector.charts.anomaly.breach.BreachSupplyBuff;
import exiledsector.charts.anomaly.breach.BreachTerrainPlugin;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.objective.ChartObjectiveIntel;
import exiledsector.charts.run.ChartContentListener;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunState;
import exiledsector.charts.wormhole.ChartWormholePlugin;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ChartPersistenceGuardTest {

    private static final List<Class<?>> RUN_RECORD_CLASSES = List.of(ChartRun.class, ChartRunState.class);
    private static final List<Class<?>> SAVED_ROLES = List.of(EveryFrameScript.class, CustomCampaignEntityPlugin.class,
            CampaignTerrainPlugin.class, Buff.class, IntelInfoPlugin.class, AbilityPlugin.class, Industry.class, FleetEventListener.class);
    private static final List<Class<?>> SECTOR_DATA_RECORDS = List.of(ChartRun.class, AtlasState.class);
    private static final Map<String, String> NEVER_SAVED = Map.of(
            "exiledsector.charts.run.ChartRunScript", "added as a transient sector script",
            "exiledsector.charts.run.ChartContentListener", "registered as a transient listener",
            "exiledsector.charts.run.ChartTransverseJumpAbility", "swapped back to the vanilla ability before every save",
            "exiledsector.charts.run.diagnostics.ChartSoakScript", "added as a transient sector script",
            "exiledsector.charts.objective.GatewayUpgradeNotice", "added as a transient sector script",
            "exiledsector.charts.wormhole.LimboBlueprintCache$Watch", "added as a transient sector script");
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
    void everySavedClassOfTheModuleHasAStableAlias() throws IOException, URISyntaxException {
        List<String> missing = new ArrayList<>();
        Set<Class<?>> visited = new HashSet<>();
        SECTOR_DATA_RECORDS.forEach(record -> collectUnaliased(record, visited, missing));
        for (Class<?> type : moduleClasses()) {
            if (isSaved(type)) {
                collectUnaliased(type, visited, missing);
            }
        }
        assertEquals(List.of(), missing);
        ChartSaveAliases.ALIASES.keySet().forEach(alias -> assertTrue(alias.startsWith("exiledSector."), alias));
    }

    @Test
    void everyClassListedAsNeverSavedStillExists() throws IOException, URISyntaxException {
        Set<String> names = new HashSet<>();
        moduleClasses().forEach(type -> names.add(type.getName()));
        NEVER_SAVED.keySet().forEach(name -> assertTrue(names.contains(name), name));
    }

    private static boolean isSaved(Class<?> type) {
        return !type.isInterface() && !Modifier.isAbstract(type.getModifiers()) && !NEVER_SAVED.containsKey(type.getName())
                && SAVED_ROLES.stream().anyMatch(role -> role.isAssignableFrom(type));
    }

    private static void collectUnaliased(Class<?> type, Set<Class<?>> visited, List<String> missing) {
        if (!type.getName().startsWith("exiledsector.") || !visited.add(type)) {
            return;
        }
        if (!ChartSaveAliases.ALIASES.containsValue(type)) {
            missing.add(type.getName());
        }
        for (Class<?> declaring = type; declaring != null && declaring.getName().startsWith("exiledsector."); declaring = declaring.getSuperclass()) {
            for (Field field : declaring.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) && !Modifier.isTransient(field.getModifiers())) {
                    savedTypesOf(field.getGenericType()).forEach(saved -> collectUnaliased(saved, visited, missing));
                }
            }
        }
    }

    private static List<Class<?>> savedTypesOf(Type type) {
        List<Class<?>> types = new ArrayList<>();
        if (type instanceof Class<?> plain) {
            types.add(plain.isArray() ? plain.getComponentType() : plain);
        } else if (type instanceof ParameterizedType parameterized) {
            types.addAll(savedTypesOf(parameterized.getRawType()));
            for (Type argument : parameterized.getActualTypeArguments()) {
                types.addAll(savedTypesOf(argument));
            }
        } else if (type instanceof GenericArrayType array) {
            types.addAll(savedTypesOf(array.getGenericComponentType()));
        }
        return types;
    }

    private static List<Class<?>> moduleClasses() throws IOException, URISyntaxException {
        Path classesRoot = Path.of(ChartSaveAliases.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        assertTrue(Files.isDirectory(classesRoot), classesRoot.toString());
        List<Class<?>> classes = new ArrayList<>();
        try (Stream<Path> files = Files.walk(classesRoot)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".class")).toList()) {
                String relative = classesRoot.relativize(file).toString().replace(file.getFileSystem().getSeparator(), ".");
                classes.add(load(relative.substring(0, relative.length() - ".class".length())));
            }
        }
        return classes;
    }

    private static Class<?> load(String className) {
        try {
            return Class.forName(className, false, ChartPersistenceGuardTest.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(className, e);
        }
    }

    @Test
    void theAtlasStateStoresOnlyJdkTypesSoItsAliasCoversEverythingItSaves() {
        List<String> offending = new ArrayList<>();
        for (Field field : AtlasState.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) {
                continue;
            }
            Class<?> type = field.getType();
            if (!type.isPrimitive() && !type.getName().startsWith("java.")) {
                offending.add(field.getName() + ": " + type.getName());
            }
        }
        assertEquals(List.of(), offending);
    }

    @Test
    void registersEveryAliasWithXStream() {
        XStream xstream = mock(XStream.class);

        ChartSaveAliases.register(xstream);

        ChartSaveAliases.ALIASES.forEach((alias, type) -> verify(xstream).alias(alias, type));
    }

    @Test
    void readsMovedClassesUnderTheirOldNamesButWritesTheStableAlias() {
        XStream xstream = mock(XStream.class);
        InOrder order = inOrder(xstream);

        ChartSaveAliases.register(xstream);

        ChartSaveAliases.MOVED.forEach((oldName, type) -> {
            assertTrue(ChartSaveAliases.ALIASES.containsValue(type), type.getName());
            order.verify(xstream).alias(oldName, type);
        });
        ChartSaveAliases.ALIASES.forEach((alias, type) -> {
            if (ChartSaveAliases.MOVED.containsValue(type)) {
                order.verify(xstream).alias(alias, type);
            }
        });
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
