package exiledsector.i18n;

public final class PseudoLeaks {

    private PseudoLeaks() {
    }

    public static String unbracketed(String text) {
        String remaining = text.replaceAll("</?(?:good|bad|hl|hullmod|node)>", "");
        String previous;
        do {
            previous = remaining;
            remaining = remaining.replaceAll("\\[(?:[^\\[\\]]|\\[[^\\[\\]~]*])*?~]", "");
        } while (!remaining.equals(previous));
        return remaining;
    }

    public static boolean hasLeak(String text) {
        return unbracketed(text).codePoints().anyMatch(Character::isLetter);
    }
}
