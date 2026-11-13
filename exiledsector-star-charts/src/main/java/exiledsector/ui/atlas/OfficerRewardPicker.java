package exiledsector.ui.atlas;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.OfficerDefeat;
import exiledsector.charts.officer.OfficerText;
import exiledsector.charts.officer.reward.CarriedItem;
import exiledsector.charts.officer.reward.OfficerRewardPool;
import exiledsector.charts.officer.reward.OfficerRewardText;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.items.ItemTooltip;
import exiledsector.items.MaterialItemPlugin;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.CampaignOverlay;
import exiledsector.ui.ChromeMetrics;
import exiledsector.ui.EscapeKey;
import exiledsector.ui.OverlayFade;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.SkillTreeSounds;
import exiledsector.ui.UiFonts;
import exiledsector.ui.socket.ItemHoverTooltip;
import exiledsector.ui.util.BorderedPanel;
import exiledsector.ui.util.GLDraw;
import exiledsector.ui.util.SpriteDraw;
import exiledsector.ui.util.TextLabel;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

public final class OfficerRewardPicker extends BaseCustomUIPanelPlugin implements CampaignOverlay.Screen {

    private static final float PANEL_MARGIN = AtlasScreen.PANEL_MARGIN;
    private static final float CHROME_PAD = AtlasScreen.CHROME_PAD;
    private static final float OPEN_SECONDS = AtlasScreen.OPEN_SECONDS;
    private static final float BACKDROP_ALPHA = AtlasScreen.BACKDROP_ALPHA;
    private static final float LATER_WIDTH = 140f;
    private static final Object LATER_BUTTON = new Object();
    private static final float CARD_WIDTH = 200f;
    private static final float CARD_HEIGHT = 250f;
    private static final float CARD_GAP = 40f;
    private static final float CARD_PAD = 14f;
    private static final float ICON_SIZE = 128f;
    private static final float NAME_MAX_HEIGHT = 80f;
    private static final float TITLE_GAP = 8f;
    private static final float IDLE_BORDER_ALPHA = 0.55f;
    private static final float HOVER_FILL_ALPHA = 0.12f;
    private static final float BORDER_WIDTH = 1.5f;
    private static final float HOVER_BORDER_WIDTH = 3f;
    private static final Color CARD_FILL = new Color(10, 14, 20);
    private static final SpriteCache ICONS = new SpriteCache(OfficerRewardPicker.class);

    private record Card(CarriedItem offer, RewardFace face, TextLabel nameLabel) {
    }

    private final EscapeKey escapeKey = new EscapeKey();
    private final CampaignOverlay.Host host;
    private final String officerName;
    private final Consumer<CarriedItem> onPick;
    private final Function<CampaignOverlay.Host, CampaignOverlay.Screen> returnTo;
    private final List<Card> cards = new ArrayList<>();
    private final Random random = new Random();
    private final OverlayFade fade = new OverlayFade(OPEN_SECONDS);
    private final TextLabel titleLabel = new TextLabel(UiFonts.TITLE, SkillTreePanelStyle.TOOLTIP_TITLE_COLOR);
    private final TextLabel hintLabel = new TextLabel(UiFonts.HINT, AtlasScreen.HINT_COLOUR);
    private ItemHoverTooltip hoverTooltip;
    private final List<CustomPanelAPI> cardAnchors = new ArrayList<>();
    private PositionAPI panelPosition;
    private Card hoveredCard;
    private Card tooltipCard;
    private boolean picked;
    private float mouseX;
    private float mouseY;
    private boolean mouseKnown;

    private OfficerRewardPicker(CampaignOverlay.Host host, String officerName, List<CarriedItem> offers, Consumer<CarriedItem> onPick,
                                Function<CampaignOverlay.Host, CampaignOverlay.Screen> returnTo) {
        this.host = host;
        this.officerName = officerName;
        this.onPick = onPick;
        this.returnTo = returnTo;
        for (CarriedItem offer : offers) {
            cards.add(card(offer));
        }
    }

