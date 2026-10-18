package exiledsector.items;

public enum MaterialTier {

    BASIC(null),
    MINOR(null),
    MAJOR("ui_cargo_special_industrial_pickup"),
    EXOTIC("ui_char_gained_story_point"),
    MYTHIC("ui_char_level_up");

    private final String dropSoundId;

    MaterialTier(String dropSoundId) {
        this.dropSoundId = dropSoundId;
    }

    public String dropSoundId() {
        return dropSoundId;
    }

    public boolean announced() {
        return compareTo(EXOTIC) >= 0;
    }
}
