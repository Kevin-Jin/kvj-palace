package kvj.shithead.frontend.gui;

import java.util.List;

import kvj.shithead.backend.Card;
import kvj.shithead.backend.Client;
import kvj.shithead.backend.Game;
import kvj.shithead.backend.TurnContext;
import kvj.shithead.backend.adapter.NoOperationAdapter;

public class GuiGame extends Game {
	private int localPlayer;

	public GuiGame(int playerCount) {
		super(playerCount);
		players[0] = new GuiLocalPlayer(0, NoOperationAdapter.getInstance());
		for (int i = 1; i < playerCount; i++)
			players[i] = new GuiRemotePlayer(i, NoOperationAdapter.getInstance());
		populateDeck();
		deal();
		for (int i = 0; i < playerCount; i++) {
			players[i].getFaceUp().add(players[i].getHand().remove(0));
			players[i].getFaceUp().add(players[i].getHand().remove(0));
			players[i].getFaceUp().add(players[i].getHand().remove(0));
		}
		addToDiscardPile(draw());
		addToDiscardPile(draw());
	}

	public int getPlayerCount() {
		return players.length;
	}

	@Override
	public void constructLocalPlayers(int start, int amount, Client client, boolean host) {
		/*for (int i = 0; i < amount; i++) {
			if (host)
				players[start + i] = new CliLocalPlayer(start + i, new HostAdapter(client, connected), scan, splitScreen);
			else
				players[start + i] = new CliLocalPlayer(start + i, new ClientAdapter(client), scan, splitScreen);
			remainingPlayers.add(Integer.valueOf(start + i));
			connectedCount++;
		}*/
	}

	@Override
	public void constructRemotePlayer(int playerId, Client client, boolean host) {
		/*if (host)
			players[playerId] = new CliRemotePlayer(playerId, new HostAdapter(client, connected), client);
		else
			players[playerId] = new CliRemotePlayer(playerId, NoOperationAdapter.getInstance(), client);
		remainingPlayers.add(Integer.valueOf(playerId));
		connectedCount++;*/
	}

	@Override
	public void run() {
		deal();
		System.out.println();

		for (currentPlayer = 0; currentPlayer < players.length; currentPlayer++) {
			TurnContext cx = players[currentPlayer].chooseFaceUp(this);
		}

		findStartingPlayer();

		for (currentPlayer = (currentPlayer + 1) % players.length; remainingPlayers.size() > 1; currentPlayer = (currentPlayer + 1) % players.length) {
			if (remainingPlayers.contains(Integer.valueOf(currentPlayer))) {
				TurnContext cx = players[currentPlayer].playTurn(this);
				if (cx.won) {
					remainingPlayers.remove(Integer.valueOf(currentPlayer));
				}
			}
		}

		for (Integer pId : remainingPlayers)
			System.out.println("Player " + (pId.intValue() + 1) + " is the shithead!");
	}

	public int getLocalPlayerNumber() {
		return localPlayer;
	}

	public List<Card> getDiscardPile() {
		return discardPile;
	}

	public int getCurrentPlayer() {
		return currentPlayer;
	}
}
