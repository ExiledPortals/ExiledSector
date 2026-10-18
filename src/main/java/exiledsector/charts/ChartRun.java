package exiledsector.charts;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class ChartRun {

    static final String ID_SEPARATOR = "|";

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
    private long layoutSeed;
    private String layoutType;
    private boolean layoutGenerated;
    private int tier;
    private String contentFaction;
    private String objective;
    private String objectiveTargets;
    private String objectiveDefeated;
    private String bossFleetId;
    private String vaultEntityId;
    private int contentFleets;
    private int contentPois;
    private boolean objectiveComplete;
    private boolean rewardClaimed;

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

    public void recordLayout(ChartLayout layout) {
        layoutSeed = layout.seed();
        layoutType = layout.systemType() == null ? null : layout.systemType().name();
        layoutGenerated = layout.generated();
    }

    public boolean hasLayout() {
        return layoutType != null;
    }

    public long layoutSeed() {
        return layoutSeed;
    }

    public String layoutType() {
        return layoutType;
    }

    public boolean isLayoutGenerated() {
        return layoutGenerated;
    }

    public void recordContent(ChartContent content) {
        tier = ChartTiers.clamp(content.tier());
        contentFaction = content.factionId();
        objective = content.objective() == null ? null : content.objective().name();
        objectiveTargets = join(content.targetIds());
        objectiveDefeated = null;
        bossFleetId = content.bossFleetId();
        vaultEntityId = content.vaultEntityId();
        contentFleets = content.fleets();
        contentPois = content.pois();
        objectiveComplete = false;
        rewardClaimed = false;
    }

    public int tier() {
        return ChartTiers.clamp(tier);
    }

    public String contentFaction() {
        return contentFaction;
    }

    public ChartObjective objective() {
        return ChartObjective.parse(objective);
    }

    public String bossFleetId() {
        return bossFleetId;
    }

    public String vaultEntityId() {
        return vaultEntityId;
    }

    public int contentFleets() {
        return contentFleets;
    }

    public int contentPois() {
        return contentPois;
    }

    public List<String> objectiveTargetIds() {
        return split(objectiveTargets);
    }

    public List<String> defeatedTargetIds() {
        return split(objectiveDefeated);
    }

    public int objectiveTargetsDefeated() {
        return defeatedTargetIds().size();
    }

    public boolean recordTargetGone(String targetId) {
        if (targetId == null || objectiveComplete || objective() == null) {
            return false;
        }
        List<String> targets = objectiveTargetIds();
        List<String> defeated = new ArrayList<>(defeatedTargetIds());
        if (!targets.contains(targetId) || defeated.contains(targetId)) {
            return false;
        }
        defeated.add(targetId);
        objectiveDefeated = join(defeated);
        return true;
    }

    public boolean isObjectiveMet() {
        List<String> targets = objectiveTargetIds();
        return objective() != null && !targets.isEmpty() && defeatedTargetIds().containsAll(targets);
    }

    public boolean isObjectiveComplete() {
        return objectiveComplete;
    }

    public boolean markObjectiveComplete() {
        if (objectiveComplete || !isObjectiveMet()) {
            return false;
        }
        objectiveComplete = true;
        return true;
    }

    public boolean claimReward() {
        if (!objectiveComplete || rewardClaimed) {
            return false;
        }
        rewardClaimed = true;
        return true;
    }

    public boolean isRewardClaimed() {
        return rewardClaimed;
    }

    static String join(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        List<String> kept = new ArrayList<>();
        for (String id : ids) {
            if (id != null && !id.isEmpty() && !id.contains(ID_SEPARATOR)) {
                kept.add(id);
            }
        }
        return kept.isEmpty() ? null : String.join(ID_SEPARATOR, kept);
    }

    static List<String> split(String joined) {
        if (joined == null || joined.isEmpty()) {
            return List.of();
        }
        return List.of(joined.split(Pattern.quote(ID_SEPARATOR)));
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
