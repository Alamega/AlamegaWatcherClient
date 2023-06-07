package com.alamega;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionException;

public class ConnectionToWS {
    private WebSocket webSocket = null;
    private final HttpClient client;
    private final URI url;

    public ConnectionToWS(HttpClient client, URI url) {
        this.client = client;
        this.url = url;
        try {
            this.webSocket = this.client.newWebSocketBuilder().header("mac", SystemScanner.getMACAddress()).buildAsync(this.url, new WebSocket.Listener() {}).join();
        } catch (CompletionException ignored) {}
    }

    public void send(String text) {
        if (webSocket == null || webSocket.isOutputClosed()) {
            try {
                webSocket = this.client.newWebSocketBuilder().header("mac", SystemScanner.getMACAddress()).buildAsync(this.url, new WebSocket.Listener() {}).join();
            } catch (CompletionException ignored) {}
        } else {
            webSocket.sendText(text, true);
        }
    }
}
