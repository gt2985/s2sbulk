package api.salesforce;

import junit.framework.Assert;

import org.junit.Test;

import com.sforce.async.AsyncApiException;

public class QueryTests extends TestBase {

	@Test
	public void test() throws AsyncApiException {
		final BulkO2OContext context = new BulkO2OContext();
		context.SourceConnection = getSourceConnection();
		context.DestinationConnection = getDestinationConnection();
		context.FieldMapName = "CampaignFieldMap.properties";

		final BulkMigrator migrator = new BulkMigrator(context);

		System.out.println(migrator.getContext().getQueryFilter());
		Assert.assertNotNull(migrator.getContext().getQueryFilter());
		Assert.assertTrue(migrator.getContext().getQueryFilter() != "");

		System.out.println(migrator.getContext().getFieldMap()
				.getProperty("QueryFilter"));
		Assert.assertNotNull(migrator.getContext().getFieldMap()
				.getProperty("QueryFilter"));
		Assert.assertTrue(migrator.getContext().getFieldMap()
				.getProperty("QueryFilter") != "");

		final String query = migrator.generateQuery();
		System.out.println(query);
		// Assert.assertTrue(query.contains("LIMIT"));
		// Query should not contain any config properties within field map.
		Assert.assertFalse(query.contains("SObject"));
		Assert.assertFalse(query.contains("BatchSize"));
		Assert.assertFalse(query.contains("WhereFilter"));

		abortJob(migrator.getJobInfo());
	}
}