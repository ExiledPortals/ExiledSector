package exiledsector.charts.anomaly.listeningpost;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD.RaidDangerLevel;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.anomaly.ChartRaidObjective;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.ui.VanillaText;

import java.util.Random;
import java.util.function.Supplier;

public class VaultRaidObjective extends ChartRaidObjective {

    public static final String OBJECTIVE_ID = "exiledSector_depotVault";

    private final int vaultPercent;
    private final Supplier<CargoAPI> vault;

    public VaultRaidObjective(MarketAPI market, RaidDangerLevel danger, int vaultPercent, Supplier<CargoAPI> vault) {
        super(market, OBJECTIVE_ID, danger);
        this.vaultPercent = vaultPercent;
        this.vault = vault;
    }

    @Override
    public String getName() {
        return Translation.gameText("chart.anomaly.listening_post.raid.objective.name");
    }

    @Override
    protected void describe(TooltipMakerAPI tooltip) {
        VanillaText.plainPara(tooltip, I18n.forGameText(() -> Translation.msg("chart.anomaly.listening_post.raid.objective.tooltip").arg("value", vaultPercent)
                .text()), 0f, Misc.getTextColor());
    }

    @Override
    protected CargoAPI take(Random random) {
        return vault == null ? null : vault.get();
    }
}
