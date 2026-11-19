package exiledsector.charts.anomaly.stormfront;

import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.ids.ShipRoles;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedactedFrontTest {

    private static void variant(SettingsAPI settings, String variantId, HullSize hullSize) {
        ShipHullSpecAPI hull = mock(ShipHullSpecAPI.class);
        when(hull.getHullSize()).thenReturn(hullSize);
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.getHullSpec()).thenReturn(hull);
        when(settings.doesVariantExist(variantId)).thenReturn(true);
        when(settings.getVariant(variantId)).thenReturn(variant);
    }

    @Test
    void onlyTheFactionsSmallCombatFrigatesJoin() {
        SettingsAPI settings = mock(SettingsAPI.class);
        variant(settings, "glimmer_Assault", HullSize.FRIGATE);
        variant(settings, "fulgent_Assault", HullSize.DESTROYER);
        FactionAPI remnant = mock(FactionAPI.class);
        when(remnant.getVariantsForRole(ShipRoles.COMBAT_SMALL))
                .thenReturn(new LinkedHashSet<>(List.of("glimmer_Assault", "fulgent_Assault", "missing_variant")));

        assertEquals(List.of("glimmer_Assault"), RedactedFront.frigateVariants(remnant, settings));
        assertEquals(List.of(), RedactedFront.frigateVariants(null, settings));
    }

    @Test
    void theJoiningCountStaysWithinTheLinesRange() {
        Random random = new Random(3L);
        Set<Integer> seen = new LinkedHashSet<>();
        for (int roll = 0; roll < 200; roll++) {
            int joining = RedactedFront.joiningCount(5, 10, random);
            assertTrue(joining >= 5 && joining <= 10, "joined " + joining);
            seen.add(joining);
        }
        assertEquals(6, seen.size());
        assertEquals(0, RedactedFront.joiningCount(0, 0, random));
    }
}
