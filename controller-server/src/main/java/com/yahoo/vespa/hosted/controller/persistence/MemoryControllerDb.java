// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.hosted.controller.persistence;

import com.yahoo.config.provision.ApplicationId;
import com.yahoo.vespa.hosted.controller.AlreadyExistsException;
import com.yahoo.vespa.hosted.controller.Application;
import com.yahoo.vespa.hosted.controller.NotExistsException;
import com.yahoo.vespa.hosted.controller.api.Tenant;
import com.yahoo.vespa.hosted.controller.api.identifiers.RotationId;
import com.yahoo.vespa.hosted.controller.api.identifiers.TenantId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A controller db implementation backed by in-memory structures. Useful for testing.
 * 
 * @author Stian Kristoffersen
 */
public class MemoryControllerDb extends ControllerDb {

    private final Map<TenantId, Tenant> tenants = new HashMap<>();
    private final Map<String, Application> applications = new HashMap<>();
    private final Map<RotationId, ApplicationId> rotationAssignments = new HashMap<>();

    @Override
    public void createTenant(Tenant tenant) {
        if (tenants.containsKey(tenant.getId())) {
            throw new AlreadyExistsException(tenant.getId());
        }
        tenants.put(tenant.getId(), tenant);
    }

    @Override
    public void updateTenant(Tenant tenant) {
        if (!tenants.containsKey(tenant.getId())) {
            throw new NotExistsException(tenant.getId());
        }
        tenants.put(tenant.getId(), tenant);
    }

    @Override
    public void deleteTenant(TenantId tenantId) {
        if (tenants.remove(tenantId) == null) {
            throw new NotExistsException(tenantId);
        }
    }

    @Override
    public Optional<Tenant> getTenant(TenantId tenantId) throws PersistenceException {
        return Optional.ofNullable(tenants.get(tenantId));
    }

    @Override
    public List<Tenant> listTenants() {
        return new ArrayList<>(tenants.values());
    }

    @Override
    public void store(Application application) {
        applications.put(path(application.id()), application);
    }

    @Override
    public void deleteApplication(ApplicationId applicationId) {
        applications.remove(path(applicationId));
    }

    @Override
    public Optional<Application> getApplication(ApplicationId applicationId) {
        return Optional.ofNullable(applications.get(path(applicationId)));
    }

    @Override
    public List<Application> listApplications() {
        return new ArrayList<>(applications.values());
    }

    @Override
    public List<Application> listApplications(TenantId tenantId) {
        return applications.values().stream()
                                    .filter(a -> a.id().tenant().value().equals(tenantId.id()))
                                    .collect(Collectors.toList());
    }

    @Override
    public Set<RotationId> getRotations() {
        return rotationAssignments.keySet();
    }

    @Override
    public Set<RotationId> getRotations(ApplicationId applicationId) {
        return rotationAssignments.entrySet().stream()
            .filter(entry -> entry.getValue().equals(applicationId))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }

    @Override
    public boolean assignRotation(RotationId rotationId, ApplicationId applicationId) {
        if (rotationAssignments.containsKey(rotationId)) {
            return false;
        } else {
            rotationAssignments.put(rotationId, applicationId);
            return true;
        }
    }

    @Override
    public Set<RotationId> deleteRotations(ApplicationId applicationId) {
        Set<RotationId> rotations = getRotations(applicationId);
        for (RotationId rotation : rotations) {
            rotationAssignments.remove(rotation);
        }
        return rotations;
    }

}
