package exiledsector.charts.officer;

import exiledsector.charts.officer.reward.CarriedItem;

import java.util.List;
import java.util.Random;

@FunctionalInterface
public interface TrophyRefit {

    TrophyRefit NONE = (officer, random) -> officer.carried();

    List<CarriedItem> refitted(AtlasOfficer officer, Random random);
}
