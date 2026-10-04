package exiledsector.skills;

import java.util.ArrayList;
import java.util.List;

public record NodeDescription(List<DescriptionLine> effects, List<DescriptionLine> details) {

    public List<DescriptionLine> all() {
        List<DescriptionLine> all = new ArrayList<>(effects);
        all.addAll(details);
        return all;
    }
}
