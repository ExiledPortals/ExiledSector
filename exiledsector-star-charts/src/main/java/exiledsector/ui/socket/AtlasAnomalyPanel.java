package exiledsector.ui.socket;

import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.VanillaText;

import java.util.List;
import java.util.function.Supplier;

public final class AtlasAnomalyPanel extends HoloPanel {

    public interface Listener {

        void closed();
    }

    public record Entry(String title, String iconPath, StyledText body, String refusal) {

        public Entry(String title, String iconPath, StyledText body) {
            this(title, iconPath, body, null);
        }
    }

    public record Listing(List<Entry> system, List<Entry> catalysts) {

        public Listing {
            system = List.copyOf(system);
            catalysts = List.copyOf(catalysts);
        }
    }

    public static final float PANEL_WIDTH = 380f;
    private static final float ICON_SIZE = 40f;

    private enum Control {BACK}

    private final Supplier<Listing> entries;
    private final Listener anomalyListener;
    private final SpriteCache spriteCache = new SpriteCache(AtlasAnomalyPanel.class);
    private UIComponentAPI headerElement;
    private TooltipMakerAPI listElement;

    private AtlasAnomalyPanel(CustomPanelAPI hostPanel, Supplier<Listing> entries, Listener anomalyListener) {
        super(hostPanel);
        this.entries = entries;
        this.anomalyListener = anomalyListener;
    }

    public static AtlasAnomalyPanel open(CustomPanelAPI hostPanel, float panelLeft, float panelTop, float panelHeight, Supplier<Listing> entries,
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
        Listing shown = entries.get();
        if (shown.system().isEmpty()) {
            VanillaText.plainPara(element, Translation.text("chart.atlas.focus.empty"), 0f, Misc.getGrayColor());
        }
        shown.system().forEach(entry -> addEntry(element, entry));
        if (!shown.catalysts().isEmpty()) {
            element.addSectionHeading(Translation.text("chart.atlas.focus.catalysts"), Alignment.MID, PAD);
            shown.catalysts().forEach(entry -> addEntry(element, entry));
        }
        listElement = replaceElement(placedComponent(listElement), element, PAD, listTop);
        refreshOpacity();
    }

    private void addEntry(TooltipMakerAPI element, Entry entry) {
        float pad = element.getHeightSoFar() > 0f ? PAD : 0f;
        boolean refused = entry.refusal() != null;
        boolean withIcon = !entry.iconPath().isEmpty() && spriteCache.ensureLoaded(entry.iconPath());
        TooltipMakerAPI text = withIcon ? element.beginImageWithText(entry.iconPath(), ICON_SIZE) : element;
        text.setParaFont(Fonts.ORBITRON_12);
        VanillaText.plainPara(text, entry.title(), withIcon ? 0f : pad, refused ? Misc.getGrayColor() : Misc.getHighlightColor());
        text.setParaFontDefault();
        if (refused) {
            VanillaText.plainPara(text, entry.refusal(), LINE_PAD, Misc.getNegativeHighlightColor());
        }
        VanillaText.addPara(text, entry.body(), LINE_PAD, refused ? Misc.getGrayColor() : Misc.getTextColor());
        if (withIcon) {
            element.addImageWithText(pad);
        }
    }
}
