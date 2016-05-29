package api.salesforce;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sforce.soap.partner.QueryResult;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.ws.ConnectionException;

public class ReferenceObject {
	private final String m_name;
	public List<String> referenceFields = new ArrayList<String>();
	private final BulkO2OContext m_context;

	public ReferenceObject(final BulkO2OContext context, final String name) {
		m_context = context;
		m_name = name;
	}

	public BulkO2OContext getContext() {
		return m_context;
	}

	public String generateQuery() {
		String query = "SELECT ";
		for (final String field : referenceFields) {
			query += field + ",";
		}
		query += " FROM " + m_name;
		return query.replace(", FROM", " FROM");
	}

	private final String OLD_ID_FIELD = "Old_ID__c";
	private Map<String, String> m_idmap = null;

	public Map<String, String> getIDMap() {
		if (m_idmap != null) {
			return m_idmap;
		}
		System.out.println("Building ID lookup map for object " + m_name
				+ " ...");
		m_idmap = new HashMap<String, String>();
		final String soql = "SELECT Id, " + OLD_ID_FIELD + " FROM " + m_name;

		QueryResult result = null;
		try {
			result = getContext().DestinationConnection.getPartnerConnection()
					.query(soql);
		} catch (final ConnectionException e) {
			e.printStackTrace();
		}
		boolean done = false;
		if (result.getSize() > 0) {
			while (!done) {
				for (final SObject record : result.getRecords()) {
					final String oldid = (String) record.getField(OLD_ID_FIELD);
					if (oldid == null) {
						System.out
								.println("ERROR: Old_ID__c is null for record "
										+ record.getId());
						continue;
					}
					m_idmap.put(oldid, record.getId());
				}
				if (result.isDone()) {
					done = true;
				} else {
					try {
						result = getContext().DestinationConnection
								.getPartnerConnection().queryMore(
										result.getQueryLocator());
					} catch (final ConnectionException e) {
						e.printStackTrace();
					}
				}
			}
		}
		System.out.println(m_idmap.size() + " " + m_name
				+ " record IDs cached in map.");
		return m_idmap;
	}
}