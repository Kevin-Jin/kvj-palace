package kvj.shithead.frontend.cli;

import java.util.Scanner;

import kvj.shithead.backend.Client;
import kvj.shithead.backend.Game;
import kvj.shithead.backend.adapter.ClientAdapter;
import kvj.shithead.backend.adapter.HostAdapter;
import kvj.shithead.backend.adapter.NoOperationAdapter;

public class CliGame extends Game {
	private final Scanner scan;

	public CliGame(Scanner scan, int playerCount) {
		super(playerCount);
		this.scan = scan;
	}

	@Override
	public void constructLocalPlayers(int start, int amount, Client client, boolean host) {
		for (int i = 0; i < amount; i++) {
			if (host)
				players[start + i] = new CliLocalPlayer(start + i, new HostAdapter(client, connected), scan, amount != 1);
			else
				players[start + i] = new CliLocalPlayer(start + i, new ClientAdapter(client), scan, amount != 1);
			remainingPlayers.add(Integer.valueOf(start + i));
			connectedCount++;
		}
	}

	@Override
	public void constructRemotePlayer(int playerId, Client client, boolean host) {
		if (host)
			players[playerId] = new CliRemotePlayer(playerId, new HostAdapter(client, connected), client);
		else
			players[playerId] = new CliRemotePlayer(playerId, NoOperationAdapter.getInstance(), client);
		remainingPlayers.add(Integer.valueOf(playerId));
		connectedCount++;
	}

	private void printSummary() {
		for (Integer pId : remainingPlayers) {
			int i = pId.intValue();
			System.out.println("Player " + (i + 1) + " has " + players[i].getFaceUp() + " as face up cards. (S)he has " + players[i].getHand().size() + " cards in his/her hand and " + players[i].getFaceDown().size() + " face down cards.");
		}
		if (discardPileSize() != 0)
			System.out.print("Last card(s) played is/are " + getSameRankCount() + " of " + getTopCard() + ". The discard pile has " + discardPileSize() + " card(s) total. ");
		else
			System.out.print("The discard pile is empty. ");
		if (canDraw())
			System.out.println("The draw deck has " + remainingDrawCards() + " card(s).");
		else
			System.out.println("The draw deck is empty.");
	}

	@Override
	public void run() {
		deal();
		System.out.println();

		for (int i = 0; i < players.length; i++) {
			System.out.print("Player " + (i + 1) + " must choose his/her face up cards. ");
			players[i].chooseFaceUp(this);
		}

		findStartingPlayer();
		System.out.println("Player " + (currentPlayer + 1) + " started the game with a " + getTopCard() + ".");

		for (currentPlayer = (currentPlayer + 1) % players.length; remainingPlayers.size() > 1; currentPlayer = (currentPlayer + 1) % players.length) {
			if (remainingPlayers.contains(Integer.valueOf(currentPlayer))) {
				printSummary();
				System.out.print("It is now Player " + (currentPlayer + 1) + "'s turn. ");

				if (players[currentPlayer].playTurn(this).won) {
					System.out.println("Player " + (currentPlayer + 1) + " has won!");
					remainingPlayers.remove(Integer.valueOf(currentPlayer));
				}
			}
		}

		for (Integer pId : remainingPlayers)
			System.out.println("Player " + (pId.intValue() + 1) + " is the shithead!");
	}
}