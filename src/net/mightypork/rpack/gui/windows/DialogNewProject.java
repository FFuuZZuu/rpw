package net.mightypork.rpack.gui.windows;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.mightypork.rpack.App;
import net.mightypork.rpack.gui.Icons;
import net.mightypork.rpack.gui.helpers.CharInputListener;
import net.mightypork.rpack.gui.helpers.TextInputValidator;
import net.mightypork.rpack.gui.widgets.SimpleStringList;
import net.mightypork.rpack.project.Projects;
import net.mightypork.rpack.tasks.Tasks;

import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXTextField;
import org.jdesktop.swingx.JXTitledSeparator;


public class DialogNewProject extends RpwDialog {

	private List<String> options;

	private JXTextField field;
	private JButton buttonOK;
	private JButton buttonCancel;
	private SimpleStringList list;


	public DialogNewProject() {

		super(App.getFrame(), "New Project");

		Box hb;
		Box vb = Box.createVerticalBox();
		vb.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		vb.add(new JXTitledSeparator("Your Projects"));

		options = Projects.getProjectNames();

		vb.add(list = new SimpleStringList(options, true));
		list.list.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {

				String s = list.getSelectedValue();
				if (s != null) field.setText(s);
			}
		});

		vb.add(Box.createVerticalStrut(10));

		//@formatter:off
		hb = Box.createHorizontalBox();
			JXLabel label = new JXLabel("Name:");
			hb.add(label);
			hb.add(Box.createHorizontalStrut(5));
	
			field = new JXTextField();
			Border bdr = BorderFactory.createCompoundBorder(field.getBorder(), BorderFactory.createEmptyBorder(3,3,3,3));
			field.setBorder(bdr);
			field.requestFocusInWindow();
			
			CharInputListener listener = new CharInputListener() {
				
				@Override
				public void onCharTyped(char c) {
				
					String s = (field.getText() + c).trim();
					
					boolean ok = true;
					ok &= (s.length() > 0);
					ok &= !options.contains(s);
					
					buttonOK.setEnabled(ok);	
				}
			};
			
			field.addKeyListener(TextInputValidator.filenames(listener));
			
			
			hb.add(field);
		vb.add(hb);

		
		vb.add(Box.createVerticalStrut(8));

		
		hb = Box.createHorizontalBox();
			hb.add(Box.createHorizontalGlue());
	
			buttonOK = new JButton("Create", Icons.MENU_NEW);
			buttonOK.setEnabled(false);
			hb.add(buttonOK);
	
			hb.add(Box.createHorizontalStrut(5));
	
			buttonCancel = new JButton("Cancel", Icons.MENU_CANCEL);
			hb.add(buttonCancel);			
		vb.add(hb);
		//@formatter:on

		getContentPane().add(vb);

		prepareForDisplay();
	}


	@Override
	public void onClose() {

		// do nothing
	}


	@Override
	protected void addActions() {

		buttonOK.addActionListener(createListener);
		buttonCancel.addActionListener(closeListener);
	}

	private ActionListener createListener = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {

			String name = field.getText().trim();
			if (name.length() == 0) {
				Alerts.error(self(), "Invalid name", "Missing project name!");
			}

			if (options.contains(name)) {
				Alerts.error(self(), "Invalid name", "Project named \"" + name + "\" already exists!");
			} else {
				// OK name
				closeDialog();

				Alerts.loading(true);
				Projects.openNewProject(name);
				Tasks.taskOnProjectChanged();
				Alerts.loading(false);
			}

		}
	};
}
