package exiledsector.charts.raid;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CoreInteractionListener;
import com.fs.starfarer.api.campaign.GroundRaidTargetPickerDelegate;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.ListenerUtil;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.impl.campaign.graid.GroundRaidObjectivePlugin;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD.RaidDangerLevel;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledlib.i18n.I18n;
import exiledlib.i18n.Style;
import exiledlib.i18n.StyledText;
import exiledlib.i18n.Translation;
import exiledsector.ui.VanillaText;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.Supplier;

public class ChartRaid {

    public interface Host {

        void raidLeft(InteractionDialogAPI dialog);

        void raidFinished(InteractionDialogAPI dialog);
    }

    static final String RAID_VALUABLE = "exiledSector_raidValuable";
    static final String RAID_GO_BACK = "exiledSector_raidGoBack";
    static final String RAID_CONFIRM = "exiledSector_raidConfirm";
    static final String RAID_NEVER_MIND = "exiledSector_raidNeverMind";
    static final String RAID_CONFIRM_CONTINUE = "exiledSector_raidConfirmContinue";
    static final String RAID_RESULT = "exiledSector_raidResult";
    static final List<String> OPTIONS = List.of(RAID_VALUABLE, RAID_GO_BACK, RAID_CONFIRM, RAID_NEVER_MIND, RAID_CONFIRM_CONTINUE, RAID_RESULT);
    static final float GRID_WIDTH = 350f;
    static final float GRID_VALUE_WIDTH = 50f;
    static final float PARAGRAPH_PAD = 10f;
    static final float GRID_PAD = 5f;
    static final float GRID_SECOND_PAD = 3f;
    static final float LOSS_VARIANCE_SHARE = 0.25f;
    static final double NORMAL_SPREAD = 0.2d;
    static final float PERCENT = 100f;

    private final InteractionDialogAPI dialog;
    private final SectorEntityToken entity;
    private final MarketAPI market;
    private final CampaignFleetAPI playerFleet;
    private final TextPanelAPI text;
    private final OptionPanelAPI options;
    private final float difficulty;
    private final Supplier<List<GroundRaidObjectivePlugin>> objectiveSource;
    private final Random random;
    private final Host host;
    private float raidMult;
    private List<GroundRaidObjectivePlugin> objectives = new ArrayList<>();
    private CargoAPI raidLoot;
    private int xpGained;
    private String contText;

    public ChartRaid(InteractionDialogAPI dialog, float difficulty, Supplier<List<GroundRaidObjectivePlugin>> objectiveSource, Random random, Host host) {
        this.dialog = dialog;
        this.entity = dialog.getInteractionTarget();
        this.market = entity == null ? null : entity.getMarket();
        this.playerFleet = Global.getSector().getPlayerFleet();
        this.text = dialog.getTextPanel();
        this.options = dialog.getOptionPanel();
        this.difficulty = difficulty;
        this.objectiveSource = objectiveSource;
        this.random = random;
        this.host = host;
    }

    public static boolean handles(Object optionData) {
        return optionData instanceof String option && OPTIONS.contains(option);
    }

    public void optionSelected(Object optionData) {
        if (RAID_VALUABLE.equals(optionData)) {
            raidValuable();
        } else if (RAID_GO_BACK.equals(optionData)) {
            host.raidLeft(dialog);
        } else if (RAID_CONFIRM.equals(optionData)) {
            raidConfirm();
        } else if (RAID_NEVER_MIND.equals(optionData)) {
            open();
        } else if (RAID_CONFIRM_CONTINUE.equals(optionData)) {
            raidConfirmContinue();
        } else if (RAID_RESULT.equals(optionData)) {
            raidResult();
        }
    }

    static float raidEffectiveness(float attackerStr, float defenderStr) {
        float effectiveness = attackerStr / Math.max(1f, attackerStr + defenderStr);
        return Math.round(effectiveness * PERCENT) / PERCENT;
    }

    static int marineTokens(float raidMult) {
        int tokens = Math.round(raidMult / MarketCMD.RE_PER_MARINE_TOKEN);
        return Math.max(MarketCMD.MIN_MARINE_TOKENS, Math.min(MarketCMD.MAX_MARINE_TOKENS, tokens));
    }