    static boolean open(SectorAPI sector, String officerName, List<CarriedItem> offers, Consumer<CarriedItem> onPick) {
        if (offers.isEmpty()) {
            return false;
        }
        boolean wasPaused = sector.isPaused();
        boolean opened = CampaignOverlay.open(sector.getCampaignUI(), host -> new OfficerRewardPicker(host, officerName, offers, onPick, null),
                () -> sector.setPaused(wasPaused));
        if (opened) {
            sector.setPaused(true);
            SkillTreeSounds.panelOpened();
        }
        return opened;
    }

    public static boolean openUnchosen(SectorAPI sector) {
        AtlasOfficer holder = OfficerDefeat.nextUnchosen(AtlasState.getIfCreated(sector));
        return holder != null && open(sector, OfficerText.title(holder), holder.pendingOffers(), answering(holder));
    }

    static Function<CampaignOverlay.Host, CampaignOverlay.Screen> unchosenScreen(AtlasOfficer holder,
                                                                               Function<CampaignOverlay.Host, CampaignOverlay.Screen> returnTo) {
        String officerName = OfficerText.title(holder);
        List<CarriedItem> offers = holder.pendingOffers();
        Consumer<CarriedItem> onPick = answering(holder);
        return host -> new OfficerRewardPicker(host, officerName, offers, onPick, returnTo);
    }

    private static Consumer<CarriedItem> answering(AtlasOfficer holder) {
        List<String> choice = holder.pendingChoice();
        return pick -> OfficerDefeat.answer(choice, pick);
    }

    public static void openWhenClear(SectorAPI sector, String officerName, List<CarriedItem> offers, Consumer<CarriedItem> onPick) {
        openWhenClear(sector, officerName, offers, () -> true, onPick);
    }

    public static void openWhenClear(SectorAPI sector, String officerName, List<CarriedItem> offers, BooleanSupplier stillOffered,
                                     Consumer<CarriedItem> onPick) {
        if (!offers.isEmpty()) {
            sector.addTransientScript(new WhenClear(officerName, List.copyOf(offers), stillOffered, onPick));
        }
    }

    private static Card card(CarriedItem offer) {
        TextLabel nameLabel = new TextLabel(UiFonts.BODY, SkillTreePanelStyle.TOOLTIP_TITLE_COLOR);
        I18n.forGameText(() -> nameLabel.setWrapped(OfficerRewardText.name(offer), CARD_WIDTH - CARD_PAD * 2f, NAME_MAX_HEIGHT));
        return new Card(offer, RewardFace.of(offer), nameLabel);
    }

    @Override
    public void attach(CustomPanelAPI panel) {
        hoverTooltip = new ItemHoverTooltip(panel);
        I18n.forGameText(() -> {
            titleLabel.set(officerName == null ? Translation.text("chart.officer.reward.title")
                    : Translation.msg("chart.officer.reward.titleNamed").arg("officer", officerName).text());
            hintLabel.set(Translation.text("chart.officer.reward.hint"));
        });
        PositionAPI position = panel.getPosition();
        TooltipMakerAPI laterElement = panel.createUIElement(LATER_WIDTH, ChromeMetrics.CONTROL_HEIGHT, false);
        I18n.forGameText(() -> laterElement.addButton(Translation.text("chart.officer.reward.later"), LATER_BUTTON, LATER_WIDTH,
                ChromeMetrics.CONTROL_HEIGHT, 0f));
        panel.addUIElement(laterElement).inTL(position.getWidth() - PANEL_MARGIN - CHROME_PAD - LATER_WIDTH, PANEL_MARGIN + CHROME_PAD);
        float rowWidth = rowWidth();
        float firstLeft = (position.getWidth() - rowWidth) / 2f;
        float top = (position.getHeight() - CARD_HEIGHT) / 2f;
        for (int index = 0; index < cards.size(); index++) {
            CustomPanelAPI anchor = panel.createCustomPanel(CARD_WIDTH, CARD_HEIGHT, null);
            panel.addComponent(anchor).inTL(firstLeft + index * (CARD_WIDTH + CARD_GAP), top);
            cardAnchors.add(anchor);
        }
    }

    private float rowWidth() {
        return cards.size() * CARD_WIDTH + Math.max(0, cards.size() - 1) * CARD_GAP;
    }

    @Override
    public void positionChanged(PositionAPI position) {
        panelPosition = position;
    }

    @Override
    public void buttonPressed(Object buttonId) {
        if (buttonId == LATER_BUTTON) {
            decideLater();
        }
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        if (panelPosition == null) {
            return;
        }
        for (InputEventAPI event : events) {
            if (event.isConsumed()) {
                continue;
            }
            if (event.isMouseMoveEvent()) {
                mouseX = event.getX();
                mouseY = event.getY();
                mouseKnown = true;
            } else if (event.isLMBUpEvent() && !fade.closing() && !picked) {
                Card clicked = cardAt(event.getX(), event.getY());
                if (clicked != null) {
                    pick(clicked);
                }
            } else if (escapeKey.released(event)) {
                decideLater();
            }
            event.consume();
        }
    }

    private void decideLater() {
        if (!fade.closing() && !picked) {
            fade.close();
        }
    }

    private void pick(Card card) {
        picked = true;
        SkillTreeSounds.socketed();
        onPick.accept(OfficerRewardPool.picked(card.offer(), random));
        fade.close();
    }

    private Card cardAt(float x, float y) {
        for (int index = 0; index < cards.size() && index < cardAnchors.size(); index++) {
            PositionAPI cardPosition = cardAnchors.get(index).getPosition();
            if (x >= cardPosition.getX() && x <= cardPosition.getX() + cardPosition.getWidth() && y >= cardPosition.getY()
                    && y <= cardPosition.getY() + cardPosition.getHeight()) {
                return cards.get(index);
            }
        }
        return null;
    }

    @Override
    public void advance(float amount) {
        if (panelPosition == null) {
            return;
        }
        if (fade.advance(amount)) {
            hoverTooltip.hide();
            if (returnTo != null) {
                host.switchTo(returnTo);
            } else {
                host.close();
            }
            return;
        }
        hoveredCard = mouseKnown && !fade.closing() ? cardAt(mouseX, mouseY) : null;
        if (hoveredCard != tooltipCard) {
            tooltipCard = hoveredCard;
            showTooltip(hoveredCard);
        } else {
            hoverTooltip.refreshIfExpansionChanged();
        }
    }

    private void showTooltip(Card card) {
        if (card == null) {
            hoverTooltip.hide();
            return;
        }
        PositionAPI anchor = cardAnchors.get(cards.indexOf(card)).getPosition();
        if (card.face().preview() != null) {
            hoverTooltip.show(card, card.face().preview(), expanded -> List.of(), anchor);
        } else if (card.offer().kind().craftingItem()) {
            hoverTooltip.show(card, ItemTooltip.width(), (tooltip, expanded) -> MaterialItemPlugin.writeTooltip(tooltip, card.offer().id()), anchor);
        } else {
            hoverTooltip.hide();
        }
    }

    @Override
    public void renderBelow(float alphaMult) {
        if (panelPosition == null) {
            return;
        }
        float alpha = fade.smoothLevel() * alphaMult;
        if (alpha <= 0f) {
            return;
        }
        GLDraw.fillQuad(panelPosition.getX(), panelPosition.getY(), panelPosition.getWidth(), panelPosition.getHeight(), Color.BLACK,
                BACKDROP_ALPHA * alpha);
        float innerLeft = panelPosition.getX() + PANEL_MARGIN;
        float innerBottom = panelPosition.getY() + PANEL_MARGIN;
        float innerWidth = panelPosition.getWidth() - PANEL_MARGIN * 2f;
        float innerHeight = panelPosition.getHeight() - PANEL_MARGIN * 2f;
        GLDraw.fillQuad(innerLeft, innerBottom, innerWidth, innerHeight, Color.BLACK, alpha);
        BorderedPanel.draw(innerLeft, innerBottom, innerWidth, innerHeight, alpha);
        float top = innerBottom + innerHeight - CHROME_PAD;
        titleLabel.setAlpha(alpha).draw(innerLeft + CHROME_PAD, top);
        hintLabel.setAlpha(alpha).draw(innerLeft + CHROME_PAD, top - titleLabel.height() - TITLE_GAP);
        for (int index = 0; index < cards.size() && index < cardAnchors.size(); index++) {
            drawCard(cards.get(index), cardAnchors.get(index).getPosition(), alpha);
        }
    }

    private void drawCard(Card card, PositionAPI cardPosition, float alpha) {
        float left = cardPosition.getX();
        float bottom = cardPosition.getY();
        boolean hovered = card == hoveredCard;
        GLDraw.fillQuad(left, bottom, CARD_WIDTH, CARD_HEIGHT, CARD_FILL, alpha);
        if (hovered) {
            GLDraw.fillQuad(left, bottom, CARD_WIDTH, CARD_HEIGHT, card.face().frameColour(), HOVER_FILL_ALPHA * alpha);
        }
        GLDraw.strokeQuad(left, bottom, CARD_WIDTH, CARD_HEIGHT, card.face().frameColour(), hovered ? HOVER_BORDER_WIDTH : BORDER_WIDTH,
                (hovered ? 1f : IDLE_BORDER_ALPHA) * alpha);
        float iconCentreY = bottom + CARD_HEIGHT - CARD_PAD - ICON_SIZE / 2f;
        if (card.face().iconPath() != null) {
            SpriteDraw.drawAtCenter(ICONS, card.face().iconPath(), left + CARD_WIDTH / 2f, iconCentreY, ICON_SIZE, ICON_SIZE, Color.WHITE, alpha);
        }
        card.nameLabel().setAlpha(alpha).draw(left + CARD_PAD, iconCentreY - ICON_SIZE / 2f - CARD_PAD);
    }

    static final class WhenClear implements EveryFrameScript {

        private static final float CHECK_SECONDS = 0.5f;

        private final String officerName;
        private final List<CarriedItem> offers;
        private final BooleanSupplier stillOffered;
        private final Consumer<CarriedItem> onPick;
        private float sinceCheck;
        private boolean done;

        WhenClear(String officerName, List<CarriedItem> offers, BooleanSupplier stillOffered, Consumer<CarriedItem> onPick) {
            this.officerName = officerName;
            this.offers = offers;
            this.stillOffered = stillOffered;
            this.onPick = onPick;
        }

        @Override
        public boolean isDone() {
            return done;
        }

        @Override
        public boolean runWhilePaused() {
            return true;
        }

        @Override
        public void advance(float amount) {
            sinceCheck += amount;
            if (done || sinceCheck < CHECK_SECONDS) {
                return;
            }
            sinceCheck = 0f;
            SectorAPI sector = Global.getSector();
            if (sector == null || !campaignClear(sector)) {
                return;
            }
            if (!stillOffered.getAsBoolean() || open(sector, officerName, offers, onPick)) {
                done = true;
            }
        }

        private static boolean campaignClear(SectorAPI sector) {
            CampaignUIAPI campaignUi = sector.getCampaignUI();
            return Global.getCurrentState() == GameState.CAMPAIGN && campaignUi != null && sector.getPlayerFleet() != null
                    && !sector.isInFastAdvance() && !sector.isInNewGameAdvance() && !campaignUi.isShowingDialog() && !campaignUi.isShowingMenu()
                    && campaignUi.getCurrentCoreTab() == null;
        }
    }
}
