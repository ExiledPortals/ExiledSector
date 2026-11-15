package exiledsector.charts.phasing.trait;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import exiledsector.i18n.Translation;

import java.util.List;

public sealed interface TraitEffect {

    void apply(MarketAPI market, String modId, String source);

    void unapply(MarketAPI market, String modId);

    String line();

    default boolean harmful() {
        return false;
    }

    private static int percent(float fraction) {
        return Math.round(fraction * 100f);
    }

    record Hazard(float points) implements TraitEffect {
        public void apply(MarketAPI market, String modId, String source) {
            market.getHazard().modifyFlat(modId, points, source);
        }

        public void unapply(MarketAPI market, String modId) {
            market.getHazard().unmodifyFlat(modId);
        }

        public String line() {
            return Translation.msg("chart.phasing.trait.hazard").arg("value", percent(points)).text();
        }

        @Override
        public boolean harmful() {
            return points > 0f;
        }
    }

    record Accessibility(float points) implements TraitEffect {
        public void apply(MarketAPI market, String modId, String source) {
            market.getAccessibilityMod().modifyFlat(modId, points, source);
        }

        public void unapply(MarketAPI market, String modId) {
            market.getAccessibilityMod().unmodifyFlat(modId);
        }

        public String line() {
            return Translation.msg("chart.phasing.trait.accessibility").arg("value", percent(points)).text();
        }
    }

    record MaxIndustries(int count) implements TraitEffect {
        public void apply(MarketAPI market, String modId, String source) {
            market.getStats().getDynamic().getMod(Stats.MAX_INDUSTRIES).modifyFlat(modId, count, source);
        }

        public void unapply(MarketAPI market, String modId) {
            market.getStats().getDynamic().getMod(Stats.MAX_INDUSTRIES).unmodifyFlat(modId);
        }

        public String line() {
            return Translation.msg("chart.phasing.trait.maxIndustries").arg("value", count).text();
        }
    }

    record ShipQuality(float points) implements TraitEffect {
        public void apply(MarketAPI market, String modId, String source) {
            market.getStats().getDynamic().getMod(Stats.PRODUCTION_QUALITY_MOD).modifyFlat(modId, points, source);
        }

        public void unapply(MarketAPI market, String modId) {
            market.getStats().getDynamic().getMod(Stats.PRODUCTION_QUALITY_MOD).unmodifyFlat(modId);
        }

        public String line() {
            return Translation.msg("chart.phasing.trait.shipQuality").arg("value", percent(points)).text();
        }
    }

    record GroundDefences(float multiplier) implements TraitEffect {
        public void apply(MarketAPI market, String modId, String source) {
            market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).modifyMult(modId, multiplier, source);
        }

        public void unapply(MarketAPI market, String modId) {
            market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).unmodifyMult(modId);
        }

        public String line() {
            return Translation.msg("chart.phasing.trait.groundDefences").arg("value", percent(multiplier - 1f)).text();
        }
    }

    record Supply(String commodityId, int amount, List<String> industryIds) implements TraitEffect {
        public Supply {
            industryIds = List.copyOf(industryIds);
        }

        public void apply(MarketAPI market, String modId, String source) {
            for (String industryId : industryIds) {
                Industry industry = market.getIndustry(industryId);
                if (industry == null) {
                    continue;
                }
                if (industry.isFunctional()) {
                    industry.getSupply(commodityId).getQuantity().modifyFlat(modId, amount, source);
                } else {
                    industry.getSupply(commodityId).getQuantity().unmodifyFlat(modId);
                }
            }
        }

        public void unapply(MarketAPI market, String modId) {
            for (String industryId : industryIds) {
                Industry industry = market.getIndustry(industryId);
                if (industry != null) {
                    industry.getSupply(commodityId).getQuantity().unmodifyFlat(modId);
                }
            }
        }

        public String line() {
            CommoditySpecAPI commodity = Global.getSettings().getCommoditySpec(commodityId);
            String commodityName = commodity == null ? commodityId : commodity.getLowerCaseName();
            return Translation.msg("chart.phasing.trait.supply").arg("value", amount).arg("commodity", commodityName).text();
        }
    }
}
