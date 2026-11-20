package exiledsector.charts.officer;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.Seeds;
import exiledsector.charts.anomaly.breach.RiftDuel;
import exiledsector.charts.anomaly.stormfront.ThunderDuel;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.officer.reward.OfficerRewardDrop;
import exiledsector.charts.officer.scar.HullFacts;
import exiledsector.charts.officer.scar.Scars;
import exiledsector.charts.phasing.DisplacementTheorem;
import exiledsector.charts.run.ChartRun;
import exiledsector.loot.Loot;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Random;

public final class OfficerDefeat {

    static final String SEED_PREFIX = "officerDefeat|";
    private static final Logger LOG = Logger.getLogger(OfficerDefeat.class);

    private OfficerDefeat() {
    }

    public static Loot bossDefeated(SectorAPI sector, ChartRun run) {
        try {
            AtlasState state = AtlasState.getIfCreated(sector);
            AtlasOfficer officer = state == null || run == null ? null : OfficerFleets.officerOf(run.atlasNode());
            if (officer == null) {
                return Loot.NONE;
            }
            return defeated(sector, state, run, officer, run.bossFlagshipId());
        } catch (RuntimeException | LinkageError e) {
            LOG.error("[ExiledSector] Recording the defeat of the Atlas officer of run " + (run == null ? null : run.runId()) + " failed", e);
            return Loot.NONE;
        }
    }

    public static Loot officerBeaten(SectorAPI sector, ChartRun run, int officerRecordId, String flagshipId) {
        try {
            AtlasState state = AtlasState.getIfCreated(sector);
            if (state == null || run == null) {
                return Loot.NONE;
            }
            AtlasOfficer officer = state.officer(officerRecordId);
            return officer == null ? Loot.NONE : defeated(sector, state, run, officer, flagshipId);
        } catch (RuntimeException | LinkageError e) {
            LOG.error("[ExiledSector] Recording the defeat of Atlas officer " + officerRecordId + " in run " + (run == null ? null : run.runId())
                    + " failed", e);
            return Loot.NONE;
        }
    }

    private static Loot defeated(SectorAPI sector, AtlasState state, ChartRun run, AtlasOfficer officer, String flagshipId) {
        Random random = Seeds.random(SEED_PREFIX + run.runId() + "|" + officer.recordId());
        Map<String, Integer> theorem = DisplacementTheorem.bossDrop(run, officer.rankId(), officer.recordId());
        HullFacts hull = OfficerFleets.flagshipHull(officer);
        String scarId = Scars.pickAfterBattle(flagshipId, officer.scars(), hull);
        AtlasOfficer scarred = scarred(officer, scarId, dModFor(scarId, officer, hull, random));
        Loot carriedLoot = OfficerRewardDrop.roll(officer.carried(), run, random).plus(RiftDuel.at(sector, run).duplicatedLoot(officer, run))
                .plus(ThunderDuel.at(sector, run).extraLoot(sector, officer, run));
        state.putOfficer(scarred.withCarried(List.of()));
        return carriedLoot.plus(List.of(), List.of(), theorem);
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

    static AtlasOfficer scarred(AtlasOfficer officer, String scarId, String dModId) {
        return officer.withScars(Scars.withScar(officer.scars(), scarId)).withScarDMod(dModId, scarId);
    }

    private static String dModFor(String scarId, AtlasOfficer officer, HullFacts hull, Random random) {
        return hull == null ? null : Scars.dModFor(scarId, officer.dmodIds(), hull, random);
    }
}
