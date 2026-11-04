package exiledsector.skills.skilleffect.logistics;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FuelAndSupplyPerDModTest {

    private static MutableShipStatsAPI statsWithDMods(int count) {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.getHullMods()).thenReturn(IntStream.range(0, count).mapToObj(i -> "dmod_" + i).toList());
        when(stats.getVariant()).thenReturn(variant);
        return stats;
    }

    private static MockedStatic<Global> dModSettings() {
        SettingsAPI settings = mock(SettingsAPI.class);
        HullModSpecAPI dMod = mock(HullModSpecAPI.class);
        when(dMod.hasTag(Tags.HULLMOD_DMOD)).thenReturn(true);
        when(settings.getHullModSpec(anyString())).thenReturn(dMod);
        MockedStatic<Global> global = Mockito.mockStatic(Global.class);
        global.when(Global::getSettings).thenReturn(settings);
        return global;
    }

    @Test
    void fuelUseAndMaintenanceBothDropForEachDModUpToFive() {
        MutableShipStatsAPI stats = statsWithDMods(6);
        StatBonus fuelUse = new StatBonus();
        MutableStat suppliesPerMonth = new MutableStat(10f);
        when(stats.getFuelUseMod()).thenReturn(fuelUse);
        when(stats.getSuppliesPerMonth()).thenReturn(suppliesPerMonth);

        try (MockedStatic<Global> ignored = dModSettings()) {
            LogisticsSkillEffect.FUEL_AND_SUPPLY_USE_MULT_PER_DMOD.apply(stats, "mod_id", -3f);
        }

        float expectedMult = (float) Math.pow(0.97, 5);
        assertEquals(100f * expectedMult, fuelUse.computeEffective(100f), 0.001f);
        assertEquals(10f * expectedMult, suppliesPerMonth.getModifiedValue(), 0.001f);
        assertTrue(LogisticsSkillEffect.FUEL_AND_SUPPLY_USE_MULT_PER_DMOD.lowerIsBetter());
    }
}
