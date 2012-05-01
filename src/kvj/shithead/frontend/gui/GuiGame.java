package kvj.shithead.frontend.gui;

import java.util.List;

import kvj.shithead.backend.Card;
import kvj.shithead.backend.Client;
import kvj.shithead.backend.Game;
import kvj.shithead.backend.Player;
import kvj.shithead.backend.TurnContext;
import kvj.shithead.backend.adapter.ClientAdapter;
import kvj.shithead.backend.adapter.HostAdapter;
import kvj.shithead.backend.adapter.NoOperationAdapter;

public class GuiGame extends Game {
	private int localPlayer;
	private ShitheadPanel view;

	public GuiGame(int playerCount) {
		super(playerCount);
	}

	public int getPlayerCount() {
		return players.length;
	}

	@Override
	public void constructLocalPlayers(int start, int amount, Client client, boolean host) {
		for (int i = 0; i < amount; i++) {
			if (host)
				players[start + i] = new GuiLocalPlayer(start + i, new HostAdapter(client, connected), this);
			else
				players[start + i] = new GuiLocalPlayer(start + i, new ClientAdapter(client), this);
			remainingPlayers.add(Integer.valueOf(start + i));
			connectedCount++;
		}
	}

	@Override
	public void constructRemotePlayer(int playerId, Client client, boolean host) {
		if (host)
			players[playerId] = new GuiRemotePlayer(playerId, new HostAdapter(client, connected), client, this);
		else
			players[playerId] = new GuiRemotePlayer(playerId, NoOperationAdapter.getInstance(), client, this);
		remainingPlayers.add(Integer.valueOf(playerId));
		connectedCount++;
	}

	@Override
	public void run() {
		view.makeDrawDeckEntities();
		deal();
		for (Player p : players)
			view.dealtCards(p);

		for (currentPlayer = 0; currentPlayer < players.length; currentPlayer++)
			players[currentPlayer].chooseFaceUp(this);

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

	public TurnContext getCurrentTurnContext() {
		return null;
	}

	public void setView(ShitheadPanel panel) {
		view = panel;
	}

	public ShitheadPanel getView() {
		return view;
	}
}
