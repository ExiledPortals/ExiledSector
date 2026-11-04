package exiledsector.ui.util;

import org.lazywizard.lazylib.ui.LazyFont;

import java.util.function.Supplier;

public record TextFont(Supplier<LazyFont> source, float size) {
}
