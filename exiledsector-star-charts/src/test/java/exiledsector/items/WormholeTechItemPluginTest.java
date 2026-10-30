package exiledsector.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.SpecialItemSpecAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.MockSector;
import exiledsector.charts.MapMemory;
import exiledsector.charts.wormhole.WormholeGateway;
import exiledsector.compat.VaultsOfKnowledgeCompat;
import exiledsector.i18n.Translation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WormholeTechItemPluginTest {

    private MockSector mockSector;
    private final ModManagerAPI modManager = mock(ModManagerAPI.class);
    private final MemoryAPI sectorMemory = MapMemory.create();

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        SettingsAPI settings = mock(SettingsAPI.class);
        when(settings.getModManager()).thenReturn(modManager);
        when(settings.getSpecialItemSpec(anyString())).thenReturn(mock(SpecialItemSpecAPI.class));
        mockSector.global.when(Global::getSettings).thenReturn(settings);
        when(mockSector.sector.getMemoryWithoutUpdate()).thenReturn(sectorMemory);
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
    }

    private static WormholeTechItemPlugin plugin(String itemId) {
        WormholeTechItemPlugin plugin = new WormholeTechItemPlugin();
        plugin.setId(itemId);
        return plugin;
    }

    @Test
    void withoutVaultsOfKnowledgeRightClickingTheBlueprintUnlocksTheGatewayAndUsesItUp() {
        WormholeTechItemPlugin blueprint = plugin(WormholeTechItems.BLUEPRINT);

        assertTrue(blueprint.hasRightClickAction());
        assertTrue(blueprint.shouldRemoveOnRightClickAction());
        assertFalse(WormholeGateway.isUnlocked(mockSector.sector));

        blueprint.performRightClickAction();

        assertTrue(WormholeGateway.isUnlocked(mockSector.sector));
    }

    @Test
    void withVaultsOfKnowledgeTheBlueprintHasNoRightClickAction() {
        when(modManager.isModEnabled(VaultsOfKnowledgeCompat.MOD_ID)).thenReturn(true);
        WormholeTechItemPlugin blueprint = plugin(WormholeTechItems.BLUEPRINT);

        assertFalse(blueprint.hasRightClickAction());
        assertFalse(blueprint.shouldRemoveOnRightClickAction());
        blueprint.performRightClickAction();
        assertFalse(WormholeGateway.isUnlocked(mockSector.sector));
    }

    @Test
    void theUpgradesNeverHaveARightClickAction() {
        assertFalse(plugin(WormholeTechItems.UPGRADE_ADVANCED).hasRightClickAction());
        assertFalse(plugin(WormholeTechItems.UPGRADE_PERFECTED).hasRightClickAction());
    }

    @Test
    void theUsageLineFollowsWhetherVaultsOfKnowledgeIsOn() {
        assertEquals("Right-click to unlock the Wormhole Gateway structure.",
                Translation.text(WormholeTechItemPlugin.usageKey(WormholeTechItems.BLUEPRINT, false)));
        assertEquals("Store in a Research Facility to research the Wormhole Gateway.",
                Translation.text(WormholeTechItemPlugin.usageKey(WormholeTechItems.BLUEPRINT, true)));
        assertEquals("Store in a Research Facility to research the Gateway Array, then upgrade your gateway.",
                Translation.text(WormholeTechItemPlugin.usageKey(WormholeTechItems.UPGRADE_ADVANCED, true)));
        assertEquals("Store in a Research Facility to research the Gateway Nexus, then upgrade your gateway.",
                Translation.text(WormholeTechItemPlugin.usageKey(WormholeTechItems.UPGRADE_PERFECTED, true)));
        assertTrue(Translation.has(WormholeTechItemPlugin.usageKey(WormholeTechItems.UPGRADE_PERFECTED, false)));
    }
}
