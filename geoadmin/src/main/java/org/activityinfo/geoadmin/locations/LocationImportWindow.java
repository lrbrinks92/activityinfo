package org.activityinfo.geoadmin.locations;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Point;
import net.miginfocom.swing.MigLayout;
import org.activityinfo.geoadmin.ImportFeature;
import org.activityinfo.geoadmin.ImportSource;
import org.activityinfo.geoadmin.Sql;
import org.activityinfo.geoadmin.model.ActivityInfoClient;
import org.activityinfo.geoadmin.model.AdminEntity;
import org.activityinfo.geoadmin.model.AdminLevel;
import org.activityinfo.geoadmin.model.NewLocation;
import org.activityinfo.geoadmin.util.GenericTableModel;
import org.activityinfo.geoadmin.util.GenericTableModel.Builder;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class LocationImportWindow extends JFrame {

    private LocationImportForm importForm;
	private JTable table;
	private List<LocationFeature> locations;
	private ActivityInfoClient client;
	private List<AdminLevel> levels;
	private ImportSource source;
	private GenericTableModel<LocationFeature> tableModel;
	private int locationTypeId;

	public LocationImportWindow(JFrame parent, ActivityInfoClient client,
			int locationTypeId,
			List<AdminLevel> levels, ImportSource source) throws Exception {
        super("Import - " + source.getFile().getName());
        setSize(650, 350);
        setLocationRelativeTo(parent);

        this.client = client;
        this.levels = levels;
        this.source = source;
        this.locationTypeId = locationTypeId;
        importForm = new LocationImportForm(source, levels);
        importForm.guessLevelColumns(client);

        locations = createRows();
        tableModel = createTableModel();


        table = new JTable(tableModel);
//        table.getColumnModel().getColumn(ImportTableModel.PARENT_COLUMN).setCellEditor(
//            new DefaultCellEditor(parentComboBox));
//        table.getColumnModel().getColumn(ImportTableModel.ACTION_COLUMN).setCellEditor(
//            new DefaultCellEditor(actionComboBox));
        table.setDefaultRenderer(Object.class,
            new LocationImportTableCellRenderer(tableModel));
        table.setAutoCreateRowSorter(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                onSelectionChanged(e);
            }
        });

        JLabel countLabel = new JLabel(source.getFeatureCount() + " features");

        JPanel panel = new JPanel(new MigLayout("fill"));
        panel.add(importForm, "wrap");
        panel.add(new JScrollPane(table,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), "span, wrap,grow");

        panel.add(countLabel);

        getContentPane().add(createToolBar(), BorderLayout.PAGE_START);
        getContentPane().add(panel, BorderLayout.CENTER);
    }

    private List<LocationFeature> createRows() {
        Random random = new Random();
		List<LocationFeature> locations = Lists.newArrayList();
		for(int i=0;i!=source.getFeatureCount();++i) {
			ImportFeature feature = source.getFeatures().get(i);
			System.out.println("Matching " + feature);
			LocationFeature location = new LocationFeature(feature);
            location.setId(random.nextInt(Integer.MAX_VALUE));
			locations.add(location);
		}
		return locations;
	}

	private GenericTableModel<LocationFeature> createTableModel() {

		Builder<LocationFeature> model = GenericTableModel.newModel(locations);
		for(final AdminLevel level : levels) {
			model.addColumn(level.getName(), String.class, new Function<LocationFeature, String>() {
				@Override
                public String apply(LocationFeature location) {
					AdminEntity entity = location.getEntities().get(level.getId());
					if(entity == null) {
						return null;
					}
					return entity.getName();
				}
			});
		}
		for(int i=0;i!=source.getAttributeCount();++i) {
			final int attributeindex = i;
			model.addColumn(source.getAttributes().get(i).getName().getLocalPart(), Object.class, new Function<LocationFeature, Object>() {
				@Override
                public Object apply(LocationFeature location) {
					return location.getFeature().getAttributeValue(attributeindex);
				}
			});
		}
		return model.build();
	}

	private void onSelectionChanged(ListSelectionEvent e) {
    }

    private JToolBar createToolBar() {
    	JButton matchButton = new JButton("Match Admin Levels");
    	matchButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				doMatch();
			}
		});

    	JButton updateButton = new JButton("Import");
        updateButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                doImport();
            }
        });


        JButton updateSQLButton = new JButton("Generate Import SQL");
        updateSQLButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    doImportSql();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(matchButton);
        toolBar.add(updateButton);
        toolBar.add(updateSQLButton);
        toolBar.addSeparator();

        return toolBar;
    }

	private void doMatch() {
		LocationAdminMatcher matcher = new LocationAdminMatcher(client, levels);
		for(AdminLevel level : levels) {
			matcher.setLevelAttribute(level, importForm.getLevelAttributeIndex(level));
		}
		for(LocationFeature location : locations) {
			location.getEntities().clear();
			for(AdminEntity entity : matcher.forFeature(location.getFeature())) {
				location.getEntities().put(entity.getLevelId(), entity);
			}
		}
		tableModel.fireTableDataChanged();
	}

    private void doImportSql() throws IOException {

        int nameIndex = importForm.getNameAttributeIndex();
        int codeIndex = importForm.getCodeAttributeIndex();

        File locationsFile = File.createTempFile("location", ".sql");

        try(FileWriter writer = new FileWriter(locationsFile)) {

            writer.append("BEGIN;\n");
            writer.append("INSERT INTO location (LocationID, LocationTypeID, Name, Axe, X, Y, timeEdited) VALUES");

            boolean needsComma = false;
            for (LocationFeature location : locations) {

                Point point = location.getPoint();

                writer.append(needsComma ? ",\n" : "\n");
                writer.append(String.format("(%d, %d, %s, %s, %f, %f, %d)",
                        location.getId(),
                        locationTypeId,
                        Sql.quote(truncate(location.getFeature().getAttributeStringValue(nameIndex))),
                        Sql.quote(truncate(location.getFeature().getAttributeStringValue(codeIndex))),
                        point.getX(),
                        point.getY(),
                        new Date().getTime()));

                needsComma = true;
            }

            writer.append(";\n");
            writer.append("INSERT INTO locationadminlink (LocationID, AdminEntityID) VALUES");

            needsComma = false;

            for (LocationFeature location : locations) {
                for (AdminEntity entity : location.getEntities().values()) {

                    writer.append(needsComma ? "\n," : "\n");
                    writer.append(String.format("(%d, %d)",
                            location.getId(),
                            entity.getId()));

                    needsComma = true;
                }
            }
            writer.append(";");
            writer.append("COMMIT;\n");
        }

        System.out.println("Wrote to " + locationsFile.getAbsolutePath());
    }

	private void doImport() {

		int nameIndex = importForm.getNameAttributeIndex();

		List<NewLocation> newLocations = Lists.newArrayList();
		for(LocationFeature location : locations) {

			Point point = location.getPoint();

			NewLocation newLocation = new NewLocation();
			newLocation.setName(truncate(location.getFeature().getAttributeStringValue(nameIndex)));
			newLocation.setLongitude(point.getX());
			newLocation.setLatitude(point.getY());

			for(AdminEntity entity : location.getEntities().values()) {
				newLocation.getAdminEntityIds().add(entity.getId());
			}

			newLocations.add(newLocation);
		}

		client.postNewLocations(locationTypeId, newLocations);

		setVisible(false);
	}

    private String truncate(String name) {
        if(name.length() < 50) {
            return name;
        }
        return name.substring(0, 50);
    }
}
