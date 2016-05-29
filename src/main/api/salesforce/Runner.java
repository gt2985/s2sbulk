package api.salesforce;

import java.io.IOException;

import com.sforce.async.AsyncApiException;
import com.sforce.ws.ConnectionException;

public class Runner {

	public static void main(final String[] args) throws ConnectionException,
	AsyncApiException, IOException {
		if (args.length == 0) {
			System.out
			.println("Usage: runner [field map file] [command: generate or migrate]");
			System.out
			.println("Requires config.properties with source_ / destination_ url,username,password,token");
			return;
		}
		System.out.println("Running with field map configuration: " + args[0]);

		final Runner job = new Runner();
		String command = "migrate";
		if (args.length > 1) {
			command = args[1];
		}
		if (command.equals("generate")) {
			job.runMapGen(args[0]);
		} else {
			job.runMigration(args[0]);
		}
	}

	public void runMigration(final String fileName) throws ConnectionException,
	AsyncApiException, IOException {
		final BulkO2OContext context = new BulkO2OContext();
		context.SourceConnection = getSourceConnection();
		context.DestinationConnection = getDestinationConnection();
		context.FieldMapName = fileName;
		final BulkMigrator migrator = new BulkMigrator(context);
		migrator.run();
		System.out.println("Done");
	}

	public void runMapGen(final String fileName) throws ConnectionException,
	AsyncApiException, IOException {
		final BulkO2OContext context = new BulkO2OContext();
		context.SourceConnection = getSourceConnection();
		context.DestinationConnection = getDestinationConnection();
		context.FieldMapName = fileName;

		final MapGenerator mapGen = new MapGenerator(context);
		mapGen.generate();
		System.out.println("Done");
	}

	private SalesforceConnection m_sourceConnection = null;

	public SalesforceConnection getSourceConnection() {
		if (m_sourceConnection == null) {
			m_sourceConnection = new SalesforceConnection()
			.withUsername(
					Settings.get(Settings.SALESFORCE_SOURCE_USERNAME))
					.withPassword(
							Settings.get(Settings.SALESFORCE_SOURCE_PASSWORD))
							.withSecurityToken(
									Settings.get(Settings.SALESFORCE_SOURCE_TOKEN))
									.withLoginUrl(
											Settings.get(Settings.SALESFORCE_SOURCE_LOGIN_URL))
											.connectWithUserCredentials();

			if (!m_sourceConnection.isValid()) {
				System.err
				.println("Could not establish source Salesforce connection. Please check config.properties file.");
			} else {
				System.out
				.println("Successfully connected to source Salesforce with config.property credentials...");
			}
		}
		return m_sourceConnection;
	}

	private SalesforceConnection m_destinationConnection = null;

	public SalesforceConnection getDestinationConnection() {
		if (m_destinationConnection == null) {
			m_destinationConnection = new SalesforceConnection()
			.withUsername(
					Settings.get(Settings.SALESFORCE_DESTINATION_USERNAME))
					.withPassword(
							Settings.get(Settings.SALESFORCE_DESTINATION_PASSWORD))
							.withSecurityToken(
									Settings.get(Settings.SALESFORCE_DESTINATION_TOKEN))
									.withLoginUrl(
											Settings.get(Settings.SALESFORCE_DESTINATION_LOGIN_URL))
											.connectWithUserCredentials();

			if (!m_destinationConnection.isValid()) {
				System.err
				.println("Could not establish destination Salesforce connection. Please check config.properties file.");
			} else {
				System.out
				.println("Successfully connected to destination Salesforce with config.property credentials...");
			}
		}
		return m_destinationConnection;
	}
}
