package exiledsector.ui.inspect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import exiledsector.effects.ModuleShareSetting;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.skilleffect.DefenseSkillEffect;
import exiledsector.ui.SkillTreePanelStyle;
import lunalib.lunaSettings.LunaSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.awt.Color;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShipTreeSummaryRendererTest {

    private MockedStatic<Global> globalMock;
    private TooltipMakerAPI info;
    private LabelAPI label;
    private FleetMemberAPI member;
    private ShipSkillData data;

    @BeforeEach
    void setUp() {
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSettings).thenReturn(mock(SettingsAPI.class));
        SkillTree.clearNodes();
        SkillTree.clearTypes();
        SkillNode root = new SkillNode("root_1", new SkillType.Builder("root", "Low Tech", "a.png", SkillTier.ROOT).build(), List.of(), 0f, 0f);
        SkillNode armor = new SkillNode("heavyarmor_1", new SkillType.Builder("heavyarmor", "Heavy Armor", "a.png", SkillTier.NOTABLE)
                .effects(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 10f))).build(), List.of("root_1"), 0f, 0f);
        SkillTree.register(root);
        SkillTree.register(armor);
        data = new ShipSkillData();
        data.chooseStartingRoot(root);
        data.allocate(armor, 0);
        data.incrementLevel();

        info = mock(TooltipMakerAPI.class);
        label = mock(LabelAPI.class);
        when(info.addPara(anyString(), anyFloat(), (Color) any(), any(String[].class))).thenReturn(label);
        when(info.addPara(anyString(), anyFloat(), (Color) any(), (Color) any(), any(String[].class))).thenReturn(label);
        member = mock(FleetMemberAPI.class);
        ShipHullSpecAPI hullSpec = mock(ShipHullSpecAPI.class);
        when(hullSpec.getHullSize()).thenReturn(HullSize.CRUISER);
        when(member.getHullSpec()).thenReturn(hullSpec);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
        SkillTree.clearNodes();
        SkillTree.clearTypes();
    }

    @Test
    void showsLevelBuildThemesStartAndNodeCountOnOneLine() {
        ShipTreeSummaryRenderer.render(info, member, new ShipTreeLookup.ShipTree(data, List.of("ballistic", "armour")), 5f);

        verify(info).addPara(eq("%s"), eq(5f), (Color) any(), (Color) any(), eq("Level 1  |  Ballistic / Armor build  |  Low Tech start  |  1 node"));
    }

    @Test
    void leavesTheBuildOutWhenTheTreeHasNoThemes() {
        ShipTreeSummaryRenderer.render(info, member, new ShipTreeLookup.ShipTree(data, List.of()), 5f);

        verify(info).addPara(eq("%s"), eq(5f), (Color) any(), (Color) any(), eq("Level 1  |  Low Tech start  |  1 node"));
    }

    @Test
    void highlightsNotableNamesAndColoursBonusNumbers() {
        ShipTreeSummaryRenderer.render(info, member, new ShipTreeLookup.ShipTree(data, List.of("ballistic", "armour")), 5f);

        verify(info).addPara(eq("%s"), anyFloat(), (Color) any(), (Color) any(), eq("Notables and keystones: Heavy Armor"));
        verify(label).setHighlight("Heavy Armor");
        verify(info).addPara(eq("%s"), anyFloat(), (Color) any(), (Color) any(), eq("    - " + DefenseSkillEffect.HULL_PERCENT.description(10f).plain()));
        verify(label).setHighlightColors(SkillTreePanelStyle.POSITIVE_STAT_COLOR);
    }

    @Test
    void shipsWithModuleSlotsShowTheModuleShareUnlessItIsOff() {
        WeaponSlotAPI moduleSlot = mock(WeaponSlotAPI.class);
        when(moduleSlot.isStationModule()).thenReturn(true);
        when(member.getHullSpec().getAllWeaponSlotsCopy()).thenReturn(List.of(moduleSlot));
        String shareLine = "This ship's modules have 30% of its weapon, flux and defense bonuses.";

        try (MockedStatic<LunaSettings> lunaSettings = Mockito.mockStatic(LunaSettings.class)) {
            lunaSettings.when(() -> LunaSettings.getInt("exiledSector", ModuleShareSetting.FIELD_ID)).thenReturn(30);
            ShipTreeSummaryRenderer.render(info, member, new ShipTreeLookup.ShipTree(data, List.of()), 5f);
            verify(info).addPara(eq("%s"), anyFloat(), (Color) any(), (Color) any(), eq(shareLine));

            lunaSettings.when(() -> LunaSettings.getInt("exiledSector", ModuleShareSetting.FIELD_ID)).thenReturn(0);
            ShipTreeSummaryRenderer.render(info, member, new ShipTreeLookup.ShipTree(data, List.of()), 5f);
            verify(info, Mockito.times(1)).addPara(eq("%s"), anyFloat(), (Color) any(), (Color) any(), eq(shareLine));
        }
    }

    @Test
    void shipsWithoutModuleSlotsNeverShowTheModuleShare() {
        ShipTreeSummaryRenderer.render(info, member, new ShipTreeLookup.ShipTree(data, List.of()), 5f);

        verify(info, Mockito.never()).addPara(eq("%s"), anyFloat(), (Color) any(), (Color) any(),
                eq("This ship's modules have 50% of its weapon, flux and defense bonuses."));
    }

    @Test
    void scaledNpcTreesShowTheMultiplierAndTheScaledBonuses() {
        ShipTreeSummaryRenderer.render(info, member, new ShipTreeLookup.ShipTree(data, List.of(), 2.5f), 5f);

        verify(info).addPara(eq("%s"), eq(5f), (Color) any(), (Color) any(), eq("Level 1  |  Low Tech start  |  1 node  |  2.5x bonuses"));
        verify(info).addPara(eq("%s"), anyFloat(), (Color) any(), (Color) any(), eq("    - " + DefenseSkillEffect.HULL_PERCENT.description(25f).plain()));
    }
}
