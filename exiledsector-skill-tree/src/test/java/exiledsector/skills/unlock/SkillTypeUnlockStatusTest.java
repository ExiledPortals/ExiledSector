package exiledsector.skills.unlock;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CharacterDataAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import exiledsector.compat.NightcrossCompat;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillType;
import lunalib.lunaSettings.LunaSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class SkillTypeUnlockStatusTest {

    private MockedStatic<Global> globalMock;
    private MockedStatic<LunaSettings> lunaSettingsMock;
    private SectorAPI sector;
    private CharacterDataAPI playerCharacter;
    private MemoryAPI sectorMemory;

    @BeforeEach
    void setUp() {
        sector = mock(SectorAPI.class);
        playerCharacter = mock(CharacterDataAPI.class);
        sectorMemory = mock(MemoryAPI.class);
        when(sector.getCharacterData()).thenReturn(playerCharacter);
        when(sector.getMemoryWithoutUpdate()).thenReturn(sectorMemory);

        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);

        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class);
        lunaSettingsMock.when(() -> LunaSettings.getBoolean("exiledSector", "exiledSector_disableBlueprintUnlock")).thenReturn(null);
        lunaSettingsMock.when(() -> LunaSettings.getBoolean("exiledSector", "exiledSector_disableCharacterStatUnlock")).thenReturn(null);
        lunaSettingsMock.when(() -> LunaSettings.getBoolean("exiledSector", "exiledSector_disableMinShipLevelUnlock")).thenReturn(null);
        lunaSettingsMock.when(() -> LunaSettings.getBoolean("exiledSector", "exiledSector_disableMemoryFlagUnlock")).thenReturn(null);
        lunaSettingsMock.when(() -> LunaSettings.getBoolean("exiledSector", "exiledSector_showHiddenNodesByDefault")).thenReturn(null);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
        lunaSettingsMock.close();
    }

    private static SkillType typeWithConditions(UnlockCondition... conditions) {
        return new SkillType.Builder("t", "T", "a.png", SkillTier.NOTABLE)
                .effects(List.of())
                .hullSizeEffects(List.of())
                .vanillaHullModId(null)
                .descriptionOverride(null)
                .todo(null)
                .optionalOptionIds(List.of())
                .exclusiveHullModIds(List.of())
                .exclusiveSkillTypeIds(List.of())
                .unlockConditions(List.of(conditions))
                .build();
    }

    private static ShipSkillData dataAtLevel(int level) {
        ShipSkillData data = new ShipSkillData();
        for (int i = 0; i < level; i++) {
            data.incrementLevel();
        }
        return data;
    }

    @Test
    void nodeWithNoConditionsIsNeverLocked() {
        SkillType type = typeWithConditions();

        assertFalse(SkillTypeUnlockStatus.isLocked(type, new ShipSkillData()));
        globalMock.verify(Global::getSector, never());
    }

    @Test
    void isLockedWhenPlayerDoesNotKnowTheHullModBlueprint() {
        SkillType type = typeWithConditions(UnlockCondition.blueprint(BlueprintCategory.HULLMOD, "escort_package"));
        when(playerCharacter.knowsHullMod("escort_package")).thenReturn(false);

        assertTrue(SkillTypeUnlockStatus.isLocked(type, new ShipSkillData()));
    }

    @Test
    void isUnlockedWhenPlayerKnowsTheHullModBlueprint() {
        SkillType type = typeWithConditions(UnlockCondition.blueprint(BlueprintCategory.HULLMOD, "escort_package"));
        when(playerCharacter.knowsHullMod("escort_package")).thenReturn(true);

        assertFalse(SkillTypeUnlockStatus.isLocked(type, new ShipSkillData()));
    }

    private void stubCharacterStat(String statId, float flatBonus) {
        PersonAPI person = mock(PersonAPI.class);
        MutableCharacterStatsAPI stats = mock(MutableCharacterStatsAPI.class);
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        StatBonus mod = mock(StatBonus.class);
        when(playerCharacter.getPerson()).thenReturn(person);
        when(person.getStats()).thenReturn(stats);
        when(stats.getDynamic()).thenReturn(dynamic);
        when(dynamic.getMod(statId)).thenReturn(mod);
        when(mod.getFlatBonus()).thenReturn(flatBonus);
    }

    @Test
    void characterStatConditionUnlocksWhenTheStatIsActive() {
        SkillType type = typeWithConditions(UnlockCondition.characterStat("has_neural_link"));
        stubCharacterStat("has_neural_link", 1f);

        assertFalse(SkillTypeUnlockStatus.isLocked(type, new ShipSkillData()));
    }

    @Test
    void characterStatConditionStaysLockedWhenTheStatIsInactive() {
        SkillType type = typeWithConditions(UnlockCondition.characterStat("has_neural_link"));
        stubCharacterStat("has_neural_link", 0f);

        assertTrue(SkillTypeUnlockStatus.isLocked(type, new ShipSkillData()));
    }

    @Test
    void minShipLevelUnlocksOnceTheShipReachesThatLevel() {
        SkillType type = typeWithConditions(UnlockCondition.minShipLevel(3));

        assertTrue(SkillTypeUnlockStatus.isLocked(type, dataAtLevel(2)));
        assertFalse(SkillTypeUnlockStatus.isLocked(type, dataAtLevel(3)));
        assertFalse(SkillTypeUnlockStatus.isLocked(type, dataAtLevel(4)));
    }

    @Test
    void memoryFlagUnlocksWhenTheSectorMemoryFlagIsSet() {
        SkillType type = typeWithConditions(UnlockCondition.memoryFlag("$playerCanUseGates"));
        when(sectorMemory.getBoolean("$playerCanUseGates")).thenReturn(true);

        assertFalse(SkillTypeUnlockStatus.isLocked(type, new ShipSkillData()));
    }

    @Test
    void memoryFlagStaysLockedWhenTheSectorMemoryFlagIsUnset() {
        SkillType type = typeWithConditions(UnlockCondition.memoryFlag("$playerCanUseGates"));
        when(sectorMemory.getBoolean("$playerCanUseGates")).thenReturn(false);

        assertTrue(SkillTypeUnlockStatus.isLocked(type, new ShipSkillData()));
    }

    @Test
    void multipleConditionsAreOrredTogether() {
        SkillType type = typeWithConditions(
                UnlockCondition.blueprint(BlueprintCategory.HULLMOD, "neural_interface"),
                UnlockCondition.characterStat("has_neural_link"));
        when(playerCharacter.knowsHullMod("neural_interface")).thenReturn(false);
        stubCharacterStat("has_neural_link", 1f);

        assertFalse(SkillTypeUnlockStatus.isLocked(type, new ShipSkillData()));
    }

    @Test
    void lockedWhenNoneOfSeveralConditionsAreSatisfied() {
        SkillType type = typeWithConditions(
                UnlockCondition.blueprint(BlueprintCategory.HULLMOD, "neural_interface"),
                UnlockCondition.characterStat("has_neural_link"));
        when(playerCharacter.knowsHullMod("neural_interface")).thenReturn(false);
        stubCharacterStat("has_neural_link", 0f);

        assertTrue(SkillTypeUnlockStatus.isLocked(type, new ShipSkillData()));
    }

    @Test
    void failsClosedForTheFailingConditionWhenLookupThrows() {
        SkillType type = typeWithConditions(UnlockCondition.blueprint(BlueprintCategory.HULLMOD, "escort_package"));
        when(sector.getCharacterData()).thenThrow(new RuntimeException("boom"));

        assertTrue(SkillTypeUnlockStatus.isLocked(type, new ShipSkillData()));
    }

    @Test
    void aFailingConditionDoesNotPreventAnotherConditionFromUnlockingTheNode() {
        SkillType type = typeWithConditions(
                UnlockCondition.blueprint(BlueprintCategory.HULLMOD, "escort_package"),
                UnlockCondition.memoryFlag("$playerCanUseGates"));
        when(playerCharacter.knowsHullMod("escort_package")).thenThrow(new RuntimeException("boom"));
        when(sectorMemory.getBoolean("$playerCanUseGates")).thenReturn(true);

        assertFalse(SkillTypeUnlockStatus.isLocked(type, new ShipSkillData()));
    }

    @Test
    void disablingABlueprintOverrideUnlocksTheNodeWithoutQueryingTheRealBlueprint() {
        SkillType type = typeWithConditions(UnlockCondition.blueprint(BlueprintCategory.HULLMOD, "escort_package"));
        lunaSettingsMock.when(() -> LunaSettings.getBoolean("exiledSector", "exiledSector_disableBlueprintUnlock")).thenReturn(true);

        assertFalse(SkillTypeUnlockStatus.isLocked(type, new ShipSkillData()));
        globalMock.verify(Global::getSector, never());
    }

    @Test
    void disablingOneConditionTypesOverrideDoesNotAffectAnother() {
        SkillType type = typeWithConditions(UnlockCondition.characterStat("has_neural_link"));
        lunaSettingsMock.when(() -> LunaSettings.getBoolean("exiledSector", "exiledSector_disableBlueprintUnlock")).thenReturn(true);
        stubCharacterStat("has_neural_link", 0f);

        assertTrue(SkillTypeUnlockStatus.isLocked(type, new ShipSkillData()));
    }

    @Test
    void isHiddenIsFalseForAnUnlockedNode() {
        SkillType type = typeWithConditions(UnlockCondition.blueprint(BlueprintCategory.HULLMOD, "escort_package"));
        when(playerCharacter.knowsHullMod("escort_package")).thenReturn(true);

        assertFalse(SkillTypeUnlockStatus.isHidden(type, new ShipSkillData()));
    }

    @Test
    void isHiddenIsTrueForALockedNodeByDefault() {
        SkillType type = typeWithConditions(UnlockCondition.blueprint(BlueprintCategory.HULLMOD, "escort_package"));
        when(playerCharacter.knowsHullMod("escort_package")).thenReturn(false);

        assertTrue(SkillTypeUnlockStatus.isHidden(type, new ShipSkillData()));
    }

    @Test
    void isHiddenIsFalseForALockedNodeWhenShowHiddenNodesByDefaultIsEnabled() {
        SkillType type = typeWithConditions(UnlockCondition.blueprint(BlueprintCategory.HULLMOD, "escort_package"));
        when(playerCharacter.knowsHullMod("escort_package")).thenReturn(false);
        lunaSettingsMock.when(() -> LunaSettings.getBoolean("exiledSector", "exiledSector_showHiddenNodesByDefault")).thenReturn(true);

        assertFalse(SkillTypeUnlockStatus.isHidden(type, new ShipSkillData()));
        assertTrue(SkillTypeUnlockStatus.isLocked(type, new ShipSkillData()));
    }

    @Test
    void aModGateThatFailsToCheckFallsBackOpenButAVanillaConditionStaysLocked() {
        SettingsAPI settings = mock(SettingsAPI.class);
        ModManagerAPI modManager = mock(ModManagerAPI.class);
        when(settings.getModManager()).thenReturn(modManager);
        when(modManager.isModEnabled(NightcrossCompat.MOD_ID)).thenReturn(true);
        globalMock.when(Global::getSettings).thenReturn(settings);
        when(sector.getMemoryWithoutUpdate()).thenThrow(new IllegalStateException("memory gone"));

        assertFalse(SkillTypeUnlockStatus.isLocked(typeWithConditions(UnlockCondition.modGate(UnlockConditionType.NIGHTCROSS_LUNAR_FLEET_DEFEATED)), null));
        assertFalse(SkillTypeUnlockStatus.isLocked(typeWithConditions(UnlockCondition.anchorFound("nightcross")), null));
        assertTrue(SkillTypeUnlockStatus.isLocked(typeWithConditions(UnlockCondition.memoryFlag("someFlag")), null));
    }

    @Test
    void everyModGateFailsOpenAndEveryConditionHasAnOverride() {
        for (UnlockConditionType type : UnlockConditionType.values()) {
            assertNotNull(type.overrideFieldId(), type.name());
            assertEquals(type.isModGate(), type.failsOpen(), type.name());
            if (type.isModGate() && !type.takesVolume()) {
                assertSame(type, UnlockCondition.modGate(type).getType());
            }
        }
        assertThrows(IllegalArgumentException.class, () -> UnlockCondition.modGate(UnlockConditionType.MEMORY_FLAG));
        assertThrows(IllegalArgumentException.class, () -> UnlockCondition.modGate(UnlockConditionType.ANCHOR_FOUND));
        assertSame(UnlockConditionType.ANCHOR_FOUND, UnlockCondition.anchorFound("nightcross").getType());
    }
}
