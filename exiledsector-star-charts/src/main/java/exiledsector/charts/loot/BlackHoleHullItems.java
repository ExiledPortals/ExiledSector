package exiledsector.charts.loot;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import exiledsector.PlayerFleet;
import exiledsector.charts.ChartIds;
import exiledsector.items.ItemChances;
import exiledsector.loot.DropType;
import exiledsector.loot.LootMultipliers;
import exiledsector.skills.skilleffect.logistics.BlackHoleSalvage;

final class BlackHoleHullItems {

    private BlackHoleHullItems() {
    }

    static LootMultipliers withBonusWherePlayerIs(LootMultipliers multipliers) {
        CampaignFleetAPI playerFleet = PlayerFleet.orNull();
        return playerFleet == null ? multipliers : withBonus(multipliers, playerFleet.getContainingLocation());
    }

    static LootMultipliers withBonus(LootMultipliers multipliers, LocationAPI location) {
        ItemChances chances = multipliers.chances();
        if (!ChartIds.isChartSystem(location) || !hasBlackHole((StarSystemAPI) location)) {
            return multipliers;
        }
        ItemChances boosted = chances.plusPercent(DropType.HULL_ITEM, BlackHoleSalvage.hullItemPercent(PlayerFleet.orNull()));
        return boosted == chances ? multipliers : multipliers.withChances(boosted);
    }

    static boolean hasBlackHole(StarSystemAPI system) {
        for (PlanetAPI planet : system.getPlanets()) {
            if (planet.isStar() && planet.isBlackHole()) {
                return true;
            }
        }
        return false;
    }
}
