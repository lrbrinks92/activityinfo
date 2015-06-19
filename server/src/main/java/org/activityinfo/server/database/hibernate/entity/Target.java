package org.activityinfo.server.database.hibernate.entity;

/*
 * #%L
 * ActivityInfo Server
 * %%
 * Copyright (C) 2009 - 2013 UNICEF
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import org.hibernate.annotations.BatchSize;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Target implements Serializable, ReallyDeleteable {

    private int id;
    private String name;
    private Date date1;
    private Date date2;
    private Project project;
    private Partner partner;
    private AdminEntity adminEntity;
    private UserDatabase userDatabase;
    private Set<TargetValue> values = new HashSet<TargetValue>(0);

    public Target() {
        super();
    }

    @Id @GeneratedValue(strategy = GenerationType.AUTO) @Column(name = "targetId", unique = true, nullable = false)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @PreRemove
    public void preRemove() {
        // bi-directional association: removing target that are part of an association, we have to clear parents first
        // or otherwise get "un-scheduling entity deletion" from org.hibernate.event.internal.DefaultPersistEventListener
        if (userDatabase != null) {
            userDatabase.getTargets().remove(this);
        }
        if (adminEntity != null) {
            adminEntity.getTargets().remove(this);
        }
        if (partner !=null) {
            partner.getTargets().remove(this);
        }
        if (project != null) {
            project.getTargets().remove(this);
        }
    }

    @Column(name = "Name", nullable = false, length = 255)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getDate1() {
        return date1;
    }

    public void setDate1(Date date1) {
        this.date1 = date1;
    }

    public Date getDate2() {
        return date2;
    }

    public void setDate2(Date date2) {
        this.date2 = date2;
    }

    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "ProjectId")
    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "PartnerId")
    public Partner getPartner() {
        return partner;
    }

    public void setPartner(Partner partner) {
        this.partner = partner;
    }

    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "AdminEntityId")
    public AdminEntity getAdminEntity() {
        return adminEntity;
    }

    public void setAdminEntity(AdminEntity adminEntity) {
        this.adminEntity = adminEntity;
    }

    public void setUserDatabase(UserDatabase userDatabase) {
        this.userDatabase = userDatabase;
    }

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "DatabaseId", nullable = false)
    public UserDatabase getUserDatabase() {
        return userDatabase;
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "target")
    @BatchSize(size = 200)
    public Set<TargetValue> getValues() {
        return values;
    }

    public void setValues(Set<TargetValue> values) {
        this.values = values;
    }

    @Override
    public void deleteReferences() {
        values.clear();
    }
}
