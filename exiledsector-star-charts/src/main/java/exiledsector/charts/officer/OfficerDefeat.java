package exiledsector.charts.officer;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.Seeds;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.officer.reward.CarriedItem;
import exiledsector.charts.officer.reward.OfficerRewardDrop;
import exiledsector.charts.officer.reward.OfficerRewardPool;
import exiledsector.charts.officer.reward.OfficerRewardText;
import exiledsector.charts.officer.reward.RewardContext;
import exiledsector.charts.officer.scar.HullFacts;
import exiledsector.charts.officer.scar.Scars;
import exiledsector.charts.run.ChartRun;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.items.ItemDrops;
import exiledsector.ui.atlas.OfficerRewardPicker;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class OfficerDefeat {

    static final String SEED_PREFIX = "officerDefeat|";
    static final String UNCHOSEN_SEED_PREFIX = "officerUnchosen|";
    private static final Logger LOG = Logger.getLogger(OfficerDefeat.class);

    public record RandomPick(AtlasOfficer officer, CarriedItem pick) {

        public String line() {
            return I18n.forGameText(() -> Translation.msg("chart.officer.reward.randomPick").arg("officer", OfficerText.title(officer))
                    .arg("reward", OfficerRewardText.groupName(pick)).text());
        }
    }

    private OfficerDefeat() {
    }

    public static ItemDrops.SiteLoot bossDefeated(SectorAPI sector, ChartRun run) {
        try {
            return defeated(sector, run);
        } catch (RuntimeException | LinkageError e) {
            LOG.error("[ExiledSector] Recording the defeat of the Atlas officer of run " + (run == null ? null : run.runId()) + " failed", e);
            return ItemDrops.SiteLoot.NONE;
        }
    }

    private static ItemDrops.SiteLoot defeated(SectorAPI sector, ChartRun run) {
        AtlasState state = AtlasState.getIfCreated(sector);
        announce(sector, pickUnchosenAtRandom(state));
        AtlasOfficer officer = state == null || run == null ? null : OfficerFleets.officerOf(run.atlasNode());
        if (officer == null) {
            return ItemDrops.SiteLoot.NONE;
        }
        Random random = Seeds.random(SEED_PREFIX + run.runId() + "|" + officer.recordId());
        HullFacts hull = OfficerFleets.flagshipHull(officer);
        String scarId = Scars.pickAfterBattle(run.bossFlagshipId(), officer.scars(), hull);
        AtlasOfficer scarred = scarred(officer, scarId, dModFor(scarId, officer, hull, random));
        ItemDrops.SiteLoot carriedLoot = OfficerRewardDrop.roll(officer.carried(), run, random);
        List<CarriedItem> offers = OfficerRewardPool.offers(RewardContext.forRun(sector, run, officer.factionId()), random);
        AtlasOfficer rewarded = rewarded(scarred, offers);
        state.putOfficer(rewarded);
        prompt(sector, rewarded);
        return carriedLoot;
    }

    public static AtlasOfficer challengeLost(AtlasState state, int loserOfficerId, String winnerFactionId, Random random) {
        AtlasOfficer loser = state == null ? null : state.officer(loserOfficerId);
        if (loser == null) {
            return null;
        }
        HullFacts hull = OfficerFleets.flagshipHull(loser);
        String scarId = Scars.pickAfterChallengeLoss(winnerFactionId, loser.scars(), hull);
        AtlasOfficer scarred = scarred(loser, scarId, dModFor(scarId, loser, hull, random));
        state.putOfficer(scarred);
        return scarred;
    }

    public static List<RandomPick> pickUnchosenAtRandom(SectorAPI sector) {
        try {
            return pickUnchosenAtRandom(AtlasState.getIfCreated(sector));
        } catch (RuntimeException | LinkageError e) {
            LOG.error("[ExiledSector] Picking the unchosen Atlas officer rewards failed", e);
            return List.of();
        }
    }

    static List<RandomPick> pickUnchosenAtRandom(AtlasState state) {
        List<RandomPick> randomPicks = new ArrayList<>();
        if (state == null) {
            return randomPicks;
        }
        for (AtlasOfficer officer : state.officers()) {
            if (!officer.hasPendingChoice()) {
                continue;
            }
            Random random = Seeds.random(UNCHOSEN_SEED_PREFIX + officer.recordId() + "|" + String.join(",", officer.pendingChoice()));
            List<CarriedItem> offers = officer.pendingOffers();
            CarriedItem pick = OfficerRewardPool.picked(offers.get(random.nextInt(offers.size())), random);
            state.putOfficer(picked(officer, pick));
            randomPicks.add(new RandomPick(officer, pick));
        }
        return randomPicks;
    }

    public static AtlasOfficer nextUnchosen(AtlasState state) {
        if (state == null) {
            return null;
        }
        for (AtlasOfficer officer : state.officers()) {
            if (officer.hasPendingChoice()) {
                return officer;
            }
        }
        return null;
    }

    public static boolean stillUnchosen(List<String> choice) {
        return holderOf(AtlasState.getIfCreated(Global.getSector()), choice) != null;
    }

    public static void answer(List<String> choice, CarriedItem pick) {
        AtlasState state = AtlasState.getIfCreated(Global.getSector());
        AtlasOfficer holder = holderOf(state, choice);
        if (holder != null) {
            state.putOfficer(picked(holder, pick));
        }
    }

    static AtlasOfficer scarred(AtlasOfficer officer, String scarId, String dModId) {
        return officer.withScars(Scars.withScar(officer.scars(), scarId)).withScarDMod(dModId, scarId);
    }

    static AtlasOfficer rewarded(AtlasOfficer officer, List<CarriedItem> offers) {
        return officer.withCarried(List.of()).withPendingOffers(offers);
    }

    static AtlasOfficer picked(AtlasOfficer officer, CarriedItem pick) {
        if (pick == null || officer.pendingOffers().stream().noneMatch(pick::sameItemAs)) {
            return officer;
        }
        return officer.withCarried(CarriedItem.joined(officer.carried(), List.of(pick))).withPendingOffers(List.of());
    }

    private static String dModFor(String scarId, AtlasOfficer officer, HullFacts hull, Random random) {
        return hull == null ? null : Scars.dModFor(scarId, officer.dmodIds(), hull, random);
    }

    private static void announce(SectorAPI sector, List<RandomPick> randomPicks) {
        if (sector == null || sector.getCampaignUI() == null) {
            return;
        }
        for (RandomPick randomPick : randomPicks) {
            sector.getCampaignUI().addMessage(randomPick.line());
        }
    }

    private static void prompt(SectorAPI sector, AtlasOfficer officer) {
        if (sector == null || !officer.hasPendingChoice()) {
            return;
        }
        List<String> choice = officer.pendingChoice();
        OfficerRewardPicker.openWhenClear(sector, OfficerText.title(officer), officer.pendingOffers(), () -> stillUnchosen(choice),
                pick -> answer(choice, pick));
    }

    static AtlasOfficer holderOf(AtlasState state, List<String> choice) {
        if (state == null) {
            return null;
        }
        for (AtlasOfficer officer : state.officers()) {
            if (officer.pendingChoice().equals(choice)) {
                return officer;
            }
        }
        return null;
    }
}
