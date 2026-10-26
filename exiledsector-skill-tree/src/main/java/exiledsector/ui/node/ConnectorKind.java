package exiledsector.ui.node;

enum ConnectorKind {
    GLOW, TEMPLATE, DULL, CONDUIT;

    static ConnectorKind of(boolean bothSatisfied, boolean firstInTemplate, boolean secondInTemplate) {
        if (bothSatisfied) {
            return GLOW;
        }
        return firstInTemplate && secondInTemplate ? TEMPLATE : DULL;
    }
}
