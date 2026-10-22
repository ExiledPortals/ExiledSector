package exiledsector.domain;

import exiledsector.ExiledSector;

import java.util.List;
import java.util.function.Function;

public final class DomainContributions<T> {

    private record Snapshot<T>(List<ExiledSectorDomain> domains, List<T> contributions) {
    }

    private final Function<ExiledSectorDomain, List<? extends T>> contribution;
    private volatile Snapshot<T> snapshot = new Snapshot<>(null, List.of());

    public DomainContributions(Function<ExiledSectorDomain, List<? extends T>> contribution) {
        this.contribution = contribution;
    }

    public List<T> all() {
        List<ExiledSectorDomain> domains = ExiledSector.domains();
        Snapshot<T> current = snapshot;
        if (current.domains() != domains) {
            List<T> contributions = domains.stream().<T>flatMap(domain -> contribution.apply(domain).stream()).toList();
            current = new Snapshot<>(domains, contributions);
            snapshot = current;
        }
        return current.contributions();
    }
}
