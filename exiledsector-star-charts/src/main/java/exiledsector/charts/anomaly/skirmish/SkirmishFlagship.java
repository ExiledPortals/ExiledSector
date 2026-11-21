package exiledsector.charts.anomaly.skirmish;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import exiledsector.charts.anomaly.StateFields;

record SkirmishFlagship(FightingSide side, int officerRecordId, String fleetId, String flagshipId, FlagshipFall fall, int fallOrder) {

    private static final int FIELD_COUNT = 5;

    SkirmishFlagship {
        fall = fall == null ? FlagshipFall.STANDING : fall;
    }

    static SkirmishFlagship standing(FightingSide side, int officerRecordId, String fleetId, String flagshipId) {
        return new SkirmishFlagship(side, officerRecordId, fleetId, flagshipId, FlagshipFall.STANDING, 0);
    }

    boolean hasFallen() {
        return fall != FlagshipFall.STANDING;
    }

    boolean flies(CampaignFleetAPI fleet) {
        return fleet != null && fleetId.equals(fleet.getId());
    }

    SkirmishFlagship fallen(FlagshipFall how, int order) {
        return new SkirmishFlagship(side, officerRecordId, fleetId, flagshipId, how, order);
    }

    String encode() {
        return StateFields.join(Integer.toString(officerRecordId), fleetId, flagshipId, fall.name(), Integer.toString(fallOrder));
    }

    static SkirmishFlagship decode(FightingSide side, String encoded) {
        return StateFields.decode(encoded, FIELD_COUNT, fields -> new SkirmishFlagship(side, Integer.parseInt(fields.text(0)), fields.text(1),
                fields.text(2), fields.enumValue(3, FlagshipFall.class), Integer.parseInt(fields.text(4))));
    }

    static boolean canRecord(String fleetId, String flagshipId) {
        return StateFields.fits(fleetId) && StateFields.fits(flagshipId);
    }
}
