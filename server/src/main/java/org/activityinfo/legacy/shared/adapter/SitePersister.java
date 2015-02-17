package org.activityinfo.legacy.shared.adapter;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import org.activityinfo.legacy.client.Dispatcher;
import org.activityinfo.legacy.shared.adapter.bindings.SiteBinding;
import org.activityinfo.legacy.shared.adapter.bindings.SiteBindingFactory;
import org.activityinfo.legacy.shared.command.BatchCommand;
import org.activityinfo.legacy.shared.command.Command;
import org.activityinfo.legacy.shared.command.CreateLocation;
import org.activityinfo.legacy.shared.command.CreateSite;
import org.activityinfo.legacy.shared.command.GetActivityForm;
import org.activityinfo.legacy.shared.command.GetAdminEntities;
import org.activityinfo.legacy.shared.command.result.BatchResult;
import org.activityinfo.legacy.shared.command.result.CommandResult;
import org.activityinfo.legacy.shared.model.AdminEntityDTO;
import org.activityinfo.legacy.shared.model.AdminLevelDTO;
import org.activityinfo.legacy.shared.model.LocationTypeDTO;
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

    public Promise<Void> persist(final FormInstance siteInstance,
                                 int locationId, int locationTypeId,
                                 double latitude, double longitude) {
        final Map<String, Object> properties = Maps.newHashMap();
        int activityId = CuidAdapter.getLegacyIdFromCuid(siteInstance.getClassId());

        properties.put("id", locationId);
        properties.put("locationTypeId", locationTypeId);
        properties.put("name", "Custom location");
        properties.put("latitude", latitude);
        properties.put("longitude", longitude);

        return dispatcher.execute(new GetActivityForm(activityId))
                .then(new SiteBindingFactory())
                .join(new Function<SiteBinding, Promise<Void>>() {
                    @Nullable @Override
                    public Promise<Void> apply(@Nullable SiteBinding binding) {
                        return persist(binding, siteInstance, new CreateLocation(properties)).thenDiscardResult();
                    }
                });
    }

    public Promise<Void> persist(final FormInstance siteInstance) {

        int activityId = CuidAdapter.getLegacyIdFromCuid(siteInstance.getClassId());
        return dispatcher.execute(new GetActivityForm(activityId))
                         .then(new SiteBindingFactory())
                         .join(new Function<SiteBinding, Promise<Void>>() {
                             @Nullable @Override
                             public Promise<Void> apply(@Nullable SiteBinding binding) {
                                 return persist(binding, siteInstance, null).thenDiscardResult();
                             }
                         });
    }

    private Promise<? extends CommandResult> persist(SiteBinding siteBinding,
                                                     FormInstance instance,
                                                     CreateLocation createLocation) {

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
        }

        if (createLocation != null) {
            siteProperties.put("locationId", createLocation.getLocationId());
            return dispatcher.execute(new BatchCommand(createLocation, new CreateSite(siteProperties)));
        } else if (siteBinding.getLocationType().isAdminLevel()) {
            final CreateSite createSite = new CreateSite(siteProperties);

            // we need to create the dummy location as well
            Promise<Command> promise = Promise.resolved(siteBinding.getAdminEntityId(instance))
                                                     .join(new FetchEntityFunction())
                                                     .then(new CreateDummyLocation(createSite.getLocationId(),
                                                             siteBinding.getLocationType()));

            return promise.join(new Function<Command, Promise<BatchResult>>() {
                @Nullable @Override
                public Promise<BatchResult> apply(@Nullable Command createLocation) {
                    return dispatcher.execute(new BatchCommand(createLocation, createSite));
                }
            });

        } else {
            return dispatcher.execute(new CreateSite(siteProperties));
        }
    }

    private class FetchEntityFunction implements Function<Integer, Promise<List<AdminEntityDTO>>> {

        @Nullable @Override
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
