package exiledsector.loot;

import com.fs.starfarer.api.campaign.SectorEntityToken;

public record DropOrigin(Kind kind, SectorEntityToken site) {

    public enum Kind {
        ANYWHERE,
        BATTLE,
        SITE
    }

    public static final DropOrigin ANYWHERE = new DropOrigin(Kind.ANYWHERE, null);
    public static final DropOrigin BATTLE = new DropOrigin(Kind.BATTLE, null);

    public DropOrigin {
        if (kind == null || (kind == Kind.SITE) != (site != null)) {
            throw new IllegalArgumentException("Only a site drop names its site");
        }
    }

    public static DropOrigin site(SectorEntityToken site) {
        return site == null ? ANYWHERE : new DropOrigin(Kind.SITE, site);
    }

    public DropOrigin whereThePlayerIs() {
        return kind == Kind.SITE ? ANYWHERE : this;
    }
}
