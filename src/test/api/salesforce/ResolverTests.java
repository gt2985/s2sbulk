package api.salesforce;

import junit.framework.Assert;

import org.junit.Test;

import com.sforce.ws.ConnectionException;

public class ResolverTests extends TestBase {

	@Test
	public void basicTests() throws ConnectionException {
		final BulkO2OContext context = new BulkO2OContext();
		context.SourceConnection = getSourceConnection();
		context.DestinationConnection = getDestinationConnection();
		context.FieldMapName = "ContactFieldMap.properties";

		final Resolver m_resolver = new Resolver(context); // Resolves lookup ID
		// references.
		m_resolver.initialize();

		final String value = m_resolver.resolve("AccountId", "orphanId");

		final String defaultValue = m_resolver
				.defaultValue("#resolve:default=0016100000VBkTnAAL");
		System.out.println("DefaultValue = " + defaultValue);
		Assert.assertEquals("0016100000VBkTnAAL", defaultValue);
	}
}