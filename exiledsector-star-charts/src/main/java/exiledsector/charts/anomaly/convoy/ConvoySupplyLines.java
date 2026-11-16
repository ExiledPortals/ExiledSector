package exiledsector.charts.anomaly.convoy;

import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.reward.CarriedItem;
import exiledsector.charts.officer.reward.RewardKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

final class ConvoySupplyLines {

    private ConvoySupplyLines() {
    }

    static boolean isOn(ChartGenContext context) {
        return ChartGenContext.orNone(context).anomalyPercent(SupplyConvoy.OFFICER_FEEDSTOCK_KEY) > 0f;
    }

    static AtlasOfficer handOver(AtlasState state, ConvoyState convoy, Random random) {
        List<CarriedItem> feedstock = carriedFeedstock(convoy.feedstock());
        if (state == null || feedstock.isEmpty()) {
            return null;
        }
        List<AtlasOfficer> active = state.officers().stream().filter(AtlasOfficer::isActive).toList();
        if (active.isEmpty()) {
            return null;
        }
        AtlasOfficer officer = active.get(random.nextInt(active.size()));
        AtlasOfficer supplied = officer.withCarried(CarriedItem.joined(officer.carried(), feedstock));
        state.putOfficer(supplied);
        return supplied;
    }

    static List<CarriedItem> carriedFeedstock(Map<String, Float> feedstock) {
        List<CarriedItem> carried = new ArrayList<>();
        for (Map.Entry<String, Float> entry : feedstock.entrySet()) {
            int count = Math.round(entry.getValue());
            if (count >= 1 && CarriedItem.isSafeId(entry.getKey())) {
                carried.add(new CarriedItem(RewardKind.FEEDSTOCK, entry.getKey(), null, count));
            }
        }
        return carried;
    }
}
