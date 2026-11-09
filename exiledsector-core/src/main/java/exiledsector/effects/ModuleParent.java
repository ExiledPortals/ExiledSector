package exiledsector.effects;

public record ModuleParent(ShipEffectContext context, ResolvedTree resolvedTree) {

    public String memberId() {
        return context.member().getId();
    }
}
