package exiledsector.charts.loot;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.RepairGantry;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.loot.EncounterSalvage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

final class ChartShipSalvage {

    static final List<Map.Entry<String, Float>> SCRAP_WEIGHTS = List.of(Map.entry(Commodities.METALS, 20f), Map.entry(Commodities.SUPPLIES, 10f),
            Map.entry(Commodities.FUEL, 10f), Map.entry(Commodities.HEAVY_MACHINERY, 1f));
    static final float CARGO_TAKEN_SHARE = 0.75f;

    record Rates(float weaponChance, float wingChance, float scrapValuePerFleetPoint, float cargoRecoveryShare, float fuelMult) {

        static Rates current() {
            SettingsAPI settings = Global.getSettings();
            CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
            DynamicStatsAPI playerStats = playerFleet.getStats().getDynamic();
            float creditsShare = (settings.getFloat("salvageFractionCreditsMin") + settings.getFloat("salvageFractionCreditsMax")) / 2f;
            float valueMult = playerStats.getValue(Stats.BATTLE_SALVAGE_MULT_FLEET) + RepairGantry.getAdjustedGantryModifierForPostCombatSalvage(playerFleet);
            float easyMult = Misc.isEasy() ? settings.getFloat("easySalvageMult") : 1f;
            return new Rates(playerStats.getValue(Stats.ENEMY_WEAPON_RECOVERY_MOD, settings.getFloat("salvageWeaponProb")),
                    playerStats.getValue(Stats.ENEMY_WING_RECOVERY_MOD, settings.getFloat("salvageWingProb")),
                    settings.getFloat("salvageValuePerFP") * easyMult * valueMult * (1f - creditsShare), settings.getFloat("salvageCargoFraction"),
                    playerStats.getValue(Stats.FUEL_SALVAGE_VALUE_MULT_FLEET));
        }

        Rates forPlayerShare(float playerShare) {
            return new Rates(weaponChance * playerShare, wingChance * playerShare, scrapValuePerFleetPoint * playerShare, cargoRecoveryShare * playerShare,
                    fuelMult);
        }
    }

    record LostShip(FleetMemberAPI member, float fleetPoints, List<String> weaponIds, List<String> wingIds) {

        static LostShip of(FleetMemberAPI member) {
            List<String> weaponIds = new ArrayList<>();
            List<String> wingIds = new ArrayList<>();
            addFittings(member.getVariant(), weaponIds, wingIds);
            return new LostShip(member, member.getFleetPointCost(), List.copyOf(weaponIds), List.copyOf(wingIds));
        }

        private static void addFittings(ShipVariantAPI variant, List<String> weaponIds, List<String> wingIds) {
            if (variant == null) {
                return;
            }
            for (String slotId : variant.getNonBuiltInWeaponSlots()) {
                String weaponId = variant.getWeaponId(slotId);
                if (weaponId != null) {
                    weaponIds.add(weaponId);
                }
            }
            wingIds.addAll(variant.getNonBuiltInWings());
            for (String slotId : variant.getModuleSlots()) {
                WeaponSlotAPI slot = variant.getSlot(slotId);
                if (slot != null && slot.isStationModule()) {
                    addFittings(variant.getModuleVariant(slotId), weaponIds, wingIds);
                }
            }
        }
    }

    private record LootableStack(CargoAPI.CargoItemType itemType, Object data, DropType dropType) {
    }

    private final Map<CampaignFleetAPI, List<LostShip>> lostShipsByFleet = new LinkedHashMap<>();
    private final Map<CampaignFleetAPI, Map<LootableStack, Float>> cargoBeforeLoot = new LinkedHashMap<>();

    void record(CampaignFleetAPI sourceFleet, List<FleetMemberAPI> lostMembers) {
        List<LostShip> lostShips = lostShipsByFleet.computeIfAbsent(sourceFleet, fleet -> new ArrayList<>());
        for (FleetMemberAPI member : lostMembers) {
            if (salvageable(member)) {
                lostShips.add(LostShip.of(member));
            }
        }
        cargoBeforeLoot.computeIfAbsent(sourceFleet, ChartShipSalvage::lootableStacks);
    }

    void addTo(CargoAPI loot, EncounterSalvage encounter, Random random) {
        addTo(loot, encounter, LootContexts::forFleet, Rates::current, random);
    }

    void addTo(CargoAPI loot, EncounterSalvage encounter, Function<CampaignFleetAPI, ChartGenContext> contexts, Supplier<Rates> vanillaRates,
               Random random) {
        if (!lostShipsByFleet.isEmpty() && encounter.playerShare() > 0f) {
            Rates rates = vanillaRates.get().forPlayerShare(encounter.playerShare());
            lostShipsByFleet.forEach((fleet, lostShips) -> addFor(loot, contexts.apply(fleet), fleet,
                    lostShips.stream().filter(ship -> !encounter.recovered(ship.member())).toList(), rates, random));
        }
        lostShipsByFleet.clear();
        cargoBeforeLoot.clear();
    }

