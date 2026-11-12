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
import exiledsector.i18n.I18n;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.items.MaterialItems;
import exiledsector.items.CraftingInhibitor;
import exiledsector.items.GrowableMaterial;
import exiledsector.items.CraftingMaterials;
import exiledsector.items.Item;
import exiledsector.items.ItemCrafting;
import exiledsector.items.CraftingMaterialFamily;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemName;
import exiledsector.items.ItemStore;
import exiledsector.items.ItemVolumes;
import exiledsector.items.ItemUnlock;
import exiledsector.items.SlottedLine;
import exiledsector.items.SlottedRows;
import exiledsector.items.UseVerdict;
import exiledsector.skills.tags.ThemeTags;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.SkillTreeSounds;
import exiledsector.ui.TooltipExpansion;
import exiledsector.ui.TooltipTextSize;
import exiledsector.ui.VanillaText;
import exiledsector.ui.cargo.CargoIconLook;
import exiledsector.ui.cargo.CargoIconLooks;
import exiledsector.ui.decoration.StarfieldRenderer;
import exiledsector.ui.util.BorderedPanel;
import exiledsector.ui.util.GLDraw;
import com.fs.starfarer.api.input.InputEventAPI;

import java.awt.Color;
import java.util.ArrayList;
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

        default void reshaped(float panelLeft) {
        }
    }

    private interface TileLeft {

        float in(BenchSpread spread);
    }

    private record TileSpot(PositionAPI tile, PositionAPI count, TileLeft left, float top, float size) {
    }

    static final float FRAME_INSET = 8f;
    static final float TRAY_SIDE_INSET = 20f;
    static final float PLATE_PAD = 6f;
    private static final float COUNT_INSET = 4f;
    private static final float PLATE_ALPHA = 0.62f;
    private static final float CONTRACT_SECONDS = 0.3f;
    private static final float TRAY_SECONDS = 0.3f;
    private static final float TRAY_FILTER_DELAY_SECONDS = 0.25f;
    private static final float VIGNETTE_SHARE = 0.06f;
    private static final float PICK_BUTTON_WIDTH = 120f;
    private static final float TOOLTIP_WIDTH = 340f;
    private static final float LARGE_TEXT_TOOLTIP_WIDTH = 530f;
    private static final float SLOT_TAG_WIDTH = 20f;
    private static final float SCROLLBAR_ROOM = 20f;
    private static final float MIN_OPEN_BUTTON_HEIGHT = 26f;
    private static final float OPEN_CONFIRM_HEIGHT = 170f;
    private static final float DEBUG_BUTTON_SHARE = 0.32f;
    private static final float DEBUG_BUTTON_GAP = 6f;
    private static final Color PLATE_COLOR = Color.BLACK;
    private static final String FALLBACK_ICON = ItemDefinition.FALLBACK_ICON;

    private enum Control {CLOSE, PICK, CONFIRM_USE, CONFIRM_GROW, CONFIRM_TRAY, CANCEL_CONFIRM, OPEN_WORMHOLE, CONFIRM_OPEN, DEBUG_ACTION}

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
    private CraftingMaterialFamily pendingCraftingItem;
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
    private final ActiveInhibitors activeInhibitors = new ActiveInhibitors();
    private final InhibitorOrbit inhibitorOrbit = new InhibitorOrbit();
    private final BenchTrays trays = new BenchTrays();
    private final BenchTrayContext trayContext = new TrayContext();
    private TooltipMakerAPI trayElement;
    private float trayProgress;
    private float trayFromWidth;
    private float trayToWidth;
    private float trayResizeProgress = 1f;
    private Runnable pendingTrayConfirm;
    private BenchTrayList.FilterBar trayFilterBar;
    private String trayFilterText = "";
    private String typedTrayFilter = "";
    private float trayFilterDelay;
    private boolean trayBuiltWithLargeText;
    private final WormholeButtonFx openFx = new WormholeButtonFx();
    private BenchLayout embeddedLayout;
    private BenchMaterialsPanel materialsPanel;
    private boolean openAllowed;
    private String noticeText;
    private boolean noticeIsWarning;
    private final List<TileSpot> tileSpots = new ArrayList<>();
    private BenchSpread.Contents spreadContents;
    private BenchSpread spread;
    private float contractProgress;
    private boolean contractWanted;
    private Runnable contractSettled;

    private CraftingBenchPanel(CustomPanelAPI hostPanel, Listener craftingBenchListener) {
        super(hostPanel);
        this.craftingBenchListener = craftingBenchListener;
    }

    public static CraftingBenchPanel openEmbedded(CustomPanelAPI hostPanel, BenchMode mode, BenchLayout layout, Listener craftingBenchListener) {
        CraftingBenchPanel craftingBenchPanel = new CraftingBenchPanel(hostPanel, craftingBenchListener);
        craftingBenchPanel.benchMode = mode;
        craftingBenchPanel.embeddedLayout = layout;
        craftingBenchPanel.sceneScale = layout.socketRadius() / BenchScene.SLOT_RADIUS;
        craftingBenchPanel.refreshSpreadContents();
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
        craftingBenchPanel.refreshSpreadContents();
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
        return panelPosition.getX() + (embeddedLayout != null ? embeddedLayout.socketX() : FRAME_INSET + spread.socketX());
    }

    private float detailsLeftX() {
        return FRAME_INSET + spread.socketX() - BenchScene.DETAILS_WIDTH / 2f * sceneScale;
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

    void contract(boolean wanted, Runnable settled) {
        queue(() -> {
            cancelConfirm();
            hoverTooltip.hide();
            if (wanted) {
                closeTray();
            }
            contractWanted = wanted;
            contractSettled = settled;
            if (contractProgress == contractTarget()) {
                settleContraction();
            }
        });
    }

    private void refreshSpreadContents() {
        BenchModeSupport support = support();
        boolean orbit = support.craftingItems().stream().anyMatch(support::inOrbit);
        spreadContents = new BenchSpread.Contents(orbit, support.reservesColumn(), embeddedLayout == null && !support.trays().isEmpty());
        spread = currentSpread();
    }

    private BenchSpread currentSpread() {
        BenchSpread full = BenchSpread.full(benchMode, sceneScale, spreadContents);
        BenchSpread contracted = full.towards(BenchSpread.contracted(benchMode, sceneScale, spreadContents), BenchSpread.eased(contractProgress));
        return contracted.towards(BenchSpread.trayOpen(benchMode, sceneScale, spreadContents, shownTrayWidth()), BenchSpread.eased(trayProgress));
    }

    private float shownTrayWidth() {
        return trayFromWidth + (trayToWidth - trayFromWidth) * BenchSpread.eased(trayResizeProgress);
    }

    private float wantedTrayWidth(BenchTray tray) {
        if (tray.iconColumns() > 0) {
            return TRAY_SIDE_INSET * 2f + BenchTrayList.tileColumnsWidth(tray.iconColumns(), tileSize());
        }
        return BenchSpread.maxTrayWidth(benchMode, sceneScale, spreadContents);
    }

    private float contractTarget() {
        return contractWanted ? 1f : 0f;
    }

    private float trayTarget() {
        return trays.isOpen() ? 1f : 0f;
    }

    private boolean reshaping() {
        return contractProgress != contractTarget() || trayProgress != trayTarget() || trayResizeProgress < 1f;
    }

    private boolean traysUsable() {
        return !contractWanted && contractProgress == 0f;
    }

    private void advanceTray(float amount) {
        float target = trayTarget();
        if (trayProgress == target && trayResizeProgress == 1f) {
            return;
        }
        float step = amount / TRAY_SECONDS;
        trayProgress = target > trayProgress ? Math.min(target, trayProgress + step) : Math.max(target, trayProgress - step);
        trayResizeProgress = Math.min(1f, trayResizeProgress + step);
        I18n.forGameText(this::applySpread);
        if (trayProgress == 1f && trayResizeProgress == 1f) {
            I18n.forGameText(() -> {
                rebuildTray();
                refreshOpacity();
            });
        } else if (trayProgress == 0f) {
            I18n.forGameText(this::rebuildNotice);
        }
    }

    private void advanceContraction(float amount) {
        float target = contractTarget();
        if (contractProgress == target) {
            return;
        }
        float step = amount / CONTRACT_SECONDS;
        contractProgress = contractWanted ? Math.min(target, contractProgress + step) : Math.max(target, contractProgress - step);
        I18n.forGameText(this::applySpread);
        if (contractProgress == target) {
            settleContraction();
        }
    }

    private void settleContraction() {
        Runnable settled = contractSettled;
        contractSettled = null;
        if (settled != null) {
            settled.run();
        }
    }

    private void applySpread() {
        float previousArtWidth = spread.artWidth();
        spread = currentSpread();
        if (recipesElement != null) {
            recipesElement.getPosition().setSize(spread.artWidth(), BenchScene.ART_HEIGHT * sceneScale);
        }
        placeTiles();
        if (detailsElement != null) {
            detailsElement.getPosition().inTL(detailsLeftX(), artY(BenchScene.detailsTop(benchMode)));
        }
        if (openElement != null) {
            openElement.getPosition().inTL(detailsLeftX(), openButtonTop());
        }
        if (spread.artWidth() != previousArtWidth) {
            float width = spread.artWidth() + FRAME_INSET * 2f;
            reshape(panelLeft + panelWidth - width, width);
            buildHeader();
            rebuildNotice();
            craftingBenchListener.reshaped(panelLeft);
        }
        refreshOpacity();
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
        return new UIComponentAPI[]{recipesElement, detailsElement, noticeElement, openElement, placedComponent(trayElement),
                trayFilterBar == null ? null : trayFilterBar.element()};
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
        float artWidth = spread.artWidth();
        float fullArtWidth = BenchScene.ART_WIDTH * sceneScale;
        float artHeight = BenchScene.ART_HEIGHT * sceneScale;
        float artShift = spread.artShift();
        if (benchMode.drawsStarfield()) {
            GLDraw.fillQuad(artLeft, artBottom, artWidth, artHeight, PLATE_COLOR, contentAlpha);
            starfield.renderWindow(artLeft + artShift, artBottom, fullArtWidth, artHeight, -artShift, artWidth, contentAlpha);
        } else {
            BenchScene.drawArt(benchMode, artLeft, artBottom, sceneScale, BenchScene.visibleArtLeft(artShift, artWidth, sceneScale), artWidth,
                    contentAlpha);
        }
        float vignette = Math.min(fullArtWidth, artHeight) * VIGNETTE_SHARE;
        GLDraw.innerGlow(artLeft, artBottom, artWidth, artHeight, vignette, PLATE_COLOR, contentAlpha);
        drawPlate(detailsLeftX(), BenchScene.detailsTop(benchMode), BenchScene.DETAILS_WIDTH, detailsArtHeight(), contentAlpha);
        drawTrayPlate(contentAlpha);
        Color subjectColor = loadedItem == null ? null : loadedItem.frameColor();
        socketFlair.render(socketScreenX(), socketScreenY(), slotRadius(), SkillTreePanelStyle.GLOW_COLOR, subjectColor,
                loadedItem == null ? null : loadedItem.iconPath(), loadedLook, contentAlpha);
        inhibitorOrbit.render(socketScreenX(), socketScreenY(), slotRadius(), appliedInhibitors(), contentAlpha);
    }

    private void drawTrayPlate(float alpha) {
        float trayWidth = spread.trayWidth();
        if (trayWidth <= 0f) {
            return;
        }
        float trayBottom = panelPosition.getY() + panelHeight - trayPlateTop() - trayHeight();
        BorderedPanel.draw(panelPosition.getX() + FRAME_INSET + spread.trayLeft(), trayBottom, trayWidth, trayHeight(), alpha);
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

    private void drawPlate(float contentLeft, float artTop, float artWidth, float artHeight, float alpha) {
        float plateWidth = artWidth * sceneScale + PLATE_PAD * 2f;
        float plateHeight = artHeight * sceneScale + PLATE_PAD * 2f;
        float plateLeft = panelPosition.getX() + contentLeft - PLATE_PAD;
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
        advanceContraction(amount);
        advanceTray(amount);
        advanceTrayFilter(amount);
        if (trayElement != null && trayBuiltWithLargeText != TooltipTextSize.large()) {
            I18n.forGameText(this::rebuildTray);
        }
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
        if (!activeInhibitors.isEmpty()) {
            inhibitorOrbit.advance(amount);
        }
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
            CraftingMaterialFamily craftingItem = pendingCraftingItem;
            cancelConfirm();
            if (craftingItem != null && usable(craftingItem)) {
                applyUse(craftingItem);
            }
        } else if (id == Control.CONFIRM_GROW) {
            GrowableMaterial material = pendingGrowth;
            cancelConfirm();
            if (material != null && growable(material)) {
                applyGrowth(material);
            }
        } else if (id == Control.CONFIRM_TRAY) {
            Runnable confirmed = pendingTrayConfirm;
            cancelConfirm();
            if (confirmed != null && loadedItem != null) {
                confirmed.run();
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
        pendingCraftingItem = null;
        pendingGrowth = null;
        pendingOpenItem = null;
        pendingTrayConfirm = null;
        closeConfirm();
    }

    @Override
    protected float confirmWidth() {
        return Math.min(CONFIRM_WIDTH, panelWidth - PAD * 2f);
    }

    private void tileClicked(CraftingMaterialFamily craftingItem, boolean skipConfirm) {
        hoverTooltip.hide();
        if (wormholeOpened || isConfirmOpen() || reshaping() || !usable(craftingItem)) {
            return;
        }
        if (skipConfirm) {
            applyUse(craftingItem);
        } else {
            openUseConfirm(craftingItem);
        }
    }

    private void tileRightClicked(GrowableMaterial material, boolean skipConfirm) {
        hoverTooltip.hide();
        if (wormholeOpened || isConfirmOpen() || reshaping() || !growable(material)) {
            return;
        }
        if (skipConfirm) {
            applyGrowth(material);
        } else {
            openGrowConfirm(material);
        }
    }

    private void openUseConfirm(CraftingMaterialFamily craftingItem) {
        pendingCraftingItem = craftingItem;
        Set<CraftingInhibitor> spentInhibitors = ItemCrafting.inhibitorsSpentBy(craftingItem, loadedItem, appliedInhibitors());
        String name = recipeName(craftingItem);
        openConfirm(USE_CONFIRM_HEIGHT + spentInhibitors.size() * INHIBITOR_LINE_HEIGHT,
                element -> writeCraftConfirm(element, Translation.msg("ui.craftingBench.confirm.useTitle").arg("name", name).arg("target", loadedItem.name()).text(),
                        support().craftingItemDescription(craftingItem), spentInhibitors),
                "ui.craftingBench.confirm.use", Control.CONFIRM_USE, "ui.craftingBench.confirm.cancel", Control.CANCEL_CONFIRM);
    }

    private void openGrowConfirm(GrowableMaterial material) {
        pendingGrowth = material;
        openConfirm(GROW_CONFIRM_HEIGHT,
                element -> writeCraftConfirm(element, Translation.msg("ui.craftingBench.confirm.growTitle").arg("name", recipeName(material)).text(),
                        StyledText.of(Translation.msg("ui.craftingBench.confirm.growBody").arg("cost", material.feedstockCost()).arg("feedstock", ItemCrafting.feedstock(playerMaterials())).text()),
                        Set.of()),
                "ui.craftingBench.confirm.grow", Control.CONFIRM_GROW, "ui.craftingBench.confirm.cancel", Control.CANCEL_CONFIRM);
    }

    private void writeCraftConfirm(TooltipMakerAPI element, String title, StyledText body, Set<CraftingInhibitor> spentInhibitors) {
        VanillaText.plainPara(element, title, 0f, Misc.getBasePlayerColor());
        VanillaText.addPara(element, body, LINE_PAD * 2f, Misc.getTextColor());
        for (CraftingInhibitor inhibitor : spentInhibitors) {
            VanillaText.plainPara(element, Translation.msg("ui.craftingBench.confirm.inhibitorSpent").arg("name", MaterialItems.displayName(inhibitor.itemId())).text(),
                    LINE_PAD * 2f, Misc.getHighlightColor());
        }
        VanillaText.plainPara(element, Translation.text("ui.craftingBench.confirm.skipHint"), LINE_PAD * 2f, Misc.getGrayColor());
    }

    private boolean growable(GrowableMaterial material) {
        if (!material.growable()) {
            showNotice(Translation.msg("ui.craftingBench.notice.notGrowable").arg("name", recipeName(material)).text());
            return false;
        }
        CraftingMaterials materials = playerMaterials();
        int ownedFeedstock = ItemCrafting.feedstock(materials);
        if (ownedFeedstock < material.feedstockCost()) {
            showNotice(Translation.msg("ui.craftingBench.notice.notEnoughFeedstock").arg("cost", material.feedstockCost()).arg("feedstock", ownedFeedstock)
                    .arg("name", recipeName(material)).text());
            return false;
        }
        return true;
    }

    private void applyGrowth(GrowableMaterial material) {
        if (!ItemCrafting.grow(material, playerMaterials())) {
            return;
        }
        noticeText = Translation.msg("ui.craftingBench.notice.grown").arg("name", recipeName(material)).text();
        SkillTreeSounds.crafted();
        socketFlair.flash(SkillTreePanelStyle.GLOW_COLOR);
        afterChange();
    }

    private boolean usable(CraftingMaterialFamily craftingItem) {
        CraftingMaterials materials = playerMaterials();
        BenchModeSupport.Refusal refusal = support().refusal(craftingItem, loadedItem, support().store(), materials, allowedUniques(), appliedInhibitors());
        if (refusal.checksInhibitors() && droppedMissingInhibitors(materials)) {
            return false;
        }
        if (refusal.allowed()) {
            return true;
        }
        showNotice(refusalText(craftingItem, refusal.verdict()));
        return false;
    }

    private Set<CraftingInhibitor> appliedInhibitors() {
        return support().usesInhibitors() ? activeInhibitors.view() : Set.of();
    }

    private boolean droppedMissingInhibitors(CraftingMaterials materials) {
        if (!support().usesInhibitors()) {
            return false;
        }
        List<CraftingInhibitor> missingInhibitors = activeInhibitors.dropMissing(materials::count);
        if (missingInhibitors.isEmpty()) {
            return false;
        }
        showNotice(Translation.msg("ui.craftingBench.notice.inhibitorMissing").arg("name", MaterialItems.displayName(missingInhibitors.get(0).itemId())).text());
        return true;
    }

    private String refusalText(CraftingMaterialFamily craftingItem, UseVerdict verdict) {
        return verdict.text(MaterialItems.displayName(craftingItem.itemId()), loadedItem == null ? "" : loadedItem.name());
    }

    private BenchModeSupport support() {
        return BenchModes.of(benchMode);
    }

    private void applyUse(CraftingMaterialFamily craftingItem) {
        String nameBefore = loadedItem.name();
        Set<CraftingInhibitor> spentInhibitors = ItemCrafting.inhibitorsSpentBy(craftingItem, loadedItem, appliedInhibitors());
        BenchModeSupport.Use used = support().use(craftingItem, loadedItem, support().store(), playerMaterials(), craftingRandom,
                allowedUniques(), appliedInhibitors(), nameBefore);
        if (used == null) {
            showNotice(refusalText(craftingItem, support().cannotUse()));
            return;
        }
        activeInhibitors.spend(spentInhibitors);
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
        float feedstockRight = CLOSE_BUTTON_WIDTH + GAP * 2f;
        if (craftingBenchListener.offersItemPicker()) {
            element.addButton(Translation.text("ui.craftingBench.pickItem"), Control.PICK, PICK_BUTTON_WIDTH, HEADER_BUTTON_HEIGHT, 0f)
                    .getPosition().inTR(feedstockRight, 0f);
            feedstockRight += PICK_BUTTON_WIDTH + GAP * 2f;
        }
        addCraftingBenchFeedstockCounts(element, feedstockRight);
        headerElement = finishHeader(element, headerElement);
    }

    private void rebuildContent() {
        rebuildTiles();
        rebuildTray();
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
        float buttonTop = openButtonTop();
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
        openElement = replaceElement(openElement, element, detailsLeftX(), buttonTop);
    }

    private float openButtonTop() {
        float plateBottom = artY(BenchScene.CHART_DETAILS_BOTTOM) + PLATE_PAD;
        return Math.max(artY(BenchScene.OPEN_BUTTON_TOP), plateBottom + PLATE_PAD);
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
        if (reshaping()) {
            return;
        }
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
        trays.keepOnly(support().trays());
        refreshSpreadContents();
        if (embeddedLayout != null) {
            rebuildMaterials();
            return;
        }
        float artHeight = BenchScene.ART_HEIGHT * sceneScale;
        TooltipMakerAPI element = panelRoot.createUIElement(spread.artWidth(), artHeight, false);
        tileSpots.clear();
        CraftingMaterials materials = playerMaterials();
        BenchModeSupport support = support();
        List<CraftingMaterialFamily> shownCraftingItems = support.craftingItems();
        List<CraftingMaterialFamily> orbitCraftingItems = shownCraftingItems.stream().filter(support::inOrbit).toList();
        List<CraftingMaterialFamily> columnCraftingItems = shownCraftingItems.stream().filter(craftingItem -> !support.inOrbit(craftingItem)).toList();
        float tileSize = tileSize();
        float socketY = benchMode.socketY() * sceneScale;
        for (int i = 0; i < orbitCraftingItems.size(); i++) {
            float angle = tileAngle(i, orbitCraftingItems.size());
            float socketOffsetX = benchMode.orbitRadiusX() * sceneScale * (float) Math.cos(angle) - tileSize / 2f;
            float tileCentreY = socketY - benchMode.orbitRadiusY() * sceneScale * (float) Math.sin(angle);
            addTile(element, orbitCraftingItems.get(i), placed -> placed.socketX() + socketOffsetX, tileCentreY - tileSize / 2f, tileSize, materials);
        }
        float columnGap = BenchScene.COLUMN_GAP * sceneScale;
        for (int i = 0; i < columnCraftingItems.size(); i++) {
            float tileTop = columnTileTop(i, columnCraftingItems.size(), socketY, tileSize, columnGap, artHeight);
            addTile(element, columnCraftingItems.get(i), BenchSpread::columnLeft, tileTop, tileSize, materials);
        }
        List<BenchTray> offeredTrays = spreadContents.trays() ? support.trays() : List.of();
        for (int i = 0; i < offeredTrays.size(); i++) {
            float tileTop = columnTileTop(i, offeredTrays.size(), socketY, tileSize, columnGap, artHeight);
            addTrayControl(element, offeredTrays.get(i), tileTop, tileSize);
        }
        recipesElement = replaceElement(recipesElement, element, FRAME_INSET, FRAME_INSET);
    }

    private void addTrayControl(TooltipMakerAPI element, BenchTray tray, float tileTop, float tileSize) {
        CraftingBenchTile control = CraftingBenchTile.entry(BenchTrayEntry.shownIcons(tray.iconPaths()), null, this::traysUsable,
                () -> trays.isOpen(tray), tileHandler(tray, anchor -> showTrayControlTooltip(tray, anchor), skipConfirm -> toggleTray(tray),
                        skipConfirm -> {
                        }));
        PositionAPI tile = element.addCustom(Global.getSettings().createCustom(tileSize, tileSize, control), 0f).getPosition();
        tileSpots.add(new TileSpot(tile, null, BenchSpread::controlsLeft, tileTop, tileSize));
        tile.inTL(spread.controlsLeft(), tileTop);
    }

    private void toggleTray(BenchTray tray) {
        hoverTooltip.hide();
        if (wormholeOpened || isConfirmOpen() || isClosing() || !traysUsable()) {
            return;
        }
        float shownWidth = shownTrayWidth();
        trays.toggle(tray);
        if (trays.isOpen()) {
            float wantedWidth = wantedTrayWidth(trays.open());
            trayFromWidth = trayProgress > 0f ? shownWidth : wantedWidth;
            trayToWidth = wantedWidth;
            trayResizeProgress = trayFromWidth == trayToWidth ? 1f : 0f;
            removeTrayElement();
            rebuildTray();
        } else {
            removeTrayElement();
        }
        rebuildNotice();
        refreshOpacity();
    }

    private void closeTray() {
        trays.close();
        removeTrayElement();
    }

    private void removeTrayElement() {
        if (trayElement != null) {
            panelRoot.removeComponent(placedComponent(trayElement));
            trayElement = null;
        }
        removeTrayFilter();
    }

    private void removeTrayFilter() {
        if (trayFilterBar != null) {
            trayFilterText = trayFilterBar.field().getText();
            panelRoot.removeComponent(trayFilterBar.element());
            trayFilterBar = null;
        }
    }

    private void rebuildTray() {
        BenchTray openTray = trays.open();
        if (openTray == null || trayProgress < 1f || trayResizeProgress < 1f || embeddedLayout != null || isClosing()) {
            removeTrayElement();
            return;
        }
        trayBuiltWithLargeText = TooltipTextSize.large();
        if (openTray.iconColumns() > 0) {
            removeTrayFilter();
            TooltipMakerAPI element = BenchTrayList.tiles(panelRoot, trayListWidth(), trayHeight() - PAD * 2f, openTray, trayContext,
                    this::trayEntryHandler, tileSize());
            trayElement = replaceElement(placedComponent(trayElement), element, trayLeft(), trayTop());
            return;
        }
        rebuildTrayFilter();
        rebuildTrayList(openTray, true);
    }

    private float trayPlateTop() {
        return PAD + HEADER_HEIGHT + GAP;
    }

    private float trayHeight() {
        return artY(BenchScene.TRAY_BOTTOM) - trayPlateTop();
    }

    private float trayLeft() {
        return FRAME_INSET + spread.trayLeft() + TRAY_SIDE_INSET;
    }

    private float trayTop() {
        return trayPlateTop() + PAD;
    }

    private float trayListWidth() {
        return spread.trayWidth() - TRAY_SIDE_INSET * 2f;
    }

    private void rebuildTrayFilter() {
        boolean focused = false;
        if (trayFilterBar != null) {
            focused = trayFilterBar.field().hasFocus();
            removeTrayFilter();
        }
        typedTrayFilter = trayFilterText;
        trayFilterDelay = 0f;
        trayFilterBar = BenchTrayList.filterBar(panelRoot, trayListWidth(), trayFilterText);
        panelRoot.addUIElement(trayFilterBar.element()).inTL(trayLeft(), trayTop());
        if (focused) {
            trayFilterBar.field().grabFocus();
        }
    }

    private void rebuildTrayList(BenchTray openTray, boolean keepScroll) {
        float restoredScroll = keepScroll && trayElement != null && trayElement.getExternalScroller() != null
                ? trayElement.getExternalScroller().getYOffset() : 0f;
        float listHeight = trayHeight() - PAD * 2f - BenchTrayList.FILTER_HEIGHT - GAP;
        TooltipMakerAPI element = BenchTrayList.rows(panelRoot, trayListWidth(), listHeight, openTray, trayContext,
                this::trayEntryHandler, trayFilterText);
        trayElement = replaceElement(placedComponent(trayElement), element, trayLeft(), trayTop() + BenchTrayList.FILTER_HEIGHT + GAP);
        if (restoredScroll > 0f && element.getExternalScroller() != null) {
            element.getExternalScroller().setYOffset(Math.min(restoredScroll, Math.max(0f, element.getHeightSoFar() - listHeight)));
        }
    }

    private void advanceTrayFilter(float amount) {
        if (trayFilterBar == null || isConfirmOpen()) {
            return;
        }
        String typed = trayFilterBar.field().getText();
        if (!typed.equals(typedTrayFilter)) {
            typedTrayFilter = typed;
            trayFilterDelay = TRAY_FILTER_DELAY_SECONDS;
        } else if (trayFilterDelay > 0f) {
            trayFilterDelay -= amount;
            BenchTray openTray = trays.open();
            if (trayFilterDelay <= 0f && openTray != null) {
                trayFilterText = typed;
                hoverTooltip.hide();
                I18n.forGameText(() -> rebuildTrayList(openTray, false));
            }
        }
    }

    private TileInput.Handler trayEntryHandler(BenchTrayEntry entry) {
        return tileHandler(entry, anchor -> showTrayEntryTooltip(entry, anchor), skipConfirm -> useTrayEntry(entry, skipConfirm, false),
                skipConfirm -> useTrayEntry(entry, skipConfirm, true));
    }

    private void useTrayEntry(BenchTrayEntry entry, boolean skipConfirm, boolean alternate) {
        hoverTooltip.hide();
        if (trayContext.busy()) {
            return;
        }
        if (alternate) {
            entry.useAlternate(trayContext, skipConfirm);
        } else {
            entry.use(trayContext, skipConfirm);
        }
    }

    private void showTrayEntryTooltip(BenchTrayEntry entry, PositionAPI anchor) {
        if (isClosing()) {
            hoverTooltip.hide();
            return;
        }
        hoverTooltip.show(entry, itemTooltipWidth(), (element, expanded) -> entry.writeTooltip(element, trayContext), anchor,
                ItemHoverTooltip.Placement.BELOW, PAD);
    }

    private void showTrayControlTooltip(BenchTray tray, PositionAPI anchor) {
        if (isClosing()) {
            hoverTooltip.hide();
            return;
        }
        hoverTooltip.show(tray, TOOLTIP_WIDTH, (element, expanded) -> {
            TooltipTextSize.applyNameFont(element);
            VanillaText.plainPara(element, Translation.text(tray.titleKey()), 0f, Misc.getBasePlayerColor());
            TooltipTextSize.applyBodyFont(element);
            VanillaText.plainPara(element, Translation.text(trays.isOpen(tray) ? "ui.craftingBench.tray.close" : "ui.craftingBench.tray.open"), GAP,
                    Misc.getGrayColor());
        }, anchor, ItemHoverTooltip.Placement.BELOW, PAD);
    }

    private void placeTiles() {
        for (TileSpot spot : tileSpots) {
            float tileLeft = spot.left().in(spread);
            spot.tile().inTL(tileLeft, spot.top());
            if (spot.count() != null) {
                placeCount(spot.count(), tileLeft, spot.top(), spot.size());
            }
        }
    }

    private void rebuildMaterials() {
        if (isClosing()) {
            return;
        }
        CraftingMaterials materials = playerMaterials();
        List<SectionedTileGrid.Tile> craftingItemTiles = new ArrayList<>();
        for (CraftingMaterialFamily craftingItem : support().craftingItems()) {
            craftingItemTiles.add(new SectionedTileGrid.Tile(craftingItemTile(craftingItem, materials), materials.count(craftingItem.itemId())));
        }
        List<SectionedTileGrid.Section> sections = new ArrayList<>();
        sections.add(new SectionedTileGrid.Section(Translation.text("ui.craftingBench.materials.craftingItems"), Misc.getBasePlayerColor(), craftingItemTiles));
        if (materialsPanel == null) {
            materialsPanel = BenchMaterialsPanel.open(hostPanel, embeddedLayout.materials(), sections, () -> queue(hoverTooltip::hide));
        } else {
            materialsPanel.show(sections);
        }
    }

    private TileInput.Handler craftingItemHandler(CraftingMaterialFamily craftingItem) {
        return tileHandler(craftingItem, anchor -> showTooltip(craftingItem, anchor), skipConfirm -> tileClicked(craftingItem, skipConfirm),
                skipConfirm -> tileRightClicked(craftingItem, skipConfirm));
    }

    private void addSpot(TooltipMakerAPI element, CustomPanelAPI tilePanel, String itemId, TileLeft tileLeft, float tileTop, float tileSize,
                         CraftingMaterials materials) {
        PositionAPI tile = element.addCustom(tilePanel, 0f).getPosition();
        PositionAPI count = addCount(element, itemId, tileSize, materials);
        tileSpots.add(new TileSpot(tile, count, tileLeft, tileTop, tileSize));
        float left = tileLeft.in(spread);
        tile.inTL(left, tileTop);
        placeCount(count, left, tileTop, tileSize);
    }

    private CraftingBenchTile craftingItemTile(CraftingMaterialFamily craftingItem, CraftingMaterials materials) {
        boolean owned = materials.count(craftingItem.itemId()) > 0;
        return CraftingBenchTile.usable(recipeIcons(craftingItem), recipeLook(craftingItem), () -> owned, craftingItemHandler(craftingItem));
    }

    private void toggleInhibitor(CraftingInhibitor inhibitor) {
        hoverTooltip.hide();
        if (wormholeOpened || isConfirmOpen()) {
            return;
        }
        String inhibitorName = MaterialItems.displayName(inhibitor.itemId());
        String noticeKey = switch (activeInhibitors.toggle(inhibitor, playerMaterials().count(inhibitor.itemId()))) {
            case OFF -> "ui.craftingBench.notice.inhibitorOff";
            case NONE_OWNED -> inhibitor.growable() ? "ui.craftingBench.notice.noneOwned" : "ui.craftingBench.notice.noneFound";
            case FULL -> "ui.craftingBench.notice.inhibitorsFull";
            case ON -> inhibitorOnKey(inhibitor);
        };
        String inhibitedTagName = inhibitor.inhibitsTag() ? ThemeTags.displayName(inhibitor.inhibitedTag()).toLowerCase(Locale.ROOT) : "";
        showNotice(Translation.msg(noticeKey).arg("name", inhibitorName).arg("count", activeInhibitors.maxActive()).arg("tag", inhibitedTagName).text());
    }

    static String inhibitorOnKey(CraftingInhibitor inhibitor) {
        return "ui.craftingBench.notice.inhibitorOn." + (inhibitor.inhibitsTag() ? "tag" : inhibitor.name().toLowerCase(Locale.ROOT));
    }

    private static float itemTooltipWidth() {
        return TooltipTextSize.large() ? LARGE_TEXT_TOOLTIP_WIDTH : TOOLTIP_WIDTH;
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

    private static PositionAPI addCount(TooltipMakerAPI element, String itemId, float tileSize, CraftingMaterials materials) {
        int ownedCount = materials.count(itemId);
        Color countColor = ownedCount > 0 ? Misc.getBrightPlayerColor() : Misc.getGrayColor();
        element.setParaFont(Fonts.ORBITRON_12);
        LabelAPI countLabel = VanillaText.plainPara(element, String.valueOf(ownedCount), 0f, countColor);
        countLabel.setAlignment(Alignment.RMID);
        PositionAPI count = countLabel.autoSizeToWidth(tileSize - COUNT_INSET * 2f);
        element.setParaFontDefault();
        return count;
    }

    private static void placeCount(PositionAPI count, float tileLeft, float tileTop, float tileSize) {
        count.inTL(tileLeft + COUNT_INSET, tileTop + tileSize - count.getHeight() - COUNT_INSET);
    }

    private void addTile(TooltipMakerAPI element, CraftingMaterialFamily craftingItem, TileLeft tileLeft, float tileTop, float tileSize,
                         CraftingMaterials materials) {
        CustomPanelAPI tilePanel = Global.getSettings().createCustom(tileSize, tileSize, craftingItemTile(craftingItem, materials));
        addSpot(element, tilePanel, craftingItem.itemId(), tileLeft, tileTop, tileSize, materials);
    }

    private void showTooltip(CraftingMaterialFamily craftingItem, PositionAPI anchor) {
        if (isClosing()) {
            hoverTooltip.hide();
            return;
        }
        hoverTooltip.show(craftingItem, itemTooltipWidth(), (element, expanded) -> writeTooltip(element, craftingItem), anchor,
                ItemHoverTooltip.Placement.BELOW, PAD);
    }

    private void writeTooltip(TooltipMakerAPI element, CraftingMaterialFamily craftingItem) {
        TooltipTextSize.applyNameFont(element);
        VanillaText.plainPara(element, recipeName(craftingItem), 0f, Misc.getBrightPlayerColor());
        TooltipTextSize.applyBodyFont(element);
        VanillaText.addPara(element, support().craftingItemDescription(craftingItem), GAP, Misc.getTextColor());
        float linePad = GAP;
        for (StyledText line : support().craftingItemLines(craftingItem)) {
            VanillaText.addPara(element, line, linePad, Misc.getTextColor());
            linePad = LINE_PAD;
        }
        for (String line : tooltipHints(craftingItem)) {
            VanillaText.plainPara(element, line, GAP, Misc.getGrayColor());
        }
    }

    private List<String> tooltipHints(CraftingMaterialFamily craftingItem) {
        List<String> hints = new ArrayList<>();
        int ownedCount = playerMaterials().count(craftingItem.itemId());
        hints.add(Translation.msg("ui.craftingBench.tile.inStorage").arg("count", ownedCount).text());
        if (ownedCount == 0) {
            if (craftingItem.growable()) {
                hints.add(Translation.text("ui.craftingBench.tile.noneOwned"));
            }
        } else {
            BenchModeSupport.Refusal refusal = support().refusal(craftingItem, loadedItem, support().store(), playerMaterials(), allowedUniques(),
                    appliedInhibitors());
            hints.add(refusal.allowed()
                    ? Translation.msg(support().useHintKey()).arg("target", loadedItem.name()).text() : refusalText(craftingItem, refusal.verdict()));
        }
        hints.add(growHint(craftingItem));
        return hints;
    }

    static String growHint(GrowableMaterial material) {
        return material.growable()
                ? Translation.msg("ui.craftingBench.tile.grow").arg("cost", material.feedstockCost()).text()
                : Translation.text("ui.craftingBench.tile.notGrowable");
    }

    private static List<String> recipeIcons(CraftingMaterialFamily craftingItem) {
        String iconPath = MaterialItems.loadedIconPath(craftingItem.itemId());
        return List.of(iconPath == null ? FALLBACK_ICON : iconPath);
    }

    private static CargoIconLook recipeLook(CraftingMaterialFamily craftingItem) {
        return CargoIconLooks.forMaterial(craftingItem.itemId());
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
        TooltipTextSize.applyBodyFont(element);
        if (loadedItem == null) {
            centred(VanillaText.plainPara(element, Translation.text(support().emptyKey()), 0f, Misc.getGrayColor()));
        } else {
            ItemName displayName = loadedItem.displayName();
            TooltipTextSize.applyNameFont(element);
            centred(VanillaText.plainPara(element, displayName.title(), 0f, displayName.rarity().color()));
            TooltipTextSize.applyBodyFont(element);
            if (displayName.baseName() != null) {
                centred(VanillaText.plainPara(element, displayName.baseName(), LINE_PAD, Misc.getTextColor()));
            }
            StyledText flavour = support().flavourLine(loadedItem);
            if (flavour != null) {
                centred(VanillaText.addPara(element, flavour, LINE_PAD, Misc.getGrayColor()));
            }
            for (StyledText line : support().headerLines(loadedItem)) {
                centred(VanillaText.addPara(element, line, LINE_PAD, Misc.getTextColor()));
            }
            float paraPad = GAP * 2f;
            float linePad = paraPad;
            for (StyledText line : support().detailLines(loadedItem)) {
                centred(VanillaText.addPara(element, line, linePad, Misc.getTextColor()));
                linePad = LINE_PAD;
            }
            List<SlottedLine> implicitLines = new ArrayList<>();
            loadedItem.implicitLines(detailsShowRollRanges).forEach(line -> implicitLines.add(new SlottedLine(null, line)));
            addDetailSection(element, "item.tooltip.implicit", implicitLines, paraPad);
            if (detailsShowRollRanges) {
                addSlottedDetailSection(element, "item.tooltip.explicit", loadedItem.slottedEffectLines(true, null), paraPad,
                        detailsWidth);
            } else {
                addDetailSection(element, "item.tooltip.explicit", loadedItem.slottedEffectLines(false, null), paraPad);
            }
        }
        float detailsLeft = embeddedLayout != null ? embeddedLayout.details().left() : detailsLeftX();
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
        element.addTooltipToPrevious(new GameTextTooltip(itemTooltipWidth(), tooltip -> {
            TooltipTextSize.applyTitleFont(tooltip);
            TooltipTextSize.applyBodyFont(tooltip);
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
        SlottedRows.Layout layout = new SlottedRows.Layout(sectionWidth - SCROLLBAR_ROOM, SLOT_TAG_WIDTH, GAP, TooltipTextSize::applyBodyFont);
        SlottedRows.add(element, lines, layout, GAP, LINE_PAD, line -> addLineDetailTooltip(element, line));
    }

    private static void centred(LabelAPI label) {
        if (label != null) {
            label.setAlignment(Alignment.MID);
        }
    }

    private void rebuildNotice() {
        BenchLayout.Rect notice = embeddedLayout != null ? embeddedLayout.notice() : benchNotice();
        TooltipMakerAPI element = panelRoot.createUIElement(notice.width(), notice.height(), false);
        if (noticeText != null) {
            Color noticeColor = noticeIsWarning ? Misc.getNegativeHighlightColor() : Misc.getPositiveHighlightColor();
            centred(VanillaText.plainPara(element, noticeText, 0f, noticeColor));
        }
        noticeElement = replaceElement(noticeElement, element, notice.left(), notice.top());
    }

    private BenchLayout.Rect benchNotice() {
        float noticeLeft = PAD;
        float noticeRight = PAD + innerWidth();
        if (trays.isOpen() || trayProgress > 0f) {
            noticeRight = FRAME_INSET + BenchSpread.trayOpen(benchMode, sceneScale, spreadContents, trayToWidth).trayLeft() - GAP;
        }
        return new BenchLayout.Rect(noticeLeft, artY(BenchScene.NOTICE_TOP), noticeRight - noticeLeft, BenchScene.NOTICE_HEIGHT * sceneScale);
    }

    private final class TrayContext implements BenchTrayContext {

        @Override
        public Item loaded() {
            return loadedItem;
        }

        @Override
        public ItemStore store() {
            return support().store();
        }

        @Override
        public CraftingMaterials materials() {
            return playerMaterials();
        }

        @Override
        public Set<CraftingInhibitor> activeInhibitors() {
            return activeInhibitors.view();
        }

        @Override
        public Random random() {
            return craftingRandom;
        }

        @Override
        public boolean busy() {
            return wormholeOpened || isConfirmOpen() || isClosing() || reshaping();
        }

        @Override
        public void useCraftingItem(CraftingMaterialFamily craftingItem, boolean skipConfirm) {
            tileClicked(craftingItem, skipConfirm);
        }

        @Override
        public void growMaterial(GrowableMaterial material, boolean skipConfirm) {
            tileRightClicked(material, skipConfirm);
        }

        @Override
        public void toggleInhibitor(CraftingInhibitor inhibitor) {
            CraftingBenchPanel.this.toggleInhibitor(inhibitor);
        }

        @Override
        public void writeCraftingItemTooltip(TooltipMakerAPI tooltip, CraftingMaterialFamily craftingItem) {
            writeTooltip(tooltip, craftingItem);
        }

        @Override
        public boolean droppedMissingInhibitors() {
            return CraftingBenchPanel.this.droppedMissingInhibitors(playerMaterials());
        }

        @Override
        public void confirmCraft(String title, StyledText body, Set<CraftingInhibitor> spentInhibitors, Runnable confirmed) {
            pendingTrayConfirm = confirmed;
            openConfirm(USE_CONFIRM_HEIGHT + spentInhibitors.size() * INHIBITOR_LINE_HEIGHT,
                    element -> writeCraftConfirm(element, title, body, spentInhibitors),
                    "ui.craftingBench.confirm.use", Control.CONFIRM_TRAY, "ui.craftingBench.confirm.cancel", Control.CANCEL_CONFIRM);
        }

        @Override
        public void crafted(Item selectedAfter, Color flash, String notice, Set<CraftingInhibitor> spentInhibitors) {
            activeInhibitors.spend(spentInhibitors);
            loadedItem = selectedAfter;
            noticeText = withSpentInhibitors(notice, spentInhibitors);
            SkillTreeSounds.socketed();
            socketFlair.flash(flash == null ? SkillTreePanelStyle.GLOW_COLOR : flash);
            afterChange();
        }

        @Override
        public void notice(String text) {
            showNotice(text);
        }
    }
}
