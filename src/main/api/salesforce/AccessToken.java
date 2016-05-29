package api.salesforce;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import com.google.gson.Gson;

public class AccessToken {
	private final String refresh_url = "https://login.salesforce.com/services/oauth2/token";
	private String access_token;
	private String refresh_token;
	private String login_url;
	private String client_id;
	private RefreshTokenResponse refreshResponse = null;

	public AccessToken withRefreshToken(final String rtoken) {
		refresh_token = rtoken;
		return this;
	}

	public AccessToken withLoginUrl(final String url) {
		login_url = url;
		return this;
	}

	public AccessToken withClientId(final String cid) {
		client_id = cid;
		return this;
	}

	public String getAccessToken() {
		return access_token;
	}

	public boolean wasSuccessful() {
		return access_token != null;
	}

	private String refreshParams() {
		return "grant_type=refresh_token&client_id=" + client_id
				+ "&refresh_token=" + refresh_token;
	}

	public AccessToken refreshToken() {
		try {
			final URL endpoint = new URL(refresh_url);
			final URLConnection conn = endpoint.openConnection();
			conn.setDoOutput(true);

			final OutputStreamWriter writer = new OutputStreamWriter(
					conn.getOutputStream());

			writer.write(refreshParams());
			writer.flush();

			String line;
			final BufferedReader reader = new BufferedReader(
					new InputStreamReader(conn.getInputStream()));

			while ((line = reader.readLine()) != null) {
				refreshResponse = new Gson().fromJson(line,
						RefreshTokenResponse.class);
				if (refreshResponse != null) {
					access_token = refreshResponse.access_token;
				}
			}
			writer.close();
			reader.close();
		} catch (final MalformedURLException e) {
			access_token = null;
			e.printStackTrace();
		} catch (final IOException e) {
			access_token = null;
			System.err.println("Token refresh failed for clientId: "
					+ client_id + ". refresh_token:" + refresh_token);
			System.err.println("Reason: " + e.getMessage());
		}
		return this;
	}

	public class RefreshTokenResponse {
		public String id;
		public String issued_at;
		public String scope;
		public String instance_url;
		public String signature;
		public String access_token;
	}
}