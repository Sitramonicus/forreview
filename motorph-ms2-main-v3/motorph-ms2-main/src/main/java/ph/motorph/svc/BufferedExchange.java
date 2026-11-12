package ph.motorph.svc;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;

final class BufferedExchange extends HttpExchange {
    private final HttpExchange original;
    private final ByteArrayOutputStream body = new ByteArrayOutputStream();
    private int status = -1;
    private long length;

    BufferedExchange(HttpExchange original) { this.original = original; }
    @Override public Headers getRequestHeaders() { return original.getRequestHeaders(); }
    @Override public Headers getResponseHeaders() { return original.getResponseHeaders(); }
    @Override public URI getRequestURI() { return original.getRequestURI(); }
    @Override public String getRequestMethod() { return original.getRequestMethod(); }
    @Override public HttpContext getHttpContext() { return original.getHttpContext(); }
    @Override public void close() { }
    @Override public InputStream getRequestBody() { return original.getRequestBody(); }
    @Override public OutputStream getResponseBody() { return body; }
    @Override public void sendResponseHeaders(int code, long responseLength) { status = code; length = responseLength; }
    @Override public InetSocketAddress getRemoteAddress() { return original.getRemoteAddress(); }
    @Override public int getResponseCode() { return status; }
    @Override public InetSocketAddress getLocalAddress() { return original.getLocalAddress(); }
    @Override public String getProtocol() { return original.getProtocol(); }
    @Override public Object getAttribute(String name) { return original.getAttribute(name); }
    @Override public void setAttribute(String name, Object value) { original.setAttribute(name, value); }
    @Override public void setStreams(InputStream in, OutputStream out) { throw new UnsupportedOperationException(); }
    @Override public HttpPrincipal getPrincipal() { return original.getPrincipal(); }

    void resetResponse() {
        body.reset();
        status = -1;
        length = 0;
        getResponseHeaders().clear();
    }

    void finish() throws IOException {
        if (status < 0) throw new IOException("Handler did not send a response");
        original.sendResponseHeaders(status, length < 0 ? -1 : body.size());
        if (length >= 0) try (OutputStream out = original.getResponseBody()) { body.writeTo(out); }
        else original.close();
    }
}
