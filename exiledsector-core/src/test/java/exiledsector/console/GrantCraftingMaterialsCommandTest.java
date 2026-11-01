package exiledsector.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.SpecialItemSpecAPI;
import exiledsector.items.MaterialItems;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GrantCraftingMaterialsCommandTest {

    private ConsoleFixture fixture;
    private SettingsAPI settings;

    @BeforeEach
    void setUp() {
        fixture = new ConsoleFixture();
        settings = mock(SettingsAPI.class);
        fixture.global.when(Global::getSettings).thenReturn(settings);
        when(settings.getSpecialItemSpec(anyString())).thenReturn(mock(SpecialItemSpecAPI.class));
    }

    @AfterEach
    void tearDown() {
        fixture.close();
    }

    private static CommandContext campaign() {
        CommandContext context = mock(CommandContext.class);
        when(context.isInCampaign()).thenReturn(true);
        return context;
    }

    private List<String> grantedIds(int expectedMaterials, float copies) {
        ArgumentCaptor<SpecialItemData> added = ArgumentCaptor.forClass(SpecialItemData.class);
        verify(fixture.cargo, times(expectedMaterials)).addSpecial(added.capture(), eq(copies));
        return added.getAllValues().stream().map(SpecialItemData::getId).toList();
    }

    @Test
    void addsTenOfEveryCraftingMaterialByDefault() {
        assertEquals(CommandResult.SUCCESS, new GrantCraftingMaterialsCommand().runCommand("", campaign()));

        List<String> materialIds = MaterialItems.ids();
        assertEquals(materialIds, grantedIds(materialIds.size(), GrantCraftingMaterialsCommand.DEFAULT_COPIES));
    }

    @Test
    void addsTheGivenNumberOfCopiesAndSkipsMaterialsTheGameDoesNotKnow() {
        String unknownId = MaterialItems.ids().get(0);
        when(settings.getSpecialItemSpec(unknownId)).thenThrow(new RuntimeException("no spec"));

        assertEquals(CommandResult.SUCCESS, new GrantCraftingMaterialsCommand().runCommand("250", campaign()));

        List<String> expected = MaterialItems.ids().subList(1, MaterialItems.ids().size());
        assertEquals(expected, grantedIds(expected.size(), 250f));
    }

    @Test
    void refusesCopiesOutsideTheLimits() {
        assertEquals(CommandResult.BAD_SYNTAX, new GrantCraftingMaterialsCommand().runCommand("0", campaign()));
        assertEquals(CommandResult.BAD_SYNTAX,
                new GrantCraftingMaterialsCommand().runCommand(String.valueOf(GrantCraftingMaterialsCommand.MAX_COPIES + 1), campaign()));
        fixture.verifyNothingGranted();
    }
}
