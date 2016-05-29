package api.salesforce;

import org.junit.Assert;
import org.junit.Test;

import com.sforce.async.AsyncApiException;
import com.sforce.async.ContentType;
import com.sforce.async.JobInfo;
import com.sforce.async.OperationEnum;

public class LoggerTests extends TestBase {

	@Test
	public void loggerTests() throws AsyncApiException {
		JobInfo job = new JobInfo();
		job.setObject("Account");
		job.setOperation(OperationEnum.insert);
		job.setContentType(ContentType.CSV);
		job = getSourceConnection().getBulkConnection().createJob(job);

		final Logger logger = new Logger(job);
		Assert.assertNotNull(logger);
		logger.write("Created batch job 123");
		logger.info("Info message");
		logger.error("This is error message");
		logger.close();

		getSourceConnection().getBulkConnection().abortJob(job.getId());
	}
}