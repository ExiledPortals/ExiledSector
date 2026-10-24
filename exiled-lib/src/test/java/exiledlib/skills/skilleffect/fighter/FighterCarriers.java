package exiledlib.skills.skilleffect.fighter;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class FighterCarriers {

    private FighterCarriers() {
    }

    static ShipAPI with(String magnitudeKey, float magnitude) {
        ShipAPI carrier = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        when(carrier.getMutableStats()).thenReturn(stats);
        when(stats.getDynamic()).thenReturn(dynamic);
        when(dynamic.getValue(magnitudeKey, 0f)).thenReturn(magnitude);
        return carrier;
    }
}
