package exiledsector.charts.officer;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.Seeds;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.officer.reward.CarriedItem;
import exiledsector.charts.officer.reward.OfficerRewardDrop;
import exiledsector.charts.officer.reward.OfficerRewardPool;
import exiledsector.charts.officer.reward.RewardContext;
import exiledsector.charts.officer.scar.HullFacts;
import exiledsector.charts.officer.scar.Scars;
import exiledsector.charts.run.ChartRun;
import exiledsector.items.ItemDrops;
import exiledsector.ui.atlas.OfficerRewardPicker;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class OfficerDefeat {

    static final String SEED_PREFIX = "officerDefeat|";
    private static final Logger LOG = Logger.getLogger(OfficerDefeat.class);
    private static final Set<List<String>> PROMPTED_CHOICES = new HashSet<>();

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

    public static void onGameLoad(SectorAPI sector) {
        PROMPTED_CHOICES.clear();
        reopenPendingChoices(sector);
    }

    public static void reopenPendingChoices(SectorAPI sector) {
        try {
            AtlasState state = AtlasState.getIfCreated(sector);
            if (state == null) {
                return;
            }
            for (AtlasOfficer officer : state.officers()) {
                prompt(sector, officer);
            }
        } catch (RuntimeException | LinkageError e) {
            LOG.error("[ExiledSector] Reopening the pending Atlas officer reward choices failed", e);
        }
    }

    static AtlasOfficer scarred(AtlasOfficer officer, String scarId, String dModId) {
        AtlasOfficer scarred = officer.withScars(Scars.withScar(officer.scars(), scarId));
        if (dModId == null || officer.dmodIds().contains(dModId)) {
            return scarred;
        }
        List<String> dModIds = new ArrayList<>(officer.dmodIds());
        dModIds.add(dModId);
        return scarred.withDModIds(dModIds);
    }

    static AtlasOfficer rewarded(AtlasOfficer officer, List<CarriedItem> offers) {
        return officer.withCarried(List.of()).withPendingOffers(offers);
    }

    static AtlasOfficer picked(AtlasOfficer officer, CarriedItem pick) {
        if (pick == null || officer.pendingOffers().stream().noneMatch(pick::stacksWith)) {
            return officer;
        }
        return officer.withCarried(CarriedItem.stack(officer.carried(), List.of(pick))).withPendingOffers(List.of());
    }

    private static String dModFor(String scarId, AtlasOfficer officer, HullFacts hull, Random random) {
        return hull == null ? null : Scars.dModFor(scarId, officer.dmodIds(), hull, random);
    }

    private static void prompt(SectorAPI sector, AtlasOfficer officer) {
        if (sector == null || !officer.hasPendingChoice()) {
            return;
        }
        List<String> choice = officer.pendingChoice();
        if (!PROMPTED_CHOICES.add(choice)) {
            return;
        }
        OfficerRewardPicker.openWhenClear(sector, OfficerText.title(officer), officer.pendingOffers(), () -> stillOffered(choice),
                pick -> answered(choice, pick));
    }

    private static boolean stillOffered(List<String> choice) {
        if (holderOf(AtlasState.getIfCreated(Global.getSector()), choice) != null) {
            return true;
        }
        PROMPTED_CHOICES.remove(choice);
        return false;
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

    private static void answered(List<String> choice, CarriedItem pick) {
        PROMPTED_CHOICES.remove(choice);
        AtlasState state = AtlasState.getIfCreated(Global.getSector());
        AtlasOfficer holder = holderOf(state, choice);
        if (holder != null) {
            state.putOfficer(picked(holder, pick));
        }
    }
}
