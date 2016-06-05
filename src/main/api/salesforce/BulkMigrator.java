package api.salesforce;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sforce.async.AsyncApiException;
import com.sforce.async.BatchInfo;
import com.sforce.async.BatchStateEnum;
import com.sforce.async.BulkConnection;
import com.sforce.async.CSVReader;
import com.sforce.async.ConcurrencyMode;
import com.sforce.async.ContentType;
import com.sforce.async.JobInfo;
import com.sforce.async.JobStateEnum;
import com.sforce.async.OperationEnum;
import com.sforce.soap.partner.DescribeSObjectResult;
import com.sforce.soap.partner.QueryResult;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.ws.ConnectionException;

/*
 * Org-to-Org (O2O) Bulk data migrator
 * Derived from See https://developer.salesforce.com/docs/atlas.en-us.api_asynch.meta/api_asynch/asynch_api_code_walkthrough.htm
 */
public class BulkMigrator {
	private final String DELIMITER = ",";
	private final List<BatchInfo> batchInfoList = new ArrayList<BatchInfo>();
	public List<String> Errors = new ArrayList<String>();
	private final BulkO2OContext m_context;
	private final Logger log;
	private final JobInfo m_job;
	private Integer batchCounter = 0;
	private final Resolver m_resolver;

	public BulkMigrator(final BulkO2OContext mContext) {
		m_context = mContext;
		printContext();
		m_resolver = new Resolver(mContext); // Resolves lookup ID references.
		m_resolver.initialize();
		JobInfo j = null;
		try {
			j = createJob(getContext().getSObjectType(),
					getContext().DestinationConnection.getBulkConnection());
		} catch (final AsyncApiException e) {
			e.printStackTrace();
		}
		m_job = j;
		createJobsDirectory(m_job);
		log = new Logger(m_job);
		log.info("Created JobID: " + m_job.getId());
	}

	public void printContext() {
		if (getContext().getConcurrencyMode().equals(ConcurrencyMode.Parallel)) {
			System.out.println("Concurrency Mode is parallel.");
		} else {
			System.out.println("Concurrency Mode is serial.");
		}
	}

	public BulkO2OContext getContext() {
		return m_context;
	}

	public boolean hasError() {
		return Errors.size() > 0;
	}

	public void run() throws ConnectionException, AsyncApiException,
	IOException {
		final String soql = generateQuery();
		log.info("Setting SOAP SOQL batch size to "
				+ getContext().getBatchSize());
		getContext().SourceConnection.getPartnerConnection().setQueryOptions(
				getContext().getBatchSize());

		QueryResult result = m_context.SourceConnection.getPartnerConnection()
				.query(soql);
		boolean done = false;
		if (result.getSize() > 0) {
			while (!done) {
				final SObject[] records = result.getRecords();
				log.info("Query result: " + records.length + " records.");
				createBatch(records);
				if (result.isDone()) {
					done = true;
				} else {
					result = m_context.SourceConnection.getPartnerConnection()
							.queryMore(result.getQueryLocator());
				}
			}
		}
		log.info("No more records to query");
		closeJob();
		awaitCompletion();
		checkResults();
	}

	public JobInfo getJobInfo() {
		return m_job;
	}

	private DescribeSObjectResult m_destinationSchema = null;

	public DescribeSObjectResult getDestinationSchema() {
		if (m_destinationSchema == null) {
			try {
				m_destinationSchema = getContext().DestinationConnection
						.getPartnerConnection().describeSObject(
								getContext().getSObjectType());
			} catch (final ConnectionException e) {
				e.printStackTrace();
			}
		}
		return m_destinationSchema;
	}

	private JobInfo createJob(final String sobjectType,
			final BulkConnection connection) throws AsyncApiException {
		final File dir = new File("jobs");
		dir.mkdir();

		JobInfo job = new JobInfo();
		job.setObject(sobjectType);
		job.setOperation(OperationEnum.insert);
		job.setConcurrencyMode(getContext().getConcurrencyMode());
		if (getContext().getOperationType().equals("upsert")) {
			job.setOperation(OperationEnum.upsert);
			if (getContext().getExternalFieldName() != null) {
				job.setExternalIdFieldName(getContext().getExternalFieldName());
			}
		}
		job.setContentType(ContentType.CSV);
		job = connection.createJob(job);
		System.out.println("Created job " + job);
		return job;
	}