    static float lootMult(float raidEffectiveness) {
        float leftover = Math.round(raidEffectiveness * PERCENT) % Math.round(MarketCMD.RE_PER_MARINE_TOKEN * PERCENT) / PERCENT;
        if (raidEffectiveness < MarketCMD.RE_PER_MARINE_TOKEN) {
            leftover = 0f;
        }
        return 1f + leftover / Math.max(MarketCMD.RE_PER_MARINE_TOKEN, raidEffectiveness);
    }

    static float effectivenessLossMult(float raidMult) {
        if (raidMult > MarketCMD.MIN_RE_TO_REDUCE_MARINE_LOSSES) {
            float extra = (raidMult - MarketCMD.MIN_RE_TO_REDUCE_MARINE_LOSSES) / (1f - MarketCMD.MIN_RE_TO_REDUCE_MARINE_LOSSES);
            return MarketCMD.MAX_MARINE_LOSS_REDUCTION_MULT + (1f - MarketCMD.MAX_MARINE_LOSS_REDUCTION_MULT) * (1f - extra);
        }
        if (raidMult < MarketCMD.RE_PER_MARINE_TOKEN) {
            return 1f + (MarketCMD.RE_PER_MARINE_TOKEN - raidMult) / MarketCMD.RE_PER_MARINE_TOKEN;
        }
        return 1f;
    }

    static float reserveLossMult(int maxTokens, float assignedTokens) {
        if (maxTokens <= assignedTokens) {
            return 1f;
        }
        return Math.max(0.5f, 1f - (maxTokens - assignedTokens) * MarketCMD.LOSS_REDUCTION_PER_RESERVE_TOKEN);
    }

    static RaidDangerLevel lossesLevel(float lossShare, boolean noneWhenEmpty) {
        for (RaidDangerLevel level : RaidDangerLevel.values()) {
            float test = level.marineLossesMult + (level.next().marineLossesMult - level.marineLossesMult) * 0.5f;
            if (noneWhenEmpty && level == RaidDangerLevel.NONE) {
                test = RaidDangerLevel.NONE.marineLossesMult;
            }
            if (test >= lossShare) {
                return level;
            }
        }
        return RaidDangerLevel.EXTREME;
    }

    static Style lossesStyle(RaidDangerLevel level) {
        return switch (level) {
            case NONE, MINIMAL, LOW -> Style.GOOD;
            case MEDIUM -> Style.HIGHLIGHT;
            case HIGH, EXTREME -> Style.BAD;
        };
    }

    static String lossesName(RaidDangerLevel level) {
        return Translation.gameText("chart.raid.losses." + level.name().toLowerCase(Locale.ROOT));
    }

    public void open() {
        dialog.getVisualPanel().showImagePortion("illustrations", "raid_prepare", 640, 400, 0, 0, 480, 300);
        float marines = playerFleet.getCargo().getMarines();
        float support = Math.min(marines, Misc.getFleetwideTotalMod(playerFleet, Stats.FLEET_GROUND_SUPPORT, 0f));
        StatBonus attackerBase = new StatBonus();
        attackerBase.modifyFlatAlways("core_marines", marines, Translation.gameText("chart.raid.marines"));
        attackerBase.modifyFlatAlways("core_support", support, Translation.gameText("chart.raid.support"));
        StatBonus attacker = playerFleet.getStats().getDynamic().getMod(Stats.PLANETARY_OPERATIONS_MOD);
        StatBonus defender = new StatBonus();
        defender.modifyFlat("difficulty", difficulty, Translation.gameText("chart.raid.resistance"));
        float attackerStr = Math.round(attacker.computeEffective(attackerBase.computeEffective(0f)));
        float defenderStr = Math.round(defender.computeEffective(0f));
        Color base = Misc.getTextColor();
        TooltipMakerAPI info = text.beginTooltip();
        info.setParaSmallInsignia();
        VanillaText.addPara(info, I18n.forGameText(() -> Translation.msg("chart.raid.strength").arg("value", (int) attackerStr).styled()), 0f, base);
        info.addStatModGrid(GRID_WIDTH, GRID_VALUE_WIDTH, PARAGRAPH_PAD, GRID_PAD, attackerBase, true, MarketCMD.statPrinter(false));
        if (!attacker.isUnmodified()) {
            info.addStatModGrid(GRID_WIDTH, GRID_VALUE_WIDTH, PARAGRAPH_PAD, GRID_SECOND_PAD, attacker, true, MarketCMD.statPrinter(true));
        }
        VanillaText.addPara(info, I18n.forGameText(() -> Translation.msg("chart.raid.difficulty").arg("value", (int) defenderStr).styled()),
                PARAGRAPH_PAD, base);
        info.addStatModGrid(GRID_WIDTH, GRID_VALUE_WIDTH, PARAGRAPH_PAD, GRID_PAD, defender, true, MarketCMD.statPrinter(true));
        text.addTooltip();
        raidMult = raidEffectiveness(attackerStr, defenderStr);
        boolean hasForces = raidMult >= MarketCMD.VALUABLES_THRESHOLD;
        int effectivenessPercent = (int) (raidMult * PERCENT);
        VanillaText.addPara(text, I18n.forGameText(() -> Translation.msg(hasForces ? "chart.raid.effectiveness" : "chart.raid.effectivenessTooLow")
                .arg("value", effectivenessPercent).styled()), base);
        if (!hasForces) {
            VanillaText.plainPara(text, Translation.gameText("chart.raid.noForces"));
        }
        options.clearOptions();
        options.addOption(Translation.gameText("chart.raid.option.designate"), RAID_VALUABLE);
        options.setEnabled(RAID_VALUABLE, hasForces);
        options.addOption(Translation.gameText("chart.raid.option.goBack"), RAID_GO_BACK);
        options.setShortcut(RAID_GO_BACK, Keyboard.KEY_ESCAPE, false, false, false, true);
    }

