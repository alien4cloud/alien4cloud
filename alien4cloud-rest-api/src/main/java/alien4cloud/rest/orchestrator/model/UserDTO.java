package alien4cloud.rest.orchestrator.model;

import java.util.Arrays;

import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.security.model.User;
import alien4cloud.utils.ReflectionUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class UserDTO {
    private String username;
    private String lastName;
    private String firstName;
    private String email;

    public static GetMultipleDataResult<UserDTO> convert(GetMultipleDataResult<User> toConvert) {
        if (toConvert == null) {
            return null;
        }
        GetMultipleDataResult<UserDTO> converted = new GetMultipleDataResult<>();
        ReflectionUtil.mergeObject(toConvert, converted, "data");
        converted.setData(convert(toConvert.getData()));
        return converted;
    }

    /**
     * Convert a List<User> to List<UserDTO>
     *
     * @param users
     * @return List<UserDTO>
     */
    public static UserDTO[] convert(User... users) {
        return Arrays.stream(users).map(user -> new UserDTO(user.getUsername(), user.getLastName(), user.getFirstName(), user.getEmail()))
                .toArray(UserDTO[]::new);
    }
}