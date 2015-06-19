package org.activityinfo.server.endpoint.kml;

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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.activityinfo.legacy.shared.command.*;
import org.activityinfo.legacy.shared.model.ActivityDTO;
import org.activityinfo.legacy.shared.model.ActivityFormDTO;
import org.activityinfo.legacy.shared.model.SchemaDTO;
import org.activityinfo.legacy.shared.model.SiteDTO;
import org.activityinfo.server.authentication.BasicAuthentication;
import org.activityinfo.server.command.DispatcherSync;
import org.activityinfo.server.database.hibernate.entity.User;
import org.activityinfo.server.endpoint.kml.xml.XmlBuilder;
import org.activityinfo.ui.client.page.entry.form.SiteRenderer;
import org.xml.sax.SAXException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Serves a KML (Google Earth) file containing the locations of all activities
 * that are visible to the user.
 * <p/>
 * Users are authenticated using Basic HTTP authentication, and will see a
 * prompt for their username (email) and password when they access from Google
 * Earth.
 *
 * @author Alex Bertram
 */
@Singleton
public class KmlDataServlet extends javax.servlet.http.HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(KmlDataServlet.class.getName());

    private final DispatcherSync dispatcher;
    private final BasicAuthentication authenticator;
    private final SiteRenderer siteRenderer;

    @Inject
    public KmlDataServlet(BasicAuthentication authenticator, DispatcherSync dispatcher) {

        this.authenticator = authenticator;
        this.dispatcher = dispatcher;
        this.siteRenderer = new SiteRenderer(new JreIndicatorValueFormatter());
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.getWriter();

        int activityId = Integer.valueOf(req.getParameter("activityId"));

        // Get Authorization header
        String auth = req.getHeader("Authorization");

        // Do we allow that user?
        User user = authenticator.doAuthentication(auth);

        if (user == null) {
            // Not allowed, or no password provided so report unauthorized
            res.setHeader("WWW-Authenticate", "BASIC realm=\"Utilisateurs authorises\"");
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        res.setContentType("application/vnd.google-earth.kml+xml");

        try {
            writeDocument(res.getWriter(), activityId);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "KML Rendering failed", e);
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    protected void writeDocument(PrintWriter out, int activityId) throws SAXException, TransformerConfigurationException {

        XmlBuilder xml = new XmlBuilder(new StreamResult(out));

        List<SiteDTO> sites = querySites(activityId);

        xml.startDocument();

        KMLNamespace kml = new KMLNamespace(xml);

        kml.startKml();

        ActivityFormDTO activity = dispatcher.execute(new GetActivityForm(activityId));
        kml.startDocument();

        kml.startStyle().at("id", "noDirectionsStyle");
        kml.startBalloonStyle();
        kml.text("$[description]");
        xml.close();
        xml.close();

        for (SiteDTO pm : sites) {

            if (pm.hasLatLong()) {

                kml.startPlaceMark();
                kml.styleUrl("#noDirectionsStyle");
                kml.name(pm.getLocationName());

                kml.startSnippet();
                xml.cdata(renderSnippet(activity, pm));
                xml.close(); // Snippet

                kml.startDescription();
                xml.cdata(renderDescription(activity, pm));
                xml.close(); // Description

                kml.startTimeSpan();
                if (pm.getDate1() != null) {
                    kml.begin(pm.getDate1().atMidnightInMyTimezone());
                    kml.end(pm.getDate2().atMidnightInMyTimezone());
                    xml.close(); // Timespan
                }

                kml.startPoint();
                kml.coordinates(pm.getLongitude(), pm.getLatitude());
                xml.close(); // Point

                xml.close(); // Placemark
            }
        }
        xml.close(); // Document
        xml.close(); // kml
        xml.endDocument();

    }

    private String renderSnippet(ActivityFormDTO activity, SiteDTO pm) {
        return activity.getName() + " à " + pm.getLocationName() + " (" + pm.getPartnerName() + ")";
    }

    private List<SiteDTO> querySites(int activityId) {

        Filter filter = new Filter();
        filter.addRestriction(DimensionType.Activity, activityId);

        return dispatcher.execute(new GetSites(filter)).getData();
    }

    private String renderDescription(ActivityFormDTO activity, SiteDTO site) {

        StringBuilder html = new StringBuilder();
        html.append(siteRenderer.renderLocation(site, activity));
        html.append(siteRenderer.renderSite(site, activity, true));
        return html.toString();
    }

}