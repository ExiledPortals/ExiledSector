package exiledsector.ui.socket;

import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.util.Misc;
import exiledlib.i18n.StyledText;
import exiledlib.i18n.Translation;
import exiledlib.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.VanillaText;

import java.util.List;
import java.util.function.Supplier;

public final class AtlasAnomalyPanel extends HoloPanel {

    public interface Listener {

        void closed();
    }

    public record Entry(String title, String iconPath, StyledText body) {
    }

    public static final float PANEL_WIDTH = 380f;
    private static final float ICON_SIZE = 40f;

    private enum Control {BACK}

    private final Supplier<List<Entry>> entries;
    private final Listener anomalyListener;
    private final SpriteCache spriteCache = new SpriteCache(AtlasAnomalyPanel.class);
    private UIComponentAPI headerElement;
    private TooltipMakerAPI listElement;

    private AtlasAnomalyPanel(CustomPanelAPI hostPanel, Supplier<List<Entry>> entries, Listener anomalyListener) {
        super(hostPanel);
        this.entries = entries;
        this.anomalyListener = anomalyListener;
    }

    public static AtlasAnomalyPanel open(CustomPanelAPI hostPanel, float panelLeft, float panelTop, float panelHeight, Supplier<List<Entry>> entries,
                                         Listener anomalyListener) {
        AtlasAnomalyPanel anomalyPanel = new AtlasAnomalyPanel(hostPanel, entries, anomalyListener);
        anomalyPanel.attach(panelLeft, panelTop, PANEL_WIDTH, panelHeight, false);
        return anomalyPanel;
    }

    public void refresh() {
        queue(this::rebuildList);
    }

    @Override
    protected void onClose() {
        hoverTooltip.hide();
        anomalyListener.closed();
    }

    @Override
    protected UIComponentAPI[] chromeComponents() {
        return new UIComponentAPI[]{headerElement};
    }

    @Override
    protected UIComponentAPI[] contentComponents() {
        return new UIComponentAPI[]{placedComponent(listElement)};
    }

    @Override
    public void buttonPressed(Object id) {
        if (id == Control.BACK) {
            queue(this::close);
        }
    }

    @Override
    protected void build() {
        rebuildHeader();
        rebuildList();
    }

    @Override
    protected void rebuildHeader() {
        headerElement = finishHeader(startHeader("chart.atlas.focus.title", "chart.atlas.focus.back", Control.BACK), headerElement);
        refreshOpacity();
    }

    private void rebuildList() {
        float listTop = PAD + HEADER_HEIGHT + GAP;
        TooltipMakerAPI element = panelRoot.createUIElement(innerWidth(), panelHeight - listTop - PAD, true);
        List<Entry> shown = entries.get();
        if (shown.isEmpty()) {
            VanillaText.plainPara(element, Translation.text("chart.atlas.focus.empty"), 0f, Misc.getGrayColor());
        }
        for (Entry entry : shown) {
            float pad = element.getHeightSoFar() > 0f ? PAD : 0f;
            boolean withIcon = !entry.iconPath().isEmpty() && spriteCache.ensureLoaded(entry.iconPath());
            TooltipMakerAPI text = withIcon ? element.beginImageWithText(entry.iconPath(), ICON_SIZE) : element;
            text.setParaFont(Fonts.ORBITRON_12);
            VanillaText.plainPara(text, entry.title(), withIcon ? 0f : pad, Misc.getHighlightColor());
            text.setParaFontDefault();
            VanillaText.addPara(text, entry.body(), LINE_PAD, Misc.getTextColor());
            if (withIcon) {
                element.addImageWithText(pad);
            }
        }
        listElement = replaceElement(placedComponent(listElement), element, PAD, listTop);
        refreshOpacity();
    }
}
