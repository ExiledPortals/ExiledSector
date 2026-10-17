package exiledsector.ui.socket;

import com.fs.starfarer.api.Global;
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
import exiledsector.skills.CommodityNames;
import exiledsector.hullitems.SocketCurrency;
import exiledsector.hullitems.SocketCustody;
import exiledsector.hullitems.SocketMaterials;
import exiledsector.hullitems.HullItem;
import exiledsector.hullitems.HullItemCrafting;
import exiledsector.hullitems.HullItemDefinition;
import exiledsector.hullitems.HullItemDefinitions;
import exiledsector.hullitems.HullItemType;
import exiledsector.hullitems.HullItemName;
import exiledsector.hullitems.HullItemRarity;
import exiledsector.hullitems.HullItemStore;
import exiledsector.hullitems.HullItemUnlock;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.SkillTreeSounds;
import exiledsector.ui.VanillaText;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.Predicate;

public final class SocketWorkbenchPanel extends HoloPanel {

    public interface Listener {

        void changed(HullItem loaded);

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
    private static final String FALLBACK_SUBROUTINE_ICON = HullItemDefinition.FALLBACK_ICON;

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
            return currency == null ? HullItemCrafting.commonSynthesisParts() : currency.partsCost();
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
    private HullItem loadedHullItem;
    private String noticeText;

    private SocketWorkbenchPanel(CustomPanelAPI hostPanel, Listener workbenchListener) {
        super(hostPanel);
        this.workbenchListener = workbenchListener;
    }

    public static SocketWorkbenchPanel open(CustomPanelAPI hostPanel, float panelLeft, float panelTop, float panelHeight,
                                            Listener workbenchListener) {
        SocketWorkbenchPanel workbenchPanel = new SocketWorkbenchPanel(hostPanel, workbenchListener);
        workbenchPanel.socketAreaHeight = socketAreaFor(panelHeight);
        workbenchPanel.attach(panelLeft, panelTop, WIDTH, panelHeight, false);
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

    public HullItem loaded() {
        return loadedHullItem;
    }

    public void load(HullItem hullItem) {
        queue(() -> {
            if (hullItem == loadedHullItem) {
                return;
            }
            loadedHullItem = hullItem;
            noticeText = null;
            socketFlair.restartLoad();
            rebuildContent();
        });
    }

    public void refresh() {
        queue(() -> {
            if (loadedHullItem != null && !HullItemStore.get().owned().contains(loadedHullItem)) {
                loadedHullItem = null;
            }
            buildHeader();
            rebuildContent();
        });
    }

    @Override
    protected void onClose() {
        hoverTooltip.hide();
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
        Color subjectColor = loadedHullItem == null ? null : loadedHullItem.rarity().color();
        socketFlair.render(cx, cy, socketAreaHeight * SOCKET_RING_SHARE, SkillTreePanelStyle.GLOW_COLOR, subjectColor,
                loadedHullItem == null ? null : loadedHullItem.iconPath(), contentAlpha);
    }

    @Override
    protected void advanceAlways(float amount) {
        socketFlair.advance(amount, loadedHullItem != null);
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
        hoverTooltip.hide();
        if (recipe.currency == null) {
            showNotice(Translation.msg("ui.workbench.notice.synthesiseHint").arg("name", recipeName(recipe)).text());
        } else {
            use(recipe.currency);
        }
    }

    private void tileRightClicked(Recipe recipe) {
        hoverTooltip.hide();
        synthesise(recipe);
    }

    private boolean synthesise(Recipe recipe) {
        SocketMaterials materials = playerMaterials();
        int ownedParts = HullItemCrafting.parts(materials);
        if (ownedParts < recipe.partsCost()) {
            showNotice(Translation.msg("ui.workbench.notice.notEnoughParts").arg("cost", recipe.partsCost()).arg("parts", ownedParts)
                    .arg("name", recipeName(recipe)).text());
            return false;
        }
        if (recipe.currency == null) {
            HullItem created = HullItemCrafting.synthesiseCommon(materials, HullItemStore.get(), craftingRandom);
            if (created == null) {
                return false;
            }
            loadedHullItem = created;
            socketFlair.restartLoad();
            noticeText = Translation.msg("ui.workbench.notice.synthesisedCommon").arg("name", created.name()).text();
        } else {
            if (!HullItemCrafting.synthesise(recipe.currency, materials)) {
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
        SocketMaterials materials = playerMaterials();
        if (materials.count(currency.commodityId()) < 1) {
            showNotice(Translation.msg("ui.workbench.notice.noneOwned").arg("name", CommodityNames.displayName(currency.commodityId())).text());
            return;
        }
        if (loadedHullItem == null) {
            showNotice(Translation.text("ui.workbench.notice.loadFirst"));
            return;
        }
        if (!HullItemCrafting.canUse(currency, loadedHullItem, allowedUniques())) {
            if (currency == SocketCurrency.TRANSPOSITION && loadedHullItem.rarity() == HullItemRarity.UNIQUE) {
                showNotice(Translation.text("ui.workbench.notice.noOtherUnique"));
                return;
            }
            showNotice(Translation.msg("ui.workbench.notice.cannotUse").arg("name", CommodityNames.displayName(currency.commodityId()))
                    .arg("target", loadedHullItem.name()).text());
            return;
        }
        String nameBefore = loadedHullItem.name();
        HullItem result = HullItemCrafting.use(currency, loadedHullItem, materials, HullItemStore.get(), craftingRandom, allowedUniques());
        if (result == null) {
            return;
        }
        loadedHullItem = result;
        noticeText = Translation.msg("ui.workbench.notice.used." + currency.name().toLowerCase(Locale.ROOT))
                .arg("before", nameBefore).arg("name", result.name()).text();
        SocketCustody.markHostsChanged(result);
        if (currency == SocketCurrency.TRANSPOSITION && SocketCustody.unsocketIfMisfit(result)) {
            String movedNotice = Translation.msg("ui.workbench.notice.movedToStorage").arg("name", result.name())
                    .arg("type", result.kind().displayName()).text();
            noticeText = Translation.msg("format.sentences").arg("a", noticeText).arg("b", movedNotice).text();
        }
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
        workbenchListener.changed(loadedHullItem);
    }

    private static Predicate<HullItemDefinition> allowedUniques() {
        return definition -> HullItemUnlock.unlockConditionMet(definition, Global.getSector());
    }

    @Override
    protected void build() {
        buildHeader();
        rebuildContent();
    }

    private void buildHeader() {
        TooltipMakerAPI element = startHeader("ui.workbench.title", "ui.workbench.close", Control.CLOSE);
        addPartsCount(element, CLOSE_BUTTON_WIDTH + GAP * 2f);
        headerElement = finishHeader(element, headerElement);
    }

    private void rebuildContent() {
        rebuildTiles();
        rebuildDetails();
        rebuildNotice();
        refreshOpacity();
    }

    private void rebuildTiles() {
        hoverTooltip.hide();
        TooltipMakerAPI element = panelRoot.createUIElement(innerWidth(), tilesHeight(), false);
        SocketMaterials materials = playerMaterials();
        Recipe[] allRecipes = Recipe.values();
        float cellWidth = innerWidth() / TILE_COLUMNS;
        for (int i = 0; i < allRecipes.length; i++) {
            float tileLeft = (i % TILE_COLUMNS) * cellWidth;
            float tileTop = (i / TILE_COLUMNS) * (TILE_SIZE + TILE_GAP);
            addTile(element, allRecipes[i], tileLeft, tileTop, materials);
        }
        recipesElement = replaceElement(recipesElement, element, PAD, tilesTop());
    }

    private void addTile(TooltipMakerAPI element, Recipe recipe, float tileLeft, float tileTop, SocketMaterials materials) {
        CustomPanelAPI tilePanel = Global.getSettings().createCustom(TILE_SIZE, TILE_SIZE, new WorkbenchTile(recipeIcons(recipe),
                new TileInput.Handler() {
                    @Override
                    public void entered(PositionAPI tilePosition) {
                        queue(() -> showTooltip(recipe, tilePosition));
                    }

                    @Override
                    public void left() {
                        queue(() -> {
                            if (hoverTooltip.isShowing(recipe)) {
                                hoverTooltip.hide();
                            }
                        });
                    }

                    @Override
                    public boolean clicked() {
                        queue(() -> tileClicked(recipe));
                        return true;
                    }

                    @Override
                    public boolean rightClicked() {
                        queue(() -> tileRightClicked(recipe));
                        return true;
                    }
                }));
        element.addCustom(tilePanel, 0f).getPosition().inTL(tileLeft, tileTop);
        if (recipe.currency == null) {
            return;
        }
        int ownedCount = materials.count(recipe.currency.commodityId());
        Color countColor = ownedCount > 0 ? Misc.getBrightPlayerColor() : Misc.getGrayColor();
        element.setParaFont(Fonts.ORBITRON_20AABOLD);
        LabelAPI countLabel = VanillaText.plainPara(element, String.valueOf(ownedCount), 0f, countColor);
        countLabel.autoSizeToWidth(COUNT_WIDTH).inTL(tileLeft + TILE_SIZE + COUNT_LEFT,
                tileTop + (TILE_SIZE - countLabel.getPosition().getHeight()) / 2f);
    }

    private void showTooltip(Recipe recipe, PositionAPI anchor) {
        if (isClosing()) {
            hoverTooltip.hide();
            return;
        }
        hoverTooltip.show(recipe, TOOLTIP_WIDTH, (element, expanded) -> writeTooltip(element, recipe), anchor,
                HullItemHoverTooltip.Placement.BELOW, PAD);
    }

    private void writeTooltip(TooltipMakerAPI element, Recipe recipe) {
        element.setParaFont(Fonts.ORBITRON_20AA);
        VanillaText.plainPara(element, recipeName(recipe), 0f, Misc.getBrightPlayerColor());
        element.setParaFontDefault();
        VanillaText.plainPara(element, Translation.text("ui.workbench.recipe." + recipe.key() + ".description"), GAP, Misc.getTextColor());
        for (String line : tooltipHints(recipe)) {
            VanillaText.plainPara(element, line, GAP, Misc.getGrayColor());
        }
    }

    private List<String> tooltipHints(Recipe recipe) {
        String synthesiseHint = Translation.msg("ui.workbench.tile.synthesise").arg("cost", recipe.partsCost()).text();
        if (recipe.currency == null) {
            return List.of(synthesiseHint);
        }
        List<String> hints = new ArrayList<>();
        int ownedCount = playerMaterials().count(recipe.currency.commodityId());
        hints.add(Translation.msg("ui.workbench.tile.inStorage").arg("count", ownedCount).text());
        if (ownedCount == 0) {
            hints.add(Translation.text("ui.workbench.tile.noneOwned"));
        } else if (loadedHullItem != null && HullItemCrafting.canUse(recipe.currency, loadedHullItem, allowedUniques())) {
            hints.add(Translation.msg("ui.workbench.tile.use").arg("target", loadedHullItem.name()).text());
        } else {
            hints.add(Translation.text("ui.workbench.tile.noTarget"));
        }
        hints.add(synthesiseHint);
        return hints;
    }

    private static List<String> recipeIcons(Recipe recipe) {
        if (recipe.currency == null) {
            List<String> icons = HullItemDefinitions.all().stream()
                    .filter(definition -> definition.kind() == HullItemType.SUBROUTINE && !definition.unique())
                    .map(HullItemDefinition::icon).distinct().toList();
            return icons.isEmpty() ? List.of(FALLBACK_SUBROUTINE_ICON) : icons;
        }
        CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(recipe.currency.commodityId());
        return List.of(spec == null ? FALLBACK_SUBROUTINE_ICON : spec.getIconName());
    }

    private static String recipeName(Recipe recipe) {
        return recipe.currency == null ? Translation.text("ui.workbench.recipe.common.name") : CommodityNames.displayName(recipe.currency.commodityId());
    }

    private void rebuildDetails() {
        float detailsTop = socketAreaTop() + socketAreaHeight + GAP;
        float detailsHeight = Math.max(BUTTON_HEIGHT, panelHeight - detailsTop - bottomReserve());
        TooltipMakerAPI element = panelRoot.createUIElement(innerWidth(), detailsHeight, true);
        if (loadedHullItem == null) {
            VanillaText.plainPara(element, Translation.text("ui.workbench.empty"), 0f, Misc.getGrayColor());
        } else {
            HullItemName displayName = loadedHullItem.displayName();
            element.setParaFont(Fonts.ORBITRON_20AA);
            VanillaText.plainPara(element, displayName.title(), 0f, displayName.rarity().color());
            element.setParaFontDefault();
            if (displayName.baseName() != null) {
                VanillaText.plainPara(element, displayName.baseName(), LINE_PAD, Misc.getGrayColor());
            }
            float paraPad = GAP * 2f;
            List<StyledText> implicitLines = loadedHullItem.implicitLines(true);
            for (StyledText line : implicitLines) {
                VanillaText.addPara(element, line, paraPad, Misc.getTextColor());
                paraPad = LINE_PAD;
            }
            if (!implicitLines.isEmpty()) {
                paraPad = GAP * 2f;
            }
            for (StyledText line : loadedHullItem.effectLines(true)) {
                VanillaText.addPara(element, line, paraPad, Misc.getTextColor());
                paraPad = LINE_PAD;
            }
        }
        detailsElement = placedComponent(replaceElement(detailsElement, element, PAD, detailsTop));
    }

    private void rebuildNotice() {
        TooltipMakerAPI element = panelRoot.createUIElement(innerWidth(), NOTICE_HEIGHT, false);
        if (noticeText != null) {
            VanillaText.plainPara(element, noticeText, 0f, Misc.getPositiveHighlightColor());
        }
        noticeElement = replaceElement(noticeElement, element, PAD, panelHeight - PAD - NOTICE_HEIGHT);
    }

    private float innerWidth() {
        return panelWidth - PAD * 2f;
    }

}
