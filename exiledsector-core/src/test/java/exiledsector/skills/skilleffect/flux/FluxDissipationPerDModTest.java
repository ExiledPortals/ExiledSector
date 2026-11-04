package exiledsector.skills.skilleffect.flux;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FluxDissipationPerDModTest {

    @Test
    void fluxDissipationRisesForEachDMod() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.getHullMods()).thenReturn(List.of("degraded_engines", "faulty_grid"));
        when(stats.getVariant()).thenReturn(variant);
        MutableStat dissipation = new MutableStat(100f);
        when(stats.getFluxDissipation()).thenReturn(dissipation);
        SettingsAPI settings = mock(SettingsAPI.class);
        HullModSpecAPI dMod = mock(HullModSpecAPI.class);
        when(dMod.hasTag(Tags.HULLMOD_DMOD)).thenReturn(true);
        when(settings.getHullModSpec(anyString())).thenReturn(dMod);

        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            global.when(Global::getSettings).thenReturn(settings);
            FluxSkillEffect.FLUX_DISSIPATION_MULT_PER_DMOD.apply(stats, "mod_id", 3f);
        }

        assertEquals(100f * (float) Math.pow(1.03, 2), dissipation.getModifiedValue(), 0.001f);
    }
}
