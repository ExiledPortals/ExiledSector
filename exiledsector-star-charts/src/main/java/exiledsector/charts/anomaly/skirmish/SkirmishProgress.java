package exiledsector.charts.anomaly.skirmish;

import exiledsector.loot.Loot;

public record SkirmishProgress(boolean flagshipFell, Loot loot, boolean bothFallen, Integer beatenOfficerId) {

    public static final SkirmishProgress NONE = new SkirmishProgress(false, Loot.NONE, false, null);

    public SkirmishProgress {
        loot = loot == null ? Loot.NONE : loot;
    }

    SkirmishProgress then(SkirmishProgress later) {
        if (!later.flagshipFell) {
            return this;
        }
        if (!flagshipFell) {
            return later;
        }
        return new SkirmishProgress(true, loot.plus(later.loot), later.bothFallen, later.beatenOfficerId);
    }
}
