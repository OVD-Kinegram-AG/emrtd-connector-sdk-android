package com.kinegram.android.emrtdconnector.internal;

import android.os.Build;
import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

/**
 * Helper class that wraps {@link WebSocketClient} with some Android specific fixes.
 */
public abstract class AndroidWebsocketClient extends WebSocketClient {
    public AndroidWebsocketClient(URI serverUri, Map<String, String> httpHeaders) {
        super(serverUri, httpHeaders);
    }

    @Override
    protected void onSetSSLParameters(SSLParameters sslParameters) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Fix for Android SDK < 24
            // https://github.com/TooTallNate/Java-WebSocket/wiki/No-such-method-error-setEndpointIdentificationAlgorithm
            super.onSetSSLParameters(sslParameters);
        }
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N && !"ws".equals(this.uri.getScheme())) {
            // Fix for Android SDK < 24
            // https://github.com/TooTallNate/Java-WebSocket/wiki/No-such-method-error-setEndpointIdentificationAlgorithm
            try {
                HostnameVerifier hostnameVerifier;
                hostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
                SSLSession session = ((SSLSocket) getSocket()).getSession();
                String expectedHostname = this.uri.getHost();
                if (!hostnameVerifier.verify(expectedHostname, session)) {
                    String msg = "Invalid host" + session.getPeerPrincipal();
                    Log.e(this.getClass().getSimpleName(), msg);
                    throw new SSLHandshakeException((msg));
                }
                // All good. Continue
            } catch (Exception e) {
                onError(e);
                return;
            }
        }
        onWebsocketOpen(handshake);
    }

    public abstract void onWebsocketOpen(ServerHandshake handshake);
}
