package exiledsector.ui.socket;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.socketables.SocketCurrency;
import exiledsector.socketables.Socketable;
import exiledsector.socketables.SocketableCrafting;
import exiledsector.socketables.SocketableDefinition;
import exiledsector.socketables.SocketableDefinitions;
import exiledsector.socketables.SocketableDisassembly;
import exiledsector.socketables.SocketableKind;
import exiledsector.socketables.SocketableName;
import exiledsector.socketables.SocketableStore;
import exiledsector.socketables.SocketableUnlock;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.SkillTreeSounds;
import exiledsector.ui.VanillaText;
import exiledsector.ui.util.BorderedPanel;
import exiledsector.ui.util.FramedPanelPlugin;
import exiledsector.ui.util.HoloTransition;
import exiledsector.ui.util.Rects;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.Predicate;

public final class SocketWorkbenchPanel extends BaseCustomUIPanelPlugin {

    public interface Listener {

        void changed(Socketable loaded);

        void closed();
    }

    public static final float WIDTH = 520f;
    private static final float PAD = 12f;
    private static final float GAP = 6f;
    private static final float LINE_PAD = 3f;
    private static final float HEADER_HEIGHT = 28f;
    private static final float BUTTON_HEIGHT = 24f;
    private static final float CLOSE_BUTTON_WIDTH = 70f;
    private static final float TILE_SIZE = 80f;
    private static final float TILE_GAP = 10f;
    private static final int TILE_COLUMNS = 2;
    private static final float COUNT_LEFT = 12f;
    private static final float COUNT_WIDTH = 120f;
    private static final float ICON_SIZE = 24f;
    private static final float COUNT_TEXT_TOP = 5f;
    private static final float NOTICE_HEIGHT = 36f;
    private static final float MIN_SOCKET_AREA = 110f;
    private static final float MIN_DETAILS_HEIGHT = 110f;
    private static final float MAX_SOCKET_AREA = 240f;
    private static final float SOCKET_AREA_SHARE = 0.32f;
    private static final float SOCKET_RING_SHARE = 0.46f;
    private static final float TOOLTIP_WIDTH = 340f;
    private static final String FALLBACK_SUBROUTINE_ICON = SocketableDefinition.FALLBACK_ICON;

    private enum Recipe {
        COMMON(null),
        AUGMENTATION(SocketCurrency.AUGMENTATION),
        RECALIBRATION(SocketCurrency.RECALIBRATION),
        TRANSPOSITION(SocketCurrency.TRANSPOSITION);

        private final SocketCurrency currency;

        Recipe(SocketCurrency currency) {
            this.currency = currency;
        }

        String key() {
            return name().toLowerCase(Locale.ROOT);
        }

        int partsCost() {
            return currency == null ? SocketableCrafting.commonSynthesisParts() : currency.partsCost();
        }
    }

    private enum Control {CLOSE}

    private final CustomPanelAPI host;
    private final Listener listener;
    private final BorderedPanel frame = new BorderedPanel(SocketWorkbenchPanel.class);
    private final HoloTransition transition = new HoloTransition();
    private final SocketWorkbenchFlair flair = new SocketWorkbenchFlair();
    private final List<Runnable> queued = new ArrayList<>();
    private final Random random = new Random();
    private CustomPanelAPI root;
    private PositionAPI position;
    private float panelWidth;
    private float height;
    private float socketArea;
    private boolean closing;
    private UIComponentAPI header;
    private UIComponentAPI recipes;
    private UIComponentAPI details;
    private UIComponentAPI notice;
    private CustomPanelAPI tooltip;
    private Recipe tooltipFor;
    private Socketable loaded;
    private String noticeText;

    private SocketWorkbenchPanel(CustomPanelAPI host, Listener listener) {
        this.host = host;
        this.listener = listener;
    }

    public static SocketWorkbenchPanel open(CustomPanelAPI host, float left, float top, float height, Listener listener) {
        SocketWorkbenchPanel panel = new SocketWorkbenchPanel(host, listener);
        panel.panelWidth = WIDTH;
        panel.height = height;
        panel.socketArea = socketAreaFor(height);
        panel.root = Global.getSettings().createCustom(WIDTH, height, panel);
        host.addComponent(panel.root).inTL(left, top);
        I18n.forGameText(panel::build);
        panel.transition.open();
        panel.applyContentOpacity(0f);
        return panel;
    }

