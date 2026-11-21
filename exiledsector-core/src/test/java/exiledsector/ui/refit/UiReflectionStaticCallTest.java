package exiledsector.ui.refit;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UiReflectionStaticCallTest {

    interface Loadable {
    }

    interface Linkable {
    }

    static final class Both implements Loadable, Linkable {
    }

    public static final class Engine {

        public static String describe(String name) {
            return "engine " + name;
        }

        public static String load(String dir, Loadable into, Linkable linkedTo) {
            return into == linkedTo ? null : "load " + dir;
        }

        public static String load(String dir, Linkable into) {
            return "two-parameter load " + dir;
        }

        public static Integer load(String dir, Loadable into, Object other) {
            return 0;
        }

        public String instanceLoad(String dir, Loadable into, Linkable linkedTo) {
            return "instance";
        }
    }

    public static final class Ambiguous {

        public static String first(Object value) {
            return "first";
        }

        public static String second(Object value) {
            return "second";
        }
    }

    public interface Stats {

        CharSequence getName();

        String getTitle();

        String describe(String prefix);

        Integer getCount();
    }

    @Test
    void noArgMethodNamesAreListedSortedForTheReturnTypeAndItsSubtypes() throws Throwable {
        assertEquals(List.of("getName", "getTitle"), UiReflection.noArgMethodNamesReturning(Stats.class, CharSequence.class));
        assertEquals(List.of("getTitle"), UiReflection.noArgMethodNamesReturning(Stats.class, String.class));
    }

    @Test
    void callStaticFindsTheMethodByNameAndParameterCount() throws Throwable {
        assertEquals("engine atlas", UiReflection.callStatic(Engine.class, "describe", "atlas"));
        assertThrows(NoSuchMethodException.class, () -> UiReflection.callStatic(Engine.class, "describe"));
    }

    @Test
    void theOnlyStaticMethodWithTheReturnTypeThatAcceptsTheArgumentsIsCalled() throws Throwable {
        Both state = new Both();

        assertNull(UiReflection.callOnlyStaticAccepting(Engine.class, String.class, "saves/a", state, state));
        assertEquals("load saves/a", UiReflection.callOnlyStaticAccepting(Engine.class, String.class, "saves/a", new Both(), state));
        assertEquals("load saves/a", UiReflection.callOnlyStaticAccepting(Engine.class, String.class, "saves/a", null, state));
    }

    @Test
    void noOrSeveralAcceptingMethodsAreReported() {
        assertThrows(NoSuchMethodException.class, () -> UiReflection.callOnlyStaticAccepting(Engine.class, String.class, "dir", 3, List.of()));
        assertThrows(IllegalStateException.class, () -> UiReflection.callOnlyStaticAccepting(Ambiguous.class, String.class, "value"));
    }
}
