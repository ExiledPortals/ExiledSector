package exiledsector.ui.node;

public enum ConnectorKind {
    GLOW, TEMPLATE, DULL, CONDUIT;

    public static ConnectorKind of(boolean bothSatisfied, boolean firstInTemplate, boolean secondInTemplate) {
        if (bothSatisfied) {
            return GLOW;
        }
        return firstInTemplate && secondInTemplate ? TEMPLATE : DULL;
    }
}
