package exiledsector.ui.refit;

import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class UiReflection {

    private static final Object NONE = new Object();
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final Class<?> METHOD = reflectionClass("Method");
    private static final Class<?> FIELD = reflectionClass("Field");
    private static final MethodHandle GET_METHODS = virtual(Class.class, "getMethods", METHOD.arrayType());
    private static final MethodHandle GET_DECLARED_FIELDS = virtual(Class.class, "getDeclaredFields", FIELD.arrayType());
    private static final MethodHandle METHOD_NAME = virtual(METHOD, "getName", String.class);
    private static final MethodHandle METHOD_PARAMETER_COUNT = virtual(METHOD, "getParameterCount", int.class);
    private static final MethodHandle METHOD_RETURN_TYPE = virtual(METHOD, "getReturnType", Class.class);
    private static final MethodHandle METHOD_INVOKE = virtual(METHOD, "invoke", Object.class, Object.class, Object[].class);
    private static final MethodHandle FIELD_TYPE = virtual(FIELD, "getType", Class.class);
    private static final MethodHandle FIELD_NAME = virtual(FIELD, "getName", String.class);
    private static final MethodHandle METHOD_TRY_SET_ACCESSIBLE = virtual(METHOD, "trySetAccessible", boolean.class);
    private static final MethodHandle FIELD_TRY_SET_ACCESSIBLE = virtual(FIELD, "trySetAccessible", boolean.class);
    private static final MethodHandle FIELD_GET = virtual(FIELD, "get", Object.class, Object.class);
    private static final MethodHandle FIELD_MODIFIERS = virtual(FIELD, "getModifiers", int.class);
    private static final int STATIC_MODIFIER = 0x0008;

    private static final int MAX_PARAMETER_COUNT = 3;
    private static final Object[] NO_ARGS = new Object[0];
    private static final Map<Class<?>, Map<String, Object[]>> METHODS = new HashMap<>();
    private static final Map<Class<?>, Map<Class<?>, Object>> FIELDS = new HashMap<>();
    private static final Map<Class<?>, Map<String, Object>> FIELDS_BY_NAME = new HashMap<>();
    private static final Map<Class<?>, Map<Class<?>, Object[]>> METHODS_BY_RETURN_TYPE = new HashMap<>();

    private UiReflection() {
    }

    public static Object call(Object target, String name) throws Throwable {
        return call(target, name, NO_ARGS);
    }

    public static Object coreUI(CampaignUIAPI campaignUI) throws Throwable {
        InteractionDialogAPI dialog = campaignUI.getCurrentInteractionDialog();
        Object dialogCore = call(dialog, "getCoreUI");
        return dialogCore != null ? dialogCore : call(campaignUI, "getCore");
    }

    public static <T> T callReturning(Object target, Class<T> returnType, Object... args) throws Throwable {
        if (target == null) return null;
        Object method = methodReturning(target.getClass(), returnType, args.length);
        if (method == NONE) return null;
        Object value = METHOD_INVOKE.invoke(method, target, args);
        return returnType.isInstance(value) ? returnType.cast(value) : null;
    }

    static Object call(Object target, String name, Object... args) throws Throwable {
        if (target == null) return null;
        Object method = method(target.getClass(), name, args.length);
        return method == NONE ? null : METHOD_INVOKE.invoke(method, target, args);
    }

    static boolean hasMethod(Object target, String name, int parameterCount) throws Throwable {
        return target != null && method(target.getClass(), name, parameterCount) != NONE;
    }

    public static List<?> children(Object panel) throws Throwable {
        return call(panel, "getChildrenNonCopy") instanceof List<?> children ? children : List.of();
    }

    public static Object childWithMethod(Object panel, String name, int parameterCount) throws Throwable {
        for (Object child : children(panel)) {
            if (hasMethod(child, name, parameterCount)) {
                return child;
            }
        }
        return null;
    }

    static <T> T fieldOfType(Object target, Class<T> type) throws Throwable {
        if (target == null) return null;
        Object field = field(target.getClass(), type);
        if (field == NONE) return null;
        Object value = FIELD_GET.invoke(field, target);
        return type.isInstance(value) ? type.cast(value) : null;
    }

    public static <T> T fieldNamed(Object target, String name, Class<T> type) throws Throwable {
        if (target == null) return null;
        Object field = namedField(target.getClass(), name);
        if (field == NONE) return null;
        Object value = FIELD_GET.invoke(field, target);
        return type.isInstance(value) ? type.cast(value) : null;
    }

    static float[] declaredFloats(Object target) throws Throwable {
        if (target == null) return new float[0];
        List<Float> values = new ArrayList<>();
        for (Object candidate : (Object[]) GET_DECLARED_FIELDS.invoke(target.getClass())) {
            if (FIELD_TYPE.invoke(candidate) == float.class && ((int) FIELD_MODIFIERS.invoke(candidate) & STATIC_MODIFIER) == 0
                    && (boolean) FIELD_TRY_SET_ACCESSIBLE.invoke(candidate)
                    && FIELD_GET.invoke(candidate, target) instanceof Float value) {
                values.add(value);
            }
        }
        float[] floats = new float[values.size()];
        for (int index = 0; index < floats.length; index++) {
            floats[index] = values.get(index);
        }
        return floats;
    }

    private static Object method(Class<?> type, String name, int parameterCount) throws Throwable {
        if (parameterCount > MAX_PARAMETER_COUNT) {
            throw new IllegalArgumentException("Only methods with up to " + MAX_PARAMETER_COUNT + " parameters are supported: " + name);
        }
        Object[] byParameterCount = METHODS.computeIfAbsent(type, key -> new HashMap<>())
                .computeIfAbsent(name, key -> new Object[MAX_PARAMETER_COUNT + 1]);
        Object cached = byParameterCount[parameterCount];
        if (cached != null) return cached;
        Object found = NONE;
        for (Object candidate : (Object[]) GET_METHODS.invoke(type)) {
            if (name.equals(METHOD_NAME.invoke(candidate)) && parameterCount == (int) METHOD_PARAMETER_COUNT.invoke(candidate)) {
                METHOD_TRY_SET_ACCESSIBLE.invoke(candidate);
                found = candidate;
                break;
            }
        }
        byParameterCount[parameterCount] = found;
        return found;
    }

    private static Object methodReturning(Class<?> type, Class<?> returnType, int parameterCount) throws Throwable {
        if (parameterCount > MAX_PARAMETER_COUNT) {
            throw new IllegalArgumentException("Only methods with up to " + MAX_PARAMETER_COUNT + " parameters are supported");
        }
        Object[] byParameterCount = METHODS_BY_RETURN_TYPE.computeIfAbsent(type, key -> new HashMap<>())
                .computeIfAbsent(returnType, key -> new Object[MAX_PARAMETER_COUNT + 1]);
        Object cached = byParameterCount[parameterCount];
        if (cached != null) return cached;
        Object found = NONE;
        for (Object candidate : (Object[]) GET_METHODS.invoke(type)) {
            if (parameterCount == (int) METHOD_PARAMETER_COUNT.invoke(candidate)
                    && returnType.isAssignableFrom((Class<?>) METHOD_RETURN_TYPE.invoke(candidate))) {
                METHOD_TRY_SET_ACCESSIBLE.invoke(candidate);
                found = candidate;
                break;
            }
        }
        byParameterCount[parameterCount] = found;
        return found;
    }

    private static Object field(Class<?> type, Class<?> valueType) throws Throwable {
        Map<Class<?>, Object> byType = FIELDS.computeIfAbsent(type, key -> new HashMap<>());
        Object cached = byType.get(valueType);
        if (cached != null) return cached;
        Object found = NONE;
        for (Class<?> current = type; current != null && found == NONE; current = current.getSuperclass()) {
            for (Object candidate : (Object[]) GET_DECLARED_FIELDS.invoke(current)) {
                if (valueType.isAssignableFrom((Class<?>) FIELD_TYPE.invoke(candidate))) {
                    boolean accessible = (boolean) FIELD_TRY_SET_ACCESSIBLE.invoke(candidate);
                    found = accessible ? candidate : NONE;
                    break;
                }
            }
        }
        byType.put(valueType, found);
        return found;
    }

    private static Object namedField(Class<?> type, String name) throws Throwable {
        Map<String, Object> byName = FIELDS_BY_NAME.computeIfAbsent(type, key -> new HashMap<>());
        Object cached = byName.get(name);
        if (cached != null) return cached;
        Object found = NONE;
        for (Class<?> current = type; current != null && found == NONE; current = current.getSuperclass()) {
            for (Object candidate : (Object[]) GET_DECLARED_FIELDS.invoke(current)) {
                if (name.equals(FIELD_NAME.invoke(candidate))) {
                    boolean accessible = (boolean) FIELD_TRY_SET_ACCESSIBLE.invoke(candidate);
                    found = accessible ? candidate : NONE;
                    break;
                }
            }
        }
        byName.put(name, found);
        return found;
    }

    private static Class<?> reflectionClass(String simpleName) {
        try {
            return Class.forName("java.lang.reflect." + simpleName, false, Class.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private static MethodHandle virtual(Class<?> owner, String name, Class<?> returnType, Class<?>... parameterTypes) {
        try {
            return LOOKUP.findVirtual(owner, name, MethodType.methodType(returnType, parameterTypes));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
