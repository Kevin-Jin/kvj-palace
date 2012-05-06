package kvj.shithead.frontend.gui;

import java.awt.CardLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import kvj.shithead.backend.Card;
import kvj.shithead.backend.Client;
import kvj.shithead.backend.PacketMaker;

public class GuiLaunchState {
	private static final int DEFAULT_PORT = 32421;

	private Timer refresher;
	private Thread gameLoop;

	private void showGame(final JPanel parent, final CardLayout parentLayout, final ShitheadPanel gamePanel) {
		//~120 fps so it looks smooth even without vsync
		refresher = new Timer(500 / 60, new ActionListener() {
			private long lastUpdate = System.currentTimeMillis();

			@Override
			public void actionPerformed(ActionEvent e) {
				long now = System.currentTimeMillis();
				gamePanel.updateState((now - lastUpdate) / 1000d);
				gamePanel.repaint();
				lastUpdate = now;
			}
		});
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				parentLayout.show(parent, "game");
			}
		});
		refresher.start();
	}

	private JPanel constructIoPanel(final JPanel parent, final CardLayout parentLayout, final ShitheadPanel gamePanel) {
		final JPanel panel = new JPanel();

		final JPanel parameters = new JPanel();
		final CardLayout parametersLayout = new CardLayout();
		parameters.setLayout(parametersLayout);
		JPanel hostParameters = new JPanel();
		final JTextField maxPlayers = new JTextField(1);
		final JTextField hostPort = new JTextField(Integer.toString(DEFAULT_PORT));
		final JButton bind = new JButton("Bind");
		final JTextField hostAddress = new JTextField(15);
		final JTextField joinPort = new JTextField(Integer.toString(DEFAULT_PORT));
		final JButton connect = new JButton("Connect");
		hostParameters.add(new JLabel("Max players: "));
		hostParameters.add(maxPlayers);
		hostParameters.add(new JLabel("Port: "));
		hostParameters.add(hostPort);
		hostParameters.add(bind);
		JPanel joinParameters = new JPanel();
		joinParameters.add(new JLabel("Host: "));
		joinParameters.add(hostAddress);
		joinParameters.add(new JLabel("Port: "));
		joinParameters.add(joinPort);
		joinParameters.add(connect);
		parameters.add(new JPanel(), "no selection");
		parameters.add(hostParameters, "host");
		parameters.add(joinParameters, "join");

		ButtonGroup hostJoinSelect = new ButtonGroup();
		JRadioButton host = new JRadioButton("Host");
		JRadioButton join = new JRadioButton("Join");
		hostJoinSelect.add(host);
		hostJoinSelect.add(join);
		host.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				parametersLayout.show(parameters, "host");
			}
		});
		join.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				parametersLayout.show(parameters, "join");
			}
		});
		bind.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				bind.setEnabled(false);
				bind.setText("Binding...");

				final int playerCount;
				try {
					playerCount = Integer.parseInt(maxPlayers.getText());
					if (playerCount <= 1 || playerCount >= 6)
						throw new NumberFormatException();
				} catch (NumberFormatException ex) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							JOptionPane.showMessageDialog(panel, "Max players must be in the range of [2-5].", "Could not bind", JOptionPane.ERROR_MESSAGE);
							bind.setEnabled(true);
							bind.setText("Bind");
						}
					});
					return;
				}

				gameLoop = new Thread(new Runnable() {
					@Override
					public void run() {
						GuiGame g = new GuiGame(playerCount, gamePanel);
						gamePanel.setModel(g);
						try {
							g.constructLocalPlayers(0, 1, null, true);
							g.populateDeck();
							ServerSocket s = new ServerSocket(Integer.parseInt(hostPort.getText()));
							showGame(parent, parentLayout, gamePanel);

							for (int i = 1, j; i < playerCount; i += j) {
								gamePanel.drawHint("Waiting for " + (playerCount - i) + " more player(s)...");
								Client client = new Client(s.accept());
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

							byte[] message = PacketMaker.serializedDeck(g.getDeckCards());
							for (Client client : g.getConnected())
								client.socket().getOutputStream().write(message);

							g.run();
						} catch (IllegalArgumentException ex) {
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									JOptionPane.showMessageDialog(panel, "Please enter a valid port number.", "Could not bind", JOptionPane.ERROR_MESSAGE);
									bind.setEnabled(true);
									bind.setText("Bind");
								}
							});
						} catch (final IOException ex) {
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									JOptionPane.showMessageDialog(panel, ex.getMessage(), "Could not bind", JOptionPane.ERROR_MESSAGE);
									bind.setEnabled(true);
									bind.setText("Bind");
								}
							});
						}
					}
				}, "gameloop");
				gameLoop.start();
			}
		});
		connect.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				connect.setEnabled(false);
				connect.setText("Connecting...");

				gameLoop = new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							Socket s = new Socket(hostAddress.getText(), Integer.parseInt(joinPort.getText()));
							Client client = new Client(s);
							int connectedPlayers = client.socket().getInputStream().read();
							int playerCount = client.socket().getInputStream().read();
							client.socket().getOutputStream().write(1);

							GuiGame g = new GuiGame(playerCount, gamePanel);
							gamePanel.setModel(g);

							int i;
							for (i = 0; i < connectedPlayers; i++)
								g.constructRemotePlayer(i, client, false);
							g.constructLocalPlayers(connectedPlayers, 1, client, false);
							showGame(parent, parentLayout, gamePanel);

							for (i = g.occupiedCount(); i < g.maxSize(); i++) {
								gamePanel.drawHint("Waiting for " + (g.maxSize() - i) + " more player(s)...");
								if (client.socket().getInputStream().read() == PacketMaker.ADD_PLAYER)
									g.constructRemotePlayer(i, client, false);
							}
							if (client.socket().getInputStream().read() == PacketMaker.DECK) {
								byte[] message = new byte[52];
								int offset = 0;
								while (offset < message.length)
									offset += client.socket().getInputStream().read(message, offset, message.length - offset);
								List<Card> cards = new ArrayList<Card>(52);
								for (i = 0; i < 52; i++)
									cards.add(Card.deserialize(message[i]));
								g.setDeck(cards);
							}

							g.run();
						} catch (IllegalArgumentException ex) {
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									JOptionPane.showMessageDialog(panel, "Please enter a valid port number.", "Could not connect", JOptionPane.ERROR_MESSAGE);
									connect.setEnabled(true);
									connect.setText("Connect");
								}
							});
						} catch (final IOException ex) {
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									JOptionPane.showMessageDialog(panel, ex.getMessage(), "Could not connect", JOptionPane.ERROR_MESSAGE);
									connect.setEnabled(true);
									connect.setText("Connect");
								}
							});
						}
					}
				}, "gameloop");
				gameLoop.start();
			}
		});
		JPanel hostJoin = new JPanel();
		hostJoin.add(host);
		hostJoin.add(join);

		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.add(hostJoin);
		panel.add(parameters);
		return panel;
	}

	public JPanel makeGuiPanel() {
		final ShitheadPanel panel = new ShitheadPanel();

		JPanel container = new JPanel();
		CardLayout layout = new CardLayout();
		container.setLayout(layout);
		container.add(constructIoPanel(container, layout, panel), "io");
		container.add(panel, "game");
		layout.show(container, "io");

		return container;
	}

	public void cleanup() {
		if (gameLoop != null)
			gameLoop.interrupt();
		if (refresher != null)
			refresher.stop();
	}
}
