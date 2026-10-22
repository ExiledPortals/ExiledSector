package exiledlib.compat;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.dark.shaders.distortion.WaveDistortion;
import org.dark.shaders.light.LightData;
import org.dark.shaders.light.LightShader;
import org.dark.shaders.light.StandardLight;
import org.dark.shaders.util.ShaderLib;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

public final class GraphicsLibGlow implements CombatGlow.Renderer {

    private static final Vector2f ZERO = new Vector2f();
    private static final float RIPPLE_FRAMES = 60f;
    private static final float RIPPLE_START_SIZE_SHARE = 0.25f;

    private final boolean lightsOn;
    private final boolean distortionOn;

    private GraphicsLibGlow(boolean lightsOn, boolean distortionOn) {
        this.lightsOn = lightsOn;
        this.distortionOn = distortionOn;
    }

    static CombatGlow.Renderer createIfActive() {
        boolean lights = ShaderLib.getShaderAPI(LightShader.class) instanceof LightShader lightShader && lightShader.isEnabled();
        boolean distortion = ShaderLib.getShaderAPI(DistortionShader.class) instanceof DistortionShader distortionShader
                && distortionShader.isEnabled();
        return lights || distortion ? new GraphicsLibGlow(lights, distortion) : null;
    }

    static void loadLightData(String path) {
        LightData.readLightDataCSVNoOverwrite(path);
    }

    @Override
    public boolean lightsOn() {
        return lightsOn;
    }

    @Override
    public boolean distortionOn() {
        return distortionOn;
    }

    @Override
    public void flash(Vector2f location, CombatEntityAPI attachedTo, Color color, float size, float intensity, float fadeSeconds) {
        StandardLight light = new StandardLight(location, ZERO, ZERO, attachedTo, intensity, size);
        light.setColor(color);
        light.fadeOut(fadeSeconds);
        LightShader.addLight(light);
    }

    @Override
    public void arc(Vector2f from, Vector2f to, Color color, float size, float intensity, float fadeSeconds) {
        StandardLight light = new StandardLight(from, to, ZERO, ZERO, null, intensity, size);
        light.setColor(color);
        light.fadeOut(fadeSeconds);
        LightShader.addLight(light);
    }

    @Override
    public void ripple(Vector2f location, Vector2f velocity, float size, float intensity, float seconds, boolean inward) {
        RippleDistortion ripple = new RippleDistortion(location, velocity);
        ripple.setSize(size);
        ripple.setIntensity(intensity);
        ripple.setFrameRate(RIPPLE_FRAMES / seconds);
        if (inward) {
            ripple.flip(true);
            ripple.fadeOutSize(seconds);
        } else {
            ripple.fadeInSize(seconds);
            ripple.fadeOutIntensity(seconds);
            ripple.setSize(size * RIPPLE_START_SIZE_SHARE);
        }
        DistortionShader.addDistortion(ripple);
    }

    @Override
    public CombatGlow.Lens lens(Vector2f location, float size, float intensity) {
        WaveDistortion wave = new WaveDistortion(location, ZERO);
        wave.setSize(size);
        wave.setIntensity(Math.max(WaveLens.MIN_INTENSITY, intensity));
        wave.flip(true);
        DistortionShader.addDistortion(wave);
        return new WaveLens(wave);
    }

    public static final class WaveLens implements CombatGlow.Lens {

        private static final float MIN_INTENSITY = 0.01f;
        private static final float GIVE_UP_FADE_SECONDS = 0.5f;

        private final WaveDistortion wave;
        private boolean fading;

        public WaveLens(WaveDistortion wave) {
            this.wave = wave;
        }

        @Override
        public void update(Vector2f location, float size, float intensity) {
            if (fading) {
                return;
            }
            if (GraphicsLibCompat.isBridgeBroken()) {
                fadeOut(GIVE_UP_FADE_SECONDS);
                return;
            }
            try {
                wave.setLocation(location);
                wave.setSize(size);
                wave.setIntensity(Math.max(MIN_INTENSITY, intensity));
            } catch (LinkageError | RuntimeException e) {
                GraphicsLibCompat.reportBroken(e);
                fadeOut(GIVE_UP_FADE_SECONDS);
            }
        }

        @Override
        public void fadeOut(float seconds) {
            if (fading) {
                return;
            }
            fading = true;
            try {
                wave.fadeOutIntensity(seconds);
            } catch (LinkageError | RuntimeException e) {
                GraphicsLibCompat.reportBroken(e);
            }
        }
    }
}
