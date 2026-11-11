package exiledsector.ui.socket;

import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.Translation;
import exiledsector.items.ItemDefinition;
import exiledsector.ui.VanillaText;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

final class BenchTrayElement {

    private BenchTrayElement() {
    }

    static TooltipMakerAPI build(CustomPanelAPI panelRoot, float trayWidth, float trayHeight, BenchTray tray, BenchTrayContext context,
                                 Function<BenchTrayEntry, TileInput.Handler> handlerFor) {
        List<BenchTrayEntry> entries = tray.entries(context);
        List<SectionedTileGrid.Tile> tiles = new ArrayList<>();
        for (BenchTrayEntry entry : entries) {
            boolean available = entry.available(context);
            CraftingBenchTile tile = CraftingBenchTile.entry(shownIcons(entry.iconPaths()), entry.look(), () -> available, () -> entry.lit(context),
                    handlerFor.apply(entry));
            String badge = entry.badge(context);
            tiles.add(new SectionedTileGrid.Tile(tile, badge == null ? "" : badge, available));
        }
        TooltipMakerAPI element = SectionedTileGrid.build(panelRoot, trayWidth, trayHeight - HoloPanel.PAD * 2f,
                List.of(new SectionedTileGrid.Section(Translation.text(tray.titleKey()), Misc.getBasePlayerColor(), tiles)));
        if (entries.isEmpty()) {
            VanillaText.plainPara(element, Translation.text(tray.emptyKey()), HoloPanel.GAP, Misc.getGrayColor());
        }
        return element;
    }

    static List<String> shownIcons(List<String> iconPaths) {
        return iconPaths == null || iconPaths.isEmpty() ? List.of(ItemDefinition.FALLBACK_ICON) : iconPaths;
    }
}
