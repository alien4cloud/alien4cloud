package alien4cloud.model.orchestrators;

import io.swagger.annotations.ApiModelProperty;

/**
 * Created by lucboutier on 06/08/15.
 */
public enum OrchestratorState {
    @ApiModelProperty("Admin has disabled the connection to the orchestrators.")
    DISABLED,
    @ApiModelProperty("Orchestrator is being connecting - this is used also when the plugin used to communicate with the orchestrators is being updated.")
    CONNECTING,
    @ApiModelProperty("Alien is connected to the orchestrators and doesn't detect any issues.")
    CONNECTED,
    @ApiModelProperty("Alien is fails to connect to the orchestrators.")
    DISCONNECTED
}