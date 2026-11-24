package exiledsector.compat;

import org.apache.log4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

public final class PrivateField<O> {

    private static final Logger LOG = Logger.getLogger(PrivateField.class);

    private final String ownerClassName;
    private final String fieldName;
    private final String fieldTypeName;
    private final String unreachableWarning;
    private Class<?> fieldType;
    private Class<?> ownerType;
    private MethodHandles.Lookup ownerLookup;
    private MethodHandle getter;
    private MethodHandle setter;
    private boolean unreachable;

    public PrivateField(Class<O> ownerType, String fieldName, Class<?> fieldType, String unreachableWarning) {
        this(ownerType.getName(), fieldName, fieldType, unreachableWarning);
        this.ownerType = ownerType;
    }

    private PrivateField(String ownerClassName, String fieldName, Class<?> fieldType, String unreachableWarning) {
        this(ownerClassName, fieldName, fieldType.getName(), unreachableWarning);
        this.fieldType = fieldType;
    }

    private PrivateField(String ownerClassName, String fieldName, String fieldTypeName, String unreachableWarning) {
        this.ownerClassName = ownerClassName;
        this.fieldName = fieldName;
        this.fieldTypeName = fieldTypeName;
        this.unreachableWarning = unreachableWarning;
    }

    public static PrivateField<Object> named(String ownerClassName, String fieldName, Class<?> fieldType, String unreachableWarning) {
        return new PrivateField<>(ownerClassName, fieldName, fieldType, unreachableWarning);
    }

    public static PrivateField<Object> namedOfType(String ownerClassName, String fieldName, String fieldTypeName, String unreachableWarning) {
        return new PrivateField<>(ownerClassName, fieldName, fieldTypeName, unreachableWarning);
    }

    public boolean reachable() {
        return lookup() != null && getter() != null;
    }

    // java:S1181: MethodHandle.invoke declares Throwable, and a changed engine may only cost what the caller reads, never the caller
    @SuppressWarnings("java:S1181")
    public Object read(O owner) {
        MethodHandle handle = owner == null ? null : getter();
        if (handle == null) {
            return null;
        }
        try {
            return handle.invoke(owner);
        } catch (Throwable e) {
            LOG.debug(ownerClassName + "." + fieldName + " could not be read this time", e);
            return null;
        }
    }

    // java:S1181: MethodHandle.invoke declares Throwable, and a changed engine may only cost what the caller writes, never the caller
    @SuppressWarnings("java:S1181")
    public boolean write(O owner, Object value) {
        MethodHandle handle = owner == null ? null : setter();
        if (handle == null || !ownerType.isInstance(owner)) {
            return false;
        }
        try {
            handle.invoke(owner, value);
            return true;
        } catch (Throwable e) {
            LOG.debug(ownerClassName + "." + fieldName + " could not be written this time", e);
            return false;
        }
    }

    private MethodHandle getter() {
        if (getter == null && lookup() != null) {
            getter = resolve(() -> ownerLookup.findGetter(ownerType, fieldName, fieldType));
        }
        return getter;
    }

    private MethodHandle setter() {
        if (setter == null && lookup() != null) {
            setter = resolve(() -> ownerLookup.findSetter(ownerType, fieldName, fieldType));
        }
        return setter;
    }

    private MethodHandles.Lookup lookup() {
        if (unreachable) {
            return null;
        }
        if (ownerLookup == null) {
            try {
                if (ownerType == null) {
                    ownerType = Class.forName(ownerClassName, false, PrivateField.class.getClassLoader());
                }
                if (fieldType == null) {
                    fieldType = Class.forName(fieldTypeName, false, PrivateField.class.getClassLoader());
                }
                ownerLookup = MethodHandles.privateLookupIn(ownerType, MethodHandles.lookup());
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                markUnreachable(e);
            }
        }
        return ownerLookup;
    }

    private MethodHandle resolve(HandleLookup handleLookup) {
        try {
            return handleLookup.find();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            markUnreachable(e);
            return null;
        }
    }

    private void markUnreachable(Throwable cause) {
        unreachable = true;
        LOG.warn("[ExiledSector] " + unreachableWarning, cause);
    }

    @FunctionalInterface
    private interface HandleLookup {
        MethodHandle find() throws ReflectiveOperationException;
    }
}