    private static float socketAreaFor(float height) {
        float free = height - socketAreaTop() - bottomReserve();
        float preferred = Math.min(MAX_SOCKET_AREA, height * SOCKET_AREA_SHARE);
        return Math.max(MIN_SOCKET_AREA, Math.min(preferred, free - MIN_DETAILS_HEIGHT));
    }

    private static float tilesTop() {
        return PAD + HEADER_HEIGHT + GAP;
    }

    private static float tilesHeight() {
        int rows = (Recipe.values().length + TILE_COLUMNS - 1) / TILE_COLUMNS;
        return rows * TILE_SIZE + (rows - 1) * TILE_GAP;
    }

    private static float socketAreaTop() {
        return tilesTop() + tilesHeight() + GAP;
    }

    private static float bottomReserve() {
        return GAP + NOTICE_HEIGHT + PAD;
    }

    public Socketable loaded() {
        return loaded;
    }

    public void load(Socketable socketable) {
        queued.add(() -> {
            if (socketable == loaded) {
                return;
            }
            loaded = socketable;
            noticeText = null;
            flair.restartLoad();
            rebuildContent();
        });
    }

    public void refresh() {
        queued.add(() -> {
            if (loaded != null && !SocketableStore.get().owned().contains(loaded)) {
                loaded = null;
            }
            buildHeader();
            rebuildContent();
        });
    }

    public void close() {
        if (root == null || closing) {
            return;
        }
        closing = true;
        hideTooltip();
        transition.close();
        applyContentOpacity(transition.contentAlpha());
        listener.closed();
    }

    public boolean isVisible() {
        return root != null;
    }

    public boolean contains(float x, float y) {
        return root != null && !closing && Rects.contains(position, x, y);
    }

    @Override
    public void positionChanged(PositionAPI position) {
        this.position = position;
    }

    @Override
    public void renderBelow(float alphaMult) {
        if (position == null) {
            return;
        }
        transition.drawProjection(position.getX(), position.getY(), position.getWidth(), position.getHeight(),
                SkillTreePanelStyle.GLOW_COLOR, alphaMult);
        float content = transition.contentAlpha();
        if (content > 0f) {
            frame.draw(position.getX(), position.getY(), position.getWidth(), position.getHeight(), content * alphaMult);
        }
    }

    @Override
    public void render(float alphaMult) {
        if (position == null) {
            return;
        }
        float content = transition.contentAlpha() * alphaMult;
        float cx = position.getX() + panelWidth / 2f;
        float cy = position.getY() + height - socketAreaTop() - socketArea / 2f;
        Color subject = loaded == null ? null : loaded.rarity().color();
        flair.render(cx, cy, socketArea * SOCKET_RING_SHARE, SkillTreePanelStyle.GLOW_COLOR, subject,
                loaded == null ? null : loaded.iconPath(), content);
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        if (closing) {
            return;
        }
        for (InputEventAPI event : events) {
            boolean press = event.isMouseDownEvent() || event.isMouseScrollEvent();
            if (!event.isConsumed() && press && contains(event.getX(), event.getY())) {
                event.consume();
            }
        }
    }

    @Override
    public void advance(float amount) {
        if (transition.isAnimating()) {
            transition.advance(amount);
            applyContentOpacity(transition.contentAlpha());
        }
        flair.advance(amount, loaded != null);
        if (closing) {
            if (transition.isFullyClosed() && root != null) {
                host.removeComponent(root);
                root = null;
            }
            return;
        }
        if (!queued.isEmpty()) {
            List<Runnable> actions = List.copyOf(queued);
            queued.clear();
            for (Runnable action : actions) {
                if (root != null) {
                    I18n.forGameText(action);
                }
            }
        }
    }

    @Override
    public void buttonPressed(Object id) {
        queued.add(() -> handleButton(id));
    }

    private void handleButton(Object id) {
        if (id == Control.CLOSE) {
            close();
        }
    }

    private void tileClicked(Recipe recipe) {
        hideTooltip();
        if (recipe.currency == null) {
            showNotice(Translation.msg("ui.workbench.notice.synthesiseHint").arg("name", recipeName(recipe)).text());
        } else {
            use(recipe.currency);
        }
    }

    private void tileRightClicked(Recipe recipe) {
        hideTooltip();
        synthesise(recipe);
    }

