package exiledsector.skills.tags;

import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import exiledsector.skills.ShipFacts;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeRequirementsTest {

    private static ShipProfile profile(HullSize hullSize, ShieldType shieldType, int fighterBays,
                                       Set<WeaponKind> weaponKinds, boolean flagship) {
        return new ShipProfile(hullSize, shieldType, fighterBays, weaponKinds, flagship, 0f, shieldType == ShieldType.PHASE);
    }

    private static ShipProfile hull(HullSize hullSize) {
        return profile(hullSize, ShieldType.FRONT, 0, Set.of(), false);
    }

    private static ShipProfile shield(ShieldType shieldType) {
        return profile(HullSize.DESTROYER, shieldType, 0, Set.of(), false);
    }

    private static ShipProfile bays(int fighterBays) {
        return profile(HullSize.DESTROYER, ShieldType.FRONT, fighterBays, Set.of(), false);
    }

    private static ShipProfile weapons(WeaponKind... kinds) {
        Set<WeaponKind> weaponKinds = EnumSet.noneOf(WeaponKind.class);
        weaponKinds.addAll(List.of(kinds));
        return profile(HullSize.DESTROYER, ShieldType.FRONT, 0, weaponKinds, false);
    }

    private static ShipProfile flagship(boolean flagship) {
        return profile(HullSize.DESTROYER, ShieldType.FRONT, 0, Set.of(), flagship);
    }

    private static ShipProfile everything() {
        return profile(HullSize.CAPITAL_SHIP, ShieldType.OMNI, 4, EnumSet.allOf(WeaponKind.class), true);
    }

    private static boolean met(String tag, ShipProfile profile) {
        return NodeRequirements.isSatisfiedBy(List.of(tag), profile);
    }

    @Test
    void reqShieldsIsMetByFrontAndOmniShieldsOnly() {
        assertTrue(met("req_shields", shield(ShieldType.FRONT)));
        assertTrue(met("req_shields", shield(ShieldType.OMNI)));
        assertFalse(met("req_shields", shield(ShieldType.NONE)));
        assertFalse(met("req_shields", shield(ShieldType.PHASE)));
    }

    @Test
    void reqNoShieldsIsMetOnlyWithoutAnyShieldOrPhaseCloak() {
        assertTrue(met("req_no_shields", shield(ShieldType.NONE)));
        assertFalse(met("req_no_shields", shield(ShieldType.FRONT)));
        assertFalse(met("req_no_shields", shield(ShieldType.PHASE)));
    }

    @Test
    void reqPhaseIsMetOnlyByAPhaseCloak() {
        assertTrue(met("req_phase", shield(ShieldType.PHASE)));
        assertFalse(met("req_phase", shield(ShieldType.OMNI)));
        assertFalse(met("req_phase", shield(ShieldType.NONE)));
    }

    @Test
    void reqFighterBaysNeedsAtLeastOneBay() {
        assertTrue(met("req_fighter_bays", bays(1)));
        assertFalse(met("req_fighter_bays", bays(0)));
    }

    @Test
    void reqNoFighterBaysNeedsZeroBays() {
        assertTrue(met("req_no_fighter_bays", bays(0)));
        assertFalse(met("req_no_fighter_bays", bays(2)));
    }

    @Test
    void reqBallisticNeedsAFittedBallisticWeapon() {
        assertTrue(met("req_ballistic", weapons(WeaponKind.BALLISTIC)));
        assertFalse(met("req_ballistic", weapons(WeaponKind.ENERGY, WeaponKind.MISSILE)));
    }

    @Test
    void reqMissileNeedsAFittedMissileWeapon() {
        assertTrue(met("req_missile", weapons(WeaponKind.MISSILE)));
        assertFalse(met("req_missile", weapons(WeaponKind.BALLISTIC)));
    }

    @Test
    void reqEnergyNeedsAFittedEnergyWeapon() {
        assertTrue(met("req_energy", weapons(WeaponKind.ENERGY, WeaponKind.BEAM)));
        assertFalse(met("req_energy", weapons(WeaponKind.BALLISTIC)));
    }

    @Test
    void reqBeamNeedsAFittedBeamWeapon() {
        assertTrue(met("req_beam", weapons(WeaponKind.ENERGY, WeaponKind.BEAM)));
        assertFalse(met("req_beam", weapons(WeaponKind.ENERGY, WeaponKind.NON_BEAM_ENERGY)));
    }

    @Test
    void reqOffensiveBeamNeedsAFittedBeamThatIsNotPointDefense() {
        assertTrue(met("req_offensive_beam", weapons(WeaponKind.ENERGY, WeaponKind.BEAM, WeaponKind.OFFENSIVE_BEAM)));
        assertFalse(met("req_offensive_beam", weapons(WeaponKind.ENERGY, WeaponKind.BEAM)));
        assertTrue(met("req_beam", weapons(WeaponKind.ENERGY, WeaponKind.BEAM)));
    }

    @Test
    void reqNonBeamEnergyNeedsAFittedEnergyWeaponThatIsNotABeam() {
        assertTrue(met("req_non_beam_energy", weapons(WeaponKind.ENERGY, WeaponKind.NON_BEAM_ENERGY)));
        assertFalse(met("req_non_beam_energy", weapons(WeaponKind.ENERGY, WeaponKind.BEAM)));
    }

    @Test
    void reqFlagshipNeedsTheFleetFlagship() {
        assertTrue(met("req_flagship", flagship(true)));
        assertFalse(met("req_flagship", flagship(false)));
    }

    @Test
    void campaignOnlyAndPlayerOnlyAreNeverMetForNpcGeneration() {
        assertFalse(met("campaign_only", everything()));
        assertFalse(met("player_only", everything()));
        assertFalse(met("campaign_only", hull(HullSize.FRIGATE)));
        assertFalse(met("player_only", shield(ShieldType.NONE)));
    }

    @Test
    void themeAndRegionTagsImposeNoRequirement() {
        List<String> tags = new ArrayList<>(SkillTags.THEME);
        tags.addAll(SkillTags.REGION);
        ShipProfile bare = profile(HullSize.FRIGATE, ShieldType.NONE, 0, Set.of(), false);

        assertNull(NodeRequirements.firstUnmet(tags, bare));
        assertTrue(NodeRequirements.isSatisfiedBy(tags, bare));
    }

    @Test
    void unknownTagsOutsideTheVocabularyAreIgnored() {
        assertTrue(met("req_something_new", hull(HullSize.FRIGATE)));
    }

    @Test
    void noTagsAreAlwaysSatisfied() {
        assertTrue(NodeRequirements.isSatisfiedBy(List.of(), hull(HullSize.FRIGATE)));
    }

    @Test
    void firstUnmetReturnsTheEarliestFailingRequirementInTagOrder() {
        List<String> tags = List.of("shield", "req_shields", "req_flagship", "req_phase", "core");

        assertEquals("req_flagship", NodeRequirements.firstUnmet(tags, shield(ShieldType.FRONT)));
    }

    @Test
    void everyRequirementOtherThanCampaignAndPlayerOnlyCanBeBothMetAndUnmet() {
        List<ShipProfile> profiles = new ArrayList<>();
        for (HullSize hullSize : List.of(HullSize.FRIGATE, HullSize.DESTROYER, HullSize.CRUISER, HullSize.CAPITAL_SHIP)) {
            profiles.add(hull(hullSize));
        }
        for (ShieldType shieldType : ShieldType.values()) {
            profiles.add(shield(shieldType));
        }
        profiles.add(everything());
        profiles.add(profile(HullSize.FRIGATE, ShieldType.NONE, 0, Set.of(), false));

        for (String requirement : SkillTags.REQUIREMENT) {
            if (SkillTags.isHullRequirement(requirement)) {
                continue;
            }
            boolean everMet = profiles.stream().anyMatch(p -> met(requirement, p));
            boolean everUnmet = profiles.stream().anyMatch(p -> !met(requirement, p));
            boolean npcExcluded = requirement.equals("campaign_only") || requirement.equals("player_only");
            assertEquals(!npcExcluded, everMet, requirement + " met by some profile");
            assertTrue(everUnmet, requirement + " unmet by some profile");
        }
    }

    @Test
    void reqCivilianHullNeedsTheCivilianGradeHullModAndIsLeftToTheEligibilityRule() {
        List<String> tags = List.of("logistics", "req_civilian_hull");

        assertNull(NodeRequirements.firstUnmetHullRequirement(tags, facts(false, false, HullMods.CIVGRADE::equals)));
        assertEquals("req_civilian_hull", NodeRequirements.firstUnmetHullRequirement(tags, facts(false, false, hullModId -> false)));
        assertTrue(NodeRequirements.isSatisfiedBy(tags, everything()));
    }

    @Test
    void everyHullRequirementCanBeBothMetAndUnmet() {
        for (String requirement : SkillTags.HULL_REQUIREMENT) {
            assertNull(NodeRequirements.firstUnmetHullRequirement(List.of(requirement), facts(false, true, hullModId -> true)), requirement);
            assertEquals(requirement, NodeRequirements.firstUnmetHullRequirement(List.of(requirement), facts(true, false, hullModId -> false)));
        }
    }

    @Test
    void reqNonPhaseHullRejectsPhaseHullsAndReqSystemChargesNeedsAChargedSystem() {
        assertEquals("req_non_phase_hull", NodeRequirements.firstUnmetHullRequirement(List.of("req_non_phase_hull"), facts(true, true, id -> false)));
        assertNull(NodeRequirements.firstUnmetHullRequirement(List.of("req_non_phase_hull"), facts(false, false, id -> false)));
        assertEquals("req_system_charges", NodeRequirements.firstUnmetHullRequirement(List.of("req_system_charges"), facts(false, false, id -> false)));
        assertNull(NodeRequirements.firstUnmetHullRequirement(List.of("req_system_charges"), facts(true, true, id -> false)));
    }

    private static ShipFacts facts(boolean phaseHull, boolean limitedSystemCharges, Predicate<String> hasHullMod) {
        return new ShipFacts(HullSize.FRIGATE, phaseHull ? ShieldType.PHASE : ShieldType.FRONT, phaseHull, 100f, limitedSystemCharges, hasHullMod);
    }
}
