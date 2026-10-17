package exiledsector.compat;

import com.fs.starfarer.api.Global;
import org.apache.log4j.Logger;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;

public final class GraphicsLibCompat {

    public static final String MOD_ID = "shaderLib";
    public static final List<String> TESTED_VERSIONS = List.of("1.12.1");
    public static final CompatTarget TARGET = new CompatTarget("GraphicsLib", List.of(MOD_ID), TESTED_VERSIONS,
            GraphicsLibCompat::missingFeatures);
    static final String LIGHT_DATA_PATH = "data/lights/exiledsector_lights.csv";
    private static final String LIGHT = "org.dark.shaders.light.";
    private static final String DISTORTION = "org.dark.shaders.distortion.";
    private static final String VECTOR = "org.lwjgl.util.vector.Vector2f";
    private static final String VOID = "void";
    private static final String BOOLEAN = "boolean";
    private static final String FLOAT = "float";
    private static final String SHADER_API = "org.dark.shaders.util.ShaderAPI";
    static final List<UsedMethod> USED_METHODS = List.of(
            staticMethod("org.dark.shaders.util.ShaderLib", "getShaderAPI", SHADER_API, "java.lang.Class"),
            staticMethod(LIGHT + "LightShader", "addLight", VOID, LIGHT + "LightAPI"),
            instanceMethod(LIGHT + "LightShader", "isEnabled", BOOLEAN),
            staticMethod(LIGHT + "LightData", "readLightDataCSVNoOverwrite", VOID, "java.lang.String"),
            instanceMethod(LIGHT + "StandardLight", "setColor", VOID, "java.awt.Color"),
            instanceMethod(LIGHT + "StandardLight", "fadeOut", VOID, FLOAT),
            staticMethod(DISTORTION + "DistortionShader", "addDistortion", VOID, DISTORTION + "DistortionAPI"),
            instanceMethod(DISTORTION + "DistortionShader", "isEnabled", BOOLEAN),
            instanceMethod(DISTORTION + "RippleDistortion", "setFrameRate", VOID, FLOAT),
            instanceMethod(DISTORTION + "RippleDistortion", "fadeInSize", VOID, FLOAT),
            instanceMethod(DISTORTION + "RippleDistortion", "fadeOutSize", VOID, FLOAT),
            instanceMethod(DISTORTION + "RippleDistortion", "fadeOutIntensity", VOID, FLOAT),
            instanceMethod(DISTORTION + "RippleDistortion", "flip", VOID, BOOLEAN),
            instanceMethod(DISTORTION + "WaveDistortion", "setLocation", VOID, VECTOR),
            instanceMethod(DISTORTION + "WaveDistortion", "setSize", VOID, FLOAT),
            instanceMethod(DISTORTION + "WaveDistortion", "setIntensity", VOID, FLOAT),
            instanceMethod(DISTORTION + "WaveDistortion", "fadeOutIntensity", VOID, FLOAT));
    private static final Logger LOG = Logger.getLogger(GraphicsLibCompat.class);

    private static boolean bridgeBroken;

    record UsedMethod(boolean isStatic, String className, String methodName, String returnClassName, List<String> parameterClassNames) {
    }

    static UsedMethod staticMethod(String className, String methodName, String returnClassName, String... parameterClassNames) {
        return new UsedMethod(true, className, methodName, returnClassName, List.of(parameterClassNames));
    }

    static UsedMethod instanceMethod(String className, String methodName, String returnClassName, String... parameterClassNames) {
        return new UsedMethod(false, className, methodName, returnClassName, List.of(parameterClassNames));
    }

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

    static List<String> missingFrom(List<UsedMethod> usedMethods, ClassLoader loader) {
        List<String> missing = new ArrayList<>();
        for (UsedMethod used : usedMethods) {
            try {
                Class<?>[] parameterTypes = new Class<?>[used.parameterClassNames().size()];
                for (int index = 0; index < parameterTypes.length; index++) {
                    parameterTypes[index] = typeNamed(used.parameterClassNames().get(index), loader);
                }
                MethodType type = MethodType.methodType(typeNamed(used.returnClassName(), loader), parameterTypes);
                Class<?> owner = typeNamed(used.className(), loader);
                if (used.isStatic()) {
                    MethodHandles.publicLookup().findStatic(owner, used.methodName(), type);
                } else {
                    MethodHandles.publicLookup().findVirtual(owner, used.methodName(), type);
                }
            } catch (ReflectiveOperationException | LinkageError e) {
                missing.add(used.className() + "." + used.methodName() + ", used for combat lights and distortions");
            }
        }
        return missing;
    }

    private static Class<?> typeNamed(String name, ClassLoader loader) throws ClassNotFoundException {
        return switch (name) {
            case VOID -> void.class;
            case FLOAT -> float.class;
            case BOOLEAN -> boolean.class;
            default -> Class.forName(name, false, loader);
        };
    }
}
