package alien4cloud.deployment;

import javax.annotation.Resource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.paas.model.PaaSDeploymentLog;

/**
 * Service is used by some premium plugins for archive the deployments logs.
 */
@Service
public class DeploymentLoggingService {
    @Resource(name = "alien-monitor-es-dao")
    private IGenericSearchDAO alienMonitorDao;

    @Value("${logs_deployment_appender.enable}")
    private boolean isEnabled;

    private final Logger deployments_logger = LogManager.getLogger("DEPLOYMENT_LOGS_LOGGER");

    private void logToFile(PaaSDeploymentLog deploymentLog) {
        switch (deploymentLog.getLevel()) {
            case DEBUG:
                deployments_logger.debug(deploymentLog.toCompactString());
                break;
            case ERROR:
                deployments_logger.error(deploymentLog.toCompactString());
                break;
            case INFO:
                deployments_logger.info(deploymentLog.toCompactString());
                break;
            case WARN:
                deployments_logger.warn(deploymentLog.toCompactString());
                break;
        }
    }

    public synchronized void save(final PaaSDeploymentLog deploymentLog) {
        try {
            if (isEnabled) {
                logToFile(deploymentLog);
            }
        } finally {
            alienMonitorDao.save(deploymentLog);
        }
    }

    public synchronized void save(final PaaSDeploymentLog[] deploymentLogs) {
        try {
            if (isEnabled) {
                for (PaaSDeploymentLog deploymentLog : deploymentLogs) {
                    logToFile(deploymentLog);
                }
            }
        } finally {
            alienMonitorDao.save(deploymentLogs);
        }
    }

}
