package exiledsector.compat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.EngagementResultForFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import exiledsector.skills.unlock.SkillTypeUnlockStatus;
import exiledsector.skills.unlock.UnlockCondition;
import lunalib.lunaSettings.LunaSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AbyssSingularityTest {

    private static final List<UnlockCondition> ABYSS_GATE = List.of(UnlockCondition.ratSingularityDefeated());

    private MockedStatic<LunaSettings> lunaSettingsMock;
    private MockedStatic<Global> globalMock;
    private SectorAPI sector;
    private MutableCharacterStatsAPI playerStats;
    private StarSystemAPI abyssSystem;

    @BeforeEach
    void setUp() {
        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class);
        lunaSettingsMock.when(() -> LunaSettings.getBoolean(anyString(), anyString())).thenReturn(null);
        SettingsAPI settings = mock(SettingsAPI.class);
        ModManagerAPI modManager = mock(ModManagerAPI.class);
        when(settings.getModManager()).thenReturn(modManager);
        when(settings.getHullSpec(AbyssSingularity.GENESIS_HULL_ID)).thenReturn(mock(ShipHullSpecAPI.class));
        AbyssSingularity.clearCachedDetection();
        when(modManager.isModEnabled(RandomAssortmentCompat.MOD_ID)).thenReturn(true);

        Map<String, Object> memoryValues = new HashMap<>();
        MemoryAPI memory = mock(MemoryAPI.class);
        when(memory.getBoolean(anyString())).thenAnswer(invocation -> Boolean.TRUE.equals(memoryValues.get(invocation.getArgument(0))));
        doAnswer(invocation -> memoryValues.put(invocation.getArgument(0), invocation.getArgument(1))).when(memory).set(anyString(), any());
        playerStats = mock(MutableCharacterStatsAPI.class);
        PersonAPI player = mock(PersonAPI.class);
        when(player.getStats()).thenReturn(playerStats);
        abyssSystem = mock(StarSystemAPI.class);
        sector = mock(SectorAPI.class);
        when(sector.getMemoryWithoutUpdate()).thenReturn(memory);
        when(sector.getPlayerPerson()).thenReturn(player);
        when(sector.getStarSystems()).thenReturn(List.of(mock(StarSystemAPI.class), abyssSystem));

        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSettings).thenReturn(settings);
        globalMock.when(Global::getSector).thenReturn(sector);
    }

    @AfterEach
    void tearDown() {
        AbyssSingularity.clearCachedDetection();
        globalMock.close();
        lunaSettingsMock.close();
    }

    private static FleetMemberAPI shipOfHull(String hullId) {
        ShipHullSpecAPI hullSpec = mock(ShipHullSpecAPI.class);
        when(hullSpec.getHullId()).thenReturn(hullId);
        when(hullSpec.getBaseHullId()).thenReturn(hullId);
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getHullSpec()).thenReturn(hullSpec);
        return member;
    }

    private static EngagementResultAPI battle(boolean playerWon, List<FleetMemberAPI> destroyed, List<FleetMemberAPI> disabled) {
        EngagementResultForFleetAPI losers = mock(EngagementResultForFleetAPI.class);
        when(losers.getDestroyed()).thenReturn(destroyed);
        when(losers.getDisabled()).thenReturn(disabled);
        EngagementResultAPI result = mock(EngagementResultAPI.class);
        when(result.didPlayerWin()).thenReturn(playerWon);
        when(result.getLoserResult()).thenReturn(losers);
        return result;
    }

    private void addWreck(String variantId) {
        DerelictShipEntityPlugin plugin = mock(DerelictShipEntityPlugin.class);
        ShipRecoverySpecial.PerShipData ship = mock(ShipRecoverySpecial.PerShipData.class);
        ship.variantId = variantId;
        DerelictShipEntityPlugin.DerelictShipData data = new DerelictShipEntityPlugin.DerelictShipData(ship, false);
        when(plugin.getData()).thenReturn(data);
        CustomCampaignEntityAPI wreck = mock(CustomCampaignEntityAPI.class);
        when(wreck.hasTag(AbyssSingularity.ABYSS_WRECK_TAG)).thenReturn(true);
        when(wreck.getCustomPlugin()).thenReturn(plugin);
        when(abyssSystem.getCustomEntities()).thenReturn(List.of(wreck));
    }

    @Test
    void theAbyssStaysGatedUntilTheSingularityIsBeaten() {
        assertTrue(SkillTypeUnlockStatus.isLocked(ABYSS_GATE, null));

        AbyssSingularity.recordBattle(sector, battle(true, List.of(shipOfHull("rat_genesis_serpent_head"), shipOfHull(AbyssSingularity.GENESIS_HULL_ID)), List.of()));

        assertTrue(AbyssSingularity.isDefeated(sector));
        assertFalse(SkillTypeUnlockStatus.isLocked(ABYSS_GATE, null));
    }

    @Test
    void ifRatRenamesTheGenesisHullTheAbyssUnlocksBecauseTheWinCanNoLongerBeSeen() {
        when(Global.getSettings().getHullSpec(AbyssSingularity.GENESIS_HULL_ID)).thenReturn(null);
        AbyssSingularity.clearCachedDetection();

        assertFalse(SkillTypeUnlockStatus.isLocked(ABYSS_GATE, null));
    }

    @Test
    void aDisabledGenesisCountsAsBeaten() {
        AbyssSingularity.recordBattle(sector, battle(true, List.of(), List.of(shipOfHull(AbyssSingularity.GENESIS_HULL_ID))));

        assertTrue(AbyssSingularity.isDefeated(sector));
    }

    @Test
    void losingOrWinningAnotherFightDoesNotUnlockIt() {
        AbyssSingularity.recordBattle(sector, battle(false, List.of(shipOfHull(AbyssSingularity.GENESIS_HULL_ID)), List.of()));
        AbyssSingularity.recordBattle(sector, battle(true, List.of(shipOfHull("rat_genesis_serpent_head"), shipOfHull("onslaught")), List.of()));

        assertFalse(AbyssSingularity.isDefeated(sector));
    }

    @Test
    void aSaveThatAlreadyBeatTheSingularityIsRecognisedByItsRequiemSkill() {
        when(playerStats.getSkillLevel(AbyssSingularity.REQUIEM_SKILL_ID)).thenReturn(2f);

        AbyssSingularity.checkSaveOnLoad(sector);

        assertTrue(AbyssSingularity.isDefeated(sector));
    }

    @Test
    void aSaveThatAlreadyBeatTheSingularityIsRecognisedByTheGenesisWreck() {
        addWreck(AbyssSingularity.GENESIS_WRECK_VARIANT_ID);

        AbyssSingularity.checkSaveOnLoad(sector);

        assertTrue(AbyssSingularity.isDefeated(sector));
    }

    @Test
    void aSaveWithoutEitherSignStaysGated() {
        addWreck("onslaught_Hull");

        AbyssSingularity.checkSaveOnLoad(sector);

        assertFalse(AbyssSingularity.isDefeated(sector));
        assertTrue(SkillTypeUnlockStatus.isLocked(ABYSS_GATE, null));
    }
}
