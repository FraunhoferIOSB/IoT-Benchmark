package de.fraunhofer.iosb.ilt.frostBenchmark;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Iterator;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BenchProperties {

	public static final String TAG_SESSION = "SESSION";
	public static final String TAG_BASE_URL = "BASE_URL";
	public static final String TAG_PROXYHOST = "proxyhost";
	public static final String TAG_PROXYPORT = "proxyport";

	public static final String TAG_SCRIPT = "SCRIPT";
	public static final String DFLT_SCRIPT = "script.json";

	public static final String TAG_TIMEOUT = "timeout";			// not used
	public static final int DFLT_TIMEOUT = 10000;

	public static final String TAG_POSTDELAY = "POSTDELAY";		// not used
	public static final int DFLT_POSTDELAY = 1000;

	public static final String TAG_COVERAGE = "COVERAGE";		// subscription coverage of datastreams in percentage 
	public static final int DFLT_COVERAGE = 100;

	public static final String TAG_PERIOD = "PERIOD";			// delay in ms between two observation posts. defines the insertion rate
	public static final int DFLT_PERIOD = 500;

	public static final String TAG_JITTER = "JITTER";			// variation interval added to period
	public static final int DFLT_JITTER = 5;

	public static final String TAG_SENSORS = "SENSORS";			// number of sensor to be used
	public static final int DFLT_SENSORS = 20;

	public static final String TAG_ANALYTIC_JOBS = "ANALYTIC_JOBS";		// number of analytic calculation cycles
	public static final int DFLT_ANALYTICS_JOBS = 20;

	public static final String TAG_ANALYTIC_LOOPS = "ANALYTIC_LOOPS";		// number of analytic calculation cycles
	public static final int DFLT_ANALYTICS_LOOPS = 20;

	public static final String TAG_WORKERS = "WORKERS";			// number of worker threads used 
	public static final int DFLT_WORKERS = 10;
	

	public static final String TAG_STATUS = "status";			// benchmark status

	public static enum STATUS {
		INITIALIZE,
		RUNNING,
		FINISHED,
		TERMINATE;
	}
	/**
	 * The logger for this class.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(BenchProperties.class);

	public int timeout = 10000;
	public int postdelay = 1000;
	public int coverage = 100;
	public int period = DFLT_PERIOD;
	public int jitter = DFLT_JITTER;
	public int sensors = DFLT_SENSORS;
	public int analyticLoops = DFLT_ANALYTICS_LOOPS;
	public int analyticJobs = DFLT_ANALYTICS_JOBS;
	public int workers = DFLT_WORKERS;

	public BenchProperties readFromEnvironment() {
		workers = getEnv(TAG_WORKERS, DFLT_WORKERS);
		coverage = getEnv(TAG_COVERAGE, DFLT_COVERAGE);
		postdelay = getEnv(TAG_POSTDELAY, DFLT_POSTDELAY);
		period = getEnv(TAG_PERIOD, DFLT_PERIOD);
		jitter = getEnv(TAG_JITTER, DFLT_JITTER);
		sensors = getEnv(TAG_SENSORS, DFLT_SENSORS);
		analyticLoops = getEnv(TAG_ANALYTIC_LOOPS, DFLT_ANALYTICS_LOOPS);
		analyticJobs = getEnv(TAG_ANALYTIC_JOBS, DFLT_ANALYTICS_JOBS);
		return this;
	}

	public BenchProperties readFromJsonNode(JsonNode adds) {
		if (adds == null) {
			return this;
		}
		workers = getProperty(adds, TAG_WORKERS, workers);
		coverage = getProperty(adds, TAG_COVERAGE, coverage);
		postdelay = getProperty(adds, TAG_POSTDELAY, postdelay);
		period = getProperty(adds, TAG_PERIOD, period);
		jitter = getProperty(adds, TAG_JITTER, jitter);
		sensors = getProperty(adds, TAG_SENSORS, sensors);
		analyticLoops = getProperty(adds, TAG_ANALYTIC_LOOPS, analyticLoops);
		analyticJobs = getProperty(adds, TAG_ANALYTIC_JOBS, analyticJobs);
		return this;
	}

	@SuppressWarnings("unchecked")
	static public JsonNode mergeProperties(JsonNode base, JsonNode adds, boolean recursive) {
		JsonNode combinedProperties = base;
		for (Iterator<Map.Entry<String, JsonNode>> fields = adds.fields(); fields.hasNext();) {
			Map.Entry<String, JsonNode> entry = fields.next();
			String key = entry.getKey();
			JsonNode newValue = entry.getValue();
			JsonNode origValue = base.get(key);
			if (recursive && newValue.isObject() && origValue.isObject()) {
				mergeProperties(origValue, newValue, true);
			} else {
				((ObjectNode) combinedProperties).set(key, newValue);
			}
		}
		return combinedProperties;
	}

	static public String getProperty(JsonNode from, String name, String dflt) {
		JsonNode value = from.get(name);
		if (value == null || !value.isTextual()) {
			return dflt;
		}
		return value.asText();
	}

	static public int getProperty(JsonNode from, String name, int dflt) {
		JsonNode value = from.get(name);
		if (value == null || !value.isNumber()) {
			return dflt;
		}
		return value.intValue();
	}

	public static String getEnv(String name, String deflt) {
		String value = System.getenv(name);
		if (value == null) {
			return deflt;
		}
		return value;
	}

	public static int getEnv(String name, int deflt) {
		String value = System.getenv(name);
		if (value == null) {
			return deflt;
		}
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException ex) {
			LOGGER.trace("Failed to parse parameter to int.", ex);
			LOGGER.info("Value for {} ({}) was not an Integer", name, value);
			return deflt;
		}
	}
}
