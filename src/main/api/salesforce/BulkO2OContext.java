package api.salesforce;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.sforce.async.ConcurrencyMode;

public class BulkO2OContext {
	public SalesforceConnection SourceConnection;
	public SalesforceConnection DestinationConnection;
	public String FieldMapName;
	
	private Properties m_fieldMap = null;
	public Properties getFieldMap() {
		if(m_fieldMap != null){
			return m_fieldMap;
		}
		if(FieldMapName == null){
			return null;
		}
		m_fieldMap = new Properties();
		InputStream input = null;
		try {
			String fileName = FieldMapName;
			input = new FileInputStream(fileName);
			m_fieldMap.load(input);
		} catch (final IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
		}
		return m_fieldMap;
	}
	
	public String getSObjectType(){
		return getFieldMap().getProperty("SObject");
	}
	
	public String getOperationType(){
		return getFieldMap().getProperty("OperationType");
	}
	
	public String getExternalFieldName(){
		return getFieldMap().getProperty("ExternalField");
	}
	
	public ConcurrencyMode getConcurrencyMode(){
		ConcurrencyMode mode = ConcurrencyMode.Parallel;
		if(getFieldMap().getProperty("ConcurrencyMode") == null){
			mode = ConcurrencyMode.Parallel;
		}
		else if(getFieldMap().getProperty("ConcurrencyMode").toLowerCase().equals("serial")){
			mode = ConcurrencyMode.Serial;
		}
		return mode;
	}
	
	private final Integer DEFAULT_BATCH_SIZE = 1000;
	public Integer getBatchSize(){
		if(getFieldMap().contains("BatchSize")){
			return DEFAULT_BATCH_SIZE;
		}
		return Integer.valueOf( getFieldMap().getProperty("BatchSize"));
	}
	
	public String getQueryFilter(){
		if(getFieldMap().getProperty("QueryFilter") != null 
				&& getFieldMap().getProperty("QueryFilter") != ""){
			return getFieldMap().getProperty("QueryFilter");
		} else {
			return null;
		}
	}
}