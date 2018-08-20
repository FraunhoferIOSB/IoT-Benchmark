package de.fraunhofer.iosb.ilt.frostBenchmark;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iosb.ilt.frostBenchmark.BenchProperties.STATUS;
import de.fraunhofer.iosb.ilt.sta.ServiceFailureException;
import de.fraunhofer.iosb.ilt.sta.model.Thing;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Scheduler {

	/**
	 * The logger for this class.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(Scheduler.class);
	public static final TypeReference<Map<String, Object>> TYPE_REF_MAP_STRING_OBJECT = new TypeReference<Map<String, Object>>() {
		// Empty by design.
	};
	private JsonNode scriptTree;
	private ObjectMapper mapper;

	public Scheduler() {
		mapper = new ObjectMapper();
	}

	public void readSchedule(String scheduleFile) {
		FileReader fr;
		try {
			fr = new FileReader(scheduleFile);
			scriptTree = mapper.readTree(fr);
		} catch (IOException e) {
			LOGGER.error("Failed to load json.", e);
		}
	}

	/**
	 * Returns the value as a number if it can be parsed to a number.
	 *
	 * @param value The value to parse.
	 * @return a Number or a String.
	 */
	private Object convert(String value) {
		try {
			return Long.parseLong(value);
		} catch (NumberFormatException ex) {
			LOGGER.trace("Value {} is not a Long", value);
		}
		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException ex) {
			LOGGER.trace("Value {} is not a Double", value);
		}
		return value;
	}

	public void sendParameter(String processName, String paramName, String valueString) throws ServiceFailureException {
		Map<String, Object> nameMap = new HashMap<>();
		nameMap.put(paramName, convert(valueString));

		Map<String, Object> propertiesMap = new HashMap<>();
		propertiesMap.put(processName, nameMap);
		propertiesMap.put(BenchProperties.TAG_STATUS, STATUS.INITIALIZE);

		Thing sessionThing = BenchData.getBenchmarkThing();
		sessionThing.setProperties(propertiesMap);
		BenchData.service.update(sessionThing);
	}

	public void runScript() throws ServiceFailureException, InterruptedException {
		if (scriptTree == null) {
			return;
		}

		JsonNode initProperties = scriptTree.get("initialize");
		sendCommands(initProperties, STATUS.INITIALIZE);

		JsonNode sequence = scriptTree.get("sequence");
		JsonNode run = null;
		for (int i = 0; i < sequence.size(); i++) {
			run = sequence.get(i);
			Long duration = run.get("duration").asLong();
			Long seqId = run.get("seq").asLong();
			System.out.println("run experiment " + seqId + " for " + duration + " msec");
			System.out.println("using settings: " + run.toString());

			sendCommands(run, STATUS.RUNNING);
			Thread.sleep(duration);
		}
		sendCommands(run, STATUS.FINISHED);
		System.out.println("finished");
	}

	public void sendCommands(STATUS status) throws ServiceFailureException {
		sendCommands(new HashMap<>(), status);
	}

	public void sendCommands(JsonNode properties, STATUS status) throws ServiceFailureException {
		Map<String, Object> propertiesMap;
		if (properties == null) {
			propertiesMap = new HashMap<>();
		} else {
			propertiesMap = mapper.convertValue(properties, TYPE_REF_MAP_STRING_OBJECT);
		}
		sendCommands(propertiesMap, status);
	}

	public void sendCommands(Map<String, Object> propertiesMap, STATUS status) throws ServiceFailureException {
		Thing sessionThing = BenchData.getBenchmarkThing();
		propertiesMap.put(BenchData.TAG_SESSION, BenchData.sessionId);
		propertiesMap.put(BenchData.TAG_TYPE, BenchData.VALUE_TYPE_CONTROL);
		propertiesMap.put(BenchProperties.TAG_STATUS, status);
		sessionThing.setProperties(propertiesMap);
		BenchData.service.update(sessionThing);
	}

}
