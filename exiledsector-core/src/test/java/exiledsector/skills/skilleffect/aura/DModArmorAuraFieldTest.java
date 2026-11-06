package exiledsector.skills.skilleffect.aura;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import exiledsector.skills.skilleffect.SkillEffectSupport;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DModArmorAuraFieldTest {

    private static final float EPSILON = 1e-4f;

    private static ShipAPI ship(int owner, Vector2f location, int dMods, float percentPerDMod) {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS);
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.getHullMods()).thenReturn(IntStream.range(0, dMods).mapToObj(i -> "dmod_" + i).toList());
        when(ship.getVariant()).thenReturn(variant);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.getMutableStats()).thenReturn(stats);
        when(stats.getDynamic().getValue(DModArmorAuraListener.PERCENT_PER_DMOD_KEY, 0f)).thenReturn(percentPerDMod);
        when(ship.isAlive()).thenReturn(true);
        when(ship.getLocation()).thenReturn(location);
        return ship;
    }

    private static DModArmorAuraListener auraSource(ShipAPI ship) {
        SettingsAPI settings = mock(SettingsAPI.class);
        HullModSpecAPI dModSpec = mock(HullModSpecAPI.class);
        when(settings.getHullModSpec(anyString())).thenReturn(dModSpec);
        when(dModSpec.hasTag(Tags.HULLMOD_DMOD)).thenReturn(true);
        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            global.when(Global::getSettings).thenReturn(settings);
            return new DModArmorAuraListener(ship);
        }
    }

    @Test
    void eachDModOnTheSourceCountsUpToTheCap() {
        assertEquals(3f, auraSource(ship(0, new Vector2f(), 3, 1f)).armorDamageTakenReductionPercent(), EPSILON);
        assertEquals(SkillEffectSupport.MAX_COUNTED_DMODS * 2f, auraSource(ship(0, new Vector2f(), 8, 2f)).armorDamageTakenReductionPercent(), EPSILON);
        assertEquals(0f, auraSource(ship(0, new Vector2f(), 0, 1f)).armorDamageTakenReductionPercent(), EPSILON);
    }

    @Test
    void aRetreatingSourceStopsCoveringItsAllies() {
        ShipAPI source = ship(0, new Vector2f(), 3, 1f);
        when(source.isRetreating()).thenReturn(true);

        assertEquals(0f, auraSource(source).armorDamageTakenReductionPercent(), EPSILON);
    }

    @Test
    void onlyTheStrongestSourceAppliesToEachAllyInRange() {
        ShipAPI strongSource = ship(0, new Vector2f(), 4, 1f);
        ShipAPI weakSource = ship(0, new Vector2f(), 2, 1f);
        ShipAPI ally = ship(0, new Vector2f(DModArmorAuraField.RANGE, 0f), 0, 0f);
        ShipAPI distantAlly = ship(0, new Vector2f(DModArmorAuraField.RANGE + 1f, 0f), 0, 0f);
        ShipAPI enemy = ship(1, new Vector2f(), 0, 0f);
        ShipAPI fighter = ship(0, new Vector2f(), 0, 0f);
        when(fighter.isFighter()).thenReturn(true);
        List<ShipAPI> everyone = List.of(strongSource, weakSource, ally, distantAlly, enemy, fighter);
        AuraField.ShipsNear near = (source, radius, filter) -> everyone.stream().filter(filter).toList();

        Map<ShipAPI, Float> reductions = DModArmorAuraField.strongestPerAlly(List.of(auraSource(weakSource), auraSource(strongSource)), near);

        assertEquals(4f, reductions.get(ally), EPSILON);
        assertEquals(4f, reductions.get(weakSource), EPSILON);
        assertEquals(2f, reductions.get(strongSource), EPSILON);
        assertFalse(reductions.containsKey(distantAlly));
        assertFalse(reductions.containsKey(enemy));
        assertFalse(reductions.containsKey(fighter));
    }

    @Test
    void theReductionAddsToTheAllysOtherArmorDamageTakenPercentsAndClearsAgain() {
        ShipAPI ally = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat armorDamageTaken = new MutableStat(1f);
        armorDamageTaken.modifyPercent("other", -10f);
        when(ally.getMutableStats()).thenReturn(stats);
        when(stats.getArmorDamageTakenMult()).thenReturn(armorDamageTaken);

        DModArmorAuraField.apply(ally, 5f);

        assertEquals(0.85f, armorDamageTaken.getModifiedValue(), EPSILON);

        DModArmorAuraField.remove(ally);

        assertEquals(0.9f, armorDamageTaken.getModifiedValue(), EPSILON);
    }
}
