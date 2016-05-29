package api.salesforce;

import org.junit.Test;

import com.sforce.async.AsyncApiException;
import com.sforce.ws.ConnectionException;

public class FieldMapGenerator extends TestBase {

	@Test
	public void tests() throws AsyncApiException, ConnectionException {
		final String FIELD_MAP_NAME = "CampaignFieldMap.properties";

		final BulkO2OContext context = new BulkO2OContext();
		context.SourceConnection = getSourceConnection();
		context.DestinationConnection = getDestinationConnection();
		context.FieldMapName = FIELD_MAP_NAME;

		final MapGenerator mapGen = new MapGenerator(context);
		mapGen.generate();
	}
}