    int numMarineTokens() {
        return marineTokens(raidMult);
    }

    void raidValuable() {
        List<GroundRaidObjectivePlugin> available = new ArrayList<>(objectiveSource.get());
        if (available.isEmpty()) {
            VanillaText.plainPara(text, Translation.gameText("chart.raid.noTargets"));
            addNeverMindOption();
            return;
        }
        dialog.showGroundRaidTargetPicker(Translation.gameText("chart.raid.picker.title"), Translation.gameText("chart.raid.picker.ok"), market,
                available, new Picker());
    }

    private final class Picker implements GroundRaidTargetPickerDelegate {

        @Override
        public void pickedGroundRaidTargets(List<GroundRaidObjectivePlugin> data) {
            float value = 0f;
            List<StyledText> names = new ArrayList<>();
            for (GroundRaidObjectivePlugin objective : data) {
                value += objective.getProjectedCreditsValue();
                names.add(StyledText.styled(objective.getNameOverride() != null ? objective.getNameOverride() : objective.getName(), Style.HIGHLIGHT));
            }
            int marines = playerFleet.getCargo().getMarines();
            RaidDangerLevel losses = lossesLevel(getAverageMarineLosses(data) / Math.max(1f, marines), true);
            StyledText lossesText = StyledText.styled(lossesName(losses).toLowerCase(Locale.ROOT), lossesStyle(losses));
            Color base = Misc.getTextColor();
            VanillaText.addPara(text, I18n.forGameText(() -> Translation.msg("chart.raid.plan").count(marines).arg("losses", lossesText).styled()), base);
            VanillaText.addPara(text, I18n.forGameText(() -> Translation.msg("chart.raid.targets").count(names.size())
                    .arg("targets", Translation.list(names)).styled()), base);
            if (value > 0f) {
                String credits = Misc.getDGSCredits(value);
                VanillaText.addPara(text, I18n.forGameText(() -> Translation.msg("chart.raid.value").arg("value", StyledText.styled(credits, Style.HIGHLIGHT))
                        .styled()), base);
            }
            VanillaText.plainPara(text, Translation.gameText("chart.raid.ready"));
            objectives = new ArrayList<>(data);
            addConfirmOptions();
        }

        @Override
        public void cancelledGroundRaidTargetPicking() {
        }

        @Override
        public int getNumMarineTokens() {
            return numMarineTokens();
        }

        @Override
        public String getRaidEffectiveness() {
            return (int) (raidMult * PERCENT) + "%";
        }

        @Override
        public String getProjectedMarineLosses(List<GroundRaidObjectivePlugin> data) {
            return lossesName(lossesLevel(getAverageMarineLosses(data) / Math.max(1f, playerFleet.getCargo().getMarines()), true));
        }

        @Override
        public Color getMarineLossesColor(List<GroundRaidObjectivePlugin> data) {
            float lossShare = getAverageMarineLosses(data) / Math.max(1f, playerFleet.getCargo().getMarines());
            if (lossShare <= 0f && data.isEmpty()) {
                return Misc.getGrayColor();
            }
            return lossesLevel(lossShare, false).color;
        }

        @Override
        public int getProjectedCreditsValue(List<GroundRaidObjectivePlugin> data) {
            int total = 0;
            for (GroundRaidObjectivePlugin objective : data) {
                total += objective.getProjectedCreditsValue();
            }
            return total;
        }

        @Override
        public int getCargoSpaceNeeded(List<GroundRaidObjectivePlugin> data) {
            int total = 0;
            for (GroundRaidObjectivePlugin objective : data) {
                total += objective.getCargoSpaceNeeded();
            }
            return total;
        }

        @Override
        public int getFuelSpaceNeeded(List<GroundRaidObjectivePlugin> data) {
            int total = 0;
            for (GroundRaidObjectivePlugin objective : data) {
                total += objective.getFuelSpaceNeeded();
            }
            return total;
        }

        @Override
        public MutableStat getMarineLossesStat(List<GroundRaidObjectivePlugin> data) {
            return ChartRaid.this.getMarineLossesStat(data);
        }

        @Override
        public float getAverageMarineLosses(List<GroundRaidObjectivePlugin> data) {
            return ChartRaid.this.getAverageMarineLosses(data);
        }

        @Override
        public boolean isDisruptIndustryMode() {
            return false;
        }

        @Override
        public boolean isCustomOnlyMode() {
            return true;
        }
    }

    MutableStat getMarineLossesStat(List<GroundRaidObjectivePlugin> data) {
        MutableStat stat = new MutableStat(1f);
        float total = 0f;
        float assignedTokens = 0f;
        for (GroundRaidObjectivePlugin objective : data) {
            total += objective.getDangerLevel().marineLossesMult * objective.getMarinesAssigned();
            assignedTokens += objective.getMarinesAssigned();
        }
        float danger = total / Math.max(1f, assignedTokens);
        float hazard = market == null ? 1f : market.getHazardValue();
        float effectivenessMult = effectivenessLossMult(raidMult);
        if (effectivenessMult < 1f) {
            float minMarines = MarketCMD.getMarinesFor(Math.round(difficulty), Math.round(assignedTokens));
            float actualMarines = playerFleet.getCargo().getMarines();
            if (actualMarines > minMarines && actualMarines > 0f) {
                effectivenessMult *= 0.5f + 0.5f * minMarines / actualMarines;
            }
        }
        float reservesMult = reserveLossMult(numMarineTokens(), assignedTokens);
        stat.modifyMultAlways("danger", danger, Translation.gameText("chart.raid.lossFactor.danger"));
        stat.modifyMult("hazard", hazard, Translation.gameText("chart.raid.lossFactor.hazard"));
        if (effectivenessMult < 1f) {
            stat.modifyMultAlways("reMult", effectivenessMult, Translation.gameText("chart.raid.lossFactor.highEffectiveness"));
        } else if (effectivenessMult > 1f) {
            stat.modifyMultAlways("reMult", effectivenessMult, Translation.gameText("chart.raid.lossFactor.lowEffectiveness"));
        }
        if (reservesMult < 1f && assignedTokens > 0f) {
            stat.modifyMultAlways("reservesMult", reservesMult, Translation.gameText("chart.raid.lossFactor.reserves"));
        }
        stat.applyMods(playerFleet.getStats().getDynamic().getStat(Stats.PLANETARY_OPERATIONS_CASUALTIES_MULT));
        ListenerUtil.modifyMarineLossesStatPreRaid(market, data, stat);
        return stat;
    }

    float getAverageMarineLosses(List<GroundRaidObjectivePlugin> data) {
        float mult = Math.min(MarketCMD.MAX_MARINE_LOSSES, getMarineLossesStat(data).getModifiedValue());
        return playerFleet.getCargo().getMarines() * mult;
    }

    private void addNeverMindOption() {
        options.clearOptions();
        options.addOption(Translation.gameText("chart.raid.option.neverMind"), RAID_NEVER_MIND);
        options.setShortcut(RAID_NEVER_MIND, Keyboard.KEY_ESCAPE, false, false, false, true);
    }

    private void addConfirmOptions() {
        options.clearOptions();
        options.addOption(Translation.gameText("chart.raid.option.launch"), RAID_CONFIRM);
        options.addOption(Translation.gameText("chart.raid.option.neverMind"), RAID_NEVER_MIND);
        options.setShortcut(RAID_NEVER_MIND, Keyboard.KEY_ESCAPE, false, false, false, true);
    }

