package exiledsector.skills.skilleffect.logistics;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeploymentPointsPerDModTest {

    private static MutableShipStatsAPI statsWithDMods(int count, StatBonus deploymentPoints) {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS);
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.getHullMods()).thenReturn(IntStream.range(0, count).mapToObj(i -> "dmod_" + i).toList());
        when(stats.getVariant()).thenReturn(variant);
        when(stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD)).thenReturn(deploymentPoints);
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
    void deploymentPointsDropForEachDModUpToFive() {
        StatBonus fewDMods = new StatBonus();
        StatBonus manyDMods = new StatBonus();

        try (MockedStatic<Global> ignored = dModSettings()) {
            LogisticsSkillEffect.DEPLOYMENT_POINTS_PERCENT_PER_DMOD.apply(statsWithDMods(2, fewDMods), "mod_id", -2f);
            LogisticsSkillEffect.DEPLOYMENT_POINTS_PERCENT_PER_DMOD.apply(statsWithDMods(7, manyDMods), "mod_id", -2f);
        }

        assertEquals(96f, fewDMods.computeEffective(100f), 0.001f);
        assertEquals(90f, manyDMods.computeEffective(100f), 0.001f);
        assertTrue(LogisticsSkillEffect.DEPLOYMENT_POINTS_PERCENT_PER_DMOD.lowerIsBetter());
    }
}
