package exiledsector.charts.loot;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import exiledsector.MockSector;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartGenContext.AppliedEffect;
import exiledsector.loot.EncounterSalvage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChartShipSalvageTest {

    private static final ChartShipSalvage.Rates EVERY_FITTING = new ChartShipSalvage.Rates(1f, 1f, 0f, 0f, 1f);

    private MockSector mockSector;
    private WeaponSpecAPI noDropWeapon;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        SettingsAPI settings = mock(SettingsAPI.class);
        when(settings.getWeaponSpec(anyString())).thenReturn(mock(WeaponSpecAPI.class));
        noDropWeapon = mock(WeaponSpecAPI.class);
        when(noDropWeapon.hasTag(Tags.NO_DROP)).thenReturn(true);
        when(settings.getWeaponSpec("unique_gun")).thenReturn(noDropWeapon);
        when(settings.getFighterWingSpec(anyString())).thenReturn(mock(FighterWingSpecAPI.class));
        mockSector.global.when(Global::getSettings).thenReturn(settings);
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
    }

    private static ChartGenContext context(ChartModifier effect, float value) {
        return ChartGenContext.of(List.of(new AppliedEffect(effect, value)));
    }

    private static ShipVariantAPI variant(Map<String, String> weaponsBySlot, List<String> wings) {
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.getNonBuiltInWeaponSlots()).thenReturn(List.copyOf(weaponsBySlot.keySet()));
        weaponsBySlot.forEach((slotId, weaponId) -> when(variant.getWeaponId(slotId)).thenReturn(weaponId));
        when(variant.getNonBuiltInWings()).thenReturn(wings);
        return variant;
    }

    private static FleetMemberAPI member(ShipVariantAPI variant, int fleetPoints) {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getVariant()).thenReturn(variant);
        when(member.getFleetPointCost()).thenReturn(fleetPoints);
        when(member.getHullSpec()).thenReturn(mock(ShipHullSpecAPI.class));
        return member;
    }

    private static CargoStackAPI commodityStack(String commodityId, float size, boolean aiCore) {
        CargoStackAPI stack = mock(CargoStackAPI.class);
        when(stack.isCommodityStack()).thenReturn(true);
        when(stack.getType()).thenReturn(CargoAPI.CargoItemType.RESOURCES);
        when(stack.getData()).thenReturn(commodityId);
        when(stack.getSize()).thenReturn(size);
        CommoditySpecAPI spec = mock(CommoditySpecAPI.class);
        when(spec.hasTag(Commodities.TAG_AI_CORE)).thenReturn(aiCore);
        when(stack.getResourceIfResource()).thenReturn(spec);
        return stack;
    }

    @Test
    void quantityAddsExtraWeaponsAndWingsFromTheFittingsOfDestroyedShips() {
        CargoAPI loot = mock(CargoAPI.class);
        FleetMemberAPI destroyer = member(variant(Map.of("WS1", "heavymg", "WS2", "unique_gun"), List.of("talon_wing")), 10);

        new ChartShipSalvage().addFor(loot, context(ChartModifier.CHART_QUANTITY_PERCENT, 100f), null, List.of(ChartShipSalvage.LostShip.of(destroyer)), EVERY_FITTING,
                new Random(1L));

        verify(loot).addWeapons("heavymg", 1);
        verify(loot, never()).addWeapons(eq("unique_gun"), anyInt());
        verify(loot).addFighters("talon_wing", 1);
    }

    @Test
    void stationModulesAndTheWeaponBonusAddFittingsToo() {
        CargoAPI loot = mock(CargoAPI.class);
        ShipVariantAPI module = variant(Map.of("M1", "hellbore"), List.of());
        ShipVariantAPI station = variant(Map.of(), List.of());
        when(station.getModuleSlots()).thenReturn(List.of("MODULE"));
        WeaponSlotAPI moduleSlot = mock(WeaponSlotAPI.class);
        when(moduleSlot.isStationModule()).thenReturn(true);
        when(station.getSlot("MODULE")).thenReturn(moduleSlot);
        when(station.getModuleVariant("MODULE")).thenReturn(module);

        new ChartShipSalvage().addFor(loot, context(ChartModifier.CHART_WEAPON_PERCENT, 200f), null, List.of(ChartShipSalvage.LostShip.of(member(station, 50))), EVERY_FITTING,
                new Random(1L));

        verify(loot).addWeapons("hellbore", 2);
    }

    @Test
    void noBonusOrAShipThatGivesNoBattleSalvageAddsNothing() {
        CargoAPI loot = mock(CargoAPI.class);
        FleetMemberAPI destroyer = member(variant(Map.of("WS1", "heavymg"), List.of()), 10);
        new ChartShipSalvage().addFor(loot, ChartGenContext.none(), null, List.of(ChartShipSalvage.LostShip.of(destroyer)), EVERY_FITTING, new Random(1L));

        FleetMemberAPI unsalvageable = member(variant(Map.of("WS1", "heavymg"), List.of()), 10);
        when(unsalvageable.getHullSpec().hasTag(Tags.NO_BATTLE_SALVAGE)).thenReturn(true);
        ChartShipSalvage salvage = new ChartShipSalvage();
        salvage.record(mock(CampaignFleetAPI.class), List.of(unsalvageable));
        salvage.addTo(loot, EncounterSalvage.FULL, fleet -> context(ChartModifier.CHART_QUANTITY_PERCENT, 100f), () -> EVERY_FITTING, new Random(1L));

        verify(loot, never()).addWeapons(anyString(), anyInt());
    }

    @Test
    void fittingsAreTakenAtTheEngagementRecoveredShipsAddNothingAndAlliesShareTheBonus() {
        CargoAPI loot = mock(CargoAPI.class);
        FleetMemberAPI stripped = member(variant(Map.of("WS1", "heavymg"), List.of("talon_wing")), 10);
        FleetMemberAPI recovered = member(variant(Map.of("WS1", "hellbore"), List.of()), 10);
        ChartShipSalvage salvage = new ChartShipSalvage();
        salvage.record(mock(CampaignFleetAPI.class), List.of(stripped, recovered));
        ShipVariantAPI strippedForRecovery = variant(Map.of(), List.of());
        when(stripped.getVariant()).thenReturn(strippedForRecovery);

        salvage.addTo(loot, new EncounterSalvage(0.5f, Set.of(recovered)), fleet -> context(ChartModifier.CHART_WEAPON_PERCENT, 200f),
                () -> EVERY_FITTING, new Random(1L));

        verify(loot).addWeapons("heavymg", 1);
        verify(loot).addFighters("talon_wing", 1);
        verify(loot, never()).addWeapons(eq("hellbore"), anyInt());
    }

    @Test
    void extraScrapSplitsItsValueLikeVanillaBattleSalvage() {
        CargoAPI loot = mock(CargoAPI.class);
        Map<String, Double> prices = Map.of(Commodities.METALS, 30d, Commodities.SUPPLIES, 100d, Commodities.FUEL, 25d, Commodities.HEAVY_MACHINERY, 150d);

        ChartShipSalvage.addScrap(loot, 2000f, 1.5f, new Random(1L), prices::get);

        verify(loot).addCommodity(Commodities.METALS, 20f);
        verify(loot).addCommodity(Commodities.SUPPLIES, 10f);
        verify(loot).addCommodity(Commodities.FUEL, 15f);
        verify(loot).addCommodity(Commodities.HEAVY_MACHINERY, 1f);
    }

    @Test
    void cargoTheDefeatedFleetLostToLootingIsRecoveredAgainByTheBonus() {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        CargoAPI fleetCargo = mock(CargoAPI.class);
        when(fleet.getCargo()).thenReturn(fleetCargo);
        CargoStackAPI crew = mock(CargoStackAPI.class);
        when(crew.isPersonnelStack()).thenReturn(true);
        List<CargoStackAPI> carried = List.of(commodityStack(Commodities.SUPPLIES, 100f, false), commodityStack(Commodities.ALPHA_CORE, 1f, true), crew);
        when(fleetCargo.getStacksCopy()).thenReturn(carried);
        ChartShipSalvage salvage = new ChartShipSalvage();
        salvage.record(fleet, List.of());
        when(fleetCargo.getQuantity(CargoAPI.CargoItemType.RESOURCES, Commodities.SUPPLIES)).thenReturn(20f);
        when(fleetCargo.getQuantity(CargoAPI.CargoItemType.RESOURCES, Commodities.ALPHA_CORE)).thenReturn(0f);
        CargoAPI loot = mock(CargoAPI.class);

        salvage.addFor(loot, context(ChartModifier.CHART_RESOURCE_PERCENT, 100f), fleet, List.of(), new ChartShipSalvage.Rates(0f, 0f, 0f, 0.5f, 1f),
                new Random(1L));

        verify(loot).addItems(CargoAPI.CargoItemType.RESOURCES, Commodities.SUPPLIES, 30f);
        verify(loot, never()).addItems(eq(CargoAPI.CargoItemType.RESOURCES), eq(Commodities.ALPHA_CORE), anyFloat());
        assertEquals(DropType.AI_CORE, ChartShipSalvage.dropTypeOf(commodityStack(Commodities.ALPHA_CORE, 1f, true)));
        assertNull(ChartShipSalvage.dropTypeOf(crew));
    }
}