	private boolean createJobsDirectory(final JobInfo job) {
		final File dir = new File("jobs");
		final boolean rootSuccess = dir.mkdir();

		final File jobdir = new File("jobs/" + job.getId());
		final boolean jobSuccess = jobdir.mkdir();

		return (rootSuccess && jobSuccess);
	}

	// Sorted list of all LValues in the property map.
	public List<String> allSourceFields() {
		final List<String> keys = new ArrayList<String>();
		for (final String key : getContext().getFieldMap()
				.stringPropertyNames()) {
			if (isConfigProperty(key)) {
				continue;
			}
			keys.add(key);
		}

		Collections.sort(keys);
		return keys;
	}

	private final List<String> configProperties = Arrays.asList("sobject",
			"queryfilter", "batchsize", "operationtype", "externalfield",
			"concurrencymode");

	public boolean isConfigProperty(final String key) {
		return configProperties.contains(key.toLowerCase());
	}

	private final String NOTE_TOKEN = "#Note";

	public boolean isNoteMappedField(final String key) {
		return (getContext().getFieldMap().containsKey(key) && getContext()
				.getFieldMap().get(key).equals(NOTE_TOKEN));
	}

	public String generateQuery() {
		String query = "SELECT ";
		for (final String key : allSourceFields()) {
			query += key + ",";
		}
		query = query.substring(0, query.length() - 1);
		query += " FROM " + getContext().getSObjectType();

		if (getContext().getQueryFilter() != null) {
			query += (" " + getContext().getQueryFilter());
		}

		log.info("Generated SOQL query to retrieve records: " + query);
		return query;
	}

	public void createBatch(final SObject[] records) throws IOException,
	AsyncApiException {
		PrintWriter out = null;
		File dir = new File("jobs");
		dir.mkdir();
		dir = new File("jobs/" + getJobInfo().getId());
		dir.mkdir();

		final String fileName = "jobs/" + getJobInfo().getId() + "/"
				+ getJobInfo().getId() + "-" + batchCounter++ + ".csv";
		out = new PrintWriter(fileName);

		// Write Header row
		String header = "";
		for (final String key : allSourceFields()) {
			if (key.toLowerCase().equals("id")) {
				continue;
			}
			final String destFieldName = getContext().getFieldMap()
					.getProperty(key);
			if (fieldNeedsLookupIDResolved(key) || fieldIsLiteralValue(key)) {
				header += key + DELIMITER;
			} else {
				header += destFieldName + DELIMITER;
			}
		}
		header += getContext().getExternalFieldName(); // Old_ID__c
		out.println(header);
		out.flush();

		for (final SObject record : records) {
			String row = "";
			if (record.getId() == null) {
				System.err.println("ExternalID field cannot be null");
				System.exit(1);
			}
			for (final String key : allSourceFields()) {
				if (key.toLowerCase().equals("id")) {
					continue;
				}
				Object fValue;
				if (fieldIsLiteralValue(key)) {
					fValue = getLiteralValue(key);
				} else {
					fValue = parseFieldValue(record, key);
				}
				if (fValue instanceof String) {
					fValue = escaped(fValue);
					if (requiresBoofing(key)) {
						fValue = boof((String) fValue);
					}
				}
				if (fieldNeedsLookupIDResolved(key)) {
					String newID = m_resolver.resolve(key, (String) fValue);
					if (newID == null) {
						final String resolveDefinition = getContext()
								.getFieldMap().getProperty(key);
						newID = m_resolver.defaultValue(resolveDefinition);
						if (newID != null) {
							System.out
							.println("Resolver using default value for "
									+ key + " : " + newID);
						} else {
							newID = "";
						}
					}
					log.info(key + " field for " + fValue + " resolved to "
							+ newID);
					fValue = newID;
				}
				row += fValue + DELIMITER;
			}
			row += record.getId();
			out.println(row);
			out.flush();
		}
		out.close();
		final BatchInfo bInfo = addBatchFileToJob(fileName);
		log.info("Created a batch for " + records.length
				+ " records. BatchID: " + bInfo.getId());
		batchInfoList.add(bInfo);
	}

	/*
	 * "Boofing" is a term used to describe appending Username or Email with
	 * bogus information to prevent conflicting with actual production values.
	 */
	private boolean requiresBoofing(final String key) {
		return getContext().getSObjectType().equals("User")
				&& (key.equals("Email") || key.equals("Username"));
	}

