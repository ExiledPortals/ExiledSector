package exiledsector.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.ScrollPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import exiledsector.ui.refit.RefitFleetList;
import exiledsector.ui.util.BorderedPanel;
import exiledsector.ui.util.Rects;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

final class FleetShipList extends BaseCustomUIPanelPlugin {

    static final float ROW_GAP = 6f;
    static final float CARD_GAP = 12f;
    static final float SCROLLBAR_ROOM = 14f;
    private static final Color CARD_BACKGROUND = new Color(0, 0, 0, 0);
    static final float LOCKED_OPACITY = 0.35f;

    record Layout(float innerHeight, boolean scrolls, float scrollOffset) {
    }

    private final CustomPanelAPI hostPanel;
    private final float cardHeight;
    private final Consumer<FleetMemberAPI> picked;
    private CustomPanelAPI listPanel;
    private PositionAPI listPosition;

    FleetShipList(CustomPanelAPI hostPanel, float cardHeight, Consumer<FleetMemberAPI> picked) {
        this.hostPanel = hostPanel;
        this.cardHeight = cardHeight;
        this.picked = picked;
    }

    static Layout layout(int shipCount, int selectedIndex, float cardHeight, float maxInnerHeight) {
        float rowsHeight = shipCount * cardHeight + Math.max(0, shipCount - 1) * ROW_GAP;
        boolean scrolls = rowsHeight > maxInnerHeight;
        float innerHeight = Math.min(rowsHeight, maxInnerHeight);
        if (!scrolls || selectedIndex < 0) {
            return new Layout(innerHeight, scrolls, 0f);
        }
        float selectedCentre = selectedIndex * (cardHeight + ROW_GAP) + cardHeight / 2f;
        float scrollOffset = Math.max(0f, Math.min(selectedCentre - innerHeight / 2f, rowsHeight - innerHeight));
        return new Layout(innerHeight, true, scrollOffset);
    }

    boolean isOpen() {
        return listPanel != null;
    }

    boolean contains(float x, float y) {
        return listPosition != null && Rects.contains(listPosition, x, y);
    }

    boolean open(float left, float bottom, float maxHeight) {
        RefitFleetList.Roster roster = RefitFleetList.read();
        List<FleetMemberAPI> members = roster.members();
        if (isOpen() || members.isEmpty()) {
            return false;
        }
        float outset = SkillTreeChrome.SHIP_CARD_FRAME_OUTSET;
        float cardWidth = SkillTreeRefitButton.SHIP_CARD_ICON_SIZE;
        Layout layout = layout(members.size(), roster.selectedIndex(), cardHeight, Math.max(cardHeight, maxHeight - outset * 2f));
        float elementWidth = cardWidth + (layout.scrolls() ? SCROLLBAR_ROOM : 0f);
        listPanel = Global.getSettings().createCustom(elementWidth + outset * 2f, layout.innerHeight() + outset * 2f, this);
        hostPanel.addComponent(listPanel).inBL(left, bottom);

        TooltipMakerAPI element = listPanel.createUIElement(elementWidth, layout.innerHeight(), layout.scrolls());
        List<ShipCardClickTarget> clickTargets = new ArrayList<>();
        for (int index = 0; index < members.size(); index++) {
            FleetMemberAPI member = members.get(index);
            boolean selected = index == roster.selectedIndex();
            boolean alreadyShown = index == roster.shownIndex();
            boolean treeOpen = SkillTreeRefitButton.treeOpen(member, member.getVariant());
            ShipCardClickTarget clickTarget = new ShipCardClickTarget(() -> pick(member, alreadyShown), () -> treeOpen, selected);
            clickTargets.add(clickTarget);
            CustomPanelAPI rowPanel = row(member, cardWidth, clickTarget);
            if (!treeOpen) {
                rowPanel.setOpacity(LOCKED_OPACITY);
            }
            element.addCustom(rowPanel, index == 0 ? 0f : ROW_GAP);
        }
        listPanel.addUIElement(element).inTL(outset, outset);
        ScrollPanelAPI scroller = element.getExternalScroller();
        if (scroller != null) {
            scroller.setYOffset(layout.scrollOffset());
        }
        PositionAPI visibleArea = scroller != null ? scroller.getPosition() : element.getPosition();
        for (ShipCardClickTarget clickTarget : clickTargets) {
            clickTarget.clipTo(visibleArea);
        }
        SkillTreeSounds.panelOpened();
        return true;
    }

    private CustomPanelAPI row(FleetMemberAPI member, float cardWidth, ShipCardClickTarget clickTarget) {
        CustomPanelAPI rowPanel = Global.getSettings().createCustom(cardWidth, cardHeight, null);
        TooltipMakerAPI card = rowPanel.createUIElement(cardWidth, cardHeight, false);
        card.addShipList(1, 1, cardWidth, CARD_BACKGROUND, List.of(member), 0f);
        rowPanel.addUIElement(card).inTL(0f, 0f);
        rowPanel.addComponent(Global.getSettings().createCustom(cardWidth, cardHeight, clickTarget)).inTL(0f, 0f);
        return rowPanel;
    }

    private void pick(FleetMemberAPI member, boolean alreadyShown) {
        close();
        if (!alreadyShown) {
            SkillTreeSounds.play("ui_button_pressed");
            picked.accept(member);
        }
    }

    void close() {
        if (listPanel == null) {
            return;
        }
        hostPanel.removeComponent(listPanel);
        listPanel = null;
        listPosition = null;
    }

    @Override
    public void positionChanged(PositionAPI listPosition) {
        this.listPosition = listPosition;
    }

    @Override
    public void renderBelow(float alphaMult) {
        if (listPosition != null) {
            BorderedPanel.draw(listPosition.getX(), listPosition.getY(), listPosition.getWidth(), listPosition.getHeight(), alphaMult);
        }
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        if (listPosition == null) {
            return;
        }
        for (InputEventAPI event : events) {
            if (event.isConsumed()) {
                continue;
            }
            boolean inside = Rects.contains(listPosition, event.getX(), event.getY());
            if (event.isMouseDownEvent() && !inside) {
                event.consume();
                close();
                return;
            }
            if ((event.isMouseDownEvent() || event.isMouseScrollEvent()) && inside) {
                event.consume();
            }
        }
    }
}
