package exiledsector.domain;

import exiledsector.ExiledSector;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class DomainContributions<T> {

    private record Snapshot<T>(List<ExiledSectorDomain> domains, long enabledMask, List<T> contributions) {
    }

    private final Function<ExiledSectorDomain, List<? extends T>> contribution;
    private final boolean enabledDomainsOnly;
    private volatile Snapshot<T> snapshot = new Snapshot<>(null, 0L, List.of());

    public DomainContributions(Function<ExiledSectorDomain, List<? extends T>> contribution) {
        this(contribution, true);
    }

    private DomainContributions(Function<ExiledSectorDomain, List<? extends T>> contribution, boolean enabledDomainsOnly) {
        this.contribution = contribution;
        this.enabledDomainsOnly = enabledDomainsOnly;
    }

    public static <T> DomainContributions<T> fromEveryDomain(Function<ExiledSectorDomain, List<? extends T>> contribution) {
        return new DomainContributions<>(contribution, false);
    }

    public List<T> all() {
        List<ExiledSectorDomain> domains = ExiledSector.domains();
        long enabledMask = enabledDomainsOnly ? enabledMask(domains) : everyDomain(domains);
        Snapshot<T> current = snapshot;
        if (current.domains() != domains || current.enabledMask() != enabledMask) {
            List<T> contributions = new ArrayList<>();
            for (int index = 0; index < domains.size(); index++) {
                if ((enabledMask & (1L << index)) != 0L) {
                    contributions.addAll(contribution.apply(domains.get(index)));
                }
            }
            current = new Snapshot<>(domains, enabledMask, List.copyOf(contributions));
            snapshot = current;
        }
        return current.contributions();
    }

    public <R> R first(Function<? super T, ? extends R> answer) {
        return firstOr(answer, null);
    }

    public <R> R firstOr(Function<? super T, ? extends R> answer, R fallback) {
        for (T contributor : all()) {
            R contributed = answer.apply(contributor);
            if (contributed != null) {
                return contributed;
            }
        }
        return fallback;
    }

    static long enabledMask(List<ExiledSectorDomain> domains) {
        long enabledMask = 0L;
        for (int index = 0; index < domains.size(); index++) {
            if (domains.get(index).enabled()) {
                enabledMask |= 1L << index;
            }
        }
        return enabledMask;
    }

    private static long everyDomain(List<ExiledSectorDomain> domains) {
        return domains.size() >= Long.SIZE ? -1L : (1L << domains.size()) - 1L;
    }
}