	private String escaped(final Object value) {
		String result = (String) value;
		final boolean containsDoubleQuote = result.contains("\"");
		final boolean containsComma = result.contains(",");
		final boolean containsNewline = (result.contains("\r") || result
				.contains("\n"));
		if (containsDoubleQuote) {
			result = result.replace("\"", "\"\"");
		}
		if (containsDoubleQuote || containsNewline || containsComma) {
			result = "\"" + result + "\"";
		}
		return result;
	}

	private String boof(final String val) {
		String suffix = Settings.get(Settings.USER_BOOF_SUFFIX);
		if (suffix == null || suffix.equals("")) {
			suffix = ".new";
		}
		return (val + suffix);
	}

	private Object parseFieldValue(final SObject record, final String fieldName) {
		Object fValue = record.getField(fieldName);
		if (fValue == null) {
			fValue = "";
		}
		return fValue;
	}

	private boolean fieldNeedsLookupIDResolved(final String key) {
		final String destFieldName = getContext().getFieldMap()
				.getProperty(key);
		return (destFieldName.toLowerCase().startsWith("#resolve") || destFieldName
				.toLowerCase().startsWith("#lookup"));
	}

	private boolean fieldIsLiteralValue(final String key) {
		final String destFieldName = getContext().getFieldMap()
				.getProperty(key);
		return (destFieldName.startsWith("\"") && destFieldName.endsWith("\""));
	}

	private String getLiteralValue(final String key) {
		final String fValue = getContext().getFieldMap().getProperty(key);
		return fValue.replace("\"", "");
	}

	private BatchInfo addBatchFileToJob(final String fileName)
			throws IOException, AsyncApiException {
		final File f = new File(fileName);
		final FileInputStream tmpInputStream = new FileInputStream(f);
		BatchInfo batchInfo;
		try {
			batchInfo = m_context.DestinationConnection.getBulkConnection()
					.createBatchFromStream(getJobInfo(), tmpInputStream);
			System.out.println(batchInfo);
		} finally {
			tmpInputStream.close();
		}
		return batchInfo;
	}

	private void closeJob() throws AsyncApiException {
		log.info("Closing Job: " + getJobInfo().getId());
		final JobInfo job = new JobInfo();
		job.setId(getJobInfo().getId());
		job.setState(JobStateEnum.Closed);
		m_context.DestinationConnection.getBulkConnection().updateJob(job);
	}

	private void awaitCompletion() throws AsyncApiException {
		log.info("Entering awaitCompletion(). Polling for status");
		long sleepTime = 0L;
		final Set<String> incomplete = new HashSet<String>();
		for (final BatchInfo bi : batchInfoList) {
			incomplete.add(bi.getId());
		}
		while (!incomplete.isEmpty()) {
			try {
				Thread.sleep(sleepTime);
			} catch (final InterruptedException e) {
			}
			log.info("Awaiting results for " + incomplete.size()
					+ " batches...");
			System.out.println("Awaiting results..." + incomplete.size());
			sleepTime = 10000L;
			final BatchInfo[] statusList = getContext().DestinationConnection
					.getBulkConnection().getBatchInfoList(getJobInfo().getId())
					.getBatchInfo();
			for (final BatchInfo b : statusList) {
				if (b.getState() == BatchStateEnum.Completed
						|| b.getState() == BatchStateEnum.Failed) {
					if (incomplete.remove(b.getId())) {
						log.info("Removing batch " + b.getId()
								+ ". Batch status: " + b);
						System.out.println("BATCH STATUS:\n" + b);
					}
				}
			}
		}
	}

	private void checkResults() throws AsyncApiException, IOException {
		log.info("Checking results...");
		for (final BatchInfo b : batchInfoList) {
			log.info("Checking results for batch ID: " + b.getId());
			final CSVReader rdr = new CSVReader(m_context.DestinationConnection
					.getBulkConnection().getBatchResultStream(
							getJobInfo().getId(), b.getId()));
			final List<String> resultHeader = rdr.nextRecord();
			final int resultCols = resultHeader.size();

			List<String> row;
			while ((row = rdr.nextRecord()) != null) {
				final Map<String, String> resultInfo = new HashMap<String, String>();
				for (int i = 0; i < resultCols; i++) {
					resultInfo.put(resultHeader.get(i), row.get(i));
				}
				final boolean success = Boolean.valueOf(resultInfo
						.get("Success"));
				final boolean created = Boolean.valueOf(resultInfo
						.get("Created"));
				final String id = resultInfo.get("Id");
				final String error = resultInfo.get("Error");
				if (success && created) {
					log.info("Created row with id " + id);
				} else if (!success) {
					log.error("Failed with error: " + error);
				}
			}
		}
	}
}