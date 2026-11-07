package exiledsector.compat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrivateFieldTest {

    static final class Holder {
        private long stamp = 3L;
    }

    @Test
    void aPrivateFieldCanBeReadAndWritten() {
        PrivateField<Holder> stamp = new PrivateField<>(Holder.class, "stamp", long.class, "unreachable");
        Holder holder = new Holder();

        assertEquals(3L, stamp.read(holder));
        assertTrue(stamp.write(holder, 7L));
        assertEquals(7L, holder.stamp);
    }

    @Test
    void aFieldNamedByItsOwnersClassNameWritesOnlyInstancesOfThatClass() {
        PrivateField<Object> stamp = PrivateField.named(Holder.class.getName(), "stamp", long.class, "unreachable");
        Holder holder = new Holder();

        assertTrue(stamp.write(holder, 9L));
        assertFalse(stamp.write(new Object(), 1L));
        assertEquals(9L, holder.stamp);
    }

    @Test
    void aMissingClassOrFieldIsUnreachableAndNeverThrows() {
        PrivateField<Object> missingClass = PrivateField.named("exiledsector.compat.NoSuchClass", "stamp", long.class, "unreachable");
        PrivateField<Holder> missingField = new PrivateField<>(Holder.class, "noSuchField", long.class, "unreachable");

        assertFalse(missingClass.reachable());
        assertFalse(missingClass.write(new Holder(), 1L));
        assertFalse(missingField.reachable());
        assertNull(missingField.read(new Holder()));
    }
}
