package exiledsector.skills.layout;

import java.util.List;
import java.util.function.Predicate;

public record TreeTags(List<String> tags, String volume) {

    public static final TreeTags NONE = new TreeTags(List.of(), null);

    public TreeTags {
        tags = tags == null ? List.of() : List.copyOf(tags);
    }

    public static TreeTags of(List<String> tags, Predicate<String> isVolume) {
        if (tags == null) {
            return NONE;
        }
        for (String tag : tags) {
            if (tag != null && isVolume.test(tag)) {
                return new TreeTags(tags, tag);
            }
        }
        return new TreeTags(tags, null);
    }
}
