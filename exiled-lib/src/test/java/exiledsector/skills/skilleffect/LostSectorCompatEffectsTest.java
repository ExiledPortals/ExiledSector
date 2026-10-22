package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.StatBonus;
import exiledsector.compat.LostSectorCompat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LostSectorCompatEffectsTest {

    private static final List<SkillEffect> AUGMENTED_EFFECTS = List.of(CompatSkillEffect.AUGMENTED_SYSTEM_REGEN_PERCENT,
            CombatSkillEffect.AUGMENTED_FLUX_SCALED_PENALTY_REDUCTION_PERCENT,
            CombatSkillEffect.AUGMENTED_INERTIAL_PROJECTILE_SPEED_PERCENT);

    private MockedStatic<Global> globalMock;
    private ModManagerAPI mods;

    @BeforeEach
    void setUp() {
        SettingsAPI settings = mock(SettingsAPI.class);
        mods = mock(ModManagerAPI.class);
        when(settings.getModManager()).thenReturn(mods);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSettings).thenReturn(settings);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    private static ShipVariantAPI variant(boolean augmented) {
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.hasHullMod(LostSectorCompat.AUGMENTED_SYSTEMS_HULLMOD_ID)).thenReturn(augmented);
        return variant;
    }

    @Test
    void augmentedSystemsOnlyCountsWhileLostSectorIsEnabled() {
        when(mods.isModEnabled("lost_sector")).thenReturn(true);
        assertTrue(LostSectorCompat.hasAugmentedSystems(variant(true)));
        assertFalse(LostSectorCompat.hasAugmentedSystems(variant(false)));

        when(mods.isModEnabled("lost_sector")).thenReturn(false);
        assertFalse(LostSectorCompat.hasAugmentedSystems(variant(true)));
    }

    @Test
    void bothTheReleasedAndTheUpcomingLostSectorModIdsCount() {
        when(mods.isModEnabled("lost_sector")).thenReturn(true);
        assertTrue(LostSectorCompat.isModEnabled());

        when(mods.isModEnabled("lost_sector")).thenReturn(false);
        when(mods.isModEnabled("lost.sector")).thenReturn(true);
        assertTrue(LostSectorCompat.isModEnabled());

        when(mods.isModEnabled("lost.sector")).thenReturn(false);
        assertFalse(LostSectorCompat.isModEnabled());
    }

    @Test
    void theRechargeBonusAppliesOnlyToAugmentedHullsWithLostSectorEnabled() {
        when(mods.isModEnabled("lost_sector")).thenReturn(true);
        ShipVariantAPI augmentedVariant = variant(true);
        ShipVariantAPI plainVariant = variant(false);
        MutableShipStatsAPI augmented = mock(MutableShipStatsAPI.class);
        StatBonus augmentedRegen = mock(StatBonus.class);
        when(augmented.getVariant()).thenReturn(augmentedVariant);
        when(augmented.getSystemRegenBonus()).thenReturn(augmentedRegen);
        MutableShipStatsAPI plain = mock(MutableShipStatsAPI.class);
        StatBonus plainRegen = mock(StatBonus.class);
        when(plain.getVariant()).thenReturn(plainVariant);
        when(plain.getSystemRegenBonus()).thenReturn(plainRegen);

        CompatSkillEffect.AUGMENTED_SYSTEM_REGEN_PERCENT.apply(augmented, "node", 10f);
        CompatSkillEffect.AUGMENTED_SYSTEM_REGEN_PERCENT.apply(plain, "node", 10f);

        verify(augmentedRegen).modifyPercent("node", 10f);
        verify(plainRegen, never()).modifyPercent(anyString(), anyFloat());
        verify(plainRegen).unmodify("node");
    }

    @Test
    void theAugmentedBonusesOnlyAppearInTooltipsWhenLostSectorIsInstalled() {
        when(mods.isModEnabled("lost_sector")).thenReturn(false);
        for (SkillEffect effect : AUGMENTED_EFFECTS) {
            assertNull(effect.description(10f), effect.toString());
        }

        when(mods.isModEnabled("lost_sector")).thenReturn(true);
        for (SkillEffect effect : AUGMENTED_EFFECTS) {
            assertNotNull(effect.description(10f), effect.toString());
        }
    }
}
