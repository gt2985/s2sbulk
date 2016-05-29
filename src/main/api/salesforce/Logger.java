package api.salesforce;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.joda.time.DateTime;

import com.sforce.async.JobInfo;

public class Logger {
	private final PrintWriter m_writer;
	private final JobInfo job;
	public enum EventType{
		INFO,
		WARNING,
		ERROR
	}
	
	public Logger(JobInfo jobInfo){
		job = jobInfo;
		PrintWriter w;
		try {
			File dir = new File("jobs");
			boolean successful = dir.mkdir();
			dir = new File("jobs/" + job.getId());
			successful = dir.mkdir();
			w = new PrintWriter("jobs/" + job.getId() + "/log.csv", "UTF-8");
		} catch (FileNotFoundException e) {
			w = null;
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			w = null;
			e.printStackTrace();
		}
		m_writer = w;
		this.writeHeader();
	}
	
	private String getFileName(String sObjectName){
		String out = new SimpleDateFormat("yyyy-MM-dd_hh-mm-ss").format(new Date());
		return out + "-" + sObjectName + "-log.csv";
	}
	
	private void writeHeader(){
		m_writer.println("timestamp,type,message");
		m_writer.flush();
	}
	
	private final String DELIMITER = ",";
	public void write(EventType type, String message){
		m_writer.println(DateTime.now().toString() + DELIMITER + type.toString() + DELIMITER + message);
		m_writer.flush();
	}
	
	public void write(String message){
		m_writer.println(DateTime.now().toString() + DELIMITER + EventType.INFO.toString() + DELIMITER + message);
		m_writer.flush();
	}
	
	public void info(String message){
		m_writer.println(DateTime.now().toString() + DELIMITER + EventType.INFO.toString() + DELIMITER + message);
		m_writer.flush();
	}
	
	public void error(String message){
		m_writer.println(DateTime.now().toString() + DELIMITER + EventType.ERROR.toString() + DELIMITER + message);
		m_writer.flush();
	}
	
	public void close(){
		m_writer.flush();
		m_writer.close();
	}
}
