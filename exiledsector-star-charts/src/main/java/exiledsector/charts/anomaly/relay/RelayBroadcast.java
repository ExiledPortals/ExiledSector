package exiledsector.charts.anomaly.relay;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.Seeds;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.OfficerText;
import exiledsector.charts.officer.reward.CarriedItem;
import exiledsector.charts.officer.reward.OfficerRewardPool;
import exiledsector.charts.officer.reward.RewardContext;
import exiledsector.charts.officer.reward.RewardGroup;
import exiledsector.charts.officer.scar.Scars;
import exiledsector.charts.run.ChartRun;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.ui.CampaignMessages;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

final class RelayBroadcast {

    static final String SEED_PREFIX = "relayBroadcast|";

    private RelayBroadcast() {
    }

    static AtlasOfficer share(SectorAPI sector, ChartRun run, RelaySlot slot, RelayChoice choice) {
        AtlasState state = AtlasState.getIfCreated(sector);
        if (state == null) {
            return null;
        }
        Random random = Seeds.random(SEED_PREFIX + run.runId() + "|" + slot.key());
        AtlasOfficer officer = pickOfficer(state.officers(), random);
        if (officer == null) {
            return null;
        }
        List<RewardGroup> groups = Collections.nCopies(RelayTuning.rewardCopies(RelayTuning.of(run)), choice.group());
        List<CarriedItem> rewards = OfficerRewardPool.drawn(groups, RewardContext.forRun(sector, run, officer.factionId()), random);
        AtlasOfficer sharedWith = shared(officer, rewards, choice.scarIds());
        state.putOfficer(sharedWith);
        String officerTitle = OfficerText.title(sharedWith);
        CampaignMessages.plain(I18n.forGameText(() -> Translation.msg(RelayText.PREFIX + "message.broadcast").arg("officer", officerTitle).text()),
                ChartRelays.MESSAGE_COLOUR);
        return sharedWith;
    }

    static AtlasOfficer pickOfficer(List<AtlasOfficer> officers, Random random) {
        List<AtlasOfficer> active = officers.stream().filter(AtlasOfficer::isActive).toList();
        return active.isEmpty() ? null : active.get(random.nextInt(active.size()));
    }

    static AtlasOfficer shared(AtlasOfficer officer, List<CarriedItem> rewards, List<String> scarIds) {
        Map<String, Integer> scars = officer.scars();
        for (String scarId : scarIds) {
            scars = Scars.withScar(scars, scarId);
        }
        return officer.withCarried(CarriedItem.joined(officer.carried(), rewards)).withScars(scars);
    }
}
