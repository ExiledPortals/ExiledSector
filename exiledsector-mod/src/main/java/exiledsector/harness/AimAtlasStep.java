package exiledsector.harness;

import com.fs.starfarer.api.Global;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasReach;
import exiledsector.charts.atlas.AtlasRuntime;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.officer.OfficerSightings;
import exiledsector.ui.ShownScreens;
import exiledsector.ui.atlas.AtlasScreen;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class AimAtlasStep implements HarnessStep {

    static final int VIEW_HALF_WIDTH = 7;
    static final int VIEW_HALF_HEIGHT = 4;
    static final int UNFINISHED_SCORE = 1;
    static final int TERRITORY_SCORE = 2;
    static final int COMPLETED_SCORE = -1;

    @Override
    public String name() {
        return "aimAtlas at the frontier";
    }

    @Override
    public StepOutcome advance(StepContext context) throws JSONException {
        if (!(ShownScreens.top() != null && ShownScreens.top().screen() instanceof AtlasScreen atlas)) {
            return StepOutcome.failed("aimAtlas needs the atlas open, for example by openAtlas");
        }
        AtlasGenerator generator = AtlasRuntime.generator(Global.getSector());
        AtlasState state = AtlasState.get(Global.getSector());
        Set<AtlasCell> shown = new LinkedHashSet<>(AtlasReach.visible(generator, state));
        shown.addAll(OfficerSightings.shownSystems(state));
        AtlasCell best = shown.stream()
                .max(Comparator.<AtlasCell>comparingInt(candidate -> viewScore(generator, state, shown, candidate))
                        .thenComparing(Comparator.comparingDouble(AtlasCell::centreDistance).reversed()))
                .orElse(AtlasCell.ORIGIN);
        AtlasCell centre = unfinishedMiddle(generator, state, shown, best);
        if (!atlas.centreOn(centre)) {
            return StepOutcome.failed("the atlas could not centre on " + centre + "; it may be focused on a system");
        }
        JSONObject data = new JSONObject();
        data.put("cell", centre.gridX() + "," + centre.gridY());
        int score = viewScore(generator, state, shown, best);
        data.put("score", score);
        context.results().addData("aimedAtlas", data);
        return StepOutcome.passed("centred the atlas on " + centre.gridX() + "," + centre.gridY() + " (view score " + score + ")");
    }

    private static AtlasCell unfinishedMiddle(AtlasGenerator generator, AtlasState state, Set<AtlasCell> shown, AtlasCell window) {
        List<AtlasCell> unfinished = shown.stream()
                .filter(cell -> inView(window, cell) && !cell.equals(AtlasCell.ORIGIN) && !generator.isVoid(cell) && !state.isCompleted(cell))
                .toList();
        if (unfinished.isEmpty()) {
            return window;
        }
        int middleX = Math.round((float) unfinished.stream().mapToInt(AtlasCell::gridX).average().orElse(window.gridX()));
        int middleY = Math.round((float) unfinished.stream().mapToInt(AtlasCell::gridY).average().orElse(window.gridY()));
        return new AtlasCell(middleX, middleY);
    }

    private static boolean inView(AtlasCell centre, AtlasCell cell) {
        return Math.abs(cell.gridX() - centre.gridX()) <= VIEW_HALF_WIDTH && Math.abs(cell.gridY() - centre.gridY()) <= VIEW_HALF_HEIGHT;
    }

    private static int viewScore(AtlasGenerator generator, AtlasState state, Set<AtlasCell> shown, AtlasCell centre) {
        int score = 0;
        for (AtlasCell cell : shown) {
            if (!inView(centre, cell) || cell.equals(AtlasCell.ORIGIN) || generator.isVoid(cell)) {
                continue;
            }
            if (state.isCompleted(cell)) {
                score += COMPLETED_SCORE;
            } else {
                score += UNFINISHED_SCORE + (state.commandCentreOf(cell) != null ? TERRITORY_SCORE : 0);
            }
        }
        return score;
    }
}
