package exiledsector.charts.run;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.Translation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExiledSectorDisableChartOptionsTest {

    @Test
    void disablesOfferedOptionsWithoutRebuildingTheOptionList() {
        InteractionDialogAPI dialog = mock(InteractionDialogAPI.class);
        OptionPanelAPI options = mock(OptionPanelAPI.class);
        when(dialog.getOptionPanel()).thenReturn(options);
        when(options.hasOption("ratCreateSettlement")).thenReturn(true);

        new ExiledSectorDisableChartOptions().execute("rule", dialog, List.of(literal("ratCreateSettlement"), literal("nex_outpostPreview")), Map.of());

        verify(options).setEnabled("ratCreateSettlement", false);
        verify(options).setTooltip("ratCreateSettlement", Translation.gameText("chart.run.option.temporarySystem"));
        verify(options, never()).setEnabled("nex_outpostPreview", false);
        verify(options, never()).removeOption(any());
        verify(options, never()).clearOptions();
    }

    private static Misc.Token literal(String text) {
        return new Misc.Token(text, Misc.TokenType.LITERAL);
    }
}
