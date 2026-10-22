package second_in_command;

public class SCData {

    public boolean isSkillActive(String skillId) {
        return SCUtils.ACTIVE_SKILLS.contains(skillId);
    }
}
