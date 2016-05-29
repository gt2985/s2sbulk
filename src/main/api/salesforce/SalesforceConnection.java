package api.salesforce;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sforce.async.AsyncApiException;
import com.sforce.async.BulkConnection;
import com.sforce.soap.apex.Connector;
import com.sforce.soap.apex.SoapConnection;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.DescribeSObjectResult;
import com.sforce.soap.partner.Field;
import com.sforce.soap.partner.GetServerTimestampResult;
import com.sforce.soap.partner.GetUserInfoResult;
import com.sforce.soap.partner.LoginResult;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.QueryResult;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;

public class SalesforceConnection {
	public final static String API_VERSION = "35.0";
	private String client_id = "client_id_goes_here"; // For OAuth connections
	private String login_url = "https://login.salesforce.com/services/Soap/u/"
			+ API_VERSION;
	private String instance_url = null; // Example: https://na1.salesforce.com/
	private String org_id = null;
	private String access_token = null;
	private String refresh_token = null;
	private String username = null;
	private String password = null;
	private String security_token = null;
	private String session_id = null;
	private String partner_url = null;
	public List<String> errors = new ArrayList<String>();
	public boolean wasRefreshed = false;

	private LoginResult loginResult;
	private PartnerConnection partnerConnection;
	private MetadataConnection metadataConnection;
	private SoapConnection apexConnection;
	private BulkConnection bulkConnection;

	public SalesforceConnection withClientId(final String cid) {
		client_id = cid;
		return this;
	}

	public SalesforceConnection withOrgId(final String oid) {
		org_id = oid;
		return this;
	}

	public SalesforceConnection withLoginUrl(final String url) {
		login_url = url;
		return this;
	}

	public SalesforceConnection withInstanceUrl(final String url) {
		instance_url = url;
		return this;
	}

	public SalesforceConnection withAccessToken(final String aToken) {
		access_token = aToken;
		return this;
	}

	public SalesforceConnection withRefreshToken(final String rToken) {
		refresh_token = rToken;
		return this;
	}

	public SalesforceConnection withUsername(final String uname) {
		username = uname;
		return this;
	}

	public SalesforceConnection withPassword(final String pw) {
		password = pw;
		return this;
	}

	public SalesforceConnection withSecurityToken(final String token) {
		security_token = token;
		return this;
	}

	public SalesforceConnection withSession(final String session) {
		session_id = session;
		return this;
	}

	public SalesforceConnection withPartnerUrl(final String url) {
		partner_url = url;
		return this;
	}

	public boolean isProduction() {
		return !isSandbox();
	}

	public boolean isSandbox() {
		if (instance_url == null) {
			errors.add("isSandbox() and isProduction() cannot be called without first setting withLoginUrl() context");
			return false;
		}
		boolean result = false;
		try {
			final URI loginURI = new URI(instance_url);
			result = loginURI.getHost().toLowerCase().startsWith("cs");
		} catch (final URISyntaxException e) {
			e.printStackTrace();
		}
		return result;
	}

	private boolean validateInputParams() {
		errors.clear();
		if (client_id == null || client_id == "") {
			errors.add("Missing client_id parameter");
		}
		if (instance_url == null || instance_url == "") {
			errors.add("Missing server_url parameter");
		}
		if (org_id == null || org_id == "") {
			errors.add("Missing org_id parameter");
		}
		if (access_token == null || access_token == "") {
			errors.add("Missing access_token parameter");
		}
		if (refresh_token == null || refresh_token == "") {
			errors.add("Missing refresh_token parameter");
		}
		return errors.size() == 0;
	}

	public boolean hasErrors() {
		return errors.size() > 0;
	}

	private URL getServerUrl() {
		URL returnURL = null;
		try {
			if (partner_url != null && partner_url != "") {
				returnURL = new URL(partner_url);
			} else {
				returnURL = new URL(instance_url + "/services/Soap/u/"
						+ API_VERSION + "/" + org_id);
			}
		} catch (final MalformedURLException e) {
			e.printStackTrace();
		}
		return returnURL;
	}

	private URL getLoginUrl() {
		URL returnURL = null;
		try {
			if (login_url.contains("/u/")) {
				returnURL = new URL(login_url);
			} else {
				returnURL = new URL(login_url + "/services/Soap/u/"
						+ API_VERSION);
			}
		} catch (final MalformedURLException e) {
			e.printStackTrace();
		}
		return returnURL;
	}

	public SalesforceConnection connectWithToken() {
		if (!validateInputParams()) {
			return this;
		}
		loginResult = new LoginResult();
		// Format
		// https://na14.salesforce.com/services/Soap/u/29.0/00Dd0000000gzcr
		loginResult.setServerUrl(getServerUrl().toExternalForm());
		loginResult.setSessionId(access_token);

		final ConnectorConfig config = new ConnectorConfig();
		config.setServiceEndpoint(getServerUrl().toExternalForm());
		config.setSessionId(access_token);
		try {
			partnerConnection = new PartnerConnection(config);
			metadataConnection = createMetadataConnection(loginResult);
			apexConnection = createApexConnection(loginResult);
		} catch (final ConnectionException e) {
			errors.add(e.getMessage());
			e.printStackTrace();
		}
		return this;
	}

	/*
	 * This method will connect to Salesforce, attempt to get UserInfo, and
	 * refresh the connection if the access_token is invalid.
	 */
	public SalesforceConnection connectWithGetUserInfo() {
		if (!validateInputParams()) {
			return this;
		}
		loginResult = new LoginResult();
		// Format
		// https://na14.salesforce.com/services/Soap/u/29.0/00Dd0000000gzcr
		loginResult.setServerUrl(getServerUrl().toExternalForm());
		loginResult.setSessionId(access_token);

		final ConnectorConfig config = new ConnectorConfig();
		config.setServiceEndpoint(getServerUrl().toExternalForm());
		config.setSessionId(access_token);
		try {
			partnerConnection = new PartnerConnection(config);
			setUserInfo(partnerConnection.getUserInfo());
			metadataConnection = createMetadataConnection(loginResult);
			apexConnection = createApexConnection(loginResult);
			bulkConnection = createBulkConnection(loginResult);
		} catch (final ConnectionException e) {
			if (e.toString().contains("INVALID_SESSION_ID")) {
				// Refresh token and try connection one more time
				// FIXME: Ugly nested catch blocks going on in here.
				System.out.println("access_token failed for orgid: " + org_id
						+ ". Attempting to refresh token...");
				final AccessToken refreshToken = new AccessToken()
				.withClientId(client_id).withLoginUrl(instance_url)
				.withRefreshToken(refresh_token).refreshToken();
				if (refreshToken.wasSuccessful()) {
					System.out.println("Token refresh was successful");
					access_token = refreshToken.getAccessToken();
					wasRefreshed = true;
					connectWithToken();
					try {
						setUserInfo(partnerConnection.getUserInfo());
					} catch (final ConnectionException e1) {
						e1.printStackTrace();
					}
				} else {
					System.err.println("Token refresh failed...");
				}
			} else {
				errors.add("Could not establish Salesforce connection. Please check credentials or API limits. "
						+ e.getMessage());
				errors.add(e.toString());
				e.printStackTrace();
			}
		}
		return this;
	}

