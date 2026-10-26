package exiledsector.charts.run.cleanup;

import org.apache.log4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

final class PrivateField<O> {

    private static final Logger LOG = Logger.getLogger(PrivateField.class);

    private final Class<O> ownerType;
    private final String fieldName;
    private final Class<?> fieldType;
    private final String unreachableWarning;
    private MethodHandle getter;
    private boolean unreachable;

    PrivateField(Class<O> ownerType, String fieldName, Class<?> fieldType, String unreachableWarning) {
        this.ownerType = ownerType;
        this.fieldName = fieldName;
        this.fieldType = fieldType;
        this.unreachableWarning = unreachableWarning;
    }

    boolean reachable() {
        if (unreachable) {
            return false;
        }
        if (getter != null) {
            return true;
        }
        try {
            getter = MethodHandles.privateLookupIn(ownerType, MethodHandles.lookup()).findGetter(ownerType, fieldName, fieldType);
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            unreachable = true;
            LOG.warn("[ExiledSector] " + unreachableWarning, e);
            return false;
        }
    }

    // java:S1181: MethodHandle.invoke declares Throwable, and a changed engine may only cost what the caller reads, never the caller
    @SuppressWarnings("java:S1181")
    Object read(O owner) {
        if (owner == null || !reachable()) {
            return null;
        }
        try {
            return getter.invoke(owner);
        } catch (Throwable e) {
            LOG.debug(ownerType.getSimpleName() + "." + fieldName + " could not be read this time", e);
            return null;
        }
    }
}