    private boolean synthesise(Recipe recipe) {
        CargoAPI cargo = cargo();
        if (cargo == null) {
            return false;
        }
        int parts = SocketableCrafting.parts(cargo);
        if (parts < recipe.partsCost()) {
            showNotice(Translation.msg("ui.workbench.notice.notEnoughParts").arg("cost", recipe.partsCost()).arg("parts", parts)
                    .arg("name", recipeName(recipe)).text());
            return false;
        }
        if (recipe.currency == null) {
            Socketable created = SocketableCrafting.synthesiseCommon(cargo, SocketableStore.get(), random);
            if (created == null) {
                return false;
            }
            loaded = created;
            flair.restartLoad();
            noticeText = Translation.msg("ui.workbench.notice.synthesisedCommon").arg("name", created.name()).text();
        } else {
            if (!SocketableCrafting.synthesise(recipe.currency, cargo)) {
                return false;
            }
            noticeText = Translation.msg("ui.workbench.notice.synthesisedCurrency").arg("name", recipeName(recipe)).text();
        }
        SkillTreeSounds.crafted();
        flair.flash(SkillTreePanelStyle.GLOW_COLOR);
        afterChange();
        return true;
    }

    private void use(SocketCurrency currency) {
        CargoAPI cargo = cargo();
        if (cargo == null) {
            return;
        }
        if (SocketableCrafting.count(cargo, currency.commodityId()) < 1) {
            showNotice(Translation.msg("ui.workbench.notice.noneOwned").arg("name", commodityName(currency.commodityId())).text());
            return;
        }
        if (loaded == null) {
            showNotice(Translation.text("ui.workbench.notice.loadFirst"));
            return;
        }
        if (!SocketableCrafting.canUse(currency, loaded, allowedUniques())) {
            showNotice(Translation.msg("ui.workbench.notice.cannotUse").arg("name", commodityName(currency.commodityId()))
                    .arg("target", loaded.name()).text());
            return;
        }
        String before = loaded.name();
        Socketable result = SocketableCrafting.use(currency, loaded, cargo, SocketableStore.get(), random, allowedUniques());
        if (result == null) {
            return;
        }
        loaded = result;
        noticeText = Translation.msg("ui.workbench.notice.used." + currency.name().toLowerCase(Locale.ROOT))
                .arg("before", before).arg("name", result.name()).text();
        SkillTreeSounds.socketed();
        flair.flash(result.rarity().color());
        afterChange();
    }

    private void showNotice(String text) {
        noticeText = text;
        rebuildNotice();
        applyContentOpacity(transition.contentAlpha());
    }

    private void afterChange() {
        buildHeader();
        rebuildContent();
        listener.changed(loaded);
    }

    private static Predicate<SocketableDefinition> allowedUniques() {
        return definition -> SocketableUnlock.canDrop(definition, Global.getSector());
    }

    private static CargoAPI cargo() {
        CampaignFleetAPI fleet = Global.getSector() == null ? null : Global.getSector().getPlayerFleet();
        return fleet == null ? null : fleet.getCargo();
    }

    private void applyContentOpacity(float opacity) {
        float chromeOpacity = opacity >= 1f && !closing ? 1f : 0f;
        if (header != null) {
            header.setOpacity(chromeOpacity);
        }
        for (UIComponentAPI component : new UIComponentAPI[]{recipes, details, notice}) {
            if (component != null) {
                component.setOpacity(opacity);
            }
        }
    }

    private void build() {
        buildHeader();
        rebuildContent();
    }

    private void buildHeader() {
        if (header != null) {
            root.removeComponent(header);
        }
        TooltipMakerAPI element = root.createUIElement(innerWidth(), HEADER_HEIGHT, false);
        element.setParaFont(Fonts.ORBITRON_20AABOLD);
        element.addPara("%s", 0f, Misc.getBasePlayerColor(), Misc.getBasePlayerColor(), Translation.text("ui.workbench.title"));
        element.addButton(Translation.text("ui.workbench.close"), Control.CLOSE, CLOSE_BUTTON_WIDTH, BUTTON_HEIGHT, 0f)
                .getPosition().inTR(0f, 0f);
        addPartsCount(element);
        root.addUIElement(element).inTL(PAD, PAD);
        header = element;
    }

