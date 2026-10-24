package exiledsector.domain;

import exiledsector.ExiledSector;
import exiledsector.items.HullUpgradesDomain;
import exiledsector.items.ItemDefinition;
import exiledsector.effects.ShipEffectSources;
import exiledsector.effects.SkillTreeEffectSource;
import exiledsector.effects.SkillTreeLootSource;
import exiledsector.items.HullUpgradeEffectSource;
import exiledsector.items.HullUpgradeLootSource;
import exiledsector.items.HullUpgradesSetting;
import exiledsector.items.Subroutine;
import exiledsector.loot.LootSources;
import exiledsector.skills.SkillTreeSetting;
import exiledsector.skills.npc.NpcTreeConfig;
import exiledsector.ui.inspect.ShipSummaryParts;
import exiledsector.ui.section.SectionShipSummary;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class DomainSwitchesTest {

    @AfterEach
    void tearDown() {
        SkillTreeSetting.setEnabledForTests(SkillTreeSetting.DEFAULT_ENABLED);
        HullUpgradesSetting.setEnabledForTests(HullUpgradesSetting.DEFAULT_ENABLED);
    }

    @Test
    void switchingHullUpgradesOffDropsItsEffectsLootAndSummaryUntilItIsSwitchedBackOn() {
        HullUpgradesSetting.setEnabledForTests(false);

        assertFalse(ShipEffectSources.all().contains(HullUpgradeEffectSource.INSTANCE));
        assertFalse(LootSources.all().contains(HullUpgradeLootSource.INSTANCE));
        assertFalse(ShipSummaryParts.all().contains(SectionShipSummary.INSTANCE));
        assertTrue(ShipEffectSources.all().contains(SkillTreeEffectSource.INSTANCE));

        HullUpgradesSetting.setEnabledForTests(true);

        assertTrue(ShipEffectSources.all().contains(HullUpgradeEffectSource.INSTANCE));
        assertTrue(LootSources.all().contains(HullUpgradeLootSource.INSTANCE));
        assertTrue(ShipSummaryParts.all().contains(SectionShipSummary.INSTANCE));
    }

    @Test
    void aSwitchedOffDomainLeavesTheEnabledDomainsButStaysInstalledForItsStateHooks() {
        HullUpgradesSetting.setEnabledForTests(false);

        assertFalse(ExiledSector.enabledDomains().stream().anyMatch(HullUpgradesDomain.class::isInstance));
        assertTrue(ExiledSector.domains().stream().anyMatch(HullUpgradesDomain.class::isInstance));

        HullUpgradesSetting.setEnabledForTests(true);

        assertTrue(ExiledSector.enabledDomains().stream().anyMatch(HullUpgradesDomain.class::isInstance));
    }

    @Test
    void switchingTheSkillTreeOffDropsItsEffectsAndLootButKeepsHullUpgrades() {
        SkillTreeSetting.setEnabledForTests(false);

        assertFalse(ShipEffectSources.all().contains(SkillTreeEffectSource.INSTANCE));
        assertFalse(LootSources.all().contains(SkillTreeLootSource.INSTANCE));
        assertTrue(ShipEffectSources.all().contains(HullUpgradeEffectSource.INSTANCE));
        assertTrue(LootSources.all().contains(HullUpgradeLootSource.INSTANCE));
    }

    @Test
    void withTheSkillTreeOffShipLevelsAreNotRatedSoLootFallsBackToDeploymentPoints() {
        assertTrue(LootSources.shipLevelsRated());

        SkillTreeSetting.setEnabledForTests(false);

        assertFalse(LootSources.shipLevelsRated());
    }

    @Test
    void withTheSkillTreeOffSubroutinesLeaveTheGenericPoolsAndSalvageAndNpcFleetsGetNoNewTrees() {
        SkillTreeSetting.setEnabledForTests(false);

        assertFalse(Subroutine.SUPPORT.inGenericPools(mock(ItemDefinition.class)));
        assertTrue(Subroutine.SUPPORT.rollSalvage(new Random(1L), 1f).isEmpty());
        assertFalse(NpcTreeConfig.isEnabled());
    }
}
