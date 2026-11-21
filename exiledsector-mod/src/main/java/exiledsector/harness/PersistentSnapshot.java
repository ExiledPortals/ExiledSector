package exiledsector.harness;

import exiledsector.ui.refit.UiReflection;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

final class PersistentSnapshot {

    private record FieldValue(UiReflection.DeclaredField field, MethodHandle getter, MethodHandle setter, Object value) {
    }

    private final String key;
    private final Object stored;
    private final List<FieldValue> fieldValues;

    private PersistentSnapshot(String key, Object stored, List<FieldValue> fieldValues) {
        this.key = key;
        this.stored = stored;
        this.fieldValues = fieldValues;
    }

    static PersistentSnapshot of(Map<String, Object> persistentData, String key) throws Throwable {
        Object stored = persistentData.get(key);
        List<FieldValue> fieldValues = new ArrayList<>();
        if (stored != null) {
            for (UiReflection.DeclaredField field : UiReflection.instanceFields(stored.getClass())) {
                MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(field.owner(), MethodHandles.lookup());
                MethodHandle getter = lookup.findGetter(field.owner(), field.name(), field.type());
                MethodHandle setter = field.isFinal() ? null : lookup.findSetter(field.owner(), field.name(), field.type());
                fieldValues.add(new FieldValue(field, getter, setter, field.isTransient() ? null : deepCopy(getter.invoke(stored))));
            }
        }
        return new PersistentSnapshot(key, stored, fieldValues);
    }

    void restore(Map<String, Object> persistentData) throws Throwable {
        if (stored == null) {
            persistentData.remove(key);
            return;
        }
        persistentData.put(key, stored);
        for (FieldValue fieldValue : fieldValues) {
            if (!fieldValue.field().isTransient()) {
                restoreField(fieldValue, deepCopy(fieldValue.value()));
            } else if (fieldValue.setter() != null && !fieldValue.field().type().isPrimitive()) {
                fieldValue.setter().invoke(stored, (Object) null);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void restoreField(FieldValue fieldValue, Object value) throws Throwable {
        if (fieldValue.setter() != null) {
            fieldValue.setter().invoke(stored, value);
            return;
        }
        Object live = fieldValue.getter().invoke(stored);
        if (live instanceof Collection<?> liveCollection && value instanceof Collection<?> saved) {
            liveCollection.clear();
            ((Collection<Object>) liveCollection).addAll(saved);
        } else if (live instanceof Map<?, ?> liveMap && value instanceof Map<?, ?> saved) {
            liveMap.clear();
            ((Map<Object, Object>) liveMap).putAll(saved);
        }
    }

    static Object deepCopy(Object value) throws Throwable {
        if (value instanceof Collection<?> collection) {
            Collection<Object> copy = newInstanceOr(collection, ArrayList::new);
            for (Object element : collection) {
                copy.add(deepCopy(element));
            }
            return copy;
        }
        if (value instanceof Map<?, ?> map) {
            Map<Object, Object> copy = newInstanceOr(map, LinkedHashMap::new);
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                copy.put(entry.getKey(), deepCopy(entry.getValue()));
            }
            return copy;
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private static <T> T newInstanceOr(Object original, Supplier<T> fallback) throws Throwable {
        MethodHandle constructor;
        try {
            constructor = MethodHandles.publicLookup().findConstructor(original.getClass(), MethodType.methodType(void.class));
        } catch (ReflectiveOperationException | RuntimeException e) {
            return fallback.get();
        }
        return (T) constructor.invoke();
    }
}