    private static void addPartsCount(TooltipMakerAPI element) {
        CommoditySpecAPI parts = Global.getSettings().getCommoditySpec(SocketableDisassembly.PARTS_COMMODITY_ID);
        if (parts == null) {
            return;
        }
        String count = String.valueOf(SocketableCrafting.parts(cargo()));
        float countWidth = Global.getSettings().computeStringWidth(count, Fonts.ORBITRON_12) + LINE_PAD;
        float countRight = CLOSE_BUTTON_WIDTH + GAP * 2f;
        element.setParaFont(Fonts.ORBITRON_12);
        LabelAPI label = element.addPara("%s", 0f, Misc.getTextColor(), Misc.getTextColor(), count);
        label.autoSizeToWidth(countWidth).inTR(countRight, COUNT_TEXT_TOP);
        element.addImage(parts.getIconName(), ICON_SIZE, ICON_SIZE, 0f);
        element.getPrev().getPosition().inTR(countRight + countWidth + LINE_PAD, 0f);
    }

    private void rebuildContent() {
        rebuildTiles();
        rebuildDetails();
        rebuildNotice();
        applyContentOpacity(transition.contentAlpha());
    }

    private void rebuildTiles() {
        hideTooltip();
        if (recipes != null) {
            root.removeComponent(recipes);
        }
        TooltipMakerAPI element = root.createUIElement(innerWidth(), tilesHeight(), false);
        CargoAPI cargo = cargo();
        Recipe[] all = Recipe.values();
        float cellWidth = innerWidth() / TILE_COLUMNS;
        for (int i = 0; i < all.length; i++) {
            float left = (i % TILE_COLUMNS) * cellWidth;
            float top = (i / TILE_COLUMNS) * (TILE_SIZE + TILE_GAP);
            addTile(element, all[i], left, top, cargo);
        }
        root.addUIElement(element).inTL(PAD, tilesTop());
        recipes = element;
    }

    private void addTile(TooltipMakerAPI element, Recipe recipe, float left, float top, CargoAPI cargo) {
        CustomPanelAPI tile = Global.getSettings().createCustom(TILE_SIZE, TILE_SIZE, new WorkbenchTile(recipeIcons(recipe),
                new WorkbenchTile.Listener() {
                    @Override
                    public void hovered(PositionAPI anchor) {
                        queued.add(() -> showTooltip(recipe, anchor));
                    }

                    @Override
                    public void left() {
                        queued.add(() -> {
                            if (tooltipFor == recipe) {
                                hideTooltip();
                            }
                        });
                    }

                    @Override
                    public void clicked() {
                        queued.add(() -> tileClicked(recipe));
                    }

                    @Override
                    public void rightClicked() {
                        queued.add(() -> tileRightClicked(recipe));
                    }
                }));
        element.addCustom(tile, 0f).getPosition().inTL(left, top);
        if (recipe.currency == null) {
            return;
        }
        int count = SocketableCrafting.count(cargo, recipe.currency.commodityId());
        Color color = count > 0 ? Misc.getBrightPlayerColor() : Misc.getGrayColor();
        element.setParaFont(Fonts.ORBITRON_20AABOLD);
        LabelAPI label = element.addPara("%s", 0f, color, color, String.valueOf(count));
        label.autoSizeToWidth(COUNT_WIDTH).inTL(left + TILE_SIZE + COUNT_LEFT, top + (TILE_SIZE - label.getPosition().getHeight()) / 2f);
    }

    private void showTooltip(Recipe recipe, PositionAPI anchor) {
        hideTooltip();
        if (closing) {
            return;
        }
        CustomPanelAPI panel = Global.getSettings().createCustom(TOOLTIP_WIDTH, TILE_SIZE, new FramedPanelPlugin(SocketWorkbenchPanel.class));
        TooltipMakerAPI element = panel.createUIElement(TOOLTIP_WIDTH - PAD * 2f, 0f, false);
        element.setParaFont(Fonts.ORBITRON_20AA);
        element.addPara("%s", 0f, Misc.getBrightPlayerColor(), Misc.getBrightPlayerColor(), recipeName(recipe));
        element.setParaFontDefault();
        element.addPara("%s", GAP, Misc.getTextColor(), Misc.getTextColor(),
                Translation.text("ui.workbench.recipe." + recipe.key() + ".description"));
        for (String line : tooltipHints(recipe)) {
            element.addPara("%s", GAP, Misc.getGrayColor(), Misc.getGrayColor(), line);
        }
        float contentHeight = element.getHeightSoFar();
        element.getPosition().setSize(TOOLTIP_WIDTH - PAD * 2f, contentHeight);
        float tooltipHeight = contentHeight + PAD * 2f;
        panel.getPosition().setSize(TOOLTIP_WIDTH, tooltipHeight);
        panel.addUIElement(element).inTL(PAD, PAD);
        PositionAPI hostPosition = host.getPosition();
        float left = Math.max(0f, Math.min(anchor.getX() - hostPosition.getX(), hostPosition.getWidth() - TOOLTIP_WIDTH));
        float top = hostPosition.getY() + hostPosition.getHeight() - anchor.getY() + GAP;
        host.addComponent(panel).inTL(left, Math.max(0f, Math.min(top, hostPosition.getHeight() - tooltipHeight)));
        tooltip = panel;
        tooltipFor = recipe;
    }

