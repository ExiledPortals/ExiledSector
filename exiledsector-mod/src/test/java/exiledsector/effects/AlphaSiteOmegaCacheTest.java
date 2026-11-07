package exiledsector.effects;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.util.Misc;
import exiledsector.ProjectFiles;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.WormholeAnchorDefinitions;
import exiledsector.skills.SkillTreeSetting;
import exiledsector.skills.npc.RealSkillData;
import org.json.CDL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlphaSiteOmegaCacheTest {

    private static final String FOUND_OMEGA_KEY = "$exiledSector_foundOmega";

    private MockedStatic<Misc> miscMock;
    private final SectorAPI sector = mock(SectorAPI.class);
    private final MemoryAPI playerMemory = mock(MemoryAPI.class);

    @BeforeEach
    void setUp() throws Exception {
        miscMock = Mockito.mockStatic(Misc.class);
        miscMock.when(() -> Misc.getSalvageSeed(any())).thenReturn(42L);
        RealSkillData.load();
        String csv = Files.readString(ProjectFiles.projectRoot().resolve(WormholeAnchorDefinitions.DATA_PATH), StandardCharsets.UTF_8);
        WormholeAnchorDefinitions.register(CDL.toJSONArray(csv.replace("\r\n", "\n")));
        when(sector.getPlayerMemoryWithoutUpdate()).thenReturn(playerMemory);
    }

    @AfterEach
    void tearDown() {
        WormholeAnchorDefinitions.clear();
        ItemDefinitions.clear();
        RealSkillData.clear();
        miscMock.close();
    }

    private static SectorEntityToken entity(String customEntityType) {
        Map<String, Object> values = new HashMap<>();
        MemoryAPI memory = mock(MemoryAPI.class);
        when(memory.getBoolean(anyString())).thenAnswer(call -> Boolean.TRUE.equals(values.get(call.<String>getArgument(0))));
        Mockito.doAnswer(call -> values.put(call.getArgument(0), call.getArgument(1))).when(memory).set(anyString(), any());
        SectorEntityToken entity = mock(SectorEntityToken.class);
        when(entity.getCustomEntityType()).thenReturn(customEntityType);
        when(entity.getMemoryWithoutUpdate()).thenReturn(memory);
        return entity;
    }

    @Test
    void theAlphaSiteCacheHoldsOneOmegaAnchorButShowingItLeavesTheVolumeShut() {
        SectorEntityToken cache = entity(Entities.ALPHA_SITE_WEAPONS_CACHE);
        CargoAPI loot = mock(CargoAPI.class);

        AlphaSiteOmegaCache.addAnchor(sector, cache, loot);
        AlphaSiteOmegaCache.addAnchor(sector, cache, loot);

        ArgumentCaptor<SpecialItemData> anchor = ArgumentCaptor.forClass(SpecialItemData.class);
        verify(loot, times(1)).addSpecial(anchor.capture(), anyFloat());
        assertEquals("wormhole_anchor_omega", ItemData.of(anchor.getValue()).definitionId());
        verify(playerMemory, never()).set(FOUND_OMEGA_KEY, true);
    }

    @Test
    void theCacheHoldsNoAnchorWhileTheSkillTreeIsSwitchedOff() {
        SkillTreeSetting.setEnabledForTests(false);
        try {
            CargoAPI loot = mock(CargoAPI.class);

            AlphaSiteOmegaCache.addAnchor(sector, entity(Entities.ALPHA_SITE_WEAPONS_CACHE), loot);

            verify(loot, never()).addSpecial(any(), anyFloat());
        } finally {
            SkillTreeSetting.setEnabledForTests(SkillTreeSetting.DEFAULT_ENABLED);
        }
    }

    @Test
    void anyOtherSalvageSiteIsLeftAlone() {
        CargoAPI loot = mock(CargoAPI.class);

        AlphaSiteOmegaCache.addAnchor(sector, entity("supply_cache"), loot);

        verify(loot, never()).addSpecial(any(), anyFloat());
        verify(playerMemory, never()).set(anyString(), any());
    }
}
