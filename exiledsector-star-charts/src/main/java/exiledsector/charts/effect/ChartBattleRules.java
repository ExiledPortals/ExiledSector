package exiledsector.charts.effect;

public record ChartBattleRules(int battleSizeBonus, boolean shipSystemsDisabled, boolean autopilotForced, float arcStrikeEmp, float weaponMisfireRate,
                               float damagePercentPerSMod) {

    public static final ChartBattleRules NONE = new ChartBattleRules(0, false, false, 0f, 0f, 0f);

    public boolean isActive() {
        return battleSizeBonus != 0 || shipSystemsDisabled || autopilotForced || arcStrikeEmp > 0f || weaponMisfireRate > 0f || damagePercentPerSMod != 0f;
    }
}
