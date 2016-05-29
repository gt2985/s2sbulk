package api.salesforce;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sforce.soap.partner.DescribeSObjectResult;
import com.sforce.soap.partner.Field;
import com.sforce.soap.partner.FieldType;
import com.sforce.ws.ConnectionException;

public class Resolver {
	private final BulkO2OContext m_context;
	private final Map<String, ReferenceObject> m_referenceFields;
	private final DescribeSObjectResult schema;
	// Supported polymorphic WhoId/WhatId types for Activities.
	private final String PREFIX_ACCOUNT = "001";
	private final String PREFIX_CONTACT = "003";
	private final String PREFIX_USER = "005";
	private final String PREFIX_OPPORTUNITY = "006";
	private final String PREFIX_LEAD = "00Q";
	private final String PREFIX_CAMPAIGN = "701";

	public Resolver(final BulkO2OContext context) {
		m_context = context;
		m_referenceFields = new MapGenerator(context).generateReferenceFields();
		DescribeSObjectResult s = null;
		try {
			s = getContext().SourceConnection.getPartnerConnection()
					.describeSObject(getContext().getSObjectType());
		} catch (final ConnectionException e) {
			e.printStackTrace();
		}
		schema = s;
	}

	public BulkO2OContext getContext() {
		return m_context;
	}

	public String resolve(final String fieldName, final String id) {
		if (fieldName == null || fieldName == "") {
			return null;
		}
		final Field f = getFieldByName(schema.getFields(), fieldName);
		if (f == null) {
			System.out
					.println("Resolver could not resolve lookup reference ID for field: "
							+ fieldName);
			System.exit(1);
		}
		String referenceToObject;
		if (fieldName.equals("WhoId") && id.startsWith(PREFIX_CONTACT)) {
			referenceToObject = "Contact";
		} else if (fieldName.equals("WhoId") && id.startsWith(PREFIX_USER)) {
			referenceToObject = "User";
		} else if (fieldName.equals("WhatId")
				&& id.startsWith(PREFIX_OPPORTUNITY)) {
			referenceToObject = "Opportunity";
		} else if (fieldName.equals("WhatId") && id.startsWith(PREFIX_ACCOUNT)) {
			referenceToObject = "Account";
		} else {
			referenceToObject = f.getReferenceTo()[0];
		}
		final ReferenceObject refObject = m_referenceFields
				.get(referenceToObject);
		if (refObject == null) {
			System.out
					.println("Resolver does not have an initialized ReferenceObject for field: "
							+ fieldName);
			System.exit(1);
		}
		final String newID = refObject.getIDMap().get(id);
		if (newID == null) {
			System.out.println("Resolver could not resolve ID for field: "
					+ fieldName + " ID:" + id);
			// newID = ""; Let defaultValue() handle nulls.
		}
		return newID;
	}

	public Map<String, ReferenceObject> getCachedObjects() {
		return m_referenceFields;
	}

	public void initialize() {
		System.out.println("Initializing Resolver...");

		final Set<String> objectsToCache = new HashSet<String>();
		for (final String key : getContext().getFieldMap()
				.stringPropertyNames()) {
			final String value = getContext().getFieldMap().getProperty(key);
			if (value.toLowerCase().startsWith("#resolve")) {
				final Field resolveField = getFieldByName(schema.getFields(),
						key);
				if (resolveField == null) {
					System.out
							.println("Could not initialize lookup reference Resolver for field: "
									+ key);
					System.exit(1);
				}
				if (resolveField.getType() != FieldType.reference) {
					System.out
							.println("Field marked for #resolve is not a lookup reference field type: "
									+ key);
					System.exit(1);
				}
				/*
				 * What is getReferenceTo for WhatId or WhoId? Probably an array
				 * of objects.
				 */
				if (key.equals("WhoId")) {
					objectsToCache.add("Contact");
					objectsToCache.add("User");
				} else if (key.equals("WhatId")) {
					objectsToCache.add("Opportunity");
					objectsToCache.add("Account");
				} else {
					final String referenceToObject = resolveField
							.getReferenceTo()[0];
					if (referenceToObject == null || referenceToObject == "") {
						System.out
								.println("Resolver could not parse the referenceToObject for field: "
										+ key);
						System.exit(1);
					}
					objectsToCache.add(referenceToObject);
				}
			}
		}
		System.out
				.println(objectsToCache.size()
						+ " objects identified in field map as needing #resolve of ID lookup references.");
		for (final String objectName : objectsToCache) {
			final ReferenceObject obj = new ReferenceObject(getContext(),
					objectName);
			obj.getIDMap();
			m_referenceFields.put(objectName, obj);
		}
	}

	private Field getFieldByName(final Field[] fields, final String fieldName) {
		Field returnField = null;
		for (final Field f : fields) {
			if (f.getName().equals(fieldName)) {
				returnField = f;
				break;
			}
		}
		return returnField;
	}

	public String defaultValue(final String resolveDefinition) {
		/*
		 * Valid syntax #resolve #resolve:default=[15-18 char ID]
		 */
		if (false == resolveDefinition.toLowerCase().contains(":default")) {
			return null;
		}
		final String[] tokens = resolveDefinition.split(":");
		final String[] keyValue = tokens[1].split("=");
		if (keyValue.length > 1) {
			return keyValue[1];
		} else {
			return null;
		}
	}
}