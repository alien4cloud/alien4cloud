package alien4cloud.deployment;

import javax.annotation.Resource;

import org.apache.log4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.paas.model.PaaSDeploymentLog;
import groovy.util.logging.Log4j;

/**
 * Service is used by some premium plugins for archive the deployments logs.
 */
@Log4j
@Service
public class DeploymentLoggingService {
    @Resource(name = "alien-monitor-es-dao")
    private IGenericSearchDAO alienMonitorDao;

    @Value("${logs_deployment_appender.enable}")
    private boolean isEnable;

    @Value("${directories.alien}/${logs_deployment_appender.directory}")
    private String logsRepository;

    @Value("${logs_deployment_appender.pattern_layout}")
    private String patternLayout;

    private Appender createAppenderForAnDeployment(String deploymentPaasId) {
        DailyRollingFileAppender appender = new DailyRollingFileAppender();
        appender.setName(deploymentPaasId);
        appender.setFile(logsRepository + "/" + deploymentPaasId + ".log");
        appender.setLayout(new PatternLayout(patternLayout));
        appender.activateOptions();
        return appender;
    }

    public synchronized void save(final PaaSDeploymentLog deploymentLog) {
        try {
            if (isEnable && deploymentLog.getDeploymentPaaSId() != null) {
                Logger currentLogger = Logger.getLogger(deploymentLog.getDeploymentPaaSId());
                if (currentLogger.getLevel() == null) {
                    currentLogger.setLevel(Level.INFO);
                }
                if (currentLogger.getAppender(deploymentLog.getDeploymentPaaSId()) == null) {
                    currentLogger.addAppender(createAppenderForAnDeployment(deploymentLog.getDeploymentPaaSId()));
                }
                currentLogger.info(deploymentLog.toString());
            }
        } finally {
            alienMonitorDao.save(deploymentLog);
        }
    }

    public void save(final PaaSDeploymentLog[] deploymentLogs) {
        for (PaaSDeploymentLog log : deploymentLogs) {
            save(log);
        }
    }

}