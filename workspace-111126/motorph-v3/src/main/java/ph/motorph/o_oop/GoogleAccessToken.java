package ph.motorph.o_oop;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import java.util.Collections;

public final class GoogleAccessToken implements FirestoreRestClient.TokenSource {
    private final GoogleCredentials credentials;

    public GoogleAccessToken() throws IOException {
        credentials = GoogleCredentials.getApplicationDefault()
            .createScoped(Collections.singletonList("https://www.googleapis.com/auth/datastore"));
    }

    GoogleAccessToken(GoogleCredentials credentials) {
        if (credentials == null) throw new IllegalArgumentException("credentials are required");
        this.credentials = credentials;
    }

    @Override public synchronized String token() throws IOException {
        credentials.refreshIfExpired();
        AccessToken access = credentials.getAccessToken();
        if (access == null || access.getTokenValue() == null) throw new IOException("No Google access token available");
        return access.getTokenValue();
    }

    @Override public synchronized boolean refresh() throws IOException {
        credentials.refresh();
        return true;
    }
}
