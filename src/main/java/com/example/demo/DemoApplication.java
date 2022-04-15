package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;


@SpringBootApplication
public class DemoApplication extends JFrame {

	@Autowired
	private Application application;

	String PATH;

	public DemoApplication() {
		initUI();
	}

	private void initUI() {

		JLabel label = new JLabel("Esercitazione MultiCl - Mario Canalella");
		JButton pathButton = new JButton("Seleziona Path file CSV");
		pathButton.addActionListener(this::actionPerformed);

		JPanel pane = (JPanel) getContentPane();
		pane.setLayout(new GridLayout(6, 1));
		pane.add(label);
		pane.add(pathButton);

		setTitle("Data Mining");
		setSize(400, 400);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(EXIT_ON_CLOSE);

		pane.validate();
		pane.repaint();
	}

	public void actionPerformed(ActionEvent e) {
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(new java.io.File("."));
		//chooser.setDialogTitle(choosertitle);
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		//
		// disable the "All files" option.
		//
		chooser.setAcceptAllFileFilterUsed(false);
		//
		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			System.out.println("getSelectedFile() : "
					+  chooser.getSelectedFile());
			addbutton(chooser.getSelectedFile().getPath());

		}
		else {
			System.out.println("No Selection ");
		}
	}

	private void addbutton(String path) {
		this.PATH = path;
		JPanel pane = (JPanel) getContentPane();
		JLabel pathLabel = new JLabel("PATH: " + path);
		JButton startButton = new JButton("Start");
		pane.add(pathLabel);
		pane.add(startButton);
		pane.validate();
		pane.repaint();


		startButton.addActionListener((ActionEvent event) -> {
			try {
				application.run(path);
			} catch (Exception e) {
				e.printStackTrace();
				JLabel label = new JLabel("Errore!");
				pane.add(label);
				reset(pane);
				return;
			}
			JLabel label = new JLabel("File generati!");
			pane.add(label);
			reset(pane);
		});
	}

	private void reset(JPanel pane) {
		JButton resetButton = new JButton("Restart");
		resetButton.addActionListener((ActionEvent lastEvent) -> {
			pane.removeAll();
			initUI();
		});
		pane.add(resetButton);
		pane.validate();
		pane.repaint();
	}

	public static void main(String[] args) {
		ConfigurableApplicationContext ctx =
				new SpringApplicationBuilder(DemoApplication.class)
				.headless(false)
				.run(args);

		EventQueue.invokeLater(() -> {
			DemoApplication ex = ctx.getBean(DemoApplication.class);
			ex.setVisible(true);
		});
	}
}