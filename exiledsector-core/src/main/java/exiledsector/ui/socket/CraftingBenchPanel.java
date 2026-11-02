package exiledsector.ui.socket;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.items.MaterialItems;
import exiledsector.items.CraftingInhibitor;
import exiledsector.items.GrowableMaterial;
import exiledsector.items.CraftingMaterials;
import exiledsector.items.Item;
import exiledsector.items.ItemCrafting;
import exiledsector.items.ItemCurrency;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemName;
import exiledsector.items.ItemStore;
import exiledsector.items.ItemVolumes;
import exiledsector.items.ItemUnlock;
import exiledsector.items.SlottedLine;
import exiledsector.items.UseVerdict;
import exiledsector.modifiers.ModifierSlot;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.SkillTreeSounds;
import exiledsector.ui.TooltipExpansion;
import exiledsector.ui.VanillaText;
import exiledsector.ui.cargo.CargoIconLook;
import exiledsector.ui.cargo.CargoIconLooks;
import exiledsector.ui.decoration.StarfieldRenderer;
import exiledsector.ui.util.GLDraw;
import com.fs.starfarer.api.input.InputEventAPI;

import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class CraftingBenchPanel extends HoloPanel {

    public interface Listener {

        void changed(Item loaded);

        void closed();

        void pressedInside();

        default boolean offersItemPicker() {
            return false;
        }

        default void requestItem() {
        }

        default BenchModeSupport.OpenTarget openTarget() {
            return null;
        }

        default void wormholeOpened(String message) {
        }

        default void debugActionApplied() {
        }
    }

    static final float FRAME_INSET = 8f;
    private static final float COUNT_INSET = 4f;
    private static final float PLATE_ALPHA = 0.62f;
    private static final float PLATE_PAD = 6f;
    private static final float VIGNETTE_SHARE = 0.06f;
    private static final float PICK_BUTTON_WIDTH = 120f;
    private static final float TOOLTIP_WIDTH = 340f;
    private static final float SLOT_TAG_WIDTH = 20f;
    private static final float SCROLLBAR_ROOM = 20f;
    private static final float MIN_OPEN_BUTTON_HEIGHT = 26f;
    private static final float OPEN_CONFIRM_HEIGHT = 170f;
    private static final float DEBUG_BUTTON_SHARE = 0.32f;
    private static final float DEBUG_BUTTON_GAP = 6f;
    private static final Color PLATE_COLOR = Color.BLACK;
    private static final String FALLBACK_ICON = ItemDefinition.FALLBACK_ICON;

    private enum Control {CLOSE, PICK, CONFIRM_USE, CONFIRM_GROW, CANCEL_CONFIRM, OPEN_WORMHOLE, CONFIRM_OPEN, DEBUG_ACTION}

    private record OpenChoiceButton(Object target) {
    }

    private static final float CONFIRM_WIDTH = 460f;
    private static final float USE_CONFIRM_HEIGHT = 180f;
    private static final float INHIBITOR_LINE_HEIGHT = 22f;
    private static final float GROW_CONFIRM_HEIGHT = 140f;

    private final Listener craftingBenchListener;
    private final CraftingBenchFlair socketFlair = new CraftingBenchFlair();
    private final StarfieldRenderer starfield = new StarfieldRenderer(new SkillTreePanelStyle());
    private final Random craftingRandom = new Random();
    private float sceneScale;
    private UIComponentAPI headerElement;
    private BenchMode benchMode = BenchMode.HULL_ITEMS;
    private ItemCurrency pendingCurrency;
    private GrowableMaterial pendingGrowth;
    private boolean detailsShowRollRanges;
    private UIComponentAPI recipesElement;
    private UIComponentAPI detailsElement;
    private UIComponentAPI noticeElement;
    private UIComponentAPI openElement;
    private ButtonAPI embeddedOpenButton;
    private Item pendingOpenItem;
    private Object pendingOpenTarget;
    private boolean wormholeOpened;
    private Item loadedItem;
    private CargoIconLook loadedLook;
    private final Set<CraftingInhibitor> activeInhibitors = EnumSet.noneOf(CraftingInhibitor.class);
    private final WormholeButtonFx openFx = new WormholeButtonFx();
    private BenchLayout embeddedLayout;
    private BenchMaterialsPanel materialsPanel;
    private boolean openAllowed;
    private String noticeText;
    private boolean noticeIsWarning;

    private CraftingBenchPanel(CustomPanelAPI hostPanel, Listener craftingBenchListener) {
        super(hostPanel);
        this.craftingBenchListener = craftingBenchListener;
    }

    public static CraftingBenchPanel openEmbedded(CustomPanelAPI hostPanel, BenchMode mode, BenchLayout layout, Listener craftingBenchListener) {
        CraftingBenchPanel craftingBenchPanel = new CraftingBenchPanel(hostPanel, craftingBenchListener);
        craftingBenchPanel.benchMode = mode;
        craftingBenchPanel.embeddedLayout = layout;
        craftingBenchPanel.sceneScale = layout.socketRadius() / BenchScene.SLOT_RADIUS;
        BenchLayout.Rect panel = layout.panel();
        craftingBenchPanel.attach(panel.left(), panel.top(), panel.width(), panel.height(), false);
        return craftingBenchPanel;
    }

    public static CraftingBenchPanel open(CustomPanelAPI hostPanel, SocketPanelPlacement area, boolean centred, Listener craftingBenchListener) {
        return open(hostPanel, area, centred, BenchMode.HULL_ITEMS, craftingBenchListener);
    }

    public static CraftingBenchPanel open(CustomPanelAPI hostPanel, SocketPanelPlacement area, boolean centred, BenchMode mode,
                                            Listener craftingBenchListener) {
        CraftingBenchPanel craftingBenchPanel = new CraftingBenchPanel(hostPanel, craftingBenchListener);
        craftingBenchPanel.benchMode = mode;
        craftingBenchPanel.sceneScale = scaleFor(area.panelWidth(), area.panelHeight());
        float width = sizeFor(craftingBenchPanel.sceneScale, BenchScene.ART_WIDTH);
        float height = sizeFor(craftingBenchPanel.sceneScale, BenchScene.ART_HEIGHT);
        float left = centred ? area.panelLeft() + (area.panelWidth() - width) / 2f : area.panelLeft();
        float top = centred ? area.panelTop() + (area.panelHeight() - height) / 2f : area.panelTop();
        craftingBenchPanel.attach(left, top, width, height, area.instantly());
        return craftingBenchPanel;
    }

    static float scaleFor(float areaWidth, float areaHeight) {
        return BenchScene.scaleFor(areaWidth - FRAME_INSET * 2f, areaHeight - FRAME_INSET * 2f);
    }

    static float sizeFor(float scale, float artSize) {
        return artSize * scale + FRAME_INSET * 2f;
    }

    static float tileAngle(int index, int tileCount) {
        if (tileCount <= 1) {
            return (float) (Math.PI / 2.0);
        }
        float firstAngle = (float) Math.PI + BenchScene.ORBIT_DROP_RADIANS;
        float lastAngle = -BenchScene.ORBIT_DROP_RADIANS;
        return firstAngle - index * (firstAngle - lastAngle) / (tileCount - 1);
    }

    static float columnTileTop(int index, int tileCount, float centreY, float tileSize, float gap, float areaHeight) {
        float columnHeight = tileCount * tileSize + Math.max(0, tileCount - 1) * gap;
        float columnTop = Math.max(0f, Math.min(centreY - columnHeight / 2f, areaHeight - columnHeight));
        return columnTop + index * (tileSize + gap);
    }

    private float artX(float artPixels) {
        return FRAME_INSET + artPixels * sceneScale;
    }

    private float artY(float artPixels) {
        return FRAME_INSET + artPixels * sceneScale;
    }

    private float tileSize() {
        return BenchScene.tileSize(sceneScale);
    }

    private float slotRadius() {
        return embeddedLayout != null ? embeddedLayout.socketRadius() : BenchScene.SLOT_RADIUS * sceneScale;
    }

    private float socketScreenX() {
        return panelPosition.getX() + (embeddedLayout != null ? embeddedLayout.socketX() : artX(benchMode.socketX()));
    }

    private float socketScreenY() {
        return panelPosition.getY() + panelHeight - (embeddedLayout != null ? embeddedLayout.socketY() : artY(benchMode.socketY()));
    }

    public void runDebugAction() {
        queue(() -> {
            if (!wormholeOpened && !isConfirmOpen()) {
                debugPressed();
            }
        });
    }

    public Item loaded() {
        return loadedItem;
    }

    public BenchMode mode() {
        return benchMode;
    }

    public void load(Item item) {
        queue(() -> {
            if (item == loadedItem || (item != null && !support().accepts(item))) {
                return;
            }
            cancelConfirm();
            loadedItem = item;
            noticeText = null;
            socketFlair.restartLoad();
            rebuildContent();
        });
    }

    public void refresh() {
        queue(() -> {
            ItemStore store = support().store();
            if (loadedItem != null && (store == null || !store.owned().contains(loadedItem))) {
                loadedItem = null;
                cancelConfirm();
            }
            buildHeader();
            rebuildContent();
        });
    }

    @Override
    protected void onClose() {
        hoverTooltip.hide();
        closeMaterials();
        craftingBenchListener.closed();
    }

    @Override
    protected void onSwappedOut() {
        closeMaterials();
    }

    private void closeMaterials() {
        if (materialsPanel != null) {
            materialsPanel.close();
            materialsPanel = null;
        }
    }

    @Override
    protected boolean drawsFrame() {
        return embeddedLayout == null;
    }

    @Override
    protected void onPressInside() {
        craftingBenchListener.pressedInside();
    }

    @Override
    protected UIComponentAPI[] chromeComponents() {
        return new UIComponentAPI[]{headerElement};
    }

    @Override
    protected UIComponentAPI[] contentComponents() {
        return new UIComponentAPI[]{recipesElement, detailsElement, noticeElement, openElement};
    }

    @Override
    public void renderBelow(float alphaMult) {
        super.renderBelow(alphaMult);
        if (panelPosition == null) {
            return;
        }
        float contentAlpha = contentAlpha() * alphaMult;
        if (contentAlpha <= 0f) {
            return;
        }
        if (embeddedLayout != null) {
            renderEmbedded(contentAlpha);
            return;
        }
        float artLeft = panelPosition.getX() + FRAME_INSET;
        float artBottom = panelPosition.getY() + FRAME_INSET;
        float artWidth = BenchScene.ART_WIDTH * sceneScale;
        float artHeight = BenchScene.ART_HEIGHT * sceneScale;
        if (benchMode.drawsStarfield()) {
            GLDraw.fillQuad(artLeft, artBottom, artWidth, artHeight, PLATE_COLOR, contentAlpha);
            starfield.renderInside(artLeft, artBottom, artWidth, artHeight, contentAlpha);
        } else {
            BenchScene.drawArt(benchMode, artLeft, artBottom, sceneScale, contentAlpha);
        }
        float vignette = Math.min(artWidth, artHeight) * VIGNETTE_SHARE;
        GLDraw.innerGlow(artLeft, artBottom, artWidth, artHeight, vignette, PLATE_COLOR, contentAlpha);
        drawPlate(BenchScene.detailsLeft(benchMode), BenchScene.detailsTop(benchMode), BenchScene.DETAILS_WIDTH, detailsArtHeight(), contentAlpha);
        Color subjectColor = loadedItem == null ? null : loadedItem.frameColor();
        socketFlair.render(socketScreenX(), socketScreenY(), slotRadius(), SkillTreePanelStyle.GLOW_COLOR, subjectColor,
                loadedItem == null ? null : loadedItem.iconPath(), loadedLook, contentAlpha);
    }

    private void renderEmbedded(float alpha) {
        BenchLayout.Rect details = embeddedLayout.details();
        GLDraw.fillQuad(panelPosition.getX() + details.left() - PLATE_PAD, panelPosition.getY() + panelHeight - details.bottom() - PLATE_PAD,
                details.width() + PLATE_PAD * 2f, details.height() + PLATE_PAD * 2f, PLATE_COLOR, PLATE_ALPHA * alpha);
        if (openElement != null && embeddedOpenButton != null) {
            PositionAPI open = embeddedOpenButton.getPosition();
            openFx.render(open.getX(), open.getY(), open.getWidth(), open.getHeight(), openAllowed, alpha);
        }
        Color subjectColor = loadedItem == null ? null : loadedItem.frameColor();
        socketFlair.render(socketScreenX(), socketScreenY(), slotRadius(), SkillTreePanelStyle.GLOW_COLOR, subjectColor,
                loadedItem == null ? null : loadedItem.iconPath(), loadedLook, alpha);
    }

    private void drawPlate(float artLeft, float artTop, float artWidth, float artHeight, float alpha) {
        float plateWidth = artWidth * sceneScale + PLATE_PAD * 2f;
        float plateHeight = artHeight * sceneScale + PLATE_PAD * 2f;
        float plateLeft = panelPosition.getX() + artX(artLeft) - PLATE_PAD;
        float plateBottom = panelPosition.getY() + panelHeight - artY(artTop) - artHeight * sceneScale - PLATE_PAD;
        GLDraw.fillQuad(plateLeft, plateBottom, plateWidth, plateHeight, PLATE_COLOR, PLATE_ALPHA * alpha);
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        if (!isClosing()) {
            for (InputEventAPI event : events) {
                if (!event.isConsumed() && TooltipExpansion.isToggle(event)) {
                    TooltipExpansion.toggle();
                    event.consume();
                }
            }
        }
        if (!isClosing() && panelPosition != null && craftingBenchListener.offersItemPicker()) {
            for (InputEventAPI event : events) {
                if (!event.isConsumed() && event.isLMBDownEvent() && insideSocket(event.getX(), event.getY())) {
                    event.consume();
                    queue(this::requestItem);
                }
            }
        }
        super.processInput(events);
    }

    private boolean insideSocket(float x, float y) {
        float dx = x - socketScreenX();
        float dy = y - socketScreenY();
        float radius = slotRadius() * BenchScene.SOCKET_CLICK_SHARE;
        return dx * dx + dy * dy <= radius * radius;
    }

    private void requestItem() {
        hoverTooltip.hide();
        craftingBenchListener.requestItem();
    }

    @Override
    protected void advanceOpen(float amount) {
        hoverTooltip.refreshIfExpansionChanged();
        if (detailsShowRollRanges != TooltipExpansion.isExpandedOrHeld()) {
            rebuildDetails();
            refreshOpacity();
        }
    }

    @Override
    protected void advanceAlways(float amount) {
        socketFlair.advance(amount, loadedItem != null);
        starfield.advance(amount);
        openFx.advance(amount);
    }

    @Override
    public void buttonPressed(Object id) {
        queue(() -> handleButton(id));
    }

    private void handleButton(Object id) {
        if (wormholeOpened) {
            return;
        }
        if (id == Control.CONFIRM_OPEN) {
            confirmOpen();
        } else if (id instanceof OpenChoiceButton choice) {
            pendingOpenTarget = choice.target();
            confirmOpen();
        } else if (id == Control.CONFIRM_USE) {
            ItemCurrency currency = pendingCurrency;
            cancelConfirm();
            if (currency != null && usable(currency)) {
                applyUse(currency);
            }
        } else if (id == Control.CONFIRM_GROW) {
            GrowableMaterial material = pendingGrowth;
            cancelConfirm();
            if (material != null && growable(material)) {
                applyGrowth(material);
            }
        } else if (id == Control.CANCEL_CONFIRM) {
            cancelConfirm();
        } else if (isConfirmOpen()) {
            return;
        } else if (id == Control.CLOSE) {
            close();
        } else if (id == Control.PICK) {
            requestItem();
        } else if (id == Control.OPEN_WORMHOLE) {
            openPressed();
        } else if (id == Control.DEBUG_ACTION) {
            debugPressed();
        }
    }

    public boolean escape() {
        if (!isConfirmOpen()) {
            return false;
        }
        queue(this::cancelConfirm);
        return true;
    }

    private void cancelConfirm() {
        pendingCurrency = null;
        pendingGrowth = null;
        pendingOpenItem = null;
        closeConfirm();
    }

    @Override
    protected float confirmWidth() {
        return Math.min(CONFIRM_WIDTH, panelWidth - PAD * 2f);
    }

    private void tileClicked(ItemCurrency currency, boolean skipConfirm) {
        hoverTooltip.hide();
        if (wormholeOpened || isConfirmOpen() || !usable(currency)) {
            return;
        }
        if (skipConfirm) {
            applyUse(currency);
        } else {
            openUseConfirm(currency);
        }
    }

    private void tileRightClicked(GrowableMaterial material, boolean skipConfirm) {
        hoverTooltip.hide();
        if (wormholeOpened || isConfirmOpen() || !growable(material)) {
            return;
        }
        if (skipConfirm) {
            applyGrowth(material);
        } else {
            openGrowConfirm(material);
        }
    }

    private void openUseConfirm(ItemCurrency currency) {
        pendingCurrency = currency;
        Set<CraftingInhibitor> spentInhibitors = ItemCrafting.inhibitorsSpentBy(currency, loadedItem, activeInhibitors);
        String name = recipeName(currency);
        openConfirm(USE_CONFIRM_HEIGHT + spentInhibitors.size() * INHIBITOR_LINE_HEIGHT,
                element -> writeCraftConfirm(element, Translation.msg("ui.craftingBench.confirm.useTitle").arg("name", name).arg("target", loadedItem.name()).text(),
                        support().currencyDescription(currency), spentInhibitors),
                "ui.craftingBench.confirm.use", Control.CONFIRM_USE, "ui.craftingBench.confirm.cancel", Control.CANCEL_CONFIRM);
    }

    private void openGrowConfirm(GrowableMaterial material) {
        pendingGrowth = material;
        openConfirm(GROW_CONFIRM_HEIGHT,
                element -> writeCraftConfirm(element, Translation.msg("ui.craftingBench.confirm.growTitle").arg("name", recipeName(material)).text(),
                        Translation.msg("ui.craftingBench.confirm.growBody").arg("cost", material.partsCost()).arg("parts", ItemCrafting.parts(playerMaterials())).text(),
                        Set.of()),
                "ui.craftingBench.confirm.grow", Control.CONFIRM_GROW, "ui.craftingBench.confirm.cancel", Control.CANCEL_CONFIRM);
    }

    private void writeCraftConfirm(TooltipMakerAPI element, String title, String body, Set<CraftingInhibitor> spentInhibitors) {
        VanillaText.plainPara(element, title, 0f, Misc.getBasePlayerColor());
        VanillaText.plainPara(element, body, LINE_PAD * 2f, Misc.getTextColor());
        for (CraftingInhibitor inhibitor : spentInhibitors) {
            VanillaText.plainPara(element, Translation.msg("ui.craftingBench.confirm.inhibitorSpent").arg("name", MaterialItems.displayName(inhibitor.itemId())).text(),
                    LINE_PAD * 2f, Misc.getHighlightColor());
        }
        VanillaText.plainPara(element, Translation.text("ui.craftingBench.confirm.skipHint"), LINE_PAD * 2f, Misc.getGrayColor());
    }

    private boolean growable(GrowableMaterial material) {
        if (!material.synthesisable()) {
            showNotice(Translation.msg("ui.craftingBench.notice.notSynthesisable").arg("name", recipeName(material)).text());
            return false;
        }
        CraftingMaterials materials = playerMaterials();
        int ownedParts = ItemCrafting.parts(materials);
        if (ownedParts < material.partsCost()) {
            showNotice(Translation.msg("ui.craftingBench.notice.notEnoughParts").arg("cost", material.partsCost()).arg("parts", ownedParts)
                    .arg("name", recipeName(material)).text());
            return false;
        }
        return true;
    }

    private void applyGrowth(GrowableMaterial material) {
        if (!ItemCrafting.synthesise(material, playerMaterials())) {
            return;
        }
        noticeText = Translation.msg("ui.craftingBench.notice.synthesisedCurrency").arg("name", recipeName(material)).text();
        SkillTreeSounds.crafted();
        socketFlair.flash(SkillTreePanelStyle.GLOW_COLOR);
        afterChange();
    }

    private boolean usable(ItemCurrency currency) {
        CraftingMaterials materials = playerMaterials();
        BenchModeSupport.Refusal refusal = support().refusal(currency, loadedItem, support().store(), materials, allowedUniques(), activeInhibitors);
        if (refusal.checksInhibitors() && droppedMissingInhibitors(materials)) {
            return false;
        }
        if (refusal.allowed()) {
            return true;
        }
        showNotice(refusalText(currency, refusal.verdict()));
        return false;
    }

    private boolean droppedMissingInhibitors(CraftingMaterials materials) {
        List<CraftingInhibitor> missingInhibitors = activeInhibitors.stream().filter(inhibitor -> materials.count(inhibitor.itemId()) < 1).toList();
        if (missingInhibitors.isEmpty()) {
            return false;
        }
        missingInhibitors.forEach(activeInhibitors::remove);
        showNotice(Translation.msg("ui.craftingBench.notice.inhibitorMissing").arg("name", MaterialItems.displayName(missingInhibitors.get(0).itemId())).text());
        return true;
    }

    private String refusalText(ItemCurrency currency, UseVerdict verdict) {
        return verdict.text(MaterialItems.displayName(currency.itemId()), loadedItem == null ? "" : loadedItem.name());
    }

    private BenchModeSupport support() {
        return BenchModes.of(benchMode);
    }

    private void applyUse(ItemCurrency currency) {
        String nameBefore = loadedItem.name();
        Set<CraftingInhibitor> spentInhibitors = ItemCrafting.inhibitorsSpentBy(currency, loadedItem, activeInhibitors);
        BenchModeSupport.Use used = support().use(currency, loadedItem, support().store(), playerMaterials(), craftingRandom,
                allowedUniques(), activeInhibitors, nameBefore);
        if (used == null) {
            showNotice(refusalText(currency, support().cannotUse()));
            return;
        }
        activeInhibitors.removeAll(spentInhibitors);
        loadedItem = used.selectedAfter();
        noticeText = withSpentInhibitors(used.notice(), spentInhibitors);
        SkillTreeSounds.socketed();
        socketFlair.flash(used.result().frameColor());
        afterChange();
    }

    private static String withSpentInhibitors(String notice, Set<CraftingInhibitor> spentInhibitors) {
        String combined = notice;
        for (CraftingInhibitor inhibitor : spentInhibitors) {
            String spentNotice = Translation.msg("ui.craftingBench.notice.inhibitorSpent").arg("name", MaterialItems.displayName(inhibitor.itemId())).text();
            combined = Translation.msg("format.sentences").arg("a", combined).arg("b", spentNotice).text();
        }
        return combined;
    }

    private void showNotice(String text) {
        noticeText = text;
        noticeIsWarning = false;
        rebuildNotice();
        refreshOpacity();
    }

    private void showWarning(String text) {
        noticeText = text;
        noticeIsWarning = true;
        rebuildNotice();
        refreshOpacity();
    }

    private void afterChange() {
        noticeIsWarning = false;
        buildHeader();
        rebuildContent();
        craftingBenchListener.changed(loadedItem);
    }

    private static Predicate<ItemDefinition> allowedUniques() {
        return definition -> ItemVolumes.dropsNow(definition) && ItemUnlock.unlockConditionMet(definition, Global.getSector());
    }

    @Override
    protected void build() {
        buildHeader();
        rebuildContent();
    }

    private void buildHeader() {
        if (embeddedLayout != null) {
            return;
        }
        TooltipMakerAPI element = startHeader(support().titleKey(), "ui.craftingBench.close", Control.CLOSE);
        float partsRight = CLOSE_BUTTON_WIDTH + GAP * 2f;
        if (craftingBenchListener.offersItemPicker()) {
            element.addButton(Translation.text("ui.craftingBench.pickItem"), Control.PICK, PICK_BUTTON_WIDTH, HEADER_BUTTON_HEIGHT, 0f)
                    .getPosition().inTR(partsRight, 0f);
            partsRight += PICK_BUTTON_WIDTH + GAP * 2f;
        }
        addPartsCount(element, partsRight);
        headerElement = finishHeader(element, headerElement);
    }

    private void rebuildContent() {
        rebuildTiles();
        rebuildDetails();
        rebuildNotice();
        rebuildOpenButton();
        refreshOpacity();
    }

    private boolean showsOpenButton() {
        return support().openTexts() != null && craftingBenchListener.openTarget() != null;
    }

    private float detailsArtHeight() {
        float detailsBottom = showsOpenButton() ? BenchScene.CHART_DETAILS_BOTTOM : BenchScene.DETAILS_BOTTOM;
        return detailsBottom - BenchScene.detailsTop(benchMode);
    }

    private BenchModeSupport.OpenCheck openBlocker() {
        BenchModeSupport.OpenTarget openTarget = craftingBenchListener.openTarget();
        return support().openCheck(Global.getSector(), support().store(), loadedItem, openTarget == null ? null : openTarget.target());
    }

    private void rebuildOpenButton() {
        if (!showsOpenButton()) {
            if (openElement != null) {
                panelRoot.removeComponent(openElement);
                openElement = null;
            }
            return;
        }
        if (embeddedLayout != null) {
            rebuildEmbeddedOpenButton();
            return;
        }
        BenchModeSupport.OpenCheck blocker = openBlocker();
        BenchModeSupport.OpenTexts openTexts = support().openTexts();
        float buttonWidth = BenchScene.DETAILS_WIDTH * sceneScale;
        float plateBottom = artY(BenchScene.CHART_DETAILS_BOTTOM) + PLATE_PAD;
        float buttonTop = Math.max(artY(BenchScene.OPEN_BUTTON_TOP), plateBottom + PLATE_PAD);
        float artBottom = artY(BenchScene.ART_HEIGHT);
        float buttonHeight = Math.min(Math.max(MIN_OPEN_BUTTON_HEIGHT, BenchScene.OPEN_BUTTON_HEIGHT * sceneScale), artBottom - buttonTop);
        BenchModeSupport.DebugAction debugAction = support().debugAction();
        float debugWidth = debugAction == null ? 0f : buttonWidth * DEBUG_BUTTON_SHARE;
        float openWidth = debugAction == null ? buttonWidth : buttonWidth - debugWidth - DEBUG_BUTTON_GAP;
        TooltipMakerAPI element = panelRoot.createUIElement(buttonWidth, buttonHeight, false);
        element.setButtonFontOrbitron20Bold();
        ButtonAPI openButton = element.addButton(Translation.text(openTexts.buttonKey()), Control.OPEN_WORMHOLE, Misc.getBrightPlayerColor(),
                Misc.getDarkPlayerColor(), Alignment.MID, CutStyle.ALL, openWidth, buttonHeight, 0f);
        openButton.setEnabled(blocker.allowsOpening() && !wormholeOpened);
        openButton.setShowTooltipWhileInactive(true);
        String tooltipText = Translation.text(blocker.allowsOpening() ? openTexts.tooltipKey() : blocker.reasonKey());
        element.addTooltipToPrevious(new GameTextTooltip(TOOLTIP_WIDTH, tooltip -> VanillaText.plainPara(tooltip, tooltipText, 0f,
                Misc.getTextColor())), TooltipMakerAPI.TooltipLocation.BELOW);
        element.setButtonFontDefault();
        if (debugAction != null) {
            addDebugButton(element, debugAction, openWidth + DEBUG_BUTTON_GAP, debugWidth, buttonHeight);
        }
        openElement = replaceElement(openElement, element, artX(BenchScene.detailsLeft(benchMode)), buttonTop);
    }

    private void rebuildEmbeddedOpenButton() {
        BenchModeSupport.OpenCheck blocker = openBlocker();
        BenchModeSupport.OpenTexts openTexts = support().openTexts();
        BenchLayout.Rect open = embeddedLayout.openButton();
        openAllowed = blocker.allowsOpening() && !wormholeOpened;
        TooltipMakerAPI element = panelRoot.createUIElement(open.width(), open.height(), false);
        element.setButtonFontOrbitron20Bold();
        ButtonAPI openButton = element.addButton(Translation.text(openTexts.buttonKey()), Control.OPEN_WORMHOLE, WormholeButtonFx.TEXT_COLOR,
                WormholeButtonFx.BACKGROUND_COLOR, Alignment.MID, CutStyle.ALL, open.width(), open.height(), 0f);
        embeddedOpenButton = openButton;
        openButton.setEnabled(openAllowed);
        openButton.setShowTooltipWhileInactive(true);
        String tooltipText = Translation.text(blocker.allowsOpening() ? openTexts.tooltipKey() : blocker.reasonKey());
        element.addTooltipToPrevious(new GameTextTooltip(TOOLTIP_WIDTH, tooltip -> VanillaText.plainPara(tooltip, tooltipText, 0f,
                Misc.getTextColor())), TooltipMakerAPI.TooltipLocation.ABOVE);
        element.setButtonFontDefault();
        openElement = replaceElement(openElement, element, open.left(), open.top());
    }

    private void addDebugButton(TooltipMakerAPI element, BenchModeSupport.DebugAction debugAction, float left, float width, float height) {
        ButtonAPI debugButton = element.addButton(Translation.text(debugAction.buttonKey()), Control.DEBUG_ACTION, Misc.getNegativeHighlightColor(),
                Misc.getDarkPlayerColor(), Alignment.MID, CutStyle.ALL, width, height, 0f);
        debugButton.setEnabled(!wormholeOpened);
        debugButton.getPosition().inTL(left, 0f);
        String tooltipText = Translation.text(debugAction.tooltipKey());
        element.addTooltipToPrevious(new GameTextTooltip(TOOLTIP_WIDTH, tooltip -> VanillaText.plainPara(tooltip, tooltipText, 0f,
                Misc.getTextColor())), TooltipMakerAPI.TooltipLocation.BELOW);
    }

    private void debugPressed() {
        hoverTooltip.hide();
        BenchModeSupport.OpenTarget openTarget = craftingBenchListener.openTarget();
        if (openTarget == null || support().debugAction() == null) {
            return;
        }
        support().runDebugAction(Global.getSector(), loadedItem, openTarget.target());
        SkillTreeSounds.panelOpened();
        craftingBenchListener.debugActionApplied();
    }

    private void openPressed() {
        hoverTooltip.hide();
        BenchModeSupport.OpenCheck blocker = openBlocker();
        if (!blocker.allowsOpening()) {
            SkillTreeSounds.refused();
            showWarning(Translation.text(blocker.reasonKey()));
            rebuildOpenButton();
            refreshOpacity();
            return;
        }
        BenchModeSupport.OpenTarget openTarget = craftingBenchListener.openTarget();
        pendingOpenItem = loadedItem;
        pendingOpenTarget = openTarget == null ? null : openTarget.target();
        List<String> confirmLines = support().confirmLines(loadedItem, openTarget);
        List<BenchModeSupport.OpenChoice> choices = support().openChoices(loadedItem, openTarget);
        if (choices.isEmpty() && embeddedLayout != null) {
            confirmOpen();
        } else if (choices.isEmpty()) {
            showOpenConfirm(confirmLines);
        } else {
            showChoiceConfirm(confirmLines, choices);
        }
    }

    private void showChoiceConfirm(List<String> confirmLines, List<BenchModeSupport.OpenChoice> choices) {
        Item confirmedItem = pendingOpenItem;
        int entries = support().openEntries(confirmedItem);
        BenchModeSupport.OpenTexts openTexts = support().openTexts();
        List<String> lines = new ArrayList<>(confirmLines);
        List<String> labels = new ArrayList<>();
        List<Object> ids = new ArrayList<>();
        for (BenchModeSupport.OpenChoice choice : choices) {
            if (choice.description() != null && !choice.description().isEmpty()) {
                lines.add(choice.description());
            }
            labels.add(choice.label());
            ids.add(new OpenChoiceButton(choice.target()));
        }
        openChoiceConfirm(element -> writeOpenConfirm(element, openTexts, confirmedItem, entries, lines), labels, ids, openTexts.confirmNoKey(),
                Control.CANCEL_CONFIRM);
    }

    private void showOpenConfirm(List<String> confirmLines) {
        Item confirmedItem = pendingOpenItem;
        int entries = support().openEntries(confirmedItem);
        BenchModeSupport.OpenTexts openTexts = support().openTexts();
        openConfirm(OPEN_CONFIRM_HEIGHT + confirmLines.size() * INHIBITOR_LINE_HEIGHT,
                element -> writeOpenConfirm(element, openTexts, confirmedItem, entries, confirmLines), openTexts.confirmYesKey(),
                Control.CONFIRM_OPEN, openTexts.confirmNoKey(), Control.CANCEL_CONFIRM);
    }

    private static void writeOpenConfirm(TooltipMakerAPI element, BenchModeSupport.OpenTexts openTexts, Item openedItem, int entries,
                                         List<String> confirmLines) {
        VanillaText.plainPara(element, Translation.msg(openTexts.confirmTitleKey()).arg("name", openedItem.name()).text(), 0f,
                openedItem.rarity().color());
        VanillaText.plainPara(element, Translation.text(openTexts.confirmBodyKey()), LINE_PAD * 2f, Misc.getTextColor());
        for (String line : confirmLines) {
            VanillaText.plainPara(element, line, LINE_PAD, Misc.getHighlightColor());
        }
        if (openTexts.confirmEntriesKey() != null) {
            VanillaText.plainPara(element, Translation.msg(openTexts.confirmEntriesKey()).count(entries).text(), LINE_PAD * 2f,
                    Misc.getGrayColor());
        }
    }

    private void confirmOpen() {
        Item openedItem = pendingOpenItem;
        Object openedTarget = pendingOpenTarget;
        cancelConfirm();
        pendingOpenTarget = null;
        if (openedItem == null || openedItem != loadedItem) {
            return;
        }
        BenchModeSupport.OpenOutcome outcome = support().openAt(Global.getSector(), support().store(), openedItem, support().openEntries(openedItem),
                craftingRandom, openedTarget);
        String message = Translation.msg(outcome.messageKey()).arg("name", openedItem.name()).text();
        if (!outcome.opened()) {
            SkillTreeSounds.refused();
            showWarning(message);
            rebuildOpenButton();
            refreshOpacity();
            return;
        }
        wormholeOpened = true;
        support().playOpened();
        rebuildOpenButton();
        refreshOpacity();
        craftingBenchListener.wormholeOpened(message);
    }

    private void rebuildTiles() {
        hoverTooltip.hide();
        if (embeddedLayout != null) {
            rebuildMaterials();
            return;
        }
        float artWidth = BenchScene.ART_WIDTH * sceneScale;
        float artHeight = BenchScene.ART_HEIGHT * sceneScale;
        TooltipMakerAPI element = panelRoot.createUIElement(artWidth, artHeight, false);
        CraftingMaterials materials = playerMaterials();
        BenchModeSupport support = support();
        List<ItemCurrency> shownCurrencies = support.currencies();
        List<ItemCurrency> orbitCurrencies = shownCurrencies.stream().filter(support::inOrbit).toList();
        List<ItemCurrency> columnCurrencies = shownCurrencies.stream().filter(currency -> !support.inOrbit(currency)).toList();
        float tileSize = tileSize();
        float socketX = benchMode.socketX() * sceneScale;
        float socketY = benchMode.socketY() * sceneScale;
        for (int i = 0; i < orbitCurrencies.size(); i++) {
            float angle = tileAngle(i, orbitCurrencies.size());
            float tileCentreX = socketX + benchMode.orbitRadiusX() * sceneScale * (float) Math.cos(angle);
            float tileCentreY = socketY - benchMode.orbitRadiusY() * sceneScale * (float) Math.sin(angle);
            addTile(element, orbitCurrencies.get(i), tileCentreX - tileSize / 2f, tileCentreY - tileSize / 2f, tileSize, materials);
        }
        float columnLeft = artWidth - BenchScene.COLUMN_RIGHT_MARGIN * sceneScale - tileSize;
        float columnGap = BenchScene.COLUMN_GAP * sceneScale;
        for (int i = 0; i < columnCurrencies.size(); i++) {
            float tileTop = columnTileTop(i, columnCurrencies.size(), socketY, tileSize, columnGap, artHeight);
            addTile(element, columnCurrencies.get(i), columnLeft, tileTop, tileSize, materials);
        }
        if (support.usesInhibitors()) {
            addInhibitorTiles(element, tileSize, materials);
        }
        recipesElement = replaceElement(recipesElement, element, FRAME_INSET, FRAME_INSET);
    }

    private void addInhibitorTiles(TooltipMakerAPI element, float tileSize, CraftingMaterials materials) {
        float inhibitorTop = benchMode.inhibitorY() * sceneScale - tileSize / 2f;
        addInhibitorTile(element, CraftingInhibitor.PREFIX, benchMode.leftInhibitorX() * sceneScale - tileSize / 2f, inhibitorTop, tileSize, materials);
        addInhibitorTile(element, CraftingInhibitor.SUFFIX, benchMode.rightInhibitorX() * sceneScale - tileSize / 2f, inhibitorTop, tileSize, materials);
    }

    private void rebuildMaterials() {
        if (isClosing()) {
            return;
        }
        CraftingMaterials materials = playerMaterials();
        List<SectionedTileGrid.Tile> currencyTiles = new ArrayList<>();
        for (ItemCurrency currency : support().currencies()) {
            currencyTiles.add(new SectionedTileGrid.Tile(currencyTile(currency, materials), materials.count(currency.itemId())));
        }
        List<SectionedTileGrid.Section> sections = new ArrayList<>();
        sections.add(new SectionedTileGrid.Section(Translation.text("ui.craftingBench.materials.currencies"), Misc.getBasePlayerColor(), currencyTiles));
        if (support().usesInhibitors()) {
            List<SectionedTileGrid.Tile> inhibitorTiles = new ArrayList<>();
            for (CraftingInhibitor inhibitor : List.of(CraftingInhibitor.PREFIX, CraftingInhibitor.SUFFIX)) {
                inhibitorTiles.add(new SectionedTileGrid.Tile(inhibitorTile(inhibitor), materials.count(inhibitor.itemId())));
            }
            sections.add(new SectionedTileGrid.Section(Translation.text("ui.craftingBench.materials.inhibitors"), Misc.getBasePlayerColor(), inhibitorTiles));
        }
        if (materialsPanel == null) {
            materialsPanel = BenchMaterialsPanel.open(hostPanel, embeddedLayout.materials(), sections, () -> queue(hoverTooltip::hide));
        } else {
            materialsPanel.show(sections);
        }
    }

    private static List<String> inhibitorIcons(CraftingInhibitor inhibitor) {
        String iconPath = MaterialItems.loadedIconPath(inhibitor.itemId());
        return List.of(iconPath == null ? FALLBACK_ICON : iconPath);
    }

    private TileInput.Handler inhibitorHandler(CraftingInhibitor inhibitor) {
        return tileHandler(inhibitor, anchor -> showInhibitorTooltip(inhibitor, anchor), skipConfirm -> toggleInhibitor(inhibitor),
                skipConfirm -> tileRightClicked(inhibitor, skipConfirm));
    }

    private TileInput.Handler currencyHandler(ItemCurrency currency) {
        return tileHandler(currency, anchor -> showTooltip(currency, anchor), skipConfirm -> tileClicked(currency, skipConfirm),
                skipConfirm -> tileRightClicked(currency, skipConfirm));
    }

    private void addInhibitorTile(TooltipMakerAPI element, CraftingInhibitor inhibitor, float tileLeft, float tileTop, float tileSize,
                             CraftingMaterials materials) {
        CustomPanelAPI tilePanel = Global.getSettings().createCustom(tileSize, tileSize, inhibitorTile(inhibitor));
        element.addCustom(tilePanel, 0f).getPosition().inTL(tileLeft, tileTop);
        addCount(element, inhibitor.itemId(), tileLeft, tileTop, tileSize, materials);
    }

    private CraftingBenchTile inhibitorTile(CraftingInhibitor inhibitor) {
        return CraftingBenchTile.toggle(inhibitorIcons(inhibitor), CargoIconLooks.forMaterial(inhibitor.itemId()), () -> activeInhibitors.contains(inhibitor), inhibitorHandler(inhibitor));
    }

    private CraftingBenchTile currencyTile(ItemCurrency currency, CraftingMaterials materials) {
        boolean owned = materials.count(currency.itemId()) > 0;
        return CraftingBenchTile.usable(recipeIcons(currency), recipeLook(currency), () -> owned, currencyHandler(currency));
    }

    private void toggleInhibitor(CraftingInhibitor inhibitor) {
        hoverTooltip.hide();
        if (wormholeOpened || isConfirmOpen()) {
            return;
        }
        String inhibitorName = MaterialItems.displayName(inhibitor.itemId());
        if (activeInhibitors.remove(inhibitor)) {
            showNotice(Translation.msg("ui.craftingBench.notice.inhibitorOff").arg("name", inhibitorName).text());
            return;
        }
        if (playerMaterials().count(inhibitor.itemId()) < 1) {
            showNotice(Translation.msg(inhibitor.synthesisable() ? "ui.craftingBench.notice.noneOwned" : "ui.craftingBench.notice.noneFound").arg("name", inhibitorName).text());
            return;
        }
        activeInhibitors.add(inhibitor);
        showNotice(Translation.msg("ui.craftingBench.notice.inhibitorOn." + inhibitor.name().toLowerCase(Locale.ROOT)).arg("name", inhibitorName).text());
    }

    private void showInhibitorTooltip(CraftingInhibitor inhibitor, PositionAPI anchor) {
        if (isClosing()) {
            hoverTooltip.hide();
            return;
        }
        hoverTooltip.show(inhibitor, TOOLTIP_WIDTH, (element, expanded) -> writeInhibitorTooltip(element, inhibitor), anchor,
                ItemHoverTooltip.Placement.BELOW, PAD);
    }

    private void writeInhibitorTooltip(TooltipMakerAPI element, CraftingInhibitor inhibitor) {
        element.setParaFont(Fonts.ORBITRON_20AA);
        VanillaText.plainPara(element, MaterialItems.displayName(inhibitor.itemId()), 0f, inhibitor.color());
        element.setParaFontDefault();
        VanillaText.plainPara(element, Translation.text(inhibitorDescriptionKey(inhibitor)), GAP, Misc.getTextColor());
        int ownedCount = playerMaterials().count(inhibitor.itemId());
        VanillaText.plainPara(element, Translation.msg("ui.craftingBench.tile.inStorage").arg("count", ownedCount).text(), GAP, Misc.getGrayColor());
        if (activeInhibitors.contains(inhibitor)) {
            VanillaText.plainPara(element, Translation.text("ui.craftingBench.inhibitor.on"), GAP, Misc.getGrayColor());
        } else if (ownedCount > 0) {
            VanillaText.plainPara(element, Translation.text("ui.craftingBench.inhibitor.off"), GAP, Misc.getGrayColor());
        }
        VanillaText.plainPara(element, growHint(inhibitor), GAP, Misc.getGrayColor());
    }

    static String inhibitorDescriptionKey(CraftingInhibitor inhibitor) {
        return MaterialItems.descriptionKey(inhibitor.itemId());
    }

    private TileInput.Handler tileHandler(Object tooltipKey, Consumer<PositionAPI> showTooltip, Consumer<Boolean> clicked,
                                          Consumer<Boolean> rightClicked) {
        return TileInput.Handler.of(tilePosition -> queue(() -> showTooltip.accept(tilePosition)), () -> queue(() -> {
            if (hoverTooltip.isShowing(tooltipKey)) {
                hoverTooltip.hide();
            }
        }), () -> {
            boolean skipConfirm = ctrlHeld();
            queue(() -> clicked.accept(skipConfirm));
            return true;
        }, () -> {
            boolean skipConfirm = ctrlHeld();
            queue(() -> rightClicked.accept(skipConfirm));
            return true;
        });
    }

    private static void addCount(TooltipMakerAPI element, String itemId, float tileLeft, float tileTop, float tileSize,
                                 CraftingMaterials materials) {
        int ownedCount = materials.count(itemId);
        Color countColor = ownedCount > 0 ? Misc.getBrightPlayerColor() : Misc.getGrayColor();
        element.setParaFont(Fonts.ORBITRON_12);
        LabelAPI countLabel = VanillaText.plainPara(element, String.valueOf(ownedCount), 0f, countColor);
        countLabel.setAlignment(Alignment.RMID);
        countLabel.autoSizeToWidth(tileSize - COUNT_INSET * 2f).inTL(tileLeft + COUNT_INSET,
                tileTop + tileSize - countLabel.getPosition().getHeight() - COUNT_INSET);
        element.setParaFontDefault();
    }

    private void addTile(TooltipMakerAPI element, ItemCurrency currency, float tileLeft, float tileTop, float tileSize,
                         CraftingMaterials materials) {
        CustomPanelAPI tilePanel = Global.getSettings().createCustom(tileSize, tileSize, currencyTile(currency, materials));
        element.addCustom(tilePanel, 0f).getPosition().inTL(tileLeft, tileTop);
        addCount(element, currency.itemId(), tileLeft, tileTop, tileSize, materials);
    }

    private void showTooltip(ItemCurrency currency, PositionAPI anchor) {
        if (isClosing()) {
            hoverTooltip.hide();
            return;
        }
        hoverTooltip.show(currency, TOOLTIP_WIDTH, (element, expanded) -> writeTooltip(element, currency), anchor,
                ItemHoverTooltip.Placement.BELOW, PAD);
    }

    private void writeTooltip(TooltipMakerAPI element, ItemCurrency currency) {
        element.setParaFont(Fonts.ORBITRON_20AA);
        VanillaText.plainPara(element, recipeName(currency), 0f, Misc.getBrightPlayerColor());
        element.setParaFontDefault();
        VanillaText.plainPara(element, support().currencyDescription(currency), GAP, Misc.getTextColor());
        float linePad = GAP;
        for (StyledText line : support().currencyLines(currency)) {
            VanillaText.addPara(element, line, linePad, Misc.getTextColor());
            linePad = LINE_PAD;
        }
        for (String line : tooltipHints(currency)) {
            VanillaText.plainPara(element, line, GAP, Misc.getGrayColor());
        }
    }

    private List<String> tooltipHints(ItemCurrency currency) {
        List<String> hints = new ArrayList<>();
        int ownedCount = playerMaterials().count(currency.itemId());
        hints.add(Translation.msg("ui.craftingBench.tile.inStorage").arg("count", ownedCount).text());
        if (ownedCount == 0) {
            if (currency.synthesisable()) {
                hints.add(Translation.text("ui.craftingBench.tile.noneOwned"));
            }
        } else {
            BenchModeSupport.Refusal refusal = support().refusal(currency, loadedItem, support().store(), playerMaterials(), allowedUniques(),
                    activeInhibitors);
            hints.add(refusal.allowed()
                    ? Translation.msg(support().useHintKey()).arg("target", loadedItem.name()).text() : refusalText(currency, refusal.verdict()));
        }
        hints.add(growHint(currency));
        return hints;
    }

    private static String growHint(GrowableMaterial material) {
        return material.synthesisable()
                ? Translation.msg("ui.craftingBench.tile.synthesise").arg("cost", material.partsCost()).text()
                : Translation.text("ui.craftingBench.tile.notSynthesisable");
    }

    private static List<String> recipeIcons(ItemCurrency currency) {
        String iconPath = MaterialItems.loadedIconPath(currency.itemId());
        return List.of(iconPath == null ? FALLBACK_ICON : iconPath);
    }

    private static CargoIconLook recipeLook(ItemCurrency currency) {
        return CargoIconLooks.forMaterial(currency.itemId());
    }

    private static String recipeName(GrowableMaterial material) {
        return MaterialItems.displayName(material.itemId());
    }

    private void rebuildDetails() {
        loadedLook = CargoIconLooks.forItem(loadedItem);
        detailsShowRollRanges = TooltipExpansion.isExpandedOrHeld();
        float detailsWidth = embeddedLayout != null ? embeddedLayout.details().width() : BenchScene.DETAILS_WIDTH * sceneScale;
        float detailsHeight = embeddedLayout != null ? embeddedLayout.details().height() : detailsArtHeight() * sceneScale;
        TooltipMakerAPI element = panelRoot.createUIElement(detailsWidth, detailsHeight, true);
        if (loadedItem == null) {
            centred(VanillaText.plainPara(element, Translation.text(support().emptyKey()), 0f, Misc.getGrayColor()));
        } else {
            ItemName displayName = loadedItem.displayName();
            element.setParaFont(Fonts.ORBITRON_20AA);
            centred(VanillaText.plainPara(element, displayName.title(), 0f, displayName.rarity().color()));
            element.setParaFontDefault();
            if (displayName.baseName() != null) {
                centred(VanillaText.plainPara(element, displayName.baseName(), LINE_PAD, Misc.getGrayColor()));
            }
            for (StyledText line : support().headerLines(loadedItem)) {
                centred(VanillaText.addPara(element, line, LINE_PAD, Misc.getGrayColor()));
            }
            float paraPad = GAP * 2f;
            float linePad = paraPad;
            for (StyledText line : support().detailLines(loadedItem)) {
                centred(VanillaText.addPara(element, line, linePad, Misc.getTextColor()));
                linePad = LINE_PAD;
            }
            List<SlottedLine> implicitLines = new ArrayList<>();
            loadedItem.implicitLines(detailsShowRollRanges).forEach(line -> implicitLines.add(new SlottedLine(null, line)));
            addDetailSection(element, "hullItem.tooltip.implicit", implicitLines, paraPad);
            if (detailsShowRollRanges) {
                addSlottedDetailSection(element, "socketable.tooltip.primaryData", loadedItem.slottedEffectLines(true, null), paraPad,
                        detailsWidth);
            } else {
                addDetailSection(element, "socketable.tooltip.primaryData", loadedItem.slottedEffectLines(false, null), paraPad);
            }
        }
        float detailsLeft = embeddedLayout != null ? embeddedLayout.details().left() : artX(BenchScene.detailsLeft(benchMode));
        float detailsTop = embeddedLayout != null ? embeddedLayout.details().top() : artY(BenchScene.detailsTop(benchMode));
        detailsElement = placedComponent(replaceElement(detailsElement, element, detailsLeft, detailsTop));
    }

    private static void addDetailSection(TooltipMakerAPI element, String headingKey, List<SlottedLine> lines, float headingPad) {
        if (lines.isEmpty()) {
            return;
        }
        element.addSectionHeading(Translation.text(headingKey), Alignment.MID, headingPad);
        float linePad = GAP;
        for (SlottedLine line : lines) {
            VanillaText.addPara(element, line.text(), linePad, Misc.getTextColor());
            addLineDetailTooltip(element, line);
            linePad = LINE_PAD;
        }
    }

    private static void addLineDetailTooltip(TooltipMakerAPI element, SlottedLine line) {
        if (!line.hasDetails()) {
            return;
        }
        element.addTooltipToPrevious(new GameTextTooltip(TOOLTIP_WIDTH, tooltip -> {
            float detailPad = 0f;
            if (line.detailTitle() != null) {
                tooltip.addTitle(line.detailTitle());
                detailPad = GAP;
            }
            for (StyledText detail : line.details()) {
                VanillaText.addPara(tooltip, detail, detailPad, Misc.getTextColor());
                detailPad = LINE_PAD;
            }
        }), TooltipMakerAPI.TooltipLocation.BELOW);
    }

    private static void addSlottedDetailSection(TooltipMakerAPI element, String headingKey, List<SlottedLine> lines, float headingPad,
                                                float sectionWidth) {
        if (lines.isEmpty()) {
            return;
        }
        element.addSectionHeading(Translation.text(headingKey), Alignment.MID, headingPad);
        float textWidth = sectionWidth - SCROLLBAR_ROOM - SLOT_TAG_WIDTH - GAP;
        float linePad = GAP;
        for (SlottedLine line : lines) {
            TooltipMakerAPI textCell = element.beginSubTooltip(textWidth);
            VanillaText.addPara(textCell, line.text(), 0f, Misc.getTextColor());
            element.endSubTooltip();
            element.addCustom(textCell, linePad);
            addLineDetailTooltip(element, line);
            linePad = LINE_PAD;
            if (line.slot() == null) {
                continue;
            }
            TooltipMakerAPI slotCell = element.beginSubTooltip(SLOT_TAG_WIDTH);
            VanillaText.plainPara(slotCell, Translation.text(slotTagKey(line.slot())), 0f, Misc.getGrayColor()).setAlignment(Alignment.RMID);
            element.endSubTooltip();
            element.addCustomDoNotSetPosition(slotCell).getPosition().rightOfTop(textCell, GAP);
        }
    }

    static String slotTagKey(ModifierSlot slot) {
        return slot == ModifierSlot.PREFIX ? "ui.craftingBench.slotTag.prefix" : "ui.craftingBench.slotTag.suffix";
    }

    private static void centred(LabelAPI label) {
        if (label != null) {
            label.setAlignment(Alignment.MID);
        }
    }

    private void rebuildNotice() {
        BenchLayout.Rect notice = embeddedLayout != null ? embeddedLayout.notice()
                : new BenchLayout.Rect(PAD, artY(BenchScene.NOTICE_TOP), innerWidth(), BenchScene.NOTICE_HEIGHT * sceneScale);
        TooltipMakerAPI element = panelRoot.createUIElement(notice.width(), notice.height(), false);
        if (noticeText != null) {
            Color noticeColor = noticeIsWarning ? Misc.getNegativeHighlightColor() : Misc.getPositiveHighlightColor();
            centred(VanillaText.plainPara(element, noticeText, 0f, noticeColor));
        }
        noticeElement = replaceElement(noticeElement, element, notice.left(), notice.top());
    }

}
