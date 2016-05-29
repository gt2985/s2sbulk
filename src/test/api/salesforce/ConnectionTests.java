package api.salesforce;

import org.junit.Assert;
import org.junit.Test;

import com.sforce.async.AsyncApiException;

public class ConnectionTests extends TestBase {

	@Test
	public void test() throws AsyncApiException {
		final BulkO2OContext context = new BulkO2OContext();
		context.SourceConnection = getSourceConnection();
		context.DestinationConnection = getDestinationConnection();
		context.FieldMapName = "CampaignFieldMap.properties";
		final BulkMigrator migrator = new BulkMigrator(context);
		Assert.assertTrue(context.SourceConnection.isValid());
		Assert.assertTrue(context.SourceConnection.getBulkConnection() != null);

		Assert.assertTrue(context.DestinationConnection.isValid());
		Assert.assertTrue(context.DestinationConnection.getBulkConnection() != null);

		abortJob(migrator.getJobInfo());
	}
}