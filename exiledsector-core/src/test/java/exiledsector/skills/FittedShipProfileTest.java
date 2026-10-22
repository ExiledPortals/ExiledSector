package exiledsector.skills;

import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import exiledsector.skills.skilleffect.FighterSkillEffect;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FittedShipProfileTest {

    private static ShipVariantAPI variantWith(String... hullModIds) {
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        Set<String> fitted = Set.of(hullModIds);
        when(variant.hasHullMod(anyString())).thenAnswer(call -> fitted.contains(call.<String>getArgument(0)));
        return variant;
    }

    @Test
    void shieldShuntRemovesShieldsAndAMakeshiftGeneratorAddsThem() {
        assertEquals(ShieldType.NONE, FittedShipProfile.fittedShieldType(ShieldType.OMNI, variantWith(HullMods.SHIELD_SHUNT)));
        assertEquals(ShieldType.FRONT, FittedShipProfile.fittedShieldType(ShieldType.NONE, variantWith(HullMods.MAKESHIFT_GENERATOR)));
        assertEquals(ShieldType.OMNI, FittedShipProfile.fittedShieldType(ShieldType.OMNI, variantWith(HullMods.MAKESHIFT_GENERATOR)));
        assertEquals(ShieldType.PHASE, FittedShipProfile.fittedShieldType(ShieldType.PHASE, variantWith(HullMods.SHIELD_SHUNT)));
        assertEquals(ShieldType.FRONT, FittedShipProfile.fittedShieldType(ShieldType.FRONT, null));
    }

    @Test
    void fighterBaysFollowConvertedHangarsConvertedBaysAndTreeNodesButNotSectionItems() {
        assertEquals(2, FittedShipProfile.fighterBays(2, variantWith(), List.of()));
        assertEquals(1, FittedShipProfile.fighterBays(0, variantWith(HullMods.CONVERTED_HANGAR), List.of()));
        assertEquals(0, FittedShipProfile.fighterBays(3, variantWith(HullMods.CONVERTED_BAY), List.of()));
        assertEquals(1, FittedShipProfile.fighterBays(0, variantWith(), List.of(new SkillTypeEffect(FighterSkillEffect.FIGHTER_BAYS_FLAT, 1f))));
        assertEquals(1, FittedShipProfile.fighterBays(0, variantWith(),
                List.of(new SkillTypeEffect(FighterSkillEffect.CONVERTED_HANGAR_FIGHTER_BAYS_FLAT, 1f))));
        assertEquals(0, FittedShipProfile.fighterBays(2, variantWith(), List.of(new SkillTypeEffect(FighterSkillEffect.REMOVE_ALL_FIGHTER_BAYS, 1f))));
    }
}
