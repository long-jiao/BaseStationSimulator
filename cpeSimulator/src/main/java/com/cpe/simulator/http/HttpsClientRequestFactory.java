package com.cpe.simulator.http;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@Slf4j
public class HttpsClientRequestFactory extends SimpleClientHttpRequestFactory {

    private String omcUrl;

    public HttpsClientRequestFactory(String omcUrl) {
        this.omcUrl = omcUrl;
    }

    @Override
    protected void prepareConnection(HttpURLConnection connection, String httpMethod) {
        try {
            if (!omcUrl.contains("https")) {
                super.prepareConnection(connection, httpMethod);
                return;
            }

            if (!(connection instanceof HttpsURLConnection)) {
                log.error("httpsUrlconnection is expected....");
                throw new RuntimeException("An instance of httpsUrlConnection is expected");
            }
            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) connection;
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            httpsURLConnection.setSSLSocketFactory(new MyCustomSSLSocketFactory(sslContext.getSocketFactory()));

            httpsURLConnection.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String s, SSLSession sslSession) {
                    return true;
                }
            });

            super.prepareConnection(httpsURLConnection, httpMethod);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private static class MyCustomSSLSocketFactory extends SSLSocketFactory {
        private final SSLSocketFactory delegate;

        public MyCustomSSLSocketFactory(SSLSocketFactory delegate) {
            this.delegate = delegate;
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return delegate.getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return delegate.getSupportedCipherSuites();
        }

        @Override
        public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
            final Socket underlyingSocket = delegate.createSocket(socket, host, port, autoClose);
            return overrideProtocol(underlyingSocket);
        }

        @Override
        public Socket createSocket(String s, int port) throws IOException, UnknownHostException {
            Socket underlyingSocket = delegate.createSocket(s, port);
            return overrideProtocol(underlyingSocket);
        }

        @Override
        public Socket createSocket(String s, int i, InetAddress inetAddress, int i1) throws IOException, UnknownHostException {
            Socket underlyingSocket = delegate.createSocket(s, i, inetAddress, i1);
            return overrideProtocol(underlyingSocket);
        }

        @Override
        public Socket createSocket(InetAddress inetAddress, int i) throws IOException {
            Socket underlyingSocket = delegate.createSocket(inetAddress, i);
            return overrideProtocol(underlyingSocket);
        }

        @Override
        public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress1, int i1) throws IOException {
            Socket underlyingSocket = delegate.createSocket(inetAddress, i, inetAddress1, i1);
            return overrideProtocol(underlyingSocket);
        }

        private Socket overrideProtocol(Socket socket) {
            if (!(socket instanceof SSLSocket)) {
                log.error("an instance of SSLSocket is expected...");
                throw new RuntimeException("An instance of SSLSocket is expected...");
            }
            ((SSLSocket) socket).setEnabledProtocols(new String[]{"TLSv1", "TLSv1.1", "TLSv1.2"});
            return socket;
        }
    }
}




