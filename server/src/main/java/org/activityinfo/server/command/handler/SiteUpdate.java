package org.activityinfo.server.command.handler;

import org.activityinfo.legacy.shared.command.GetSites;
import org.activityinfo.legacy.shared.command.result.SiteResult;
import org.activityinfo.legacy.shared.exception.CommandException;
import org.activityinfo.legacy.shared.impl.ActivityFormCache;
import org.activityinfo.legacy.shared.model.*;
import org.activityinfo.legacy.shared.util.JsonUtil;
import org.activityinfo.server.command.DispatcherSync;
import org.activityinfo.server.command.handler.crud.PropertyMap;
import org.activityinfo.server.database.hibernate.entity.*;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;


public class SiteUpdate {

    private static final String ACTIVITY_ID = "activityId";
    private static final String REPORTING_PERIOD_ID = "reportingPeriodId";
    private static final String PARTNER_ID = "partnerId";
    private static final String PROJECT_ID = "projectId";
    private static final String LOCATION_ID = "locationId";

    private final EntityManager entityManager;
    private final PermissionOracle permissionOracle;
    private final DispatcherSync dispatcher;
    
    
    private static final Logger LOGGER = Logger.getLogger(SiteUpdate.class.getName());

    @Inject
    public SiteUpdate(EntityManager entityManager, PermissionOracle permissionOracle, DispatcherSync dispatcher) {
        this.entityManager = entityManager;
        this.permissionOracle = permissionOracle;
        this.dispatcher = dispatcher;
    }

    public void createOrUpdateSite(User user, int siteId, PropertyMap propertyMap) throws CommandException {

        Site site = entityManager.find(Site.class, siteId);
        
        if(site == null) {
            createSite(user, siteId, propertyMap);

        } else {
            updateSite(user, site, propertyMap);
        }
    }

    private void createSite(User user, int siteId, PropertyMap propertyMap) {
        Activity activity = activityReference(propertyMap);
        Partner partner = partnerReference(propertyMap);

        permissionOracle.assertEditSiteAllowed(user, activity, partner);
        
        long newVersion = incrementVersion(activity);
        
        Site site = new Site();
        site.setId(siteId);
        site.setActivity(activity);
        site.setPartner(partner);
        site.setVersion(newVersion);
        applyProperties(site, propertyMap);
        validateProperties(site);

        entityManager.persist(site);
        updateAttributeValues(site, propertyMap);

        if(site.getActivity().getReportingFrequency() == ActivityFormDTO.REPORT_ONCE &&
            propertyMap.containsKey(REPORTING_PERIOD_ID)) {

            newReportingPeriod(site, propertyMap);
        }
        
        persistHistory(site, true, user, propertyMap);
    }

    private void updateSite(User user, Site site, PropertyMap changes) {
        permissionOracle.assertEditAllowed(site, user);
        
        // Before do anything else, increment the activity version and flush
        // so that we establish essentially a lock on this region
        site.setVersion(incrementVersion(site.getActivity()));

        ensureInitialHistoryEntry(site, user);
        
        if(changes.containsKey(PARTNER_ID)) {
            Partner newPartner = partnerReference(changes);
            permissionOracle.assertEditSiteAllowed(user, site.getActivity(), newPartner);
            site.setPartner(newPartner);
        }

        applyProperties(site, changes);
        updateAttributeValues(site, changes);

        if(site.getActivity().getReportingFrequency() == ActivityFormDTO.REPORT_ONCE) {
            updateUniqueReportingPeriod(site, changes);
        }

        validateProperties(site);

        persistHistory(site, false, user, changes);
    }

    private long incrementVersion(Activity activity) {
        long newVersion = activity.incrementSiteVersion();
        entityManager.flush();
        return newVersion;
    }

    private void updateUniqueReportingPeriod(Site site, PropertyMap propertyMap) {
        ReportingPeriod period;
        if(!site.getReportingPeriods().isEmpty()) {
            period = site.getReportingPeriods().iterator().next();
        } else {
            period = new ReportingPeriod(site);
        }
        period.setDate1(site.getDate1());
        period.setDate2(site.getDate2());        
    
        updateIndicatorValues(period, propertyMap);
    }

    private Activity activityReference(PropertyMap map) {
        int activityId = map.getRequiredInt(ACTIVITY_ID);
        Activity activity = entityManager.find(Activity.class, activityId);
        if(activity == null) {
            throw new CommandException("No activity with id " + activityId);
        }
        return activity;
    }

    private void newReportingPeriod(Site site, PropertyMap propertyMap) {
        ReportingPeriod period = new ReportingPeriod(site);
        period.setDate1(propertyMap.getDate("date1"));
        period.setDate2(propertyMap.getDate("date2"));
        period.setId(propertyMap.getRequiredInt(REPORTING_PERIOD_ID));
        entityManager.persist(period);

        updateIndicatorValues(period, propertyMap);

    }

