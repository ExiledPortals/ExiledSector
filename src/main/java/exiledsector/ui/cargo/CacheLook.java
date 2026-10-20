package exiledsector.ui.cargo;

import java.util.List;

public record CacheLook(List<String> chipIconPaths) {

    public CacheLook {
        chipIconPaths = chipIconPaths == null ? List.of() : List.copyOf(chipIconPaths);
    }
}
