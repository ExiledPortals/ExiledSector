package exiledsector.charts.atlas;

import exiledsector.charts.anomaly.AtlasRule;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.items.UseVerdict;
import exiledsector.items.crafting.material.CraftingMaterials;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

public final class ActiveCatalysts {

    public enum Toggle {ON, OFF, NONE_OWNED}

    public record Planned(AtlasCatalysts.Catalyst catalyst, UseVerdict verdict) {

        public boolean applies() {
            return verdict.allowed();
        }
    }

    private ActiveCatalysts() {
    }

    public static boolean isOn(AtlasState state, String itemId) {
        return state.isCatalystOn(itemId);
    }

    public static Toggle toggle(AtlasState state, AtlasCatalysts.Catalyst catalyst, int owned) {
        if (state.setCatalystOn(catalyst.itemId(), false)) {
            return Toggle.OFF;
        }
        if (owned < 1) {
            return Toggle.NONE_OWNED;
        }
        state.setCatalystOn(catalyst.itemId(), true);
        return Toggle.ON;
    }

    public static void turnOffMissing(AtlasState state, ToIntFunction<String> owned) {
        for (String itemId : state.activeCatalystIds()) {
            if (AtlasCatalysts.byItemId(itemId) == null || owned.applyAsInt(itemId) < 1) {
                state.setCatalystOn(itemId, false);
            }
        }
    }

    public static List<Planned> plan(AtlasGenerator generator, AtlasState state, AtlasCell cell, ToIntFunction<String> owned) {
        List<String> present = AtlasAnomalies.effective(generator, state, cell);
        List<String> added = new ArrayList<>();
        List<Planned> planned = new ArrayList<>();
        for (String itemId : state.activeCatalystIds()) {
            AtlasCatalysts.Catalyst catalyst = AtlasCatalysts.byItemId(itemId);
            if (catalyst == null || owned.applyAsInt(itemId) < 1) {
                continue;
            }
            UseVerdict verdict = verdict(generator, state, cell, catalyst, present, added);
            if (verdict.allowed()) {
                added.add(catalyst.anomalyId());
            }
            planned.add(new Planned(catalyst, verdict));
        }
        return List.copyOf(planned);
    }

    public static List<Planned> alsoApplyingIn(List<Planned> planned, List<Planned> shown) {
        Set<String> shownApplying = shown.stream().filter(Planned::applies).map(entry -> entry.catalyst().itemId()).collect(Collectors.toSet());
        return planned.stream().filter(entry -> !entry.applies() || shownApplying.contains(entry.catalyst().itemId())).toList();
    }

    public static List<String> anomalyIds(List<Planned> planned) {
        return planned.stream().filter(Planned::applies).map(entry -> entry.catalyst().anomalyId()).toList();
    }

    public static void spend(AtlasState state, List<Planned> planned, CraftingMaterials materials) {
        planned.stream().filter(Planned::applies).forEach(entry -> materials.take(entry.catalyst().itemId(), 1));
        turnOffMissing(state, materials::count);
    }

    private static UseVerdict verdict(AtlasGenerator generator, AtlasState state, AtlasCell cell, AtlasCatalysts.Catalyst catalyst,
                                      List<String> present, List<String> added) {
        String anomalyId = catalyst.anomalyId();
        AtlasAnomalies.InsertCheck check = AtlasAnomalies.canInsert(generator, state, cell, anomalyId, null,
                catalyst.effect() == AtlasCatalysts.Effect.STAR);
        List<String> rivals = present;
        if (check.allows()) {
            check = addedCheck(anomalyId, present, added);
            rivals = added;
        }
        if (check.allows()) {
            return UseVerdict.ALLOW;
        }
        UseVerdict refused = (check == AtlasAnomalies.InsertCheck.DOES_NOT_FIT
                ? AtlasRule.of(ChartAnomalies.byId(anomalyId)).insertVerdict(generator, state, cell)
                : UseVerdict.refused(check.refusalKey())).with("anomaly", AtlasNames.anomaly(anomalyId));
        if (check == AtlasAnomalies.InsertCheck.CONFLICT) {
            String other = rivals.stream().filter(existing -> ChartAnomalies.conflicts(existing, anomalyId)).findFirst().orElse("");
            refused = refused.with("other", AtlasNames.anomaly(other));
        }
        return refused;
    }

    private static AtlasAnomalies.InsertCheck addedCheck(String anomalyId, List<String> present, List<String> added) {
        if (added.contains(anomalyId)) {
            return AtlasAnomalies.InsertCheck.ALREADY_PRESENT;
        }
        if (present.size() + added.size() >= AtlasAnomalies.MAX_PER_SYSTEM) {
            return AtlasAnomalies.InsertCheck.FULL;
        }
        boolean conflicts = added.stream().anyMatch(existing -> ChartAnomalies.conflicts(existing, anomalyId));
        return conflicts ? AtlasAnomalies.InsertCheck.CONFLICT : AtlasAnomalies.InsertCheck.ALLOWED;
    }
}
