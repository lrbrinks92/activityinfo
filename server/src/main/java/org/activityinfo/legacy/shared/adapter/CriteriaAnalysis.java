package org.activityinfo.legacy.shared.adapter;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.activityinfo.core.shared.criteria.*;
import org.activityinfo.model.legacy.CuidAdapter;
import org.activityinfo.model.resource.ResourceId;

import java.util.List;
import java.util.Set;

/**
 * Created by alex on 3/15/14.
 */
public class CriteriaAnalysis extends CriteriaVisitor {

    /**
     * Instances must be a subclass of all of these FormClasses
     */
    private final Set<ResourceId> classCriteria = Sets.newHashSet();

    private final Set<ResourceId> parentCriteria = Sets.newHashSet();

    private boolean rootOnly = false;
    private boolean classUnion = true;
    private boolean querySiteById = false;

    /**
     * Must be one of these ids
     */
    private final Multimap<Character, Integer> ids = HashMultimap.create();

    public ResourceId getParentCriteria() {
        return parentCriteria.iterator().next();
    }

    public boolean isRootOnly() {
        return rootOnly;
    }

    @Override
    public void visitClassCriteria(ClassCriteria criteria) {
        classCriteria.add(criteria.getClassId());
    }

    @Override
    public void visitInstanceIdCriteria(IdCriteria criteria) {
        // this is implicitly a union criteria
        // separate the instances out into domains
        for (ResourceId id : criteria.getInstanceIds()) {
            Preconditions.checkNotNull(id, "ids cannot be null");

            char domain = id.getDomain();
            if (domain != CuidAdapter.ACTIVITY_CATEGORY_DOMAIN) {
                ids.put(domain, CuidAdapter.getLegacyIdFromCuid(id));
            }

            if (domain == CuidAdapter.SITE_DOMAIN) {
                querySiteById = true;
            } else if (querySiteById) {
                throw new RuntimeException("Id domain is not SITE_DOMAIN while querySiteById flag is set to true." +
                        "All ids must be site ids. It's not allowed to mix ids");
            }
        }
    }

    @Override
    public void visitParentCriteria(ParentCriteria criteria) {
        if (criteria.selectsRoot()) {
            rootOnly = true;
        } else {
            parentCriteria.add(criteria.getParentId());
        }
    }

    @Override
    public void visitIntersection(CriteriaIntersection intersection) {
        // A ∩ (B ∩ C) = A ∩ B ∩ C
        for (Criteria criteria : intersection) {
            criteria.accept(this);
        }
    }

    @Override
    public void visitUnion(CriteriaUnion criteriaUnion) {
        classUnion = true; // todo temp fix! - in general wrong approach, will work in flat case only!
        for (Criteria criteria : criteriaUnion.getElements()) {
            if (classUnion && !(criteria instanceof ClassCriteria)) {
                classUnion = false;
            }
            criteria.accept(this);
        }
    }

    public boolean isEmptySet() {
        if (classCriteria.size() > 1 && !classUnion) {
            // a single instance cannot (at this time) be a member of more than one
            // class, so the result of this query is logically the empty set
            return true;
        }

        if (parentCriteria.size() > 1 || (rootOnly && !parentCriteria.isEmpty())) {
            // likewise, a single instance cannot be a child of multiple parents, so
            // the result of this query is logically the empty set
            return true;
        }

        return false;
    }

    public boolean isRestrictedToSingleClass() {
        return classCriteria.size() == 1;
    }

    public boolean isRestrictedByUnionOfClasses() {
        return classUnion && !classCriteria.isEmpty();
    }

    public boolean isRestrictedById() {
        return !ids.isEmpty();
    }

    public boolean isLocationQuery() {
        return isRestrictedToSingleClass() && getClassRestriction().getDomain() == CuidAdapter.LOCATION_TYPE_DOMAIN;
    }

    public boolean isSiteQueryByClass() {
        return isRestrictedToSingleClass() && getClassRestriction().getDomain() == CuidAdapter.ACTIVITY_DOMAIN;
    }

    public boolean isQuerySiteById() {
        return querySiteById;
    }

    public boolean isAncestorQuery() {
        return rootOnly || !parentCriteria.isEmpty();
    }

    public ResourceId getClassRestriction() {
        return classCriteria.iterator().next();
    }

    public Set<ResourceId> getClassCriteria() {
        return classCriteria;
    }

    public Multimap<Character, Integer> getIds() {
        return ids;
    }

    public List<Integer> getIds(char domain) {
        return Lists.newArrayList(ids.get(domain));
    }

    public static CriteriaAnalysis analyze(Criteria criteria) {
        CriteriaAnalysis analysis = new CriteriaAnalysis();
        criteria.accept(analysis);
        return analysis;
    }
}
