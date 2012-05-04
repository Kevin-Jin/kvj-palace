package kvj.shithead.frontend.cli;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import kvj.shithead.backend.Card;
import kvj.shithead.backend.Client;
import kvj.shithead.backend.PacketMaker;

public class CliLaunch {
	private static final int DEFAULT_PORT = 32421;

	public static void main(String[] args) throws IOException {
		Scanner scan = new Scanner(System.in);

		System.out.print("host or join?: ");
		String playMethod = scan.nextLine();
		while (!playMethod.equalsIgnoreCase("host") && !playMethod.equalsIgnoreCase("join")) {
			System.out.print("Try again: ");
			playMethod = scan.nextLine();
		}
		if (playMethod.equalsIgnoreCase("host")) {
			System.out.print("Maximum players: ");
			int maxPlayers = Integer.parseInt(scan.nextLine());
			while (maxPlayers <= 1 || maxPlayers >= 6) {
				System.out.print("Only up to five people may play. Try again: ");
				maxPlayers = Integer.parseInt(scan.nextLine());
			}
			System.out.print("How many are to play on this computer? (leave blank if all players will): ");
			String localPlayersStr = scan.nextLine();
			int localPlayers;
			while ((localPlayers = localPlayersStr.isEmpty() ? maxPlayers : Integer.parseInt(localPlayersStr)) < 1 || localPlayers > maxPlayers) {
				System.out.print("Please enter a value less than or equal to the maximum amount of players: ");
				localPlayersStr = scan.nextLine();
			}
			CliGame g = new CliGame(scan, maxPlayers, localPlayers != 1);
			g.constructLocalPlayers(0, localPlayers, null, true);
			g.populateDeck();
			if (localPlayers < maxPlayers) {
				System.out.print("Listen port (leave blank for default port): ");
				String port = scan.nextLine();

				ServerSocket s = new ServerSocket(port.isEmpty() ? DEFAULT_PORT : Integer.parseInt(port));
				for (int i = localPlayers, j; i < maxPlayers; i += j) {
					System.out.println("Waiting for " + (maxPlayers - i) + " more player(s)...");
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
			}
			g.run();
		} else if (playMethod.equalsIgnoreCase("join")) {
			System.out.print("IP Address: ");
			String ip = scan.nextLine();
			System.out.print("Port (leave blank for default port): ");
			String port = scan.nextLine();
			Client client = new Client(new Socket(ip, port.isEmpty() ? DEFAULT_PORT : Integer.parseInt(port)));
			int connectedPlayers = client.socket().getInputStream().read();
			int maxPlayers = client.socket().getInputStream().read();

			System.out.println("Server reports that there is/are " + (maxPlayers - connectedPlayers) + " vacant spot(s).");
			System.out.print("How many are to play on this computer? (leave blank if only one will): ");
			String localPlayersStr = scan.nextLine();
			int localPlayers;
			while ((localPlayers = localPlayersStr.isEmpty() ? 1 : Integer.parseInt(localPlayersStr)) < 1 || localPlayers > (maxPlayers - connectedPlayers)) {
				System.out.print("Please enter a value less than or equal to the amount of vacant spot(s): ");
				localPlayersStr = scan.nextLine();
			}
			client.socket().getOutputStream().write(localPlayers);
			CliGame g = new CliGame(scan, maxPlayers, localPlayers != 1);
			int i;
			for (i = 0; i < connectedPlayers; i++)
				g.constructRemotePlayer(i, client, false);
			g.constructLocalPlayers(connectedPlayers, localPlayers, client, false);
			for (i = g.occupiedCount(); i < g.maxSize(); i++) {
				System.out.println("Waiting for " + (g.maxSize() - i) + " more player(s)...");
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
		}
	}
}
