package exiledsector.charts.loot;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import exiledsector.PlayerFleet;
import exiledsector.items.ItemChances;
import exiledsector.items.ItemKind;
import exiledsector.loot.LootMultipliers;
import exiledsector.skills.skilleffect.logistics.BlackHoleSalvage;

import java.util.EnumMap;
import java.util.Map;

final class BlackHoleHullItems {

    private BlackHoleHullItems() {
    }

    static LootMultipliers withBonusWherePlayerIs(LootMultipliers multipliers) {
        CampaignFleetAPI playerFleet = PlayerFleet.orNull();
        return playerFleet == null ? multipliers : withBonus(multipliers, playerFleet.getContainingLocation());
    }

    static LootMultipliers withBonus(LootMultipliers multipliers, LocationAPI location) {
        ItemChances chances = multipliers.itemChances();
        if (!ChartDrops.isSystem(location) || !hasBlackHole((StarSystemAPI) location)) {
            return multipliers;
        }
        ItemChances boosted = withExtraHullItemShare(chances, BlackHoleSalvage.hullItemPercent(PlayerFleet.orNull()) / 100f);
        return boosted == chances ? multipliers : new LootMultipliers(boosted, multipliers.feedstockMult(), multipliers.currencyMult());
    }

    static boolean hasBlackHole(StarSystemAPI system) {
        for (PlanetAPI planet : system.getPlanets()) {
            if (planet.isStar() && planet.isBlackHole()) {
                return true;
            }
        }
        return false;
    }

    static ItemChances withExtraHullItemShare(ItemChances chances, float extraShare) {
        if (!(extraShare > 0f)) {
            return chances;
        }
        Map<ItemKind, Float> targeted = new EnumMap<>(ItemKind.class);
        targeted.putAll(chances.targeted());
        for (ItemKind kind : ItemKind.sectionItemTypes()) {
            targeted.merge(kind, extraShare, Float::sum);
        }
        return new ItemChances(chances.general(), targeted, chances.rarity());
    }
}
