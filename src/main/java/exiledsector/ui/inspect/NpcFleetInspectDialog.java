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
    private DialogCallbacks callbacks;

    public NpcFleetInspectDialog(List<CampaignFleetAPI> fleets, Runnable onDismissed) {
        this.fleets = List.copyOf(fleets);
        this.onDismissed = onDismissed;
    }

    @Override
    public void init(CustomPanelAPI panel, DialogCallbacks callbacks) {
        this.callbacks = callbacks;
        I18n.forGameText(() -> buildPanel(panel));
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

    private void buildPanel(CustomPanelAPI panel) {
        float width = panel.getPosition().getWidth();
        float height = panel.getPosition().getHeight();

        TooltipMakerAPI content = panel.createUIElement(width, height - BUTTON_HEIGHT - PAD * 2f, true);
        for (CampaignFleetAPI fleet : fleets) {
            addFleet(panel, content, fleet, width, columnsIn(width));
        }
        panel.addUIElement(content).inTL(0f, 0f);

        TooltipMakerAPI buttons = panel.createUIElement(width, BUTTON_HEIGHT, false);
        buttons.addButton(Translation.text("inspect.close"), CLOSE, BUTTON_WIDTH, BUTTON_HEIGHT, 0f);
        panel.addUIElement(buttons).inBL((width - BUTTON_WIDTH) / 2f, PAD);
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

    private static void addFleet(CustomPanelAPI panel, TooltipMakerAPI content, CampaignFleetAPI fleet, float width, int columns) {
        NpcFleetLeveller.ensure(fleet);
        List<FleetMemberAPI> members = fleet.getFleetData().getMembersListCopy();
        List<FleetMemberAPI> levelled = levelledMembers(fleet);
        List<ShipEntry> ships = new ArrayList<>();
        for (FleetMemberAPI member : levelled) {
            ShipTreeLookup.ShipTree tree = ShipTreeLookup.find(member);
            if (tree != null) {
                ships.add(new ShipEntry(member, tree));
            }
        }
        content.addSectionHeading(fleet.getFullName(), Alignment.MID, PAD);
        addSocketables(panel, content, ships, width - SCROLLBAR_ALLOWANCE);
        VanillaText.addPara(content, Translation.msg("inspect.levelledShips").arg("levelled", levelled.size())
                .arg("total", members.size()).styled(), PAD, Misc.getTextColor());
        if (columns <= 1) {
            for (ShipEntry ship : ships) {
                addShip(content, fleet, ship, PAD * 2f);
            }
            return;
        }
        float rowWidth = width - SCROLLBAR_ALLOWANCE;
        float columnWidth = (rowWidth - COLUMN_GAP * (columns - 1)) / columns;
        for (int first = 0; first < ships.size(); first += columns) {
            addShipRow(content, fleet, ships.subList(first, Math.min(ships.size(), first + columns)), rowWidth, columnWidth);
        }
    }

    private static void addSocketables(CustomPanelAPI panel, TooltipMakerAPI content, List<ShipEntry> ships, float width) {
        List<SocketableIconStrip.Entry> entries = new ArrayList<>();
        for (ShipEntry ship : ships) {
            List<StyledText> footer = List.of(Translation.msg("inspect.socketable.installedIn").arg("ship", shipTitle(ship.member())).styled(),
                    Translation.styled("inspect.socketable.drops"));
            for (String socketableId : ship.tree().data().getSocketedItems().values()) {
                Socketable socketable = SocketableStore.lookup(socketableId);
                if (socketable != null) {
                    entries.add(new SocketableIconStrip.Entry(socketable, footer));
                }
            }
        }
        if (entries.isEmpty()) {
            return;
        }
        content.addPara("%s", PAD, Misc.getBasePlayerColor(), Translation.text("inspect.socketables"));
        content.addCustom(SocketableIconStrip.create(panel, width, entries), PAD / 2f);
    }

    private static void addShipRow(TooltipMakerAPI content, CampaignFleetAPI fleet, List<ShipEntry> ships,
                                   float width, float columnWidth) {
        CustomPanelAPI row = Global.getSettings().createCustom(width, 0f, null);
        float rowHeight = 0f;
        for (int column = 0; column < ships.size(); column++) {
            TooltipMakerAPI cell = row.createUIElement(columnWidth, 0f, false);
            addShip(cell, fleet, ships.get(column), 0f);
            float cellHeight = cell.getHeightSoFar();
            row.addUIElement(cell).inTL(column * (columnWidth + COLUMN_GAP), 0f);
            rowHeight = Math.max(rowHeight, cellHeight);
        }
        row.getPosition().setSize(width, rowHeight);
        content.addCustom(row, PAD * 2f);
    }

    private static void addShip(TooltipMakerAPI tooltip, CampaignFleetAPI fleet, ShipEntry ship, float pad) {
        tooltip.addPara("%s", pad, Misc.getBasePlayerColor(), shipTitle(ship.member()));
        tooltip.addShipList(1, 1, ICON_SIZE, fleet.getFaction().getBaseUIColor(), List.of(ship.member()), PAD / 2f);
        ShipTreeSummaryRenderer.render(tooltip, ship.member(), ship.tree(), PAD / 2f);
    }

    private record ShipEntry(FleetMemberAPI member, ShipTreeLookup.ShipTree tree) {
    }

    private static String shipTitle(FleetMemberAPI member) {
        String hull = member.getHullSpec().getHullNameWithDashClass();
        String name = member.getShipName();
        return name == null || name.isBlank() ? hull : Translation.msg("inspect.shipTitle").arg("name", name).arg("hull", hull).text();
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
        if (callbacks != null) {
            callbacks.dismissDialog();
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
