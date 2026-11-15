package exiledsector.i18n;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.MarketConditionSpecAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.loading.IndustrySpecAPI;

public final class SpecLocalisation {

    private SpecLocalisation() {
    }

    public static void hullMod(String hullModId) {
        HullModSpecAPI hullModSpec = Global.getSettings().getHullModSpec(hullModId);
        if (hullModSpec == null) {
            return;
        }
        hullModSpec.setDisplayName(Translation.data("hullmod." + hullModId + ".name", hullModSpec.getDisplayName()));
        hullModSpec.setDescriptionFormat(Translation.data("hullmod." + hullModId + ".description", hullModSpec.getDescriptionFormat()));
    }

    public static void marketCondition(String conditionId) {
        MarketConditionSpecAPI conditionSpec = Global.getSettings().getMarketConditionSpec(conditionId);
        if (conditionSpec != null) {
            conditionSpec.setName(Translation.data("condition." + conditionId + ".name", conditionSpec.getName()));
            conditionSpec.setDesc(Translation.data("condition." + conditionId + ".description", conditionSpec.getDesc()));
        }
    }

    public static void industry(String industryId) {
        IndustrySpecAPI industrySpec = Global.getSettings().getIndustrySpec(industryId);
        if (industrySpec != null) {
            industrySpec.setName(Translation.data("industry." + industryId + ".name", industrySpec.getName()));
            industrySpec.setDesc(Translation.data("industry." + industryId + ".description", industrySpec.getDesc()));
        }
    }
}
