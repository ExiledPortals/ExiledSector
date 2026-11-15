package exiledsector.ui.chart;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.MarketConditionSpecAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireAll;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.phasing.PhaseLock;
import exiledsector.charts.phasing.PhasedPlanetRecord;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.ui.VanillaText;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ExiledSectorPhaseLockOption extends BaseCommandPlugin {

    public static final String OPTION_ID = "exiledSector_phaseLock";
    static final String LOCK = "lock";
    static final int OPTION_ORDER = 5;

    @Override
    public boolean doesCommandAddOptions() {
        return true;
    }

    @Override
    public int getOptionOrder(List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        return OPTION_ORDER;
    }

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) {
            return false;
        }
        SectorAPI sector = Global.getSector();
        SectorEntityToken target = dialog.getInteractionTarget();
        PhaseLock.Offer offer = PhaseLock.offerFor(sector, target);
        if (offer == null) {
            return false;
        }
        if (params != null && !params.isEmpty() && LOCK.equals(params.get(0).getString(memoryMap))) {
            PhasedPlanetRecord lock = PhaseLock.lock(sector, (PlanetAPI) target);
            if (lock != null) {
                VanillaText.plainPara(dialog.getTextPanel(), lockedText(lock, PhaseLock.idleGateMarket(sector)));
            }
            FireAll.fire(null, dialog, memoryMap, ExiledSectorAtlasOption.POPULATE_OPTIONS);
            return true;
        }
        addOption(dialog.getOptionPanel(), sector, target, offer);
        return true;
    }

    private static void addOption(OptionPanelAPI options, SectorAPI sector, SectorEntityToken target, PhaseLock.Offer offer) {
        if (options.hasOption(OPTION_ID)) {
            return;
        }
        MarketAPI gateMarket = PhaseLock.idleGateMarket(sector);
        options.addOption(Translation.gameText("chart.phasing.lock.option"), OPTION_ID, tooltip(target, gateMarket, offer));
        if (offer != PhaseLock.Offer.AVAILABLE) {
            options.setEnabled(OPTION_ID, false);
            return;
        }
        PhasedPlanetRecord replaced = PhaseLock.currentLock(gateMarket);
        if (replaced != null && !Objects.equals(replaced.planetId(), target.getId())) {
            options.addOptionConfirmation(OPTION_ID,
                    I18n.forGameText(() -> Translation.msg("chart.phasing.lock.replace").arg("planet", replaced.name()).text()),
                    Translation.gameText("chart.phasing.lock.replaceYes"), Translation.gameText("chart.phasing.lock.replaceNo"));
        }
    }

    static String tooltip(SectorEntityToken target, MarketAPI gateMarket, PhaseLock.Offer offer) {
        String planetName = target.getName();
        return switch (offer) {
            case NOT_SURVEYED -> Translation.gameText("chart.phasing.lock.notSurveyed");
            case LOCKED -> I18n.forGameText(() -> Translation.msg("chart.phasing.lock.alreadyLocked").arg("planet", planetName).text());
            case AVAILABLE -> I18n.forGameText(() -> Translation.msg("chart.phasing.lock.tooltip").arg("planet", planetName)
                    .arg("colony", gateMarket == null ? "" : gateMarket.getName()).text());
        };
    }

    static String lockedText(PhasedPlanetRecord lock, MarketAPI gateMarket) {
        String colony = gateMarket == null ? "" : gateMarket.getName();
        String locked = I18n.forGameText(() -> Translation.msg("chart.phasing.lock.done").arg("planet", lock.name()).arg("colony", colony).text());
        if (lock.traitConditionIds().isEmpty()) {
            return locked;
        }
        String traits = String.join(", ", lock.traitConditionIds().stream().map(ExiledSectorPhaseLockOption::conditionName).toList());
        return locked + "\n\n" + I18n.forGameText(() -> Translation.msg("chart.phasing.lock.traits").arg("traits", traits).text());
    }

    private static String conditionName(String conditionId) {
        MarketConditionSpecAPI spec = Global.getSettings().getMarketConditionSpec(conditionId);
        return spec == null ? conditionId : spec.getName();
    }
}
