package exiledsector.compat;

import com.fs.starfarer.api.Global;
import org.apache.log4j.Logger;

import java.util.List;

import static exiledsector.compat.ApiMember.BOOLEAN;
import static exiledsector.compat.ApiMember.FLOAT;
import static exiledsector.compat.ApiMember.VOID;
import static exiledsector.compat.ApiMember.constructor;
import static exiledsector.compat.ApiMember.instanceMethod;
import static exiledsector.compat.ApiMember.staticMethod;

public final class GraphicsLibCompat {

    public static final String MOD_ID = "shaderLib";
    public static final List<String> TESTED_VERSIONS = List.of("1.12.1");
    public static final CompatTarget TARGET = new CompatTarget("GraphicsLib", List.of(MOD_ID), TESTED_VERSIONS,
            GraphicsLibCompat::missingFeatures);
    static final String LIGHT_DATA_PATH = "data/lights/exiledsector_lights.csv";
    private static final String LIGHT = "org.dark.shaders.light.";
    private static final String DISTORTION = "org.dark.shaders.distortion.";
    private static final String VECTOR = "org.lwjgl.util.vector.Vector2f";
    private static final String SHADER_API = "org.dark.shaders.util.ShaderAPI";
    private static final String COMBAT_ENTITY = "com.fs.starfarer.api.combat.CombatEntityAPI";
    private static final String BEAM = "com.fs.starfarer.api.combat.BeamAPI";
    static final List<ApiMember> USED_METHODS = List.of(
            staticMethod("org.dark.shaders.util.ShaderLib", "getShaderAPI", SHADER_API, "java.lang.Class"),
            constructor(LIGHT + "StandardLight", VECTOR, VECTOR, VECTOR, COMBAT_ENTITY, FLOAT, FLOAT),
            constructor(LIGHT + "StandardLight", VECTOR, VECTOR, VECTOR, VECTOR, BEAM, FLOAT, FLOAT),
            constructor(DISTORTION + "RippleDistortion", VECTOR, VECTOR),
            constructor(DISTORTION + "WaveDistortion", VECTOR, VECTOR),
            staticMethod(LIGHT + "LightShader", "addLight", VOID, LIGHT + "LightAPI"),
            instanceMethod(LIGHT + "LightShader", "isEnabled", BOOLEAN),
            staticMethod(LIGHT + "LightData", "readLightDataCSVNoOverwrite", VOID, "java.lang.String"),
            instanceMethod(LIGHT + "StandardLight", "setColor", VOID, "java.awt.Color"),
            instanceMethod(LIGHT + "StandardLight", "fadeOut", VOID, FLOAT),
            staticMethod(DISTORTION + "DistortionShader", "addDistortion", VOID, DISTORTION + "DistortionAPI"),
            instanceMethod(DISTORTION + "DistortionShader", "isEnabled", BOOLEAN),
            instanceMethod(DISTORTION + "RippleDistortion", "setSize", VOID, FLOAT),
            instanceMethod(DISTORTION + "RippleDistortion", "setIntensity", VOID, FLOAT),
            instanceMethod(DISTORTION + "RippleDistortion", "setFrameRate", VOID, FLOAT),
            instanceMethod(DISTORTION + "RippleDistortion", "fadeInSize", VOID, FLOAT),
            instanceMethod(DISTORTION + "RippleDistortion", "fadeOutSize", VOID, FLOAT),
            instanceMethod(DISTORTION + "RippleDistortion", "fadeOutIntensity", VOID, FLOAT),
            instanceMethod(DISTORTION + "RippleDistortion", "flip", VOID, BOOLEAN),
            instanceMethod(DISTORTION + "WaveDistortion", "setLocation", VOID, VECTOR),
            instanceMethod(DISTORTION + "WaveDistortion", "setSize", VOID, FLOAT),
            instanceMethod(DISTORTION + "WaveDistortion", "setIntensity", VOID, FLOAT),
            instanceMethod(DISTORTION + "WaveDistortion", "flip", VOID, BOOLEAN),
            instanceMethod(DISTORTION + "WaveDistortion", "fadeOutIntensity", VOID, FLOAT));
    private static final Logger LOG = Logger.getLogger(GraphicsLibCompat.class);

    private static boolean bridgeBroken;

    private GraphicsLibCompat() {
    }

    public static boolean isModEnabled() {
        return TARGET.isEnabled();
    }

    public static void loadLightData() {
        if (!isModEnabled()) {
            return;
        }
        try {
            GraphicsLibGlow.loadLightData(LIGHT_DATA_PATH);
        } catch (LinkageError | RuntimeException e) {
            reportBroken(e);
        }
    }

    static CombatGlow.Renderer activeRenderer() {
        if (bridgeBroken || !isModEnabled()) {
            return null;
        }
        try {
            return GraphicsLibGlow.createIfActive();
        } catch (LinkageError | RuntimeException e) {
            reportBroken(e);
            return null;
        }
    }

    static boolean isBridgeBroken() {
        return bridgeBroken;
    }

    static void resetBridgeForTests() {
        bridgeBroken = false;
    }

    static void reportBroken(Throwable cause) {
        if (!bridgeBroken) {
            LOG.warn("[ExiledSector] GraphicsLib lighting for Exiled Sector effects is off: the installed GraphicsLib didn't accept our calls.",
                    cause);
        }
        bridgeBroken = true;
    }

    static List<String> missingFeatures() {
        return missingFrom(USED_METHODS, Global.getSettings().getScriptClassLoader());
    }

    static List<String> missingFrom(List<ApiMember> usedMethods, ClassLoader loader) {
        return ApiMember.missingFrom(usedMethods, loader).stream()
                .map(used -> used.displayName() + ", used for combat lights and distortions")
                .toList();
    }
}
