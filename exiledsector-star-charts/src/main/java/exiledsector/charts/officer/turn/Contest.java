package exiledsector.charts.officer.turn;

import exiledsector.charts.officer.AtlasOfficer;

public record Contest(Intent intent, AtlasOfficer attacker, AtlasOfficer defender, boolean defended, float attackerChancePercent, float roll) {

    public boolean abstractAttackerWins() {
        return roll * 100f < attackerChancePercent;
    }
}
