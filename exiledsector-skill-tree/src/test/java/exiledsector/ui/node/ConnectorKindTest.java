package exiledsector.ui.node;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConnectorKindTest {

    @Test
    void anEdgeWithBothEndsAllocatedAlwaysUsesTheNormalLook() {
        assertEquals(ConnectorKind.GLOW, ConnectorKind.of(true, true, true));
        assertEquals(ConnectorKind.GLOW, ConnectorKind.of(true, false, false));
    }

    @Test
    void anEdgeBetweenTwoTemplateNodesIsGreenUntilBothAreAllocated() {
        assertEquals(ConnectorKind.TEMPLATE, ConnectorKind.of(false, true, true));
    }

    @Test
    void anEdgeLeavingTheTemplateIsDull() {
        assertEquals(ConnectorKind.DULL, ConnectorKind.of(false, true, false));
        assertEquals(ConnectorKind.DULL, ConnectorKind.of(false, false, true));
        assertEquals(ConnectorKind.DULL, ConnectorKind.of(false, false, false));
    }
}
