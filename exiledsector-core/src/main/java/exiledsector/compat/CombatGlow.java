package exiledsector.compat;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import exiledsector.ExiledSector;
import exiledsector.ModSettings;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

public final class CombatGlow {

    public static final boolean DEFAULT_ENABLED = true;
    static final int MAX_LIVE_LIGHTS = 20;
    static final int MAX_LIVE_RIPPLES = 6;
    public static final String CUSTOM_DATA_KEY = "exiledLib_combatGlow";

    public interface Lens {
        Lens NONE = new Lens() {
            @Override
            public void update(Vector2f location, float size, float intensity) {
            }

            @Override
            public void fadeOut(float seconds) {
            }
        };

        void update(Vector2f location, float size, float intensity);

        void fadeOut(float seconds);
    }

    public record Burst(Color lightColor, float lightSizeShare, float lightIntensity, float lightSeconds, float rippleSizeShare,
                        float rippleIntensityShare, float rippleSeconds, boolean inward) {

        public void play(Vector2f location, CombatEntityAPI attachedTo, Vector2f rippleVelocity, float radius) {
            play(location, attachedTo, rippleVelocity, radius, lightColor);
        }

        public void play(Vector2f location, CombatEntityAPI attachedTo, Vector2f rippleVelocity, float radius, Color color) {
            flash(location, attachedTo, color, radius * lightSizeShare, lightIntensity, lightSeconds);
            ripple(location, rippleVelocity, radius * rippleSizeShare, radius * rippleIntensityShare, rippleSeconds, inward);
        }
    }

    public interface Renderer {
        boolean lightsOn();

        boolean distortionOn();

        void flash(Vector2f location, CombatEntityAPI attachedTo, Color color, float size, float intensity, float fadeSeconds);

        void arc(Vector2f from, Vector2f to, Color color, float size, float intensity, float fadeSeconds);

        void ripple(Vector2f location, Vector2f velocity, float size, float intensity, float seconds, boolean inward);

        Lens lens(Vector2f location, float size, float intensity);
    }

    public static final class Battle {
        private Renderer renderer;
        private final TimedSlots lights = new TimedSlots(MAX_LIVE_LIGHTS);
        private final TimedSlots ripples = new TimedSlots(MAX_LIVE_RIPPLES);

        public Battle(Renderer renderer) {
            this.renderer = renderer;
        }

        private void giveUp(Throwable cause) {
            renderer = null;
            GraphicsLibCompat.reportBroken(cause);
        }
    }

    private CombatGlow() {
    }

    public static String enabledFieldId() {
        return ExiledSector.key("graphicsLibEffects");
    }

    public static void flash(Vector2f location, CombatEntityAPI attachedTo, Color color, float size, float intensity, float fadeSeconds) {
        CombatEngineAPI engine = combatEngine();
        Battle battle = battle(engine);
        if (battle == null || !battle.renderer.lightsOn() || !nearView(engine, location, size)
                || !battle.lights.claim(engine.getTotalElapsedTime(false), fadeSeconds)) {
            return;
        }
        try {
            battle.renderer.flash(location, attachedTo, color, size, intensity, fadeSeconds);
        } catch (LinkageError | RuntimeException e) {
            battle.giveUp(e);
        }
    }

    public static void arc(Vector2f from, Vector2f to, Color color, float size, float intensity, float fadeSeconds) {
        CombatEngineAPI engine = combatEngine();
        Battle battle = battle(engine);
        if (battle == null || !battle.renderer.lightsOn() || !nearView(engine, from, size) && !nearView(engine, to, size)
                || !battle.lights.claim(engine.getTotalElapsedTime(false), fadeSeconds)) {
            return;
        }
        try {
            battle.renderer.arc(from, to, color, size, intensity, fadeSeconds);
        } catch (LinkageError | RuntimeException e) {
            battle.giveUp(e);
        }
    }

    public static void ripple(Vector2f location, Vector2f velocity, float size, float intensity, float seconds, boolean inward) {
        CombatEngineAPI engine = combatEngine();
        Battle battle = battle(engine);
        if (battle == null || !battle.renderer.distortionOn() || !nearView(engine, location, size)
                || !battle.ripples.claim(engine.getTotalElapsedTime(false), seconds)) {
            return;
        }
        try {
            battle.renderer.ripple(location, velocity, size, intensity, seconds, inward);
        } catch (LinkageError | RuntimeException e) {
            battle.giveUp(e);
        }
    }

    public static Lens lens(Vector2f location, float size, float intensity) {
        Battle battle = battle(combatEngine());
        if (battle == null || !battle.renderer.distortionOn()) {
            return Lens.NONE;
        }
        try {
            return battle.renderer.lens(location, size, intensity);
        } catch (LinkageError | RuntimeException e) {
            battle.giveUp(e);
            return Lens.NONE;
        }
    }

    private static CombatEngineAPI combatEngine() {
        if (Global.getSettings() == null || Global.getCurrentState() != GameState.COMBAT) {
            return null;
        }
        return Global.getCombatEngine();
    }

    private static Battle battle(CombatEngineAPI engine) {
        if (engine == null) {
            return null;
        }
        Object stored = engine.getCustomData().get(CUSTOM_DATA_KEY);
        if (stored instanceof Battle battle) {
            return battle.renderer == null || GraphicsLibCompat.isBridgeBroken() ? null : battle;
        }
        boolean wanted = GraphicsLibCompat.isModEnabled() && ModSettings.booleanOr(enabledFieldId(), DEFAULT_ENABLED);
        Battle battle = new Battle(wanted ? GraphicsLibCompat.activeRenderer() : null);
        engine.getCustomData().put(CUSTOM_DATA_KEY, battle);
        return battle.renderer == null ? null : battle;
    }

    private static boolean nearView(CombatEngineAPI engine, Vector2f location, float radius) {
        ViewportAPI viewport = engine.getViewport();
        return viewport == null || viewport.isNearViewport(location, radius);
    }
}
