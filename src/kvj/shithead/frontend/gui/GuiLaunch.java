package kvj.shithead.frontend.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JApplet;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class GuiLaunch extends JApplet {
	private ShitheadPanel panel;
	private Timer refresher;

	@Override
	public void init() {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					panel = new ShitheadPanel(new GuiGame(5));
					refresher = new Timer(1000 / 60, new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							panel.repaint();
						}
					});

					getContentPane().add(panel);
				}
			});
		} catch (InterruptedException e) {
			System.err.println("Could not initialize applet");
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			System.err.println("Could not initialize applet");
			e.printStackTrace();
		}
	}

	@Override
	public void start() {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					refresher.start();
				}
			});
		} catch (InterruptedException e) {
			System.err.println("Could not start applet");
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			System.err.println("Could not start applet");
			e.printStackTrace();
		}
	}

	@Override
	public void stop() {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					refresher.stop();
				}
			});
		} catch (InterruptedException e) {
			System.err.println("Could not stop applet");
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			System.err.println("Could not stop applet");
			e.printStackTrace();
		}
	}

	@Override
	public void destroy() {
		refresher = null;
		panel = null;
	}

	private static final long serialVersionUID = -8306109139033051570L;

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				final ShitheadPanel panel = new ShitheadPanel(new GuiGame(5));
				Timer refresher = new Timer(1000 / 60, new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						panel.updateState();
						panel.repaint();
					}
				});

				JFrame frame = new JFrame("Shithead (AKA Palace)");
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				frame.setResizable(false);
				frame.getContentPane().add(panel);
				frame.pack();
				frame.setVisible(true);

				refresher.start();
			}
		});
	}
}
