package exiledsector.ui;

import exiledsector.ui.util.TextFont;

public final class UiFonts {

    static final String FONT_FOLDER = "graphics/fonts/";
    static final String ORBITRON_12_CONDENSED_FILE = "orbitron12condensed.fnt";
    static final String ORBITRON_20_FILE = "orbitron20aa.fnt";
    static final String ORBITRON_20_BOLD_FILE = "orbitron20aabold.fnt";
    static final String ORBITRON_24_BOLD_FILE = "orbitron24aabold.fnt";
    static final String INSIGNIA_15_FILE = "insignia15LTaa.fnt";
    static final String INSIGNIA_17_FILE = "insignia17LTaa.fnt";
    static final String INSIGNIA_21_FILE = "insignia21LTaa.fnt";
    static final String INSIGNIA_25_FILE = "insignia25LTaa.fnt";

    public static final TextFont HEADING = nativeFont(ORBITRON_12_CONDENSED_FILE, 15f);
    public static final TextFont BODY = nativeFont(INSIGNIA_15_FILE, 15f);
    public static final TextFont TITLE = nativeFont(ORBITRON_20_BOLD_FILE, 20f);
    public static final TextFont BUTTON = nativeFont(ORBITRON_20_FILE, 20f);
    public static final TextFont DIALOG_TEXT = nativeFont(INSIGNIA_21_FILE, 21f);
    public static final TextFont MESSAGE = DIALOG_TEXT.resized(18f);
    public static final TextFont MAP_LABEL = nativeFont(INSIGNIA_25_FILE, 24f);
    public static final TextFont LARGE_TITLE = nativeFont(ORBITRON_24_BOLD_FILE, 24f);
    public static final TextFont LARGE_DETAIL = nativeFont(INSIGNIA_17_FILE, 17f);

    private UiFonts() {
    }

    private static TextFont nativeFont(String file, float lineHeight) {
        String path = FONT_FOLDER + file;
        return new TextFont(() -> SkillTreePanelStyle.localeFontOr(path), lineHeight);
    }
}
