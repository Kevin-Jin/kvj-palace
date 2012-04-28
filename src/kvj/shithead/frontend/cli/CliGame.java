package kvj.shithead.frontend.cli;

import java.util.Scanner;

import kvj.shithead.backend.Client;
import kvj.shithead.backend.Game;
import kvj.shithead.backend.TurnContext;
import kvj.shithead.backend.adapter.ClientAdapter;
import kvj.shithead.backend.adapter.HostAdapter;
import kvj.shithead.backend.adapter.NoOperationAdapter;

public class CliGame extends Game {
	private final Scanner scan;
	private final boolean splitScreen;

	public CliGame(Scanner scan, int playerCount, boolean splitScreen) {
		super(playerCount);
		this.scan = scan;
		this.splitScreen = splitScreen;
	}

	@Override
	public void constructLocalPlayers(int start, int amount, Client client, boolean host) {
		for (int i = 0; i < amount; i++) {
			if (host)
				players[start + i] = new CliLocalPlayer(start + i, new HostAdapter(client, connected), scan, splitScreen);
			else
				players[start + i] = new CliLocalPlayer(start + i, new ClientAdapter(client), scan, splitScreen);
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
			System.out.println("Player " + (i + 1) + " has " + CliGameUtil.listRanks(players[i].getFaceUp()) + " as face up cards. (S)he has " + players[i].getHand().size() + " cards in his/her hand and " + players[i].getFaceDown().size() + " face down cards.");
		}
		if (discardPileSize() != 0)
			System.out.print("Last card(s) played is/are " + getSameRankCount() + " of " + getTopCardRank() + ". The discard pile has " + discardPileSize() + " card(s) total. ");
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

		for (currentPlayer = 0; currentPlayer < players.length; currentPlayer++) {
			System.out.print("Player " + (currentPlayer + 1) + " must choose his/her face up cards. ");
			TurnContext cx = players[currentPlayer].chooseFaceUp(this);
			if (splitScreen && players[currentPlayer] instanceof CliLocalPlayer)
				System.out.println("Player " + (currentPlayer + 1) + " " + cx.events + ".");
		}

		findStartingPlayer();
		System.out.println("Player " + (currentPlayer + 1) + " started the game with a " + getTopCardRank() + ".");

		for (currentPlayer = (currentPlayer + 1) % players.length; remainingPlayers.size() > 1; currentPlayer = (currentPlayer + 1) % players.length) {
			if (remainingPlayers.contains(Integer.valueOf(currentPlayer))) {
				printSummary();
				System.out.print("It is now Player " + (currentPlayer + 1) + "'s turn. ");

				TurnContext cx = players[currentPlayer].playTurn(this);
				if (splitScreen && players[currentPlayer] instanceof CliLocalPlayer)
					System.out.println("Player " + (currentPlayer + 1) + " " + cx.events + ".");
				if (cx.won) {
					System.out.println("Player " + (currentPlayer + 1) + " has won!");
					remainingPlayers.remove(Integer.valueOf(currentPlayer));
				}
			}
		}

		for (Integer pId : remainingPlayers)
			System.out.println("Player " + (pId.intValue() + 1) + " is the shithead!");
	}
}
