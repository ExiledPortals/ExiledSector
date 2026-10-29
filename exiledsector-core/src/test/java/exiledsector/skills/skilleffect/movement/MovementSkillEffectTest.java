package exiledsector.skills.skilleffect.movement;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MovementSkillEffectTest {

    private MockedStatic<Global> globalMock;

    private SettingsAPI settings;

    private MutableShipStatsAPI stats;

    private DynamicStatsAPI dynamic;

    private ShipAPI ship;

    @BeforeEach
    void setUp() {
        settings = mock(SettingsAPI.class);
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSettings).thenReturn(settings);
        globalMock.when(Global::getCombatEngine).thenReturn(engine);
        stats = mock(MutableShipStatsAPI.class);
        dynamic = mock(DynamicStatsAPI.class);
        ship = mock(ShipAPI.class);
        when(stats.getDynamic()).thenReturn(dynamic);
        when(ship.getMutableStats()).thenReturn(stats);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    private void giveDMods(int count) {
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        HullModSpecAPI dmod = mock(HullModSpecAPI.class);
        when(dmod.hasTag(Tags.HULLMOD_DMOD)).thenReturn(true);
        when(settings.getHullModSpec(anyString())).thenReturn(dmod);
        when(variant.getHullMods()).thenReturn(IntStream.range(0, count).mapToObj(i -> "dmod_" + i).toList());
        when(stats.getVariant()).thenReturn(variant);
    }

    @Test
    void perDModTopSpeedCompoundsAndStopsCountingAtFiveDMods() {
        giveDMods(7);
        MutableStat maxSpeed = new MutableStat(100f);
        when(stats.getMaxSpeed()).thenReturn(maxSpeed);

        MovementSkillEffect.TOP_SPEED_MULT_PER_DMOD.apply(stats, "mod_id", 2f);

        assertEquals(100f * (float) Math.pow(1.02, 5), maxSpeed.getModifiedValue(), 0.001f);
    }
}
