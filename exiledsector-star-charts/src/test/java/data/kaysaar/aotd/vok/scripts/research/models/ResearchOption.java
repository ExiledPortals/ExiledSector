package data.kaysaar.aotd.vok.scripts.research.models;

public class ResearchOption {

    // java:S116: the field name copies Vaults of Knowledge's own public field
    @SuppressWarnings("java:S116")
    public String Id;

    public ResearchOption(String id) {
        Id = id;
    }
}
