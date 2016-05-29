package api.salesforce;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sforce.soap.partner.DescribeSObjectResult;
import com.sforce.soap.partner.Field;
import com.sforce.soap.partner.FieldType;
import com.sforce.ws.ConnectionException;

public class MapGenerator {
	private final BulkO2OContext context;

	public MapGenerator(final BulkO2OContext c) {
		context = c;
	}

	/*
	 * Given a source and destination connection, and object type, generate a
	 * template map. Outputs to standard system out.
	 */
	public void generate() throws ConnectionException {
		final String sObjectType = context.getSObjectType();
		final DescribeSObjectResult sourceSchema = context.SourceConnection
				.getPartnerConnection().describeSObject(sObjectType);
		final DescribeSObjectResult destinationSchema = context.DestinationConnection
				.getPartnerConnection().describeSObject(sObjectType);

		final List<Field> matchList = new ArrayList<Field>();
		final List<Field> noMatchList = new ArrayList<Field>();
		final List<FieldMapResult> ignoredList = new ArrayList<FieldMapResult>();

		for (final Field sourceField : sourceSchema.getFields()) {
			final Field destinationField = getFieldByName(
					destinationSchema.getFields(), sourceField.getName());
			if (destinationField == null) {
				noMatchList.add(sourceField);
				continue;
			}
			final FieldMapResult result = isIgnored(destinationField);
			if (context.getExternalFieldName() != null
					&& destinationField.getName().equals(
							context.getExternalFieldName())) {
				result.ignored = true;
				result.ignoredReason = "already defined as ExternalID field";
				continue;
			}
			if (result.ignored) {
				ignoredList.add(result);
				continue;
			} else {
				matchList.add(sourceField);
			}
		}

		printNameValuePairs(matchList);
		System.out.println("");
		printWithComment(noMatchList);
		System.out.println("");
		printIgnored(ignoredList);
	}

	public Map<String, ReferenceObject> generateReferenceFields() {
		final Map<String, ReferenceObject> referenceObjectMap = new HashMap<String, ReferenceObject>();
		final String sObjectType = context.getSObjectType();
		DescribeSObjectResult sourceSchema = null;
		DescribeSObjectResult destinationSchema = null;

		try {
			sourceSchema = context.SourceConnection.getPartnerConnection()
					.describeSObject(sObjectType);
			destinationSchema = context.DestinationConnection
					.getPartnerConnection().describeSObject(sObjectType);
		} catch (final ConnectionException e) {
			e.printStackTrace();
		}

		for (final Field sourceField : sourceSchema.getFields()) {
			if (sourceField.getType() != FieldType.reference) {
				continue;
			}
			final Field destinationField = getFieldByName(
					destinationSchema.getFields(), sourceField.getName());
			if (destinationField == null) {
				continue;
			}
			if (destinationField.getType() != FieldType.reference) {
				// System.out.println("#" + sourceField.getName() +
				// "= ??? Matching field on destination not of type 'reference'");
			} else {
				final String referenceToObject = destinationField
						.getReferenceTo()[0];
				ReferenceObject obj = referenceObjectMap.get(referenceToObject);
				if (obj == null) {
					obj = new ReferenceObject(context, referenceToObject);
				}
				obj.referenceFields.add(destinationField.getName());
				referenceObjectMap.put(referenceToObject, obj);
			}
		}
		return referenceObjectMap;
	}

	private FieldMapResult isIgnored(final Field fieldDefinition) {
		final FieldMapResult result = new FieldMapResult();
		result.fieldName = fieldDefinition.getName();
		if (fieldDefinition.getName().toLowerCase().equals("id")) {
			return result;
		}
		if (fieldDefinition.isCalculated()) {
			result.ignored = true;
			result.ignoredReason = "isCalculated";
		} else if (fieldDefinition.isAutoNumber()) {
			result.ignored = true;
			result.ignoredReason = "isAutoNumber";
		} else if (fieldDefinition.isEncrypted()) {
			result.ignored = true;
			result.ignoredReason = "isEncrypted";
		} else if (fieldDefinition.isCreateable() == false) {
			result.ignored = true;
			result.ignoredReason = "is not Createable";
		} else if (fieldDefinition.isUpdateable() == false) {
			result.ignored = true;
			result.ignoredReason = "is not updateable";
		} else if (fieldDefinition.getType() == FieldType.id) {
			result.ignored = true;
			result.ignoredReason = "field type is ID";
		} else if (fieldDefinition.getType() == FieldType.reference) {
			result.ignored = true;
			result.ignoredReason = "#resolve : field type is reference";
		}
		return result;
	}

	private void printNameValuePairs(final List<Field> list) {
		System.out
		.println("# The following fields exist in both source and destination");
		for (final Field f : list) {
			System.out.println(f.getName() + "=" + f.getName());
		}
	}

	private void printWithComment(final List<Field> list) {
		System.out
		.println("# The following fields exist in source, but no match found in destination");
		for (final Field f : list) {
			System.out.println("#" + f.getName() + "=?");
		}
	}

	private void printIgnored(final List<FieldMapResult> list) {
		System.out
		.println("# The following fields exist in source, but are ignored because of their field type or schema.");
		for (final FieldMapResult result : list) {
			System.out.println("#" + result.fieldName + "= ("
					+ result.ignoredReason + ")");
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

	private class FieldMapResult {
		public String fieldName = "";
		public boolean ignored = false;
		public String ignoredReason = "";
	}
}