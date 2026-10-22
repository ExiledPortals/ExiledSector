package exiledlib.compat;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;

public record ApiMember(Kind kind, String className, String memberName, String typeName, List<String> parameterClassNames) {

    enum Kind {
        STATIC_METHOD, INSTANCE_METHOD, CONSTRUCTOR, STATIC_GETTER
    }

    static final String VOID = "void";
    public static final String BOOLEAN = "boolean";
    static final String FLOAT = "float";
    public static final String INT = "int";
    private static final String CONSTRUCTOR_NAME = "<init>";

    public static ApiMember staticMethod(String className, String methodName, String returnClassName, String... parameterClassNames) {
        return new ApiMember(Kind.STATIC_METHOD, className, methodName, returnClassName, List.of(parameterClassNames));
    }

    public static ApiMember instanceMethod(String className, String methodName, String returnClassName, String... parameterClassNames) {
        return new ApiMember(Kind.INSTANCE_METHOD, className, methodName, returnClassName, List.of(parameterClassNames));
    }

    static ApiMember constructor(String className, String... parameterClassNames) {
        return new ApiMember(Kind.CONSTRUCTOR, className, CONSTRUCTOR_NAME, VOID, List.of(parameterClassNames));
    }

    public static ApiMember staticGetter(String className, String fieldName, String fieldClassName) {
        return new ApiMember(Kind.STATIC_GETTER, className, fieldName, fieldClassName, List.of());
    }

    public String displayName() {
        return kind == Kind.CONSTRUCTOR ? className + " constructor" : className + "." + memberName;
    }

    MethodHandle resolve(ClassLoader loader) throws ReflectiveOperationException {
        Class<?> owner = typeNamed(className, loader);
        Class<?> type = typeNamed(typeName, loader);
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        return switch (kind) {
            case STATIC_GETTER -> lookup.findStaticGetter(owner, memberName, type);
            case STATIC_METHOD -> lookup.findStatic(owner, memberName, methodType(type, loader));
            case INSTANCE_METHOD -> lookup.findVirtual(owner, memberName, methodType(type, loader));
            case CONSTRUCTOR -> lookup.findConstructor(owner, methodType(type, loader));
        };
    }

    private MethodType methodType(Class<?> returnType, ClassLoader loader) throws ClassNotFoundException {
        Class<?>[] parameterTypes = new Class<?>[parameterClassNames.size()];
        for (int index = 0; index < parameterTypes.length; index++) {
            parameterTypes[index] = typeNamed(parameterClassNames.get(index), loader);
        }
        return MethodType.methodType(returnType, parameterTypes);
    }

    public static List<ApiMember> missingFrom(List<ApiMember> members, ClassLoader loader) {
        List<ApiMember> missing = new ArrayList<>();
        for (ApiMember member : members) {
            try {
                member.resolve(loader);
            } catch (ReflectiveOperationException | LinkageError e) {
                missing.add(member);
            }
        }
        return missing;
    }

    private static Class<?> typeNamed(String name, ClassLoader loader) throws ClassNotFoundException {
        return switch (name) {
            case VOID -> void.class;
            case FLOAT -> float.class;
            case BOOLEAN -> boolean.class;
            case INT -> int.class;
            default -> Class.forName(name, false, loader);
        };
    }
}