    private void validateProperties(Site site) {
        if(site.getLocation() == null) {
            throw new CommandException("LocationId is required.");
        }
    }

    private void applyProperties(Site site, PropertyMap propertyMap) {
        if(propertyMap.containsKey(LOCATION_ID)) {
            site.setLocation(locationReference(propertyMap));
        } 
        if(propertyMap.containsKey("date1")) {
            site.setDate1(propertyMap.getDate("date1"));
        }
        if(propertyMap.containsKey("date2")) {
            site.setDate2(propertyMap.getDate("date2"));
        }
        if(propertyMap.containsKey("comments")) {
            site.setComments(propertyMap.getOptionalString("comments"));
        }
        if(propertyMap.containsKey(PROJECT_ID)) {
            if (propertyMap.get(PROJECT_ID) == null) {
                site.setProject(null);
            } else {
                Project project = entityManager.find(Project.class, propertyMap.getRequiredInt(PROJECT_ID));
                site.setProject(project);
            }
        }
    }

    private Location locationReference(PropertyMap cmd) {
        Location location = entityManager.find(Location.class, cmd.getRequiredInt(LOCATION_ID));
        if (location == null) {
            throw new CommandException("No location with id " + cmd.getRequiredInt(LOCATION_ID));
        }
        return location;
    }

    private Partner partnerReference(PropertyMap propertyMap) {
        int partnerId = propertyMap.getRequiredInt(PARTNER_ID);
        Partner partner = entityManager.find(Partner.class, partnerId);
        if(partner == null) {
            throw new CommandException("No partner with id " + partnerId);
        }
        return partner;
    }
    
    private void updateIndicatorValues(ReportingPeriod period, PropertyMap propertyMap) {
        
        Map<Integer, IndicatorValue> existingValues = new HashMap<>();
        for (IndicatorValue value : period.getIndicatorValues()) {
            existingValues.put(value.getIndicator().getId(), value);
        }
        
        for (String property : propertyMap.getNames()) {
            if(property.startsWith(IndicatorDTO.PROPERTY_PREFIX)) {

                int indicatorId = IndicatorDTO.indicatorIdForPropertyName(property);
                Object value = propertyMap.get(property);

                IndicatorValue valueEntity = existingValues.get(indicatorId);

                if (valueEntity == null) {
                    if (value != null) {
                        Indicator indicator = entityManager.getReference(Indicator.class, indicatorId);
                        valueEntity = new IndicatorValue(period, indicator);
                        valueEntity.setValue(value);
                        existingValues.put(indicatorId, valueEntity);
                        entityManager.persist(valueEntity);
                    }
                } else {
                    valueEntity.setValue(value);
                }
            }            
        }
    }
    
    private void updateAttributeValues(Site site, PropertyMap propertyMap) {


        Map<Integer, AttributeValue> existingValues = new HashMap<>();
        for (AttributeValue value : site.getAttributeValues()) {
            existingValues.put(value.getAttribute().getId(), value);
        }

        for (String property : propertyMap.getNames()) {
            if (property.startsWith(AttributeDTO.PROPERTY_PREFIX)) {

                int id = AttributeDTO.idForPropertyName(property);
                Boolean value = propertyMap.get(property);
                
                AttributeValue valueEntity = existingValues.get(id);
                if(valueEntity == null) {
                    if(value == Boolean.TRUE) {
                        valueEntity = new AttributeValue(site, entityManager.getReference(Attribute.class, id), true);
                        entityManager.persist(valueEntity);
                    } 
                } else {
                    valueEntity.setValue(value == Boolean.TRUE);
                }
            }
        }
    }

    public void ensureInitialHistoryEntry(Site site, User user) {
         long count = entityManager.createQuery("select count(h.id) from " +
                 "SiteHistory h where h.site = :site", Long.class)
                    .setParameter("site", site)
                    .getSingleResult();
        
        if (count == 0) {
            // update, but first entry -> repair history by adding baseline
            // record with complete site json
            LOGGER.fine("site is not new, but history was empty. Adding baseline record..");
            SiteResult siteResult = dispatcher.execute(GetSites.byId(site.getId()));
            SiteDTO siteDTO = siteResult.getData().get(0);
            String completeProperties = JsonUtil.encodeMap(siteDTO.getProperties()).toString();

            SiteHistory baseline = new SiteHistory();
            baseline.setSite(site);
            baseline.setUser(user);
            baseline.setJson(completeProperties);
            baseline.setTimeCreated(new Date().getTime());
            baseline.setInitial(false);
            entityManager.persist(baseline);
        }
    }

    public void persistHistory(Site site, boolean isNew, User user, PropertyMap changes) {
        SiteHistory history = new SiteHistory();
        history.setSite(site);
        history.setUser(user);
        String json = JsonUtil.encodeMap(changes.asMap()).toString();
        history.setJson(json);
        history.setTimeCreated(System.currentTimeMillis());
        history.setInitial(isNew);

        entityManager.persist(history);
    }

}
