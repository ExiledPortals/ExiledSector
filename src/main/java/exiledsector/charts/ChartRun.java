package exiledsector.charts;

public final class ChartRun {

    private final String runId;
    private final String pocketSystemId;
    private final String outerWormholeId;
    private final String innerWormholeId;
    private final String originLocationId;
    private final float originX;
    private final float originY;
    private final int entriesAllowed;
    private int entriesUsed;
    private boolean outerCollapsed;
    private boolean returnRequested;
    private ChartRunState runState = ChartRunState.OPEN;

    public ChartRun(String runId, String originLocationId, float originX, float originY, int entriesAllowed) {
        this.runId = runId;
        this.pocketSystemId = ChartIds.pocketSystemId(runId);
        this.outerWormholeId = ChartIds.outerWormholeId(runId);
        this.innerWormholeId = ChartIds.innerWormholeId(runId);
        this.originLocationId = originLocationId;
        this.originX = originX;
        this.originY = originY;
        this.entriesAllowed = Math.max(1, entriesAllowed);
    }

    public static ChartRun adoptedWhileInside(String runId, String originLocationId, float originX, float originY) {
        ChartRun adopted = new ChartRun(runId, originLocationId, originX, originY, 1);
        adopted.entriesUsed = adopted.entriesAllowed;
        adopted.outerCollapsed = true;
        adopted.runState = ChartRunState.INSIDE;
        return adopted;
    }

    public String runId() {
        return runId;
    }

    public String pocketSystemId() {
        return pocketSystemId;
    }

    public String outerWormholeId() {
        return outerWormholeId;
    }

    public String innerWormholeId() {
        return innerWormholeId;
    }

    public String originLocationId() {
        return originLocationId;
    }

    public float originX() {
        return originX;
    }

    public float originY() {
        return originY;
    }

    public int entriesAllowed() {
        return entriesAllowed;
    }

    public int entriesUsed() {
        return entriesUsed;
    }

    public int entriesLeft() {
        return Math.max(0, entriesAllowed - entriesUsed);
    }

    public boolean isOuterCollapsed() {
        return outerCollapsed;
    }

    public boolean isReturnRequested() {
        return returnRequested;
    }

    public ChartRunState runState() {
        if (runState == null) {
            runState = ChartRunState.CLOSING;
        }
        return runState;
    }

    public boolean canEnter() {
        return runState() == ChartRunState.OPEN && !outerCollapsed && entriesLeft() > 0;
    }

    public boolean recordArrival() {
        if (runState() != ChartRunState.OPEN) {
            return false;
        }
        entriesUsed = Math.min(entriesAllowed, entriesUsed + 1);
        runState = ChartRunState.INSIDE;
        returnRequested = false;
        if (entriesLeft() == 0 && !outerCollapsed) {
            outerCollapsed = true;
            return true;
        }
        return false;
    }

    public void requestReturn() {
        if (runState() == ChartRunState.INSIDE || runState() == ChartRunState.CLOSING) {
            returnRequested = true;
        }
    }

    public void cancelReturn() {
        returnRequested = false;
    }

    public ChartRunState recordLeftPocket() {
        if (runState() == ChartRunState.INSIDE) {
            boolean outerStillOpen = returnRequested && !outerCollapsed && entriesLeft() > 0;
            runState = outerStillOpen ? ChartRunState.OPEN : ChartRunState.CLOSING;
        }
        returnRequested = false;
        return runState;
    }

    public boolean collapseOuter() {
        if (runState() != ChartRunState.OPEN) {
            return false;
        }
        outerCollapsed = true;
        runState = ChartRunState.CLOSING;
        return true;
    }

    public void beginClosing() {
        if (runState() != ChartRunState.CLOSED) {
            runState = ChartRunState.CLOSING;
        }
    }

    public void resumeInside() {
        if (runState() == ChartRunState.CLOSING) {
            runState = ChartRunState.INSIDE;
        }
    }

    public void markClosed() {
        runState = ChartRunState.CLOSED;
    }
}
