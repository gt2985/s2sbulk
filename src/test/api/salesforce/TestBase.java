package api.salesforce;

import com.sforce.async.AsyncApiException;
import com.sforce.async.JobInfo;
import com.sforce.async.JobStateEnum;

public class TestBase {
	private SalesforceConnection m_sourceConnection = null;

	public SalesforceConnection getSourceConnection() {
		if (m_sourceConnection == null) {
			m_sourceConnection = new SalesforceConnection()
			.withUsername(Settings.get(Settings.SALESFORCE_SOURCE_USERNAME))
			.withPassword(Settings.get(Settings.SALESFORCE_SOURCE_PASSWORD))
			.withSecurityToken(Settings.get(Settings.SALESFORCE_SOURCE_TOKEN))
			.withLoginUrl(Settings.get(Settings.SALESFORCE_SOURCE_LOGIN_URL))
			.connectWithUserCredentials();

			if (!m_sourceConnection.isValid()) {
				System.err
				.println("Could not establish source Salesforce connection. Please check config.properties file.");
			} else {
				System.out
				.println("Successfully connected to source Salesforce with config.property credentials for: " + Settings.get(Settings.SALESFORCE_SOURCE_USERNAME));
			}
		}
		return m_sourceConnection;
	}
	
	private SalesforceConnection m_destinationConnection = null;

	public SalesforceConnection getDestinationConnection() {
		if (m_destinationConnection == null) {
			m_destinationConnection = new SalesforceConnection()
			.withUsername(Settings.get(Settings.SALESFORCE_DESTINATION_USERNAME))
			.withPassword(Settings.get(Settings.SALESFORCE_DESTINATION_PASSWORD))
			.withSecurityToken(Settings.get(Settings.SALESFORCE_DESTINATION_TOKEN))
			.withLoginUrl(Settings.get(Settings.SALESFORCE_DESTINATION_LOGIN_URL))
			.connectWithUserCredentials();

			if (!m_destinationConnection.isValid()) {
				System.err
				.println("Could not establish destination Salesforce connection. Please check config.properties file.");
			} else {
				System.out
				.println("Successfully connected to destination Salesforce with config.property credentials for: " + Settings.get(Settings.SALESFORCE_DESTINATION_USERNAME));
			}
		}
		return m_destinationConnection;
	}
	
	public void closeJob(JobInfo job) throws AsyncApiException{
		JobInfo newJob = new JobInfo();
		newJob.setId(job.getId());		
		newJob.setState(JobStateEnum.Closed);
    	getDestinationConnection().getBulkConnection().updateJob(newJob);
	}
	
	public void abortJob(JobInfo job) throws AsyncApiException{
		JobInfo newJob = new JobInfo();
		newJob.setId(job.getId());
		newJob.setState(JobStateEnum.Aborted);
    	getDestinationConnection().getBulkConnection().updateJob(newJob);
	}
}