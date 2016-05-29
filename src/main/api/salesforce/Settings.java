package api.salesforce;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Settings {

	public static final String SALESFORCE_SOURCE_LOGIN_URL = "source_url";
	public static final String SALESFORCE_SOURCE_PASSWORD = "source_password";
	public static final String SALESFORCE_SOURCE_TOKEN = "source_token";
	public static final String SALESFORCE_SOURCE_USERNAME = "source_username";

	public static final String SALESFORCE_DESTINATION_LOGIN_URL = "destination_url";
	public static final String SALESFORCE_DESTINATION_PASSWORD = "destination_password";
	public static final String SALESFORCE_DESTINATION_TOKEN = "destination_token";
	public static final String SALESFORCE_DESTINATION_USERNAME = "destination_username";

	public static final String USER_BOOF_SUFFIX = "user_boof_suffix";

	public static String get(final String key) {
		final String val = System.getenv(key);

		if (val != null && val != "") {
			return val;
		}

		// Look in local config file
		final Properties props = Settings.loadProperties();
		if (props == null) {
			return null;
		} else {
			return (String) props.get(key);
		}
	}

	public static Properties loadProperties() {
		final Properties envProperties = new Properties();
		InputStream input = null;
		try {
			input = new FileInputStream("config.properties");
			envProperties.load(input);
		} catch (final IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
		}
		return envProperties;
	}
}