package exiledsector.compat;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import org.junit.jupiter.api.Test;
import org.dark.shaders.distortion.WaveDistortion;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GraphicsLibCompatTest {

    @Test
    void onlyTheBridgeClassTouchesGraphicsLibTypesAndTheCompatCheckOnlyNamesThem() throws IOException {
        try (Stream<Path> sources = Files.walk(Path.of("src/main/java"))) {
            List<String> mentioning = sources.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> read(path).contains("org.dark."))
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .toList();
            assertEquals(List.of("GraphicsLibCompat.java", "GraphicsLibGlow.java"), mentioning);
        }
        String compat = read(Path.of("src/main/java/exiledsector/compat/GraphicsLibCompat.java"));
        assertFalse(compat.contains("import org.dark."));
        assertFalse(compat.lines().anyMatch(line -> line.contains("org.dark.") && !line.contains("\"org.dark.")));
    }

    private static String read(Path source) {
        try {
            return Files.readString(source);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void graphicsLibIsCheckedAtStartupButNeverRequired() throws IOException {
        assertTrue(CompatChecks.targets().contains(GraphicsLibCompat.TARGET));
        assertFalse(Files.readString(Path.of("mod_info.json")).contains(GraphicsLibCompat.MOD_ID));
        assertTrue(Files.exists(Path.of(GraphicsLibCompat.LIGHT_DATA_PATH)));
    }

    @Test
    void everyGraphicsLibMethodWeCallIsInTheTestedVersionAndARenamedOneIsReported() {
        ClassLoader loader = GraphicsLibCompatTest.class.getClassLoader();
        assertEquals(List.of(), GraphicsLibCompat.missingFrom(GraphicsLibCompat.USED_METHODS, loader));

        List<String> missing = GraphicsLibCompat.missingFrom(List.of(
                ApiMember.staticMethod("org.dark.shaders.light.LightShader", "addLightRenamed", "void", "org.dark.shaders.light.LightAPI"),
                ApiMember.instanceMethod("org.dark.shaders.gone.Shader", "isEnabled", "boolean")), loader);
        assertEquals(2, missing.size());
        assertTrue(missing.get(0).startsWith("org.dark.shaders.light.LightShader.addLightRenamed"));
    }

    @Test
    void aGraphicsLibErrorMidBattleTurnsOurEffectsOffInsteadOfReachingCombatCode() {
        SettingsAPI settings = mock(SettingsAPI.class);
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        Map<String, Object> customData = new HashMap<>();
        CombatGlow.Renderer broken = mock(CombatGlow.Renderer.class);
        when(broken.lightsOn()).thenReturn(true);
        when(broken.distortionOn()).thenReturn(true);
        Mockito.doThrow(new NoSuchMethodError("addLight")).when(broken)
                .flash(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyFloat(), Mockito.anyFloat(), Mockito.anyFloat());
        customData.put(CombatGlow.CUSTOM_DATA_KEY, new CombatGlow.Battle(broken));
        when(engine.getCustomData()).thenReturn(customData);
        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            global.when(Global::getSettings).thenReturn(settings);
            global.when(Global::getCurrentState).thenReturn(GameState.COMBAT);
            global.when(Global::getCombatEngine).thenReturn(engine);

            CombatGlow.flash(new Vector2f(), null, Color.WHITE, 100f, 1f, 1f);
            CombatGlow.flash(new Vector2f(), null, Color.WHITE, 100f, 1f, 1f);
            CombatGlow.ripple(new Vector2f(), new Vector2f(), 100f, 10f, 1f, false);

            verify(broken, times(1)).flash(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyFloat(), Mockito.anyFloat(), Mockito.anyFloat());
            verify(broken, Mockito.never()).ripple(Mockito.any(), Mockito.any(), Mockito.anyFloat(), Mockito.anyFloat(), Mockito.anyFloat(),
                    Mockito.anyBoolean());
        }
    }

    @Test
    void lightSlotsRunOutAndComeBackOnceTheirLightsHaveFaded() {
        TimedSlots slots = new TimedSlots(2);

        assertTrue(slots.claim(0f, 1f));
        assertTrue(slots.claim(0.1f, 0.5f));
        assertFalse(slots.claim(0.2f, 1f));
        assertTrue(slots.claim(0.6f, 1f));
        assertFalse(slots.claim(0.9f, 1f));
        assertTrue(slots.claim(1f, 1f));
    }

    @Test
    void outsideCombatEveryEffectQuietlyDoesNothing() {
        CombatGlow.flash(new Vector2f(), null, Color.WHITE, 100f, 1f, 1f);
        CombatGlow.arc(new Vector2f(), new Vector2f(10f, 0f), Color.WHITE, 100f, 1f, 1f);
        CombatGlow.ripple(new Vector2f(), new Vector2f(), 100f, 10f, 1f, false);

        assertSame(CombatGlow.Lens.NONE, CombatGlow.lens(new Vector2f(), 100f, 10f));
    }

    @Test
    void withoutGraphicsLibEachBattleChecksForItOnceAndThenSkipsEveryEffect() {
        SettingsAPI settings = mock(SettingsAPI.class);
        ModManagerAPI modManager = mock(ModManagerAPI.class);
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        Map<String, Object> customData = new HashMap<>();
        when(settings.getModManager()).thenReturn(modManager);
        when(modManager.isModEnabled(anyString())).thenReturn(false);
        when(engine.getCustomData()).thenReturn(customData);
        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            global.when(Global::getSettings).thenReturn(settings);
            global.when(Global::getCurrentState).thenReturn(GameState.COMBAT);
            global.when(Global::getCombatEngine).thenReturn(engine);

            for (int hit = 0; hit < 5; hit++) {
                CombatGlow.flash(new Vector2f(), null, Color.WHITE, 100f, 1f, 1f);
                CombatGlow.ripple(new Vector2f(), new Vector2f(), 100f, 10f, 1f, true);
            }

            assertSame(CombatGlow.Lens.NONE, CombatGlow.lens(new Vector2f(), 100f, 10f));
            verify(modManager, times(1)).isModEnabled(GraphicsLibCompat.MOD_ID);
            verify(engine, Mockito.never()).getViewport();
        }
    }

    @Test
    void aLensWhoseGraphicsLibCallsFailTurnsGraphicsLibOffAndFadesItselfOut() {
        GraphicsLibCompat.resetBridgeForTests();
        try {
            WaveDistortion wave = mock(WaveDistortion.class);
            Mockito.doThrow(new NoSuchMethodError("setLocation")).when(wave).setLocation(Mockito.any());
            GraphicsLibGlow.WaveLens lens = new GraphicsLibGlow.WaveLens(wave);

            lens.update(new Vector2f(), 100f, 1f);
            lens.update(new Vector2f(), 100f, 1f);
            lens.fadeOut(1f);

            assertTrue(GraphicsLibCompat.isBridgeBroken());
            verify(wave, times(1)).setLocation(Mockito.any());
            verify(wave, times(1)).fadeOutIntensity(Mockito.anyFloat());
        } finally {
            GraphicsLibCompat.resetBridgeForTests();
        }
    }

    @Test
    void aLensFadesOutInsteadOfFreezingOnceAnotherCallBrokeTheBridge() {
        GraphicsLibCompat.resetBridgeForTests();
        try {
            WaveDistortion wave = mock(WaveDistortion.class);
            GraphicsLibGlow.WaveLens lens = new GraphicsLibGlow.WaveLens(wave);
            GraphicsLibCompat.reportBroken(new NoSuchMethodError("addLight"));

            lens.update(new Vector2f(), 100f, 1f);
            lens.fadeOut(1f);

            verify(wave, Mockito.never()).setLocation(Mockito.any());
            verify(wave, times(1)).fadeOutIntensity(Mockito.anyFloat());
        } finally {
            GraphicsLibCompat.resetBridgeForTests();
        }
    }

    @Test
    void aBurstFlashesAndRipplesScaledByItsRadius() {
        GraphicsLibCompat.resetBridgeForTests();
        SettingsAPI settings = mock(SettingsAPI.class);
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        Map<String, Object> customData = new HashMap<>();
        CombatGlow.Renderer renderer = mock(CombatGlow.Renderer.class);
        when(renderer.lightsOn()).thenReturn(true);
        when(renderer.distortionOn()).thenReturn(true);
        customData.put(CombatGlow.CUSTOM_DATA_KEY, new CombatGlow.Battle(renderer));
        when(engine.getCustomData()).thenReturn(customData);
        Vector2f location = new Vector2f(10f, 20f);
        Vector2f velocity = new Vector2f(1f, 0f);
        CombatGlow.Burst burst = new CombatGlow.Burst(Color.RED, 2f, 0.5f, 1.5f, 3f, 0.1f, 0.8f, true);
        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            global.when(Global::getSettings).thenReturn(settings);
            global.when(Global::getCurrentState).thenReturn(GameState.COMBAT);
            global.when(Global::getCombatEngine).thenReturn(engine);

            burst.play(location, null, velocity, 100f);
            burst.play(location, null, velocity, 100f, Color.BLUE);

            verify(renderer).flash(location, null, Color.RED, 200f, 0.5f, 1.5f);
            verify(renderer).flash(location, null, Color.BLUE, 200f, 0.5f, 1.5f);
            verify(renderer, times(2)).ripple(location, velocity, 300f, 10f, 0.8f, true);
        } finally {
            GraphicsLibCompat.resetBridgeForTests();
        }
    }
}
