package exiledsector.skills;

public final class LinkCache<T> {

    private T links;
    private int dataRevision;
    private Object lookup;
    private int lookupRevision;

    public boolean isFresh(int currentDataRevision, Object currentLookup, int currentLookupRevision) {
        return links != null && dataRevision == currentDataRevision && lookup == currentLookup && lookupRevision == currentLookupRevision;
    }

    public T links() {
        return links;
    }

    public T store(T builtLinks, int builtDataRevision, Object builtLookup, int builtLookupRevision) {
        links = builtLinks;
        dataRevision = builtDataRevision;
        lookup = builtLookup;
        lookupRevision = builtLookupRevision;
        return builtLinks;
    }
}
