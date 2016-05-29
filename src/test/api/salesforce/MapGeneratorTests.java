package api.salesforce;

import org.junit.Test;

import com.sforce.ws.ConnectionException;

public class MapGeneratorTests extends TestBase {

	@Test
	public void tests() throws ConnectionException {
		final BulkO2OContext context = new BulkO2OContext();
		context.SourceConnection = getSourceConnection();
		context.DestinationConnection = getDestinationConnection();
		context.FieldMapName = "CampaignFieldMap.properties";

		final MapGenerator mapGen = new MapGenerator(context);
		mapGen.generate();
	}
}