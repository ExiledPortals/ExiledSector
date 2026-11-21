package exiledsector.charts.phasing;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.ItemEffectsRepo;
import com.fs.starfarer.api.impl.campaign.econ.impl.PopulationAndInfrastructure;
import com.fs.starfarer.api.util.Pair;
import exiledsector.compat.VaultsOfKnowledgeCompat;

public final class HypershuntPower {

    private HypershuntPower() {
    }

    public static int rangeLightYears() {
        return ItemEffectsRepo.CORONAL_TAP_LIGHT_YEARS;
    }

    public static boolean reaches(MarketAPI market) {
        SectorEntityToken colony = market == null ? null : market.getPrimaryEntity();
        if (colony == null) {
            return false;
        }
        return vanillaHypershuntInRange(colony) || VaultsOfKnowledgeCompat.isWithinHypershuntRange(colony);
    }

    static boolean vanillaHypershuntInRange(SectorEntityToken colony) {
        if (colony.getLocationInHyperspace() == null) {
            return false;
        }
        Pair<SectorEntityToken, Float> nearest = PopulationAndInfrastructure.getNearestCoronalTap(colony.getLocationInHyperspace(), true);
        return nearest != null && nearest.two != null && nearest.two <= rangeLightYears();
    }
}
