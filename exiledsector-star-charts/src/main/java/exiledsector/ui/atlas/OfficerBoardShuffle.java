package exiledsector.ui.atlas;

import exiledsector.charts.officer.OfficerRank;
import exiledsector.charts.officer.turn.TurnResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class OfficerBoardShuffle {

    static final float DURATION_SECONDS = 4f;
    static final float DEPART_START = 0.5f;
    static final float DEPART_SECONDS = 1f;
    static final float SLIDE_START = 1f;
    static final float SLIDE_SECONDS = 2f;
    static final float ARRIVE_START = 2.5f;
    static final float ARRIVE_SECONDS = 1f;
    static final float MARK_IN_START = 0.5f;
    static final float MARK_FADE_SECONDS = 0.5f;
    static final float SETTLE_START = 3.5f;

    enum RankMark { NONE, ROSE, FELL }

    record Mover(int recordId, OfficerBoardLayout.Seat from, OfficerBoardLayout.Seat to, RankMark mark) {

        boolean arrives() {
            return from == null;
        }

        boolean departs() {
            return to == null;
        }
    }

    private final OfficerBoardLayout before;
    private final OfficerBoardLayout after;
    private final List<Mover> movers;

    private OfficerBoardShuffle(OfficerBoardLayout before, OfficerBoardLayout after, List<Mover> movers) {
        this.before = before;
        this.after = after;
        this.movers = movers;
    }

    static OfficerBoardShuffle of(List<OfficerRank> ranksLowestFirst, List<OfficerBoardLayout.Entry> current, List<TurnResult.StandingChange> changes) {
        Map<Integer, TurnResult.StandingChange> changeById = new HashMap<>();
        for (TurnResult.StandingChange change : changes) {
            changeById.put(change.recordId(), change);
        }
        List<OfficerBoardLayout.Entry> previous = new ArrayList<>();
        Set<Integer> currentIds = new HashSet<>();
        for (OfficerBoardLayout.Entry entry : current) {
            currentIds.add(entry.recordId());
            TurnResult.StandingChange change = changeById.get(entry.recordId());
            if (change == null) {
                previous.add(entry);
            } else if (change.before() != null) {
                previous.add(new OfficerBoardLayout.Entry(entry.recordId(), change.before().rankId(), change.before().renown(),
                        change.before().seat()));
            }
        }
        for (TurnResult.StandingChange change : changes) {
            if (!currentIds.contains(change.recordId()) && change.before() != null) {
                previous.add(new OfficerBoardLayout.Entry(change.recordId(), change.before().rankId(), change.before().renown(),
                        change.before().seat()));
            }
        }
        OfficerBoardLayout before = OfficerBoardLayout.of(ranksLowestFirst, previous);
        OfficerBoardLayout after = OfficerBoardLayout.of(ranksLowestFirst, current);
        List<Mover> movers = new ArrayList<>();
        for (OfficerBoardLayout.Entry entry : previous) {
            movers.add(new Mover(entry.recordId(), before.seat(entry.recordId()), after.seat(entry.recordId()),
                    mark(before, after, entry.recordId())));
        }
        for (OfficerBoardLayout.Entry entry : current) {
            if (!before.contains(entry.recordId())) {
                movers.add(new Mover(entry.recordId(), null, after.seat(entry.recordId()), RankMark.NONE));
            }
        }
        return new OfficerBoardShuffle(before, after, List.copyOf(movers));
    }

    private static RankMark mark(OfficerBoardLayout before, OfficerBoardLayout after, int recordId) {
        OfficerBoardLayout.Seat from = before.seat(recordId);
        OfficerBoardLayout.Seat to = after.seat(recordId);
        if (from == null || to == null || from.row() == to.row()) {
            return RankMark.NONE;
        }
        return to.row() < from.row() ? RankMark.ROSE : RankMark.FELL;
    }

    OfficerBoardLayout before() {
        return before;
    }

    OfficerBoardLayout after() {
        return after;
    }

    List<Mover> movers() {
        return movers;
    }

    int widestRow() {
        return Math.max(before.widestRow(), after.widestRow());
    }

    static float slideProgress(float elapsedSeconds) {
        float linear = clamp((elapsedSeconds - SLIDE_START) / SLIDE_SECONDS);
        return linear * linear * (3f - 2f * linear);
    }

    static float departAlpha(float elapsedSeconds) {
        return 1f - clamp((elapsedSeconds - DEPART_START) / DEPART_SECONDS);
    }

    static float arriveAlpha(float elapsedSeconds) {
        return clamp((elapsedSeconds - ARRIVE_START) / ARRIVE_SECONDS);
    }

    static float markAlpha(float elapsedSeconds) {
        float in = clamp((elapsedSeconds - MARK_IN_START) / MARK_FADE_SECONDS);
        float out = 1f - clamp((elapsedSeconds - SETTLE_START) / MARK_FADE_SECONDS);
        return Math.min(in, out);
    }

    static float arrowAlpha(float elapsedSeconds) {
        return clamp((elapsedSeconds - SETTLE_START) / (DURATION_SECONDS - SETTLE_START));
    }

    static float lerp(float from, float to, float progress) {
        return from + (to - from) * progress;
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