    void addFor(CargoAPI loot, ChartGenContext context, CampaignFleetAPI fleet, List<LostShip> lostShips, Rates rates, Random random) {
        float weaponShare = extraShare(context, DropType.WEAPON);
        float scrapFleetPoints = 0f;
        for (LostShip ship : lostShips) {
            scrapFleetPoints += ship.fleetPoints();
            if (weaponShare > 0f) {
                addFittings(loot, ship, rates, weaponShare, random);
            }
        }
        addScrap(loot, scrapFleetPoints * rates.scrapValuePerFleetPoint() * extraShare(context, DropType.RESOURCE), rates.fuelMult(), random,
                ChartShipSalvage::basePrice);
        addCarriedCargo(loot, fleet, cargoBeforeLoot.get(fleet), context, rates, random);
    }

    static float extraShare(ChartGenContext context, DropType type) {
        return Math.max(0f, context.dropMult(type) - 1f);
    }

    private static boolean salvageable(FleetMemberAPI member) {
        return member != null && !member.isFighterWing() && (member.getHullSpec() == null || !member.getHullSpec().hasTag(Tags.NO_BATTLE_SALVAGE));
    }

    private static void addFittings(CargoAPI loot, LostShip ship, Rates rates, float extraShare, Random random) {
        SettingsAPI settings = Global.getSettings();
        for (String weaponId : ship.weaponIds()) {
            int extraWeapons = ChartVanillaLoot.stochasticRound(rates.weaponChance() * extraShare, random);
            if (extraWeapons > 0 && !settings.getWeaponSpec(weaponId).hasTag(Tags.NO_DROP)) {
                loot.addWeapons(weaponId, extraWeapons);
            }
        }
        for (String wingId : ship.wingIds()) {
            int extraWings = ChartVanillaLoot.stochasticRound(rates.wingChance() * extraShare, random);
            if (extraWings > 0 && !settings.getFighterWingSpec(wingId).hasTag(Tags.WING_NO_DROP)) {
                loot.addFighters(wingId, extraWings);
            }
        }
    }

    static void addScrap(CargoAPI loot, float scrapValue, float fuelMult, Random random, ToDoubleFunction<String> basePrice) {
        if (!(scrapValue > 0f)) {
            return;
        }
        double weightedPrice = 0d;
        for (Map.Entry<String, Float> commodity : SCRAP_WEIGHTS) {
            weightedPrice += commodity.getValue() * basePrice.applyAsDouble(commodity.getKey());
        }
        if (!(weightedPrice > 0d)) {
            return;
        }
        for (Map.Entry<String, Float> commodity : SCRAP_WEIGHTS) {
            float units = (float) (scrapValue * commodity.getValue() / weightedPrice);
            if (commodity.getKey().equals(Commodities.FUEL)) {
                units *= Math.max(1f, fuelMult);
            }
            int wholeUnits = ChartVanillaLoot.stochasticRound(units, random);
            if (wholeUnits > 0) {
                loot.addCommodity(commodity.getKey(), wholeUnits);
            }
        }
    }

    private static double basePrice(String commodityId) {
        CommoditySpecAPI spec = Global.getSector().getEconomy().getCommoditySpec(commodityId);
        return spec == null ? 0d : spec.getBasePrice();
    }

    private static Map<LootableStack, Float> lootableStacks(CampaignFleetAPI fleet) {
        Map<LootableStack, Float> stacks = new LinkedHashMap<>();
        CargoAPI cargo = fleet == null ? null : fleet.getCargo();
        if (cargo == null) {
            return stacks;
        }
        for (CargoStackAPI stack : cargo.getStacksCopy()) {
            DropType dropType = dropTypeOf(stack);
            if (dropType != null) {
                stacks.merge(new LootableStack(stack.getType(), stack.getData(), dropType), stack.getSize(), Float::sum);
            }
        }
        return stacks;
    }

    static DropType dropTypeOf(CargoStackAPI stack) {
        if (stack == null || stack.isNull() || stack.isPersonnelStack()) {
            return null;
        }
        if (stack.isWeaponStack() || stack.isFighterWingStack()) {
            return DropType.WEAPON;
        }
        if (stack.isCommodityStack()) {
            CommoditySpecAPI spec = stack.getResourceIfResource();
            return spec != null && spec.hasTag(Commodities.TAG_AI_CORE) ? DropType.AI_CORE : DropType.RESOURCE;
        }
        return null;
    }

    private static void addCarriedCargo(CargoAPI loot, CampaignFleetAPI fleet, Map<LootableStack, Float> stacksBeforeLoot, ChartGenContext context,
                                        Rates rates, Random random) {
        CargoAPI cargo = fleet == null ? null : fleet.getCargo();
        if (cargo == null || stacksBeforeLoot == null) {
            return;
        }
        stacksBeforeLoot.forEach((stack, sizeBeforeLoot) -> {
            float lost = sizeBeforeLoot - cargo.getQuantity(stack.itemType(), stack.data());
            float extra = lost * CARGO_TAKEN_SHARE * rates.cargoRecoveryShare() * extraShare(context, stack.dropType());
            int wholeExtra = ChartVanillaLoot.stochasticRound(extra, random);
            if (wholeExtra > 0) {
                loot.addItems(stack.itemType(), stack.data(), wholeExtra);
            }
        });
    }
}
