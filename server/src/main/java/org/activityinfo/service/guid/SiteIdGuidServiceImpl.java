package org.activityinfo.service.guid;

import com.google.inject.Inject;
import org.activityinfo.model.legacy.KeyGenerator;
import org.activityinfo.server.database.hibernate.EntityManagerProvider;
import org.activityinfo.server.database.hibernate.entity.Activity;
import org.activityinfo.server.database.hibernate.entity.Location;
import org.activityinfo.server.database.hibernate.entity.Partner;
import org.activityinfo.server.database.hibernate.entity.Site;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;

public class SiteIdGuidServiceImpl implements SiteIdGuidService {
    private final EntityManagerProvider entityManagerProvider;

    @Inject
    public SiteIdGuidServiceImpl(EntityManagerProvider entityManagerProvider) {
        this.entityManagerProvider = entityManagerProvider;
    }

    @Override
    public int getSiteId(int activityId, String guid) {
        boolean commit = false;
        EntityManager entityManager = entityManagerProvider.get();
        EntityTransaction entityTransaction = entityManager.getTransaction();
        entityTransaction.begin();

        try {
            return entityManager.createQuery("from Site as site where site.siteGuid = :guid", Site.class).
                    setParameter("guid", guid).getSingleResult().getId();
        } catch (NoResultException noResultException) { // This situation is not actually exceptional here
            int formInstanceId = new KeyGenerator().generateInt();
            Activity activity = entityManager.find(Activity.class, activityId);
            Location location = activity.getLocationType().getLocations().iterator().next();
            Partner partner = activity.getDatabase().getPartners().iterator().next();

            Site site = new Site();
            site.setId(formInstanceId);
            site.setActivity(activity);
            site.setLocation(location);
            site.setPartner(partner);
            site.setSiteGuid(guid);

            entityManager.persist(site);
            commit = true;
            return formInstanceId;
        } finally {
            if (commit) {
                entityTransaction.commit();
            } else {
                entityTransaction.rollback();
            }

            entityManager.close();
        }
    }
}
