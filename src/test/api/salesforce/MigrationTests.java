package api.salesforce;

import java.io.IOException;

import org.junit.Test;

import com.sforce.async.AsyncApiException;
import com.sforce.ws.ConnectionException;

public class MigrationTests extends TestBase {

	@Test
	public void tests() throws ConnectionException, AsyncApiException,
			IOException {
		final BulkO2OContext context = new BulkO2OContext();
		context.SourceConnection = getSourceConnection();
		context.DestinationConnection = getDestinationConnection();
		context.FieldMapName = "CampaignFieldMap.properties";
		final BulkMigrator migrator = new BulkMigrator(context);
		migrator.run();
	}
}