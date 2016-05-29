package api.salesforce;

import java.util.Properties;
import org.junit.Test;
import com.sforce.async.AsyncApiException;
import junit.framework.Assert;

public class FieldMapTests extends TestBase{

	@Test
	public void tests() throws AsyncApiException {
		BulkO2OContext context = new BulkO2OContext();
		context.SourceConnection = this.getSourceConnection();
		context.DestinationConnection = this.getDestinationConnection();
		context.FieldMapName = "CampaignFieldMap.properties";
		BulkMigrator migrator = new BulkMigrator(context);
		Properties fieldMap = migrator.getContext().getFieldMap();
		Assert.assertFalse(migrator.hasError());
		Assert.assertTrue(fieldMap != null);
		for(String key : fieldMap.stringPropertyNames()){
			System.out.print(key + " = ");
			System.out.println(fieldMap.get(key));
		}
		
		Assert.assertEquals("upsert", migrator.getContext().getOperationType());
		
		Assert.assertNull(migrator.getContext().getFieldMap().get("foo"));
		
		Assert.assertTrue(migrator.isConfigProperty("BatchSize"));
		Assert.assertTrue(migrator.getContext().getBatchSize() > 0);
		
		Assert.assertTrue(migrator.isConfigProperty("SObject"));
		Assert.assertEquals("Campaign", migrator.getContext().getSObjectType());	
		
		Assert.assertNotNull(migrator.getContext().getExternalFieldName());
		
		// Note mapped fields are converted to Notes during migration
		// TODO Open design question. Migrate Note fields in trigger, or during transform?
		// Pros: One-time xform. Doesn't clutter org with one-time code, or triggers that eval unused fields.
		// Do this in second pass?
		
		Assert.assertFalse(migrator.isNoteMappedField("foo"));
		Assert.assertFalse(migrator.isNoteMappedField("Name"));
		
		this.abortJob( migrator.getJobInfo() );
	}
}