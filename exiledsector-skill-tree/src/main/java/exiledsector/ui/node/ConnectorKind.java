package exiledsector.ui.node;

enum ConnectorKind {
    GLOW, TEMPLATE, DULL;

    static ConnectorKind of(boolean bothSatisfied, boolean firstInTemplate, boolean secondInTemplate) {
        if (bothSatisfied) {
            return GLOW;
        }
        return firstInTemplate && secondInTemplate ? TEMPLATE : DULL;
    }
}
