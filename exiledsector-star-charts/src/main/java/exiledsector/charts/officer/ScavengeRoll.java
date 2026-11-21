package exiledsector.charts.officer;

import exiledsector.charts.officer.reward.CarriedItem;

import java.util.Random;

@FunctionalInterface
public interface ScavengeRoll {

    ScavengeRoll NONE = (officer, random) -> null;

    CarriedItem roll(AtlasOfficer officer, Random random);
}
