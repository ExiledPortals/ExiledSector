package exiledsector.ui;

import com.fs.starfarer.api.campaign.CargoAPI;
import exiledsector.i18n.Translation;
import exiledsector.items.DeintegrationSchematic;
import exiledsector.skills.ShipSkillData;

final class IntegrationResetButton extends SchematicButton {

    private final Runnable afterReset;
    private int integratedCount;

    IntegrationResetButton(String memberId, Runnable afterReset) {
        super(memberId, DeintegrationSchematic.INSTANCE, DeintegrationSchematic.ICON_PATH, Translation.text("ui.integrationReset.title"));
        this.afterReset = afterReset;
    }

    @Override
    int heldIn(CargoAPI cargo) {
        return DeintegrationSchematic.held(cargo);
    }

    @Override
    void read(ShipSkillData data) {
        integratedCount = data.getIntegratedNodeIds().size();
    }

    @Override
    String label() {
        return Translation.msg("ui.integrationReset.button").arg("count", integratedCount).text();
    }

    @Override
    String tooltipBody(int held, String itemName) {
        String countLine = integratedCount == 0 ? Translation.text("ui.integrationReset.none")
                : Translation.msg("ui.integrationReset.body").count(integratedCount).text();
        String itemLine;
        if (held == 0) {
            itemLine = Translation.msg("ui.integrationReset.noItems").arg("item", itemName).text();
        } else if (integratedCount == 0) {
            itemLine = Translation.msg("ui.integrationReset.held").arg("item", itemName).arg("held", held).text();
        } else {
            itemLine = Translation.msg("ui.integrationReset.useHint").arg("item", itemName).arg("held", held).text();
        }
        return countLine + "\n\n" + itemLine;
    }

    @Override
    boolean canUse() {
        return integratedCount > 0;
    }

    @Override
    String dialogBody(int held) {
        return Translation.msg("ui.integrationReset.dialog.body").count(held).text();
    }

    @Override
    boolean use(CargoAPI cargo, ShipSkillData data) {
        if (DeintegrationSchematic.useOn(cargo, data) == 0) {
            return false;
        }
        afterReset.run();
        return true;
    }
}
