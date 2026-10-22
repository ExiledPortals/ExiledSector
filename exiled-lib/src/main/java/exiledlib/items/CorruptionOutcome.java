package exiledlib.items;

import java.util.List;

public enum CorruptionOutcome {

    NOTHING,
    IMPLICIT_REPLACED,
    VALUES_RANDOMISED,
    MODIFIERS_RANDOMISED,
    INNATE,
    IMPLICIT_ADDED,
    BECAME_RARE;

    public static final List<CorruptionOutcome> NANOSWARM_OUTCOMES = List.of(NOTHING, IMPLICIT_REPLACED, VALUES_RANDOMISED, MODIFIERS_RANDOMISED);
    public static final List<CorruptionOutcome> UNIQUE_HULL_ITEM_OUTCOMES = List.of(NOTHING, IMPLICIT_REPLACED, VALUES_RANDOMISED,
            MODIFIERS_RANDOMISED, BECAME_RARE);
    public static final List<CorruptionOutcome> SUBROUTINE_OUTCOMES = List.of(NOTHING, IMPLICIT_ADDED, VALUES_RANDOMISED, MODIFIERS_RANDOMISED,
            BECAME_RARE);
}
