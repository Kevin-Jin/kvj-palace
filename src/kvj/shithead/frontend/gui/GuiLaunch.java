package kvj.shithead.frontend.gui;

import java.lang.reflect.InvocationTargetException;

import javax.swing.JApplet;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class GuiLaunch extends JApplet {
	private GuiLaunchState state;

	@Override
	public void init() {
		state = new GuiLaunchState();
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					getContentPane().add(state.makeGuiPanel());
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
	public void stop() {
		state.cleanup();
	}

	@Override
	public void destroy() {
		state = null;
	}

	private static final long serialVersionUID = -8306109139033051570L;

	public static void main(String[] args) {
		final GuiLaunchState state = new GuiLaunchState();
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				JFrame frame = new JFrame("Shithead (AKA Palace)");
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				frame.setResizable(false);

				frame.getContentPane().add(state.makeGuiPanel());

				frame.pack();
				frame.setVisible(true);
			}
		});
	}
}
