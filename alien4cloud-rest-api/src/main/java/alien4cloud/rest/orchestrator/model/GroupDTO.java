package alien4cloud.rest.orchestrator.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.security.model.Group;
import alien4cloud.utils.ReflectionUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class GroupDTO {
    private String id;
    private String name;
    private String email;
    private String description;

    public static GetMultipleDataResult<GroupDTO> convert(GetMultipleDataResult<Group> toConvert) {
        if (toConvert == null) {
            return null;
        }
        GetMultipleDataResult<GroupDTO> converted = new GetMultipleDataResult<>();
        ReflectionUtil.mergeObject(toConvert, converted, "data");
        converted.setData(convert(toConvert.getData()));
        return converted;
    }

    /**
     * Convert a List<Group> to List<GroupDTO>
     *
     * @param groups
     * @return List<GroupDTO>
     */
    public static GroupDTO[] convert(Group... groups) {
        return groups == null ? null
                : Arrays.stream(groups).map(group -> new GroupDTO(group.getId(), group.getName(), group.getEmail(), group.getDescription()))
                        .toArray(GroupDTO[]::new);
    }

    /**
     * Convert a List<Group> to List<GroupDTO>
     *
     * @param groups
     * @return List<GroupDTO>
     */
    public static List<GroupDTO> convert(Collection<Group> groups) {
        return groups == null ? null
                : groups.stream().map(group -> new GroupDTO(group.getId(), group.getName(), group.getEmail(), group.getDescription()))
                        .collect(Collectors.toList());
    }
}