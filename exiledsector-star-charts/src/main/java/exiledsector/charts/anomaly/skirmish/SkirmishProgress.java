package exiledsector.charts.anomaly.skirmish;

import exiledsector.items.ItemDrops;

public record SkirmishProgress(boolean flagshipFell, ItemDrops.SiteLoot loot, boolean bothFallen, Integer beatenOfficerId) {

    public static final SkirmishProgress NONE = new SkirmishProgress(false, ItemDrops.SiteLoot.NONE, false, null);

    public SkirmishProgress {
        loot = loot == null ? ItemDrops.SiteLoot.NONE : loot;
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
