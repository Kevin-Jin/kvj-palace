package kvj.shithead.backend;

import java.io.IOException;
import java.net.Socket;

public class Client {
	private final Socket connection;
	public final byte[] buffer;
	private int bufferLimit;

	public Client(Socket connection) {
		this.connection = connection;
		buffer = new byte[64];
		bufferLimit = 0;
	}

	public boolean fillBuffer(int min) {
		assert buffer.length >= min;
		int read = 0;
		try {
			while (read != -1 && bufferLimit < min) {
				read = connection.getInputStream().read(buffer, bufferLimit, buffer.length - bufferLimit);
				bufferLimit += read;
			}
		} catch (IOException e) {
			close(e.getMessage());
			return false;
		}

		if (read == -1) {
			close("EOF reached.");
			return false;
		}
		assert bufferLimit >= min;
		return true;
	}

	public void compactBuffer(int size) {
		assert bufferLimit >= size;
		System.arraycopy(buffer, size, buffer, 0, bufferLimit - size);
		bufferLimit -= size;
	}

	public void send(byte[] message) {
		try {
			connection.getOutputStream().write(message);
		} catch (IOException e) {
			close(e.getMessage());
		}
	}

	private void close(String message) {
		System.err.println("Closed connection to " + connection.getInetAddress() + ": " + message);
		try {
			connection.close();
		} catch (IOException e) {
			System.err.println("Could not close " + connection.getInetAddress() + ".");
			e.printStackTrace();
		}
	}

	public Socket socket() {
		return connection;
	}
}
