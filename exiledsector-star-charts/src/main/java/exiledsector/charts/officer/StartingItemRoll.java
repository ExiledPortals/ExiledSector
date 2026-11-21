package exiledsector.charts.officer;

import exiledsector.charts.officer.reward.CarriedItem;

import java.util.Random;

@FunctionalInterface
public interface StartingItemRoll {

    StartingItemRoll NONE = (officer, random) -> null;

    CarriedItem roll(AtlasOfficer officer, Random random);
}
