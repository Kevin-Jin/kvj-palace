package kvj.shithead.frontend.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;

import javax.swing.JApplet;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import kvj.shithead.backend.Client;
import kvj.shithead.backend.Game;
import kvj.shithead.backend.PacketMaker;

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

	private static final int DEFAULT_PORT = 32421;

	private static void waitForConnectors(Game g, ServerSocket socket, int start, int end) throws IOException {
		for (int i = start, j; i < end; i += j) {
			System.out.println("Waiting for " + (end - i) + " more player(s)...");
			Client client = new Client(socket.accept());
			client.socket().getOutputStream().write(g.occupiedCount());
			client.socket().getOutputStream().write(g.maxSize());
			int playersAmount = client.socket().getInputStream().read();
			for (j = 0; j < playersAmount; j++)
				g.constructRemotePlayer(i + j, client, true);
			for (Client alreadyConnected : g.getConnected())
				for (j = 0; j < playersAmount; j++)
					alreadyConnected.socket().getOutputStream().write(PacketMaker.ADD_PLAYER);
			g.clientConnected(client);
		}
	}

	public static void main(String[] args) throws IOException, InvocationTargetException, InterruptedException {
		final GuiGame model = new GuiGame(5);
		model.constructLocalPlayers(0, 1, null, true);
		model.populateDeck();
		waitForConnectors(model, new ServerSocket(DEFAULT_PORT), 1, 5);
		byte[] message = PacketMaker.serializedDeck(model.getDeckCards());
		for (Client client : model.getConnected())
			client.socket().getOutputStream().write(message);

		SwingUtilities.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				final ShitheadPanel panel = new ShitheadPanel(model);
				model.setView(panel);

				//~120 fps so it looks smooth even without vsync
				Timer refresher = new Timer(500 / 60, new ActionListener() {
					private long lastUpdate = System.currentTimeMillis();

					@Override
					public void actionPerformed(ActionEvent e) {
						long now = System.currentTimeMillis();
						panel.updateState((now - lastUpdate) / 1000d);
						panel.repaint();
						lastUpdate = now;
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
		model.run();
	}
}
