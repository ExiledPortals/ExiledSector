package exiledsector.ui;

import com.fs.starfarer.api.campaign.CargoAPI;
import exiledsector.i18n.Translation;
import exiledsector.items.LogisticsSchematic;
import exiledsector.skills.LogisticsNodes;
import exiledsector.skills.ShipSkillData;

import java.awt.Color;

final class LogisticsSlots extends SchematicButton {

    private int allocated;
    private int cap;

    LogisticsSlots(String memberId) {
        super(memberId, LogisticsSchematic.INSTANCE, LogisticsSchematic.ICON_PATH, Translation.text("ui.logisticsSlots.title"));
    }

    @Override
    int heldIn(CargoAPI cargo) {
        return LogisticsSchematic.held(cargo);
    }

    @Override
    void read(ShipSkillData data) {
        allocated = LogisticsNodes.allocated(data);
        cap = LogisticsNodes.cap(data);
    }

    @Override
    String label() {
        return Translation.msg("ui.logisticsSlots.button").arg("used", allocated).arg("cap", cap).text();
    }

    @Override
    Color labelColor() {
        return allocated > cap ? SkillTreePanelStyle.NEGATIVE_STAT_COLOR : null;
    }

    @Override
    String tooltipBody(int held, String itemName) {
        String countLine = Translation.msg("ui.logisticsSlots.body").arg("used", allocated).arg("cap", cap).text();
        String itemLine = held > 0 ? Translation.msg("ui.logisticsSlots.useHint").arg("item", itemName).arg("held", held).text()
                : Translation.msg("ui.logisticsSlots.noItems").arg("item", itemName).text();
        return countLine + "\n\n" + itemLine;
    }

    @Override
    boolean canUse() {
        return true;
    }

    @Override
    String dialogBody(int held) {
        return Translation.msg("ui.logisticsSlots.dialog.body").count(held).arg("cap", cap).arg("newCap", cap + 1).text();
    }

    @Override
    boolean use(CargoAPI cargo, ShipSkillData data) {
        return LogisticsSchematic.useOn(cargo, data);
    }
}
