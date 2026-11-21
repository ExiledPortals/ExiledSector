package exiledsector.items.crafting.material;

public enum MaterialTier {

    COMMON(null),
    RARE("ui_cargo_special_industrial_pickup"),
    UNIQUE("ui_char_gained_story_point"),
    MYTHIC("ui_char_level_up");

    private final String dropSoundId;

    MaterialTier(String dropSoundId) {
        this.dropSoundId = dropSoundId;
    }

    public String dropSoundId() {
        return dropSoundId;
    }

    public boolean announced() {
        return compareTo(UNIQUE) >= 0;
    }
}
