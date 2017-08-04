package org.alien4cloud.server;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.inject.Inject;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.server.MaintenanceModeState.MaintenanceLog;
import org.springframework.stereotype.Service;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.webconfiguration.MaintenanceFilter;
import lombok.Getter;

/**
 * Service that manage maintenance mode enablement.
 */
@Slf4j
@Service
public class MaintenanceModeService {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO dao;
    @Inject
    private MaintenanceFilter maintenanceFilter;

    @Getter
    private MaintenanceModeState maintenanceModeState = null;

    @PostConstruct
    public void init() {
        // Load the maintenance mode state from elasticsearch if any
        maintenanceFilter.setMaintenanceModeService(this);
        maintenanceModeState = dao.findById(MaintenanceModeState.class, MaintenanceModeState.MMS_ID);
        if (maintenanceModeState != null) {
            log.info("Maintenance mode is enabled");
        }
    }

    @PreDestroy
    public void destroy() {
        maintenanceFilter.setMaintenanceModeService(null);
    }

    public boolean isMaintenanceModeEnabled() {
        return maintenanceModeState != null;
    }

    /**
     * Enable the maintenance mode.
     * 
     * @param user Name of the user that enabled the maintenance mode.
     */
    public synchronized void enable(String user) {
        if (isMaintenanceModeEnabled()) {
            throw new AlreadyExistException("Maintenance mode is already enabled.");
        }

        log.info("Maintenance mode is enabled");

        this.maintenanceModeState = new MaintenanceModeState();
        this.maintenanceModeState.setUser(user);
        this.maintenanceModeState.setUserTriggered(true);
        this.maintenanceModeState.setProgressPercent(1);
        this.maintenanceModeState.setLog(Lists.newArrayList(new MaintenanceLog(user, "Maintenance operation started on alien4cloud.")));

        this.dao.save(maintenanceModeState);
    }

    /**
     * Update the current state of the maintenance mode.
     * 
     * @param user Name of the user that performed the update.
     * @param message A message to update the maintenance.
     * @param progressPercentage The new percentage of the maintenance.
     */
    public synchronized void update(String user, String message, Integer progressPercentage) {
        if (maintenanceModeState == null) {
            throw new NotFoundException("Maintenance mode is not enabled.");
        }

        this.maintenanceModeState.getLog().add(new MaintenanceLog(user, message));
        if (progressPercentage != null) {
            this.maintenanceModeState.setProgressPercent(progressPercentage);
        }

        this.dao.save(maintenanceModeState);
    }

    /**
     * Disable maintenance mode.
     */
    public synchronized void disable() {
        if (maintenanceModeState == null) {
            throw new NotFoundException("Maintenance mode is not enabled.");
        }

        this.dao.delete(MaintenanceModeState.class, maintenanceModeState.getId());
        this.maintenanceModeState = null;

        log.info("Maintenance mode is disabled");
    }
}