package exiledsector.ui.inspect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CustomVisualDialogDelegate;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.effects.NpcFleetLeveller;
import exiledsector.i18n.I18n;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.socketables.Socketable;
import exiledsector.socketables.SocketableStore;
import exiledsector.ui.VanillaText;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;

public class NpcFleetInspectDialog implements CustomVisualDialogDelegate {

    public static final float HEIGHT = 620f;
    static final float COLUMN_WIDTH = 720f;
    static final float COLUMN_GAP = 20f;
    static final float SCREEN_MARGIN = 120f;
    private static final float SCROLLBAR_ALLOWANCE = 20f;
    private static final float PAD = 10f;
    private static final float BUTTON_HEIGHT = 25f;
    private static final float BUTTON_WIDTH = 160f;
    private static final float ICON_SIZE = 64f;
    private static final String CLOSE = "exiledSector_inspectClose";

    private final List<CampaignFleetAPI> fleets;
    private final Runnable onDismissed;
    private DialogCallbacks dialogCallbacks;

    public NpcFleetInspectDialog(List<CampaignFleetAPI> fleets, Runnable onDismissed) {
        this.fleets = List.copyOf(fleets);
        this.onDismissed = onDismissed;
    }

    @Override
    public void init(CustomPanelAPI dialogPanel, DialogCallbacks dialogCallbacks) {
        this.dialogCallbacks = dialogCallbacks;
        I18n.forGameText(() -> buildPanel(dialogPanel));
    }

    public static float width() {
        return widthFor(columnsFor(Global.getSettings().getScreenWidth()));
    }

    static int columnsFor(float screenWidth) {
        return screenWidth >= widthFor(2) + SCREEN_MARGIN ? 2 : 1;
    }

    static float widthFor(int columns) {
        return COLUMN_WIDTH * columns + COLUMN_GAP * (columns - 1);
    }

    private static int columnsIn(float panelWidth) {
        return panelWidth >= widthFor(2) ? 2 : 1;
    }

    private void buildPanel(CustomPanelAPI dialogPanel) {
        float panelWidth = dialogPanel.getPosition().getWidth();
        float panelHeight = dialogPanel.getPosition().getHeight();

        TooltipMakerAPI contentElement = dialogPanel.createUIElement(panelWidth, panelHeight - BUTTON_HEIGHT - PAD * 2f, true);
        for (CampaignFleetAPI fleet : fleets) {
            addFleet(dialogPanel, contentElement, fleet, panelWidth, columnsIn(panelWidth));
        }
        dialogPanel.addUIElement(contentElement).inTL(0f, 0f);

        TooltipMakerAPI buttonsElement = dialogPanel.createUIElement(panelWidth, BUTTON_HEIGHT, false);
        buttonsElement.addButton(Translation.text("inspect.close"), CLOSE, BUTTON_WIDTH, BUTTON_HEIGHT, 0f);
        dialogPanel.addUIElement(buttonsElement).inBL((panelWidth - BUTTON_WIDTH) / 2f, PAD);
    }

