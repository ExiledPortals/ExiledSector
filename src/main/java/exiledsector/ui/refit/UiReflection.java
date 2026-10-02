package exiledsector.ui.refit;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class UiReflection {

    private static final Object NONE = new Object();
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final Class<?> METHOD = reflectionClass("Method");
    private static final Class<?> FIELD = reflectionClass("Field");
    private static final MethodHandle GET_METHODS = virtual(Class.class, "getMethods", METHOD.arrayType());
    private static final MethodHandle GET_DECLARED_FIELDS = virtual(Class.class, "getDeclaredFields", FIELD.arrayType());
    private static final MethodHandle METHOD_NAME = virtual(METHOD, "getName", String.class);
    private static final MethodHandle METHOD_PARAMETER_COUNT = virtual(METHOD, "getParameterCount", int.class);
    private static final MethodHandle METHOD_INVOKE = virtual(METHOD, "invoke", Object.class, Object.class, Object[].class);
    private static final MethodHandle FIELD_TYPE = virtual(FIELD, "getType", Class.class);
    private static final MethodHandle METHOD_TRY_SET_ACCESSIBLE = virtual(METHOD, "trySetAccessible", boolean.class);
    private static final MethodHandle FIELD_TRY_SET_ACCESSIBLE = virtual(FIELD, "trySetAccessible", boolean.class);
    private static final MethodHandle FIELD_GET = virtual(FIELD, "get", Object.class, Object.class);

    private static final int MAX_PARAMETER_COUNT = 3;
    private static final Object[] NO_ARGS = new Object[0];
    private static final Map<Class<?>, Map<String, Object[]>> METHODS = new HashMap<>();
    private static final Map<Class<?>, Map<Class<?>, Object>> FIELDS = new HashMap<>();

    private UiReflection() {
    }

    static Object call(Object target, String name) throws Throwable {
        return call(target, name, NO_ARGS);
    }

    static Object call(Object target, String name, Object... args) throws Throwable {
        if (target == null) return null;
        Object method = method(target.getClass(), name, args.length);
        return method == NONE ? null : METHOD_INVOKE.invoke(method, target, args);
    }

    static boolean hasMethod(Object target, String name, int parameterCount) throws Throwable {
        return target != null && method(target.getClass(), name, parameterCount) != NONE;
    }

    static List<?> children(Object panel) throws Throwable {
        return call(panel, "getChildrenNonCopy") instanceof List<?> children ? children : List.of();
    }

    static Object childWithMethod(Object panel, String name, int parameterCount) throws Throwable {
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
