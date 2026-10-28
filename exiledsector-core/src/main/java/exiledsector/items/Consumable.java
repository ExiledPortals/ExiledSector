package exiledsector.items;

import java.util.Collection;
import java.util.List;

public interface Consumable<T, R> {

    String itemId();

    UseVerdict check(T target);

    R apply(T target);

    default Collection<String> requiredItemIds() {
        return List.of();
    }

    default Collection<String> spentAlongside(T target) {
        return List.of();
    }
}
