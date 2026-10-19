package exiledsector.items;

import java.util.List;

public enum CorruptionOutcome {

    NOTHING,
    IMPLICIT_REPLACED,
    VALUES_RANDOMISED,
    MODIFIERS_RANDOMISED,
    INNATE;

    public static final List<CorruptionOutcome> NANOSWARM_OUTCOMES = List.of(NOTHING, IMPLICIT_REPLACED, VALUES_RANDOMISED, MODIFIERS_RANDOMISED);
}
