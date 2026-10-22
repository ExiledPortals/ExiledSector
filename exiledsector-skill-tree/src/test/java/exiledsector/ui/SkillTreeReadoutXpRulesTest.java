package exiledsector.ui;

import exiledsector.skills.progression.ShipLevelConfig;
import lunalib.lunaSettings.LunaSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillTreeReadoutXpRulesTest {

    private static final String WITHOUT_DIFFICULTY = "Earn XP by destroying enemy ships. The larger the hull, the more XP gained. "
            + "Losing combat awards 50% of the XP.";

    private MockedStatic<LunaSettings> lunaSettingsMock;

    @BeforeEach
    void setUp() {
        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class, invocation -> null);
    }

    @AfterEach
    void tearDown() {
        lunaSettingsMock.close();
    }

    private void configure(float strength, float maxMultiplier) {
        lunaSettingsMock.when(() -> LunaSettings.getFloat("exiledSector", ShipLevelConfig.XP_DIFFICULTY_STRENGTH_FIELD_ID))
                .thenReturn(strength);
        lunaSettingsMock.when(() -> LunaSettings.getFloat("exiledSector", ShipLevelConfig.XP_DIFFICULTY_MAX_MULTIPLIER_FIELD_ID))
                .thenReturn(maxMultiplier);
    }

    @Test
    void atDefaultsTheReadoutQuotesVanillasFullDifficultyBonus() {
        String rules = SkillTreeChrome.xpRulesText();

        assertTrue(rules.contains("Harder battles give up to +500% more XP"), rules);
        assertTrue(rules.endsWith("Losing combat awards 50% of the XP."), rules);
    }

    @Test
    void theQuotedBonusFollowsTheStrengthAndCapSettings() {
        configure(0.5f, 6f);
        assertTrue(SkillTreeChrome.xpRulesText().contains("up to +250% more XP"));

        configure(1f, 2f);
        assertTrue(SkillTreeChrome.xpRulesText().contains("up to +100% more XP"));
    }

    @Test
    void theReadoutDropsTheDifficultySentenceWhenTheBonusIsTurnedOff() {
        configure(0f, 6f);
        assertEquals(WITHOUT_DIFFICULTY, SkillTreeChrome.xpRulesText());

        configure(1f, 1f);
        assertEquals(WITHOUT_DIFFICULTY, SkillTreeChrome.xpRulesText());
    }
}
