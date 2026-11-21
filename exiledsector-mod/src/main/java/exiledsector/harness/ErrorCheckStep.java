package exiledsector.harness;

import java.util.List;

final class ErrorCheckStep implements HarnessStep {

    static final int REPORTED_ERRORS = 5;

    static final class Cursor {

        private int checkedErrors;
    }

    private final Cursor cursor;
    private final boolean ignoring;

    private ErrorCheckStep(Cursor cursor, boolean ignoring) {
        this.cursor = cursor;
        this.ignoring = ignoring;
    }

    static ErrorCheckStep expectNoErrors(Cursor cursor) {
        return new ErrorCheckStep(cursor, false);
    }

    static ErrorCheckStep ignoreErrorsSoFar(Cursor cursor) {
        return new ErrorCheckStep(cursor, true);
    }

    @Override
    public String name() {
        return ignoring ? "ignoreErrorsSoFar" : "expectNoErrors";
    }

    @Override
    public StepOutcome advance(StepContext context) {
        List<String> newErrors = context.log().errorsFrom(cursor.checkedErrors);
        cursor.checkedErrors += newErrors.size();
        if (ignoring) {
            return StepOutcome.passed("ignoring " + newErrors.size() + " earlier errors");
        }
        if (newErrors.isEmpty()) {
            return StepOutcome.passed("no errors logged");
        }
        return StepOutcome.failed(newErrors.size() + " errors logged, first: " + String.join(" || ",
                newErrors.subList(0, Math.min(REPORTED_ERRORS, newErrors.size()))));
    }
}