    static float normalRandom(Random random, float min, float max) {
        double spread = Math.max(0d, Math.min(1d, random.nextGaussian() * NORMAL_SPREAD + 0.5d));
        return min + (float) spread * (max - min);
    }

    static int randomizedLosses(float averageLosses, int marines, Random random) {
        float variance = averageLosses * LOSS_VARIANCE_SHARE;
        float losses = normalRandom(random, averageLosses - variance, averageLosses + variance);
        if (losses < 1f) {
            losses = random.nextFloat() < losses ? 1f : 0f;
        }
        return Math.max(0, Math.min(marines, Math.round(losses)));
    }

    void raidConfirm() {
        if (objectives.isEmpty()) {
            open();
            return;
        }
        int marines = playerFleet.getCargo().getMarines();
        int losses = randomizedLosses(getAverageMarineLosses(objectives), marines, random);
        if (losses <= 0) {
            VanillaText.plainPara(text, Translation.gameText("chart.raid.noCasualties"));
        } else {
            VanillaText.plainPara(text, Translation.gameText("chart.raid.casualties"));
            playerFleet.getCargo().removeMarines(losses);
            text.setFontSmallInsignia();
            VanillaText.addPara(text, I18n.forGameText(() -> Translation.msg("chart.raid.marinesLost").count(losses).styled()), Misc.getNegativeHighlightColor());
            text.setFontInsignia();
        }
        CargoAPI result = performRaid(random, raidMult);
        result.sort();
        result.updateSpaceUsed();
        raidLoot = result;
        if (xpGained > 0) {
            Global.getSector().getPlayerStats().addXP(xpGained, text);
        }
        String illustration = result.getTotalCrew() + result.getSpaceUsed() + result.getFuel() < 10 ? "raid_covert_result" : "raid_valuables_result";
        dialog.getVisualPanel().showImagePortion("illustrations", illustration, 640, 400, 0, 0, 480, 300);
        boolean withContinue = objectives.stream().anyMatch(GroundRaidObjectivePlugin::withContinueBeforeResult);
        if (withContinue) {
            options.clearOptions();
            options.addOption(Translation.gameText("chart.raid.option.continue"), RAID_CONFIRM_CONTINUE);
        } else {
            raidConfirmContinue();
        }
    }

    void raidConfirmContinue() {
        int raidCredits = Math.max(0, (int) raidLoot.getCredits().get());
        if (raidCredits > 0) {
            playerFleet.getCargo().getCredits().add(raidCredits);
            String credits = Misc.getDGSCredits(raidCredits);
            text.setFontSmallInsignia();
            VanillaText.addPara(text, I18n.forGameText(() -> Translation.msg("chart.raid.creditsGained").arg("credits", credits).styled()),
                    Misc.getPositiveHighlightColor());
            text.setFontInsignia();
            raidLoot.getCredits().set(0f);
        }
        contText = raidLoot.isEmpty() ? null : Translation.gameText("chart.raid.option.pick");
        Global.getSoundPlayer().playUISound("ui_raid_finished", 1f, 1f);
        options.clearOptions();
        options.addOption(contText != null ? contText : Translation.gameText("chart.raid.option.continue"), RAID_RESULT);
    }

    CargoAPI performRaid(Random raidRandom, float raidEffectiveness) {
        CargoAPI result = Global.getFactory().createCargo(true);
        long baseSeed = raidRandom.nextLong();
        int xp = 0;
        for (GroundRaidObjectivePlugin objective : objectives) {
            Random objectiveRandom = new Random(Misc.seedUniquifier() ^ (baseSeed * objective.getClass().getName().hashCode()));
            xp += objective.performRaid(result, objectiveRandom, lootMult(raidEffectiveness), text);
        }
        xpGained = xp;
        return result;
    }

    void raidResult() {
        if (raidLoot == null || raidLoot.isEmpty()) {
            finished();
            return;
        }
        dialog.getVisualPanel().showLoot(Translation.gameText("chart.raid.loot.title"), raidLoot, false, true, true, new CoreInteractionListener() {
            @Override
            public void coreUIDismissed() {
                finished();
            }
        });
    }

    private void finished() {
        objectives = new ArrayList<>();
        raidLoot = null;
        host.raidFinished(dialog);
    }
}
