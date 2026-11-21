package exiledsector.skills.layout;

public record SkillNodeDecoration(String ringBeltPath, String ringBeltColor, Float ringBeltWidth,
                                   String wormholeColor, String pairedNodeId) {

    public static final SkillNodeDecoration NONE = new SkillNodeDecoration(null, null, null, null, null);
}
