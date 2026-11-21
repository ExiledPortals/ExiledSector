package exiledsector.items.custody;

import exiledsector.items.Item;
import exiledsector.items.ItemStore;
import exiledsector.items.pattern.HullPattern;
import exiledsector.items.pattern.HullPatternLosses;
import exiledsector.items.pattern.HullPatternRules;
import exiledsector.items.pattern.HullPatterns;
import exiledsector.skills.ShipSkillData;

import java.util.List;
import java.util.Random;

public final class HullPatternCustody implements ShipCustody {

    public static final HullPatternCustody INSTANCE = new HullPatternCustody();

    private HullPatternCustody() {
    }

    @Override
    public boolean holdsItems(ShipSkillData shipData) {
        return false;
    }

    @Override
    public boolean holdsItems(String shipId, ShipSkillData shipData) {
        HullPatterns patterns = HullPatterns.get();
        return patterns.ofPatternShip(shipId) != null || patterns.ofFollower(shipId) != null;
    }

    @Override
    public List<CargoRefund> release(ShipSkillData shipData, boolean destroyed, ItemStore store) {
        return List.of();
    }

    @Override
    public List<CargoRefund> release(String shipId, ShipSkillData shipData, boolean destroyed, ItemStore store) {
        HullPatterns patterns = HullPatterns.get();
        HullPattern followed = destroyed ? patterns.ofFollower(shipId) : null;
        if (followed != null) {
            HullPatternLosses.followerDestroyed(followed, shipId, shipData, store, new Random());
        }
        HullPattern owned = patterns.ofPatternShip(shipId);
        if (owned != null) {
            HullPatternLosses.patternShipLeft(owned);
        }
        return List.of();
    }

    @Override
    public boolean unsocketIfMisfit(ShipSkillData shipData, Item item) {
        return false;
    }

    @Override
    public void forgetShip(String shipId) {
        HullPatterns patterns = HullPatterns.get();
        HullPattern followed = patterns.ofFollower(shipId);
        if (followed != null) {
            HullPatternRules.unlink(followed, shipId);
        }
        HullPattern owned = patterns.ofPatternShip(shipId);
        if (owned != null) {
            HullPatternRules.delete(owned);
        }
    }
}