    private List<String> tooltipHints(Recipe recipe) {
        String synthesise = Translation.msg("ui.workbench.tile.synthesise").arg("cost", recipe.partsCost()).text();
        if (recipe.currency == null) {
            return List.of(synthesise);
        }
        List<String> hints = new ArrayList<>();
        int count = SocketableCrafting.count(cargo(), recipe.currency.commodityId());
        hints.add(Translation.msg("ui.workbench.tile.inStorage").arg("count", count).text());
        if (count == 0) {
            hints.add(Translation.text("ui.workbench.tile.noneOwned"));
        } else if (loaded != null && SocketableCrafting.canUse(recipe.currency, loaded, allowedUniques())) {
            hints.add(Translation.msg("ui.workbench.tile.use").arg("target", loaded.name()).text());
        } else {
            hints.add(Translation.text("ui.workbench.tile.noTarget"));
        }
        hints.add(synthesise);
        return hints;
    }

    private void hideTooltip() {
        if (tooltip != null) {
            host.removeComponent(tooltip);
            tooltip = null;
            tooltipFor = null;
        }
    }

    private static List<String> recipeIcons(Recipe recipe) {
        if (recipe.currency == null) {
            List<String> icons = SocketableDefinitions.all().stream()
                    .filter(definition -> definition.kind() == SocketableKind.SUBROUTINE && !definition.unique())
                    .map(SocketableDefinition::icon).distinct().toList();
            return icons.isEmpty() ? List.of(FALLBACK_SUBROUTINE_ICON) : icons;
        }
        CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(recipe.currency.commodityId());
        return List.of(spec == null ? FALLBACK_SUBROUTINE_ICON : spec.getIconName());
    }

    private static String recipeName(Recipe recipe) {
        return recipe.currency == null ? Translation.text("ui.workbench.recipe.common.name") : commodityName(recipe.currency.commodityId());
    }

    private static String commodityName(String commodityId) {
        CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(commodityId);
        return spec == null ? commodityId : spec.getName();
    }

    private void rebuildDetails() {
        if (details != null) {
            root.removeComponent(details);
        }
        float top = socketAreaTop() + socketArea + GAP;
        float detailsHeight = Math.max(BUTTON_HEIGHT, height - top - bottomReserve());
        TooltipMakerAPI element = root.createUIElement(innerWidth(), detailsHeight, true);
        if (loaded == null) {
            element.addPara("%s", 0f, Misc.getGrayColor(), Misc.getGrayColor(), Translation.text("ui.workbench.empty"));
        } else {
            SocketableName name = loaded.displayName();
            element.setParaFont(Fonts.ORBITRON_20AA);
            element.addPara("%s", 0f, name.rarity().color(), name.rarity().color(), name.title());
            element.setParaFontDefault();
            if (name.baseName() != null) {
                element.addPara("%s", LINE_PAD, Misc.getGrayColor(), Misc.getGrayColor(), name.baseName());
            }
            float pad = GAP * 2f;
            for (StyledText line : loaded.effectLines(true)) {
                VanillaText.addPara(element, line, pad, Misc.getTextColor());
                pad = LINE_PAD;
            }
        }
        root.addUIElement(element).inTL(PAD, top);
        details = element.getExternalScroller() != null ? element.getExternalScroller() : element;
    }

    private void rebuildNotice() {
        if (notice != null) {
            root.removeComponent(notice);
        }
        TooltipMakerAPI element = root.createUIElement(innerWidth(), NOTICE_HEIGHT, false);
        if (noticeText != null) {
            element.addPara("%s", 0f, Misc.getPositiveHighlightColor(), Misc.getPositiveHighlightColor(), noticeText);
        }
        root.addUIElement(element).inTL(PAD, height - PAD - NOTICE_HEIGHT);
        notice = element;
    }

    private float innerWidth() {
        return panelWidth - PAD * 2f;
    }

}
