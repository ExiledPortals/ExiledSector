package exiledsector.skills.skilleffect.stationmodule;

public enum ModuleShare {
    SHARE,
    SKIP,
    EXCLUDE;

    public boolean isShared() {
        return this == SHARE;
    }
}
