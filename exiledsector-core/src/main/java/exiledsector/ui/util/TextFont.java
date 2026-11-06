package exiledsector.ui.util;

import org.lazywizard.lazylib.ui.LazyFont;

import java.util.function.Supplier;

public record TextFont(Supplier<LazyFont> source, float size) {

    public TextFont resized(float newSize) {
        return new TextFont(source, newSize);
    }

    public LazyFont font() {
        return source.get();
    }
}