    static List<FleetMemberAPI> levelledMembers(CampaignFleetAPI fleet) {
        List<FleetMemberAPI> levelled = new ArrayList<>();
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            if (ShipTreeLookup.isLevelledNpc(member)) {
                levelled.add(member);
            }
        }
        return levelled;
    }

    private static void addFleet(CustomPanelAPI dialogPanel, TooltipMakerAPI contentElement, CampaignFleetAPI fleet, float contentWidth,
                                 int columns) {
        NpcFleetLeveller.ensure(fleet);
        List<FleetMemberAPI> allMembers = fleet.getFleetData().getMembersListCopy();
        List<FleetMemberAPI> levelled = levelledMembers(fleet);
        List<ShipEntry> shipEntries = new ArrayList<>();
        for (FleetMemberAPI member : levelled) {
            ShipTreeLookup.ShipTree shipTree = ShipTreeLookup.find(member);
            if (shipTree != null) {
                shipEntries.add(new ShipEntry(member, shipTree));
            }
        }
        contentElement.addSectionHeading(fleet.getFullName(), Alignment.MID, PAD);
        addSocketables(dialogPanel, contentElement, shipEntries, contentWidth - SCROLLBAR_ALLOWANCE);
        VanillaText.addPara(contentElement, Translation.msg("inspect.levelledShips").arg("levelled", levelled.size())
                .arg("total", allMembers.size()).styled(), PAD, Misc.getTextColor());
        if (columns <= 1) {
            for (ShipEntry shipEntry : shipEntries) {
                addShip(contentElement, fleet, shipEntry, PAD * 2f);
            }
            return;
        }
        float rowWidth = contentWidth - SCROLLBAR_ALLOWANCE;
        float columnWidth = (rowWidth - COLUMN_GAP * (columns - 1)) / columns;
        for (int first = 0; first < shipEntries.size(); first += columns) {
            addShipRow(contentElement, fleet, shipEntries.subList(first, Math.min(shipEntries.size(), first + columns)), rowWidth, columnWidth);
        }
    }

    private static void addSocketables(CustomPanelAPI dialogPanel, TooltipMakerAPI contentElement, List<ShipEntry> shipEntries,
                                       float stripWidth) {
        List<SocketableIconStrip.Entry> stripEntries = new ArrayList<>();
        for (ShipEntry shipEntry : shipEntries) {
            List<StyledText> footer = List.of(Translation.msg("inspect.socketable.installedIn").arg("ship", shipTitle(shipEntry.member())).styled(),
                    Translation.styled("inspect.socketable.drops"));
            for (String socketableId : shipEntry.shipTree().skillData().getSocketedItems().values()) {
                Socketable socketable = SocketableStore.lookup(socketableId);
                if (socketable != null) {
                    stripEntries.add(new SocketableIconStrip.Entry(socketable, footer));
                }
            }
        }
        if (stripEntries.isEmpty()) {
            return;
        }
        contentElement.addPara("%s", PAD, Misc.getBasePlayerColor(), Translation.text("inspect.socketables"));
        contentElement.addCustom(SocketableIconStrip.create(dialogPanel, stripWidth, stripEntries), PAD / 2f);
    }

    private static void addShipRow(TooltipMakerAPI contentElement, CampaignFleetAPI fleet, List<ShipEntry> shipEntries,
                                   float rowWidth, float columnWidth) {
        CustomPanelAPI rowPanel = Global.getSettings().createCustom(rowWidth, 0f, null);
        float rowHeight = 0f;
        for (int column = 0; column < shipEntries.size(); column++) {
            TooltipMakerAPI cellElement = rowPanel.createUIElement(columnWidth, 0f, false);
            addShip(cellElement, fleet, shipEntries.get(column), 0f);
            float cellHeight = cellElement.getHeightSoFar();
            rowPanel.addUIElement(cellElement).inTL(column * (columnWidth + COLUMN_GAP), 0f);
            rowHeight = Math.max(rowHeight, cellHeight);
        }
        rowPanel.getPosition().setSize(rowWidth, rowHeight);
        contentElement.addCustom(rowPanel, PAD * 2f);
    }

    private static void addShip(TooltipMakerAPI tooltip, CampaignFleetAPI fleet, ShipEntry shipEntry, float pad) {
        tooltip.addPara("%s", pad, Misc.getBasePlayerColor(), shipTitle(shipEntry.member()));
        tooltip.addShipList(1, 1, ICON_SIZE, fleet.getFaction().getBaseUIColor(), List.of(shipEntry.member()), PAD / 2f);
        ShipTreeSummaryRenderer.render(tooltip, shipEntry.member(), shipEntry.shipTree(), PAD / 2f);
    }

    private record ShipEntry(FleetMemberAPI member, ShipTreeLookup.ShipTree shipTree) {
    }

    private static String shipTitle(FleetMemberAPI member) {
        String hullName = member.getHullSpec().getHullNameWithDashClass();
        String shipName = member.getShipName();
        return shipName == null || shipName.isBlank() ? hullName
                : Translation.msg("inspect.shipTitle").arg("name", shipName).arg("hull", hullName).text();
    }

    @Override
    public CustomUIPanelPlugin getCustomPanelPlugin() {
        return new BaseCustomUIPanelPlugin() {
            @Override
            public void buttonPressed(Object buttonId) {
                if (CLOSE.equals(buttonId)) {
                    close();
                }
            }

            @Override
            public void processInput(List<InputEventAPI> events) {
                for (InputEventAPI event : events) {
                    if (!event.isConsumed() && event.isKeyDownEvent() && event.getEventValue() == Keyboard.KEY_ESCAPE) {
                        event.consume();
                        close();
                    }
                }
            }
        };
    }

    private void close() {
        if (dialogCallbacks != null) {
            dialogCallbacks.dismissDialog();
        }
    }

    @Override
    public float getNoiseAlpha() {
        return 0f;
    }

    @Override
    public void advance(float amount) {
    }

    @Override
    public void reportDismissed(int option) {
        if (onDismissed != null) {
            onDismissed.run();
        }
    }
}
