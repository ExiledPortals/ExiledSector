package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.HullModEffect;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SkillEffectSupportTest {

    @Test
    void multFromConvertsAPercentMagnitudeIntoAMultiplier() {
        assertEquals(1.1f, SkillEffectSupport.multFrom(10f), 0.0001f);
        assertEquals(0.9f, SkillEffectSupport.multFrom(-10f), 0.0001f);
        assertEquals(1f, SkillEffectSupport.multFrom(0f), 0.0001f);
    }

    @Test
    void applyMultOnAMutableStatUsesTheConvertedMultiplier() {
        MutableStat stat = mock(MutableStat.class);

        SkillEffectSupport.applyMult(stat, "mod_id", 25f);

        verify(stat).modifyMult("mod_id", 1.25f);
    }

    @Test
    void applyMultOnAStatBonusUsesTheConvertedMultiplier() {
        StatBonus stat = mock(StatBonus.class);

        SkillEffectSupport.applyMult(stat, "mod_id", -25f);

        verify(stat).modifyMult("mod_id", 0.75f);
    }

    @Test
    void applyDModEffectMultSetsTheVanillaStatAndReappliesOnlyDMods() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        MutableStat dmodEffectMult = mock(MutableStat.class);
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        SettingsAPI settings = mock(SettingsAPI.class);
        HullModSpecAPI dmodSpec = mock(HullModSpecAPI.class);
        HullModSpecAPI otherSpec = mock(HullModSpecAPI.class);
        HullModEffect dmodEffect = mock(HullModEffect.class);
        HullModEffect otherEffect = mock(HullModEffect.class);
        when(stats.getDynamic()).thenReturn(dynamic);
        when(dynamic.getStat(Stats.DMOD_EFFECT_MULT)).thenReturn(dmodEffectMult);
        when(stats.getVariant()).thenReturn(variant);
        when(variant.getHullMods()).thenReturn(List.of("degraded_engines", "heavyarmor"));
        when(variant.getHullSize()).thenReturn(HullSize.CRUISER);
        when(settings.getHullModSpec("degraded_engines")).thenReturn(dmodSpec);
        when(settings.getHullModSpec("heavyarmor")).thenReturn(otherSpec);
        when(dmodSpec.hasTag(Tags.HULLMOD_DMOD)).thenReturn(true);
        when(dmodSpec.getEffect()).thenReturn(dmodEffect);
        when(dmodSpec.getId()).thenReturn("degraded_engines");
        when(otherSpec.getEffect()).thenReturn(otherEffect);

        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            global.when(Global::getSettings).thenReturn(settings);

            SkillEffectSupport.applyDModEffectMult(stats, "mod_id", -5f);
        }

        verify(dmodEffectMult).modifyMult("mod_id", 0.95f);
        verify(dmodEffect).applyEffectsBeforeShipCreation(HullSize.CRUISER, stats, "degraded_engines");
        verifyNoInteractions(otherEffect);
    }

    private static float compoundMultWithDMods(int dModCount, float magnitudePerDMod) {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        SettingsAPI settings = mock(SettingsAPI.class);
        HullModSpecAPI dmodSpec = mock(HullModSpecAPI.class);
        when(stats.getVariant()).thenReturn(variant);
        when(variant.getHullMods()).thenReturn(IntStream.range(0, dModCount).mapToObj(i -> "dmod_" + i).toList());
        when(settings.getHullModSpec(anyString())).thenReturn(dmodSpec);
        when(dmodSpec.hasTag(Tags.HULLMOD_DMOD)).thenReturn(true);

        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            global.when(Global::getSettings).thenReturn(settings);
            return SkillEffectSupport.compoundMultPerDMod(stats, magnitudePerDMod);
        }
    }

    @Test
    void perDModMultipliersCompoundForEachDModUpToTheCap() {
        assertEquals((float) Math.pow(1.02, 3), compoundMultWithDMods(3, 2f), 0.0001f);
    }

    @Test
    void perDModMultipliersCountAtMostFiveDMods() {
        assertEquals(5, SkillEffectSupport.MAX_COUNTED_DMODS);
        assertEquals((float) Math.pow(1.02, 5), compoundMultWithDMods(7, 2f), 0.0001f);
        assertEquals((float) Math.pow(0.98, 5), compoundMultWithDMods(7, -2f), 0.0001f);
    }

    @Test
    void pctMoreDescribesAPositiveMagnitudeAsMore() {
        assertEquals("<good>30%</good> more beam weapon damage.", StatMode.MULT.description(30f, "beam weapon damage").toMarkup());
    }

    @Test
    void pctMoreDescribesANegativeMagnitudeAsLess() {
        assertEquals("<bad>20%</bad> less flux dissipation.", StatMode.MULT.description(-20f, "flux dissipation").toMarkup());
    }

    @Test
    void multAndPercentEffectsOnTheSameStatDescribeThemselvesDifferently() {
        String percentText = FluxSkillEffect.FLUX_DISSIPATION_PERCENT.description(30f).plain();
        String multText = FluxSkillEffect.FLUX_DISSIPATION_MULT.description(30f).plain();

        assertEquals("30% increased flux dissipation.", percentText);
        assertEquals("30% more flux dissipation.", multText);
    }
}
