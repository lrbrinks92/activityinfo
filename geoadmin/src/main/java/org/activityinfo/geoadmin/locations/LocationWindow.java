package org.activityinfo.geoadmin.locations;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.activityinfo.geoadmin.GeoAdmin;
import org.activityinfo.geoadmin.ImportSource;
import org.activityinfo.geoadmin.model.*;
import org.activityinfo.geoadmin.util.GenericTableModel;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import java.util.prefs.Preferences;

public class LocationWindow extends JFrame {

	private Preferences prefs = Preferences.userNodeForPackage(GeoAdmin.class);

	private ActivityInfoClient client;
	private LocationType locationType;
	private Country country;

	private List<AdminLevel> levels;

	public LocationWindow(JFrame parent, Country country, LocationType locationType, ActivityInfoClient client) {
		super(locationType.getName());
		setSize(650, 350);
		setLocationRelativeTo(parent);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		this.client = client;
		this.country = country;
		this.locationType = locationType;
		this.levels = client.getAdminLevels(country);
		
		addToolbar();
		addTable();
	}

	private void addTable() {

		List<Location> locations = Lists.newArrayList(); //  client.getLocations(locationType.getId());

		GenericTableModel.Builder<Location> model = GenericTableModel.newModel(locations);
		model.addColumn("name", String.class, new Function<Location, String>() {
			@Override
			public String apply(Location location) {
				return location.getName();
			}
		});
		for(final AdminLevel level : levels) {
			model.addColumn(level.getName(), String.class, new Function<Location, String>() {

				@Override
				public String apply(Location location) {
					AdminEntity adminEntity = location.getAdminEntities().get(level.getId());
					if(adminEntity != null) {
						return adminEntity.getName();
					} else {
						return null;
					}
				}
			});
		}		

		JTable table = new JTable(model.build());

		JScrollPane scrollPane = new JScrollPane(table);

		getContentPane().add(scrollPane, BorderLayout.CENTER);
	}

	private void addToolbar() {

		Button importButton = new Button("Import");
		importButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				doImport();
			}

		});

		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		toolBar.add(importButton);

		getContentPane().add(toolBar, BorderLayout.PAGE_START);
	}


	private void doImport()  {
		File importFile = selectFile();
		if(importFile == null) {
			return;
		}
		
		try {
			ImportSource source = new ImportSource(importFile);
			LocationImportWindow window = new LocationImportWindow(this, client, locationType.getId(), levels, source);
			window.setVisible(true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}

	protected File selectFile() {
		File initialDir = new File(prefs.get("import_loc_dir_" + country.getCode(),
				prefs.get("import_loc_dir", "")));

		JFileChooser chooser = new JFileChooser(initialDir);
		chooser.setFileFilter(new FileNameExtensionFilter("Shapefiles", "shp"));
		int returnVal = chooser.showOpenDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File file = chooser.getSelectedFile();
			prefs.put("import_loc_dir_" + country.getCode(), file.getParent());
			prefs.put("import_loc_dir", file.getParent());
			return file;
		}
		return null;
	}
}
