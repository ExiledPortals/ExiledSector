package exiledsector.motes;

public enum MoteKind {

    STARDUST("exiledsector_stardust_mote_launcher");

    private final String launcherWeaponId;

    MoteKind(String launcherWeaponId) {
        this.launcherWeaponId = launcherWeaponId;
    }

    public String launcherWeaponId() {
        return launcherWeaponId;
    }
}
