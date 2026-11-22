package exiledsector.harness;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.atlas.AtlasAnomalies;
import exiledsector.charts.atlas.AtlasAnomalyRecord;
import exiledsector.charts.atlas.AtlasCatalysts;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasDevTools;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasRuntime;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.items.crafting.material.CraftingMaterials;
import exiledsector.ui.ShownScreens;
import exiledsector.ui.atlas.AtlasScreen;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class FocusAtlasSystemStep implements HarnessStep {

    static final int CANDIDATE_RADIUS = 8;

    private final int systemIndex;
    private final List<String> anomalyIds;
    private final List<String> catalystAnomalyIds;
    private AtlasCell cell;
    private final List<String> insertions = new ArrayList<>();

    FocusAtlasSystemStep(int systemIndex, List<String> anomalyIds, List<String> catalystAnomalyIds) {
        this.systemIndex = systemIndex;
        this.anomalyIds = List.copyOf(anomalyIds);
        this.catalystAnomalyIds = List.copyOf(catalystAnomalyIds);
    }

    @Override
    public String name() {
        return "focusAtlasSystem " + systemIndex + " " + anomalyIds + (catalystAnomalyIds.isEmpty() ? "" : " catalysts " + catalystAnomalyIds);
    }

    @Override
    public StepOutcome advance(StepContext context) throws JSONException {
        if (context.firstFrame()) {
            StepOutcome notInCampaign = HarnessCampaign.notInCampaign("focusAtlasSystem");
            return notInCampaign != null ? notInCampaign : prepareAndOpen(Global.getSector());
        }
        if (!(ShownScreens.top() != null && ShownScreens.top().screen() instanceof AtlasScreen atlas)) {
            return new StepOutcome(false, false, "waiting for the atlas to open");
        }
        if (!atlas.focusOn(cell)) {
            return StepOutcome.failed("the atlas could not focus " + cell);
        }
        JSONObject data = new JSONObject();
        data.put("cell", cell.gridX() + "," + cell.gridY());
        data.put("insertions", insertions);
        context.results().addData("focusedSystem", data);
        return StepOutcome.passed("focused " + cell.gridX() + "," + cell.gridY() + " with " + insertions);
    }

    private StepOutcome prepareAndOpen(SectorAPI sector) {
        AtlasGenerator generator = AtlasRuntime.generator(sector);
        AtlasState state = AtlasState.get(sector);
        List<AtlasCell> candidates = new ArrayList<>(AtlasCell.ORIGIN.around(CANDIDATE_RADIUS).stream()
                .filter(candidate -> !candidate.equals(AtlasCell.ORIGIN) && !generator.isVoid(candidate) && generator.specialAt(candidate) == null
                        && !state.isCompleted(candidate))
                .toList());
        candidates.sort(Comparator.comparingDouble(AtlasCell::centreDistance).thenComparingInt(AtlasCell::gridX).thenComparingInt(AtlasCell::gridY));
        if (systemIndex < 0 || systemIndex >= candidates.size()) {
            return StepOutcome.failed("system index " + systemIndex + " is outside the " + candidates.size() + " unfinished systems near the origin");
        }
        cell = candidates.get(systemIndex);
        if (AtlasDevTools.reach(generator, state, cell) == null) {
            return StepOutcome.failed("no lane path reaches " + cell);
        }
        for (String anomalyId : anomalyIds) {
            AtlasAnomalies.Insertion insertion = AtlasAnomalies.insert(generator, state, cell, anomalyId, AtlasAnomalyRecord.Source.WORLD, null, null, true);
            insertions.add(anomalyId + " " + insertion.check());
        }
        for (String catalystAnomalyId : catalystAnomalyIds) {
            AtlasCatalysts.Catalyst catalyst = AtlasCatalysts.all().stream().filter(candidate -> candidate.anomalyId().equals(catalystAnomalyId)).findFirst()
                    .orElse(null);
            if (catalyst == null) {
                return StepOutcome.failed("no catalyst adds " + catalystAnomalyId);
            }
            CraftingMaterials.forPlayer(sector.getPlayerFleet().getCargo()).add(catalyst.itemId(), 1);
            state.setCatalystOn(catalyst.itemId(), true);
            insertions.add(catalystAnomalyId + " catalyst " + catalyst.itemId() + " on");
        }
        if (!AtlasScreen.open(sector.getCampaignUI())) {
            return StepOutcome.failed("the atlas did not open; another Exiled Sector screen may already be open");
        }
        return StepOutcome.RUNNING;
    }
}
