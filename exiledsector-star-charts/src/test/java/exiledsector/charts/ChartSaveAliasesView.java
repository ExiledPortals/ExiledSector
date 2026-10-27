package exiledsector.charts;

public final class ChartSaveAliasesView {

    private ChartSaveAliasesView() {
    }

    public static boolean aliased(Class<?> type) {
        return ChartSaveAliases.ALIASES.containsValue(type);
    }
}
