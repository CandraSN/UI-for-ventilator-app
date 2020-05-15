package com.example.androidesp32;

import okhttp3.Response;
import okhttp3.WebSocket;
import okio.ByteString;

interface EchoWebSocket {
    void onOpen(WebSocket webSocket, Response response);

    void onMessage(WebSocket webSocket, String text);

    void onByte(WebSocket webSocket, ByteString bytes);

    void onClosing(WebSocket webSocket, int code, String reason);

    void onFailure(WebSocket webSocket, Throwable t, Response response);
}
