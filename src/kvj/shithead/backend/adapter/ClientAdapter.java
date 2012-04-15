package kvj.shithead.backend.adapter;

import kvj.shithead.backend.Client;

public class ClientAdapter extends BroadcastAdapter {
	private final Client server;

	public ClientAdapter(Client server) {
		this.server = server;
	}

	@Override
	protected void broadcast(byte[] message) {
		server.send(message);
	}
}
