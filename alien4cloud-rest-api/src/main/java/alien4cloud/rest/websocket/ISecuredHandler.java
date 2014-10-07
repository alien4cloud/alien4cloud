package alien4cloud.rest.websocket;

import java.security.Principal;

public interface ISecuredHandler {

    boolean canHandleDestination(String destination);

    void checkAuthorization(Principal user, String destination);
}
