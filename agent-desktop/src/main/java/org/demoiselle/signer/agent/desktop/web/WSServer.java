package org.demoiselle.signer.agent.desktop.web;

import static io.undertow.Handlers.path;
import static io.undertow.Handlers.websocket;

import java.net.BindException;

import com.google.gson.Gson;

import io.undertow.Undertow;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import io.undertow.websockets.spi.WebSocketHttpExchange;

public class WSServer extends AbstractReceiveListener {

	private static final String DEFAULT_HOST_WS_SERVER = "localhost";
	private static final int DEFAULT_PORT_WS_SERVER = 9091;
	private static WSServer instance = null;
	
	private Undertow undertow = null;
	
	public static WSServer getInstance() {
		if (WSServer.instance == null)
			WSServer.instance = new WSServer();
		return WSServer.instance;
	}

	private WSServer() {
		this.initializeWSServer(WSServer.DEFAULT_HOST_WS_SERVER, WSServer.DEFAULT_PORT_WS_SERVER);
		this.start();
	}

	private void initializeWSServer(String host, int port) {
		final WSServer listener = this;
		this.undertow = Undertow.builder().addHttpListener(port, host)
				.setHandler(path().addPrefixPath("/", websocket(new WebSocketConnectionCallback() {
					public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
						channel.getReceiveSetter().set(listener);
						channel.resumeReceives();
					}
				}))).build();
	}

	public void start() {
		this.undertow.start();
	}

	public void stop() {
		this.undertow.stop();
	}

	protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) {
		String result = null;
		Request request = null;
		try {
			final Gson gson = new Gson();
			try {
				request = gson.fromJson(message.getData(), Request.class);
			} catch (Throwable error) {
				throw new InterpreterException(error);
			}
			result = new Execute().executeCommand(request);
			WebSockets.sendText(result, channel, null);
		} catch (InterpreterException error) {
			ErrorResponse response = new ErrorResponse(request, "Error on try to interpreter a JSON message");
			WebSockets.sendText(response.toJson(), channel, null);
		} catch (Throwable error) {
			ErrorResponse response = new ErrorResponse(request, error);
			WebSockets.sendText(response.toJson(), channel, null);
		}
	}
	
	public static void main(String[] args) {
		WSServer.getInstance();
	}
}