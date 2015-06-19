package org.activityinfo.legacy.shared.adapter;

import com.bedatadriven.rebar.time.calendar.LocalDate;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.activityinfo.legacy.client.Dispatcher;
import org.activityinfo.legacy.shared.adapter.bindings.SiteBinding;
import org.activityinfo.legacy.shared.adapter.bindings.SiteBindingFactory;
import org.activityinfo.legacy.shared.command.*;
import org.activityinfo.legacy.shared.command.result.BatchResult;
import org.activityinfo.legacy.shared.command.result.CommandResult;
import org.activityinfo.legacy.shared.command.result.LocationResult;
import org.activityinfo.legacy.shared.model.*;
import org.activityinfo.model.form.FormInstance;
import org.activityinfo.model.legacy.CuidAdapter;
import org.activityinfo.model.legacy.KeyGenerator;
import org.activityinfo.promise.Promise;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Persists a FormInstance as a Site
 */
public class SitePersister {

    private final Dispatcher dispatcher;

    public SitePersister(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public Promise<Void> persist(final FormInstance siteInstance) {

        int activityId = CuidAdapter.getLegacyIdFromCuid(siteInstance.getClassId());
        final Promise<SchemaDTO> schemaPromise = dispatcher.execute(new GetSchema());
        final Promise<SiteBinding> siteBinding = dispatcher.execute(new GetActivityForm(activityId))
                .then(new SiteBindingFactory());
        return Promise.waitAll(schemaPromise, siteBinding).
                join(new Function<Void, Promise<LocationResult>>() {
                    @Override
                    public Promise<LocationResult> apply(@Nullable Void input) {
                        GetLocations query = new GetLocations();
                        UserDatabaseDTO databaseById = schemaPromise.get().getDatabaseById(siteBinding.get().getActivity().getDatabaseId());
                        for (LocationTypeDTO locationTypeDTO : databaseById.getCountry().getLocationTypes()) {
                            if (locationTypeDTO.isNationwide()) {
                                query.setLocationTypeId(locationTypeDTO.getId());
                                break;
                            }
                        }
                        // yuriyz : dummy result in case we don't need location for fallback
                        // it's tricky but it produce even worse code if load location inside persist() method where it is really needed
                        if (query.getLocationTypeId() == null) {
                            return Promise.resolved(new LocationResult(Lists.newArrayList(new LocationDTO())));
                        }
                        return dispatcher.execute(query);
                    }
                }).
                join(new Function<LocationResult, Promise<Void>>() {
                    @Override
                    public Promise<Void> apply(LocationResult locationResult) {
                        return persist(siteBinding.get(), siteInstance, locationResult.getData().get(0)).thenDiscardResult();
                    }
                });
    }

    private Promise<? extends CommandResult> persist(SiteBinding siteBinding, FormInstance instance, LocationDTO locationDTO) {

        Map<String, Object> siteProperties = siteBinding.toChangePropertyMap(instance);
        siteProperties.put("activityId", siteBinding.getActivity().getId());
        if (siteProperties.get("reportingPeriodId") == null) {  // indicators are not saved if report id is not set
            siteProperties.put("reportingPeriodId", new KeyGenerator().generateInt());
        }

        if (siteProperties.get("partnerId") == null) {
            siteProperties.put("partnerId", siteBinding.getDefaultPartnerId());
        }

        if (siteBinding.getLocationType().isNationwide()) {
            siteProperties.put("locationId", siteBinding.getLocationType().getId());
        } else if (!siteProperties.containsKey("locationId")) { // set the locationtypeid to nationwide if the user deletes the location field
            siteProperties.put("locationId", locationDTO.getId());
        }

        // default values for start and end dates (if corresponding form field were removed)
        if (!siteProperties.containsKey("date1") || siteProperties.get("date1") == null) {
            siteProperties.put("date1", new LocalDate());
        }
        if (!siteProperties.containsKey("date2") || siteProperties.get("date2") == null) {
            siteProperties.put("date2", new LocalDate());
        }

        final CreateSite createSite = new CreateSite(siteProperties);

        if (siteBinding.getLocationType().isAdminLevel()) {
            // we need to create the dummy location as well
            Promise<Command> createLocation = Promise.resolved(siteBinding.getAdminEntityId(instance))
                    .join(new FetchEntityFunction())
                    .then(new CreateDummyLocation(createSite.getLocationId(),
                            siteBinding.getLocationType()));

            return createLocation.join(new Function<Command, Promise<BatchResult>>() {
                @Nullable
                @Override
                public Promise<BatchResult> apply(@Nullable Command createLocation) {
                    return dispatcher.execute(new BatchCommand(createLocation, createSite));
                }
            });

        } else {
            return dispatcher.execute(createSite);
        }
    }

    private class FetchEntityFunction implements Function<Integer, Promise<List<AdminEntityDTO>>> {

        @Nullable
        @Override
        public Promise<List<AdminEntityDTO>> apply(@Nullable Integer input) {
            GetAdminEntities query = new GetAdminEntities().setEntityId(input);

            Promise<AdminEntityDTO> entity = dispatcher.execute(query)
                    .then(new SingleListResultAdapter<AdminEntityDTO>());

            Promise<List<AdminEntityDTO>> parents = entity.join(new FetchParentsFunction());

            return Promise.prepend(entity, parents);
        }
    }

    private class FetchParentsFunction implements Function<AdminEntityDTO, Promise<List<AdminEntityDTO>>> {

        @Override
        public Promise<List<AdminEntityDTO>> apply(AdminEntityDTO input) {
            if (input.getParentId() == null) {
                return Promise.resolved(Collections.<AdminEntityDTO>emptyList());
            } else {
                return Promise.resolved(input.getParentId()).join(new FetchEntityFunction());
            }
        }
    }

    private class CreateDummyLocation implements Function<List<AdminEntityDTO>, Command> {

        private final LocationTypeDTO locationType;
        private int locationId;

        private CreateDummyLocation(int locationId, LocationTypeDTO locationType) {
            this.locationType = locationType;
            this.locationId = locationId;
        }

        @Override
        public CreateLocation apply(List<AdminEntityDTO> entities) {

            AdminEntityDTO entity = entities.get(0);
            Preconditions.checkState(entity.getLevelId() == locationType.getBoundAdminLevelId());

            Map<String, Object> properties = new HashMap<>();
            properties.put("id", locationId);
            properties.put("locationTypeId", locationType.getId());
            properties.put("name", entity.getName());

            for (AdminEntityDTO parent : entities) {
                properties.put(AdminLevelDTO.getPropertyName(parent.getLevelId()), parent.getId());
            }

            return new CreateLocation(properties);
        }
    }
}
