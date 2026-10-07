package exiledsector.ui.socket;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.socketables.SocketCurrency;
import exiledsector.socketables.Socketable;
import exiledsector.socketables.SocketableCrafting;
import exiledsector.socketables.SocketableDefinition;
import exiledsector.socketables.SocketableDefinitions;
import exiledsector.socketables.SocketableKind;
import exiledsector.socketables.SocketableName;
import exiledsector.socketables.SocketableRarity;
import exiledsector.socketables.SocketableStore;
import exiledsector.socketables.SocketableUnlock;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.SkillTreeSounds;
import exiledsector.ui.VanillaText;
import exiledsector.ui.util.FramedPanelPlugin;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.Predicate;

public final class SocketWorkbenchPanel extends HoloPanel {

    public interface Listener {

        void changed(Socketable loaded);

        void closed();

        void pressedInside();
    }

    public static final float WIDTH = 520f;
    private static final float BUTTON_HEIGHT = 24f;
    private static final float TILE_SIZE = 80f;
    private static final float TILE_GAP = 10f;
    private static final int TILE_COLUMNS = 2;
    private static final float COUNT_LEFT = 12f;
    private static final float COUNT_WIDTH = 120f;
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

    private final Listener workbenchListener;
    private final SocketWorkbenchFlair socketFlair = new SocketWorkbenchFlair();
    private final Random craftingRandom = new Random();
    private float socketAreaHeight;
    private UIComponentAPI headerElement;
    private UIComponentAPI recipesElement;
    private UIComponentAPI detailsElement;
    private UIComponentAPI noticeElement;
    private CustomPanelAPI tooltipPanel;
    private Recipe tooltipRecipe;
    private Socketable loadedSocketable;
    private String noticeText;

    private SocketWorkbenchPanel(CustomPanelAPI hostPanel, Listener workbenchListener) {
        super(hostPanel, SocketWorkbenchPanel.class);
        this.workbenchListener = workbenchListener;
    }

    public static SocketWorkbenchPanel open(CustomPanelAPI hostPanel, float panelLeft, float panelTop, float panelHeight,
                                            Listener workbenchListener) {
        SocketWorkbenchPanel workbenchPanel = new SocketWorkbenchPanel(hostPanel, workbenchListener);
        workbenchPanel.socketAreaHeight = socketAreaFor(panelHeight);
        workbenchPanel.attach(panelLeft, panelTop, WIDTH, panelHeight);
        return workbenchPanel;
    }

    private static float socketAreaFor(float panelHeight) {
        float freeHeight = panelHeight - socketAreaTop() - bottomReserve();
        float preferredHeight = Math.min(MAX_SOCKET_AREA, panelHeight * SOCKET_AREA_SHARE);
        return Math.max(MIN_SOCKET_AREA, Math.min(preferredHeight, freeHeight - MIN_DETAILS_HEIGHT));
    }

    private static float tilesTop() {
        return PAD + HEADER_HEIGHT + GAP;
    }

    private static float tilesHeight() {
        int tileRows = (Recipe.values().length + TILE_COLUMNS - 1) / TILE_COLUMNS;
        return tileRows * TILE_SIZE + (tileRows - 1) * TILE_GAP;
    }

    private static float socketAreaTop() {
        return tilesTop() + tilesHeight() + GAP;
    }

    private static float bottomReserve() {
        return GAP + NOTICE_HEIGHT + PAD;
    }

    public Socketable loaded() {
        return loadedSocketable;
    }

    public void load(Socketable socketable) {
        queue(() -> {
            if (socketable == loadedSocketable) {
                return;
            }
            loadedSocketable = socketable;
            noticeText = null;
            socketFlair.restartLoad();
            rebuildContent();
        });
    }

    public void refresh() {
        queue(() -> {
            if (loadedSocketable != null && !SocketableStore.get().owned().contains(loadedSocketable)) {
                loadedSocketable = null;
            }
            buildHeader();
            rebuildContent();
        });
    }

    @Override
    protected void onClose() {
        hideTooltip();
        workbenchListener.closed();
    }

    @Override
    protected void onPressInside() {
        workbenchListener.pressedInside();
    }

    @Override
    protected UIComponentAPI[] chromeComponents() {
        return new UIComponentAPI[]{headerElement};
    }

    @Override
    protected UIComponentAPI[] contentComponents() {
        return new UIComponentAPI[]{recipesElement, detailsElement, noticeElement};
    }

    @Override
    public void render(float alphaMult) {
        if (panelPosition == null) {
            return;
        }
        float contentAlpha = contentAlpha() * alphaMult;
        float cx = panelPosition.getX() + panelWidth / 2f;
        float cy = panelPosition.getY() + panelHeight - socketAreaTop() - socketAreaHeight / 2f;
        Color subjectColor = loadedSocketable == null ? null : loadedSocketable.rarity().color();
        socketFlair.render(cx, cy, socketAreaHeight * SOCKET_RING_SHARE, SkillTreePanelStyle.GLOW_COLOR, subjectColor,
                loadedSocketable == null ? null : loadedSocketable.iconPath(), contentAlpha);
    }

    @Override
    protected void advanceAlways(float amount) {
        socketFlair.advance(amount, loadedSocketable != null);
    }

    @Override
    public void buttonPressed(Object id) {
        queue(() -> handleButton(id));
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
        CargoAPI cargo = playerCargo();
        if (cargo == null) {
            return false;
        }
        int ownedParts = SocketableCrafting.parts(cargo);
        if (ownedParts < recipe.partsCost()) {
            showNotice(Translation.msg("ui.workbench.notice.notEnoughParts").arg("cost", recipe.partsCost()).arg("parts", ownedParts)
                    .arg("name", recipeName(recipe)).text());
            return false;
        }
        if (recipe.currency == null) {
            Socketable created = SocketableCrafting.synthesiseCommon(cargo, SocketableStore.get(), craftingRandom);
            if (created == null) {
                return false;
            }
            loadedSocketable = created;
            socketFlair.restartLoad();
            noticeText = Translation.msg("ui.workbench.notice.synthesisedCommon").arg("name", created.name()).text();
        } else {
            if (!SocketableCrafting.synthesise(recipe.currency, cargo)) {
                return false;
            }
            noticeText = Translation.msg("ui.workbench.notice.synthesisedCurrency").arg("name", recipeName(recipe)).text();
        }
        SkillTreeSounds.crafted();
        socketFlair.flash(SkillTreePanelStyle.GLOW_COLOR);
        afterChange();
        return true;
    }

    private void use(SocketCurrency currency) {
        CargoAPI cargo = playerCargo();
        if (cargo == null) {
            return;
        }
        if (SocketableCrafting.count(cargo, currency.commodityId()) < 1) {
            showNotice(Translation.msg("ui.workbench.notice.noneOwned").arg("name", commodityName(currency.commodityId())).text());
            return;
        }
        if (loadedSocketable == null) {
            showNotice(Translation.text("ui.workbench.notice.loadFirst"));
            return;
        }
        if (!SocketableCrafting.canUse(currency, loadedSocketable, allowedUniques())) {
            if (currency == SocketCurrency.TRANSPOSITION && loadedSocketable.rarity() == SocketableRarity.UNIQUE) {
                showNotice(Translation.text("ui.workbench.notice.noOtherUnique"));
                return;
            }
            showNotice(Translation.msg("ui.workbench.notice.cannotUse").arg("name", commodityName(currency.commodityId()))
                    .arg("target", loadedSocketable.name()).text());
            return;
        }
        String nameBefore = loadedSocketable.name();
        Socketable result = SocketableCrafting.use(currency, loadedSocketable, cargo, SocketableStore.get(), craftingRandom, allowedUniques());
        if (result == null) {
            return;
        }
        loadedSocketable = result;
        noticeText = Translation.msg("ui.workbench.notice.used." + currency.name().toLowerCase(Locale.ROOT))
                .arg("before", nameBefore).arg("name", result.name()).text();
        SkillTreeSounds.socketed();
        socketFlair.flash(result.rarity().color());
        afterChange();
    }

    private void showNotice(String text) {
        noticeText = text;
        rebuildNotice();
        refreshOpacity();
    }

    private void afterChange() {
        buildHeader();
        rebuildContent();
        workbenchListener.changed(loadedSocketable);
    }

    private static Predicate<SocketableDefinition> allowedUniques() {
        return definition -> SocketableUnlock.unlockConditionMet(definition, Global.getSector());
    }

    @Override
    protected void build() {
        buildHeader();
        rebuildContent();
    }

    private void buildHeader() {
        TooltipMakerAPI element = startHeader("ui.workbench.title", "ui.workbench.close", Control.CLOSE);
        headerElement = finishHeader(element, headerElement, CLOSE_BUTTON_WIDTH + GAP * 2f);
    }

    private void rebuildContent() {
        rebuildTiles();
        rebuildDetails();
        rebuildNotice();
        refreshOpacity();
    }

    private void rebuildTiles() {
        hideTooltip();
        if (recipesElement != null) {
            panelRoot.removeComponent(recipesElement);
        }
        TooltipMakerAPI element = panelRoot.createUIElement(innerWidth(), tilesHeight(), false);
        CargoAPI cargo = playerCargo();
        Recipe[] allRecipes = Recipe.values();
        float cellWidth = innerWidth() / TILE_COLUMNS;
        for (int i = 0; i < allRecipes.length; i++) {
            float tileLeft = (i % TILE_COLUMNS) * cellWidth;
            float tileTop = (i / TILE_COLUMNS) * (TILE_SIZE + TILE_GAP);
            addTile(element, allRecipes[i], tileLeft, tileTop, cargo);
        }
        panelRoot.addUIElement(element).inTL(PAD, tilesTop());
        recipesElement = element;
    }

    private void addTile(TooltipMakerAPI element, Recipe recipe, float tileLeft, float tileTop, CargoAPI cargo) {
        CustomPanelAPI tilePanel = Global.getSettings().createCustom(TILE_SIZE, TILE_SIZE, new WorkbenchTile(recipeIcons(recipe),
                new WorkbenchTile.Listener() {
                    @Override
                    public void hovered(PositionAPI tilePosition) {
                        queue(() -> showTooltip(recipe, tilePosition));
                    }

                    @Override
                    public void left() {
                        queue(() -> {
                            if (tooltipRecipe == recipe) {
                                hideTooltip();
                            }
                        });
                    }

                    @Override
                    public void clicked() {
                        queue(() -> tileClicked(recipe));
                    }

                    @Override
                    public void rightClicked() {
                        queue(() -> tileRightClicked(recipe));
                    }
                }));
        element.addCustom(tilePanel, 0f).getPosition().inTL(tileLeft, tileTop);
        if (recipe.currency == null) {
            return;
        }
        int ownedCount = SocketableCrafting.count(cargo, recipe.currency.commodityId());
        Color countColor = ownedCount > 0 ? Misc.getBrightPlayerColor() : Misc.getGrayColor();
        element.setParaFont(Fonts.ORBITRON_20AABOLD);
        LabelAPI countLabel = element.addPara("%s", 0f, countColor, countColor, String.valueOf(ownedCount));
        countLabel.autoSizeToWidth(COUNT_WIDTH).inTL(tileLeft + TILE_SIZE + COUNT_LEFT,
                tileTop + (TILE_SIZE - countLabel.getPosition().getHeight()) / 2f);
    }

    private void showTooltip(Recipe recipe, PositionAPI anchor) {
        hideTooltip();
        if (isClosing()) {
            return;
        }
        CustomPanelAPI newTooltipPanel = Global.getSettings().createCustom(TOOLTIP_WIDTH, TILE_SIZE,
                new FramedPanelPlugin(SocketWorkbenchPanel.class));
        TooltipMakerAPI element = newTooltipPanel.createUIElement(TOOLTIP_WIDTH - PAD * 2f, 0f, false);
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
        newTooltipPanel.getPosition().setSize(TOOLTIP_WIDTH, tooltipHeight);
        newTooltipPanel.addUIElement(element).inTL(PAD, PAD);
        PositionAPI hostPosition = hostPanel.getPosition();
        float tooltipLeft = Math.max(0f, Math.min(anchor.getX() - hostPosition.getX(), hostPosition.getWidth() - TOOLTIP_WIDTH));
        float tooltipTop = hostPosition.getY() + hostPosition.getHeight() - anchor.getY() + GAP;
        hostPanel.addComponent(newTooltipPanel).inTL(tooltipLeft, Math.max(0f, Math.min(tooltipTop, hostPosition.getHeight() - tooltipHeight)));
        tooltipPanel = newTooltipPanel;
        tooltipRecipe = recipe;
    }

    private List<String> tooltipHints(Recipe recipe) {
        String synthesiseHint = Translation.msg("ui.workbench.tile.synthesise").arg("cost", recipe.partsCost()).text();
        if (recipe.currency == null) {
            return List.of(synthesiseHint);
        }
        List<String> hints = new ArrayList<>();
        int ownedCount = SocketableCrafting.count(playerCargo(), recipe.currency.commodityId());
        hints.add(Translation.msg("ui.workbench.tile.inStorage").arg("count", ownedCount).text());
        if (ownedCount == 0) {
            hints.add(Translation.text("ui.workbench.tile.noneOwned"));
        } else if (loadedSocketable != null && SocketableCrafting.canUse(recipe.currency, loadedSocketable, allowedUniques())) {
            hints.add(Translation.msg("ui.workbench.tile.use").arg("target", loadedSocketable.name()).text());
        } else {
            hints.add(Translation.text("ui.workbench.tile.noTarget"));
        }
        hints.add(synthesiseHint);
        return hints;
    }

    private void hideTooltip() {
        if (tooltipPanel != null) {
            hostPanel.removeComponent(tooltipPanel);
            tooltipPanel = null;
            tooltipRecipe = null;
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
        if (detailsElement != null) {
            panelRoot.removeComponent(detailsElement);
        }
        float detailsTop = socketAreaTop() + socketAreaHeight + GAP;
        float detailsHeight = Math.max(BUTTON_HEIGHT, panelHeight - detailsTop - bottomReserve());
        TooltipMakerAPI element = panelRoot.createUIElement(innerWidth(), detailsHeight, true);
        if (loadedSocketable == null) {
            element.addPara("%s", 0f, Misc.getGrayColor(), Misc.getGrayColor(), Translation.text("ui.workbench.empty"));
        } else {
            SocketableName displayName = loadedSocketable.displayName();
            element.setParaFont(Fonts.ORBITRON_20AA);
            element.addPara("%s", 0f, displayName.rarity().color(), displayName.rarity().color(), displayName.title());
            element.setParaFontDefault();
            if (displayName.baseName() != null) {
                element.addPara("%s", LINE_PAD, Misc.getGrayColor(), Misc.getGrayColor(), displayName.baseName());
            }
            float paraPad = GAP * 2f;
            for (StyledText line : loadedSocketable.effectLines(true)) {
                VanillaText.addPara(element, line, paraPad, Misc.getTextColor());
                paraPad = LINE_PAD;
            }
        }
        panelRoot.addUIElement(element).inTL(PAD, detailsTop);
        detailsElement = element.getExternalScroller() != null ? element.getExternalScroller() : element;
    }

    private void rebuildNotice() {
        if (noticeElement != null) {
            panelRoot.removeComponent(noticeElement);
        }
        TooltipMakerAPI element = panelRoot.createUIElement(innerWidth(), NOTICE_HEIGHT, false);
        if (noticeText != null) {
            element.addPara("%s", 0f, Misc.getPositiveHighlightColor(), Misc.getPositiveHighlightColor(), noticeText);
        }
        panelRoot.addUIElement(element).inTL(PAD, panelHeight - PAD - NOTICE_HEIGHT);
        noticeElement = element;
    }

    private float innerWidth() {
        return panelWidth - PAD * 2f;
    }

}