	public SalesforceConnection connectWithUserCredentials() {
		try {
			loginResult = loginToSalesforce();
			if (loginResult == null) {
				return this;
			}
			partnerConnection = createPartnerConnection(loginResult);
			metadataConnection = createMetadataConnection(loginResult);
			apexConnection = createApexConnection(loginResult);
			bulkConnection = createBulkConnection(loginResult);
		} catch (final ConnectionException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		return this;
	}

	public SalesforceConnection connectWithSession() {
		loginResult = new LoginResult();
		loginResult.setServerUrl(getServerUrl().toExternalForm());
		loginResult.setSessionId(session_id);

		final ConnectorConfig config = new ConnectorConfig();
		config.setServiceEndpoint(getServerUrl().toExternalForm());
		config.setSessionId(session_id);

		try {
			partnerConnection = new PartnerConnection(config);
			metadataConnection = createMetadataConnection(loginResult);
			apexConnection = createApexConnection(loginResult);
			bulkConnection = createBulkConnection(loginResult);
		} catch (final ConnectionException e) {
			errors.add(e.getMessage());
			e.printStackTrace();
		}

		return this;
	}

	private LoginResult loginToSalesforce() {
		LoginResult result = null;
		try {
			final ConnectorConfig config = new ConnectorConfig();
			config.setAuthEndpoint(getLoginUrl().toExternalForm());
			config.setServiceEndpoint(getLoginUrl().toExternalForm());
			config.setManualLogin(true);
			// System.out.println("Connecting with " + this.username + " " +
			// this.password + " " + this.security_token);
			result = (new PartnerConnection(config)).login(username, password
					+ security_token);
		} catch (final ConnectionException e) {
			e.printStackTrace();
			result = null;
		}
		return result;
	}

	private PartnerConnection createPartnerConnection(
			final LoginResult loginResult) throws ConnectionException {
		final ConnectorConfig config = new ConnectorConfig();
		config.setServiceEndpoint(loginResult.getServerUrl());
		config.setSessionId(loginResult.getSessionId());
		return new PartnerConnection(config);
	}

	private MetadataConnection createMetadataConnection(
			final LoginResult loginResult) throws ConnectionException {
		final String sessionId = loginResult.getSessionId();
		final String url = loginResult.getServerUrl().replaceAll("/u/", "/m/");

		final ConnectorConfig config = new ConnectorConfig();
		config.setServiceEndpoint(url);
		config.setSessionId(sessionId);

		return new MetadataConnection(config);
	}

	private SoapConnection createApexConnection(final LoginResult loginResult)
			throws ConnectionException {
		final String sessionId = loginResult.getSessionId();
		final String url = loginResult.getServerUrl().replaceAll("/u/", "/s/");

		final ConnectorConfig config = new ConnectorConfig();
		config.setServiceEndpoint(url);
		config.setSessionId(sessionId);

		return Connector.newConnection(config);
	}
	
	private BulkConnection createBulkConnection(final LoginResult loginResult) {
		ConnectorConfig config = new ConnectorConfig();
		config.setSessionId(loginResult.getSessionId() );
		// The endpoint for the Bulk API service is the same as for the normal
		// SOAP uri until the /Soap/ part. From here it's '/async/versionNumber'		
		String soapEndpoint = loginResult.getServerUrl(); 
		String restEndpoint = soapEndpoint.replace("Soap/u", "async");
		restEndpoint = restEndpoint.substring(0, restEndpoint.length() - 16);
		restEndpoint += "/";
		System.out.println(restEndpoint);
		config.setRestEndpoint(restEndpoint);
		// This should only be false when doing debugging.
		config.setCompression(true);
		// Set this to true to see HTTP requests and responses on stdout
		config.setTraceMessage(false);
		BulkConnection connection;
		try {
			connection = new BulkConnection(config);
		} catch (AsyncApiException e) {
			connection = null;
			e.printStackTrace();
		}
		return connection;
	}

	public List<SObject> querySObjects(final String soql)
			throws ConnectionException {
		final List<SObject> m_SObjects = new ArrayList<SObject>();

		QueryResult result = getPartnerConnection().query(soql);
		boolean done = false;
		if (result.getSize() > 0) {
			while (!done) {
				final SObject[] records = result.getRecords();
				for (final SObject obj : records) {
					m_SObjects.add(obj);
				}
				if (result.isDone()) {
					done = true;
				} else {
					result = getPartnerConnection().queryMore(
							result.getQueryLocator());
				}
			}
		}

		return m_SObjects;
	}

	public boolean isValid() {
		try {
			if (loginResult == null) {
				errors.add("loginResult is null");
				return false;
			}
			if (partnerConnection == null) {
				errors.add("partnerConnection is null");
				return false;
			}
			final GetServerTimestampResult result = getPartnerConnection()
					.getServerTimestamp();
			if (result != null) {
				return true;
			} else {
				errors.add("getServerTimestamp returned null.");
				return false;
			}
		} catch (final ConnectionException e) {
			errors.add(e.getMessage());
			return false;
		}
	}

	public String getAccessToken() {
		return access_token;
	}

	public SalesforceConnection refresh() {
		errors.clear();
		final AccessToken refreshToken = new AccessToken()
		.withClientId(client_id).withLoginUrl(instance_url)
		.withRefreshToken(refresh_token).refreshToken();
		if (refreshToken.wasSuccessful()) {
			access_token = refreshToken.getAccessToken();
			wasRefreshed = true;
			connectWithToken();
		}
		return this;
	}

	private GetUserInfoResult m_userInfoResult = null;

	public GetUserInfoResult getUserInfo() {
		if (m_userInfoResult == null) {
			try {
				m_userInfoResult = partnerConnection.getUserInfo();
			} catch (final ConnectionException e) {
				e.printStackTrace();
				errors.add("Could not retrieve UserInfo. Error message: "
						+ e.getMessage());
				return null;
			}
		}
		return m_userInfoResult;
	}

	private SalesforceConnection setUserInfo(final GetUserInfoResult info) {
		m_userInfoResult = info;
		return this;
	}

	public LoginResult getLoginResult() {
		return loginResult;
	}

	public PartnerConnection getPartnerConnection() {
		return partnerConnection;
	}

	public MetadataConnection getMetadataConnection() {
		return metadataConnection;
	}

	public SoapConnection getApexConnection() {
		return apexConnection;
	}
	
	public BulkConnection getBulkConnection(){
		return bulkConnection;
	}

	public String getAllFieldsByObject(final String sobjectName) {
		String field_list = "";

		DescribeSObjectResult describeSObjectResult = null;
		try {
			describeSObjectResult = getPartnerConnection().describeSObject(
					sobjectName);
		} catch (final ConnectionException e) {
			e.printStackTrace();
		}

		final Field[] fields = describeSObjectResult.getFields();

		for (final Field field : fields) {
			field_list += field.getName() + ",";
		}
		return field_list.substring(0, field_list.length() - 1);
	}

	public void dumpErrors() {
		for (final String err : errors) {
			System.err.println(err);
		}
	}
}