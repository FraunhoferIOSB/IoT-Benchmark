package de.fraunhofer.iosb.ilt.frostBenchmark;

import com.fasterxml.jackson.databind.JsonNode;
import de.fraunhofer.iosb.ilt.sta.ServiceFailureException;
import de.fraunhofer.iosb.ilt.sta.model.Datastream;
import de.fraunhofer.iosb.ilt.sta.model.Thing;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author scf
 */
public class ProcessorScheduler {

	/**
	 * The logger for this class.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(ProcessorScheduler.class);

	private BenchProperties benchProperties;
	private String mqttUrl;
	private boolean cleanSession = true;

	private List<ProcessorWorker> workers = new ArrayList<>();
	private Random random = new Random();

	public ProcessorScheduler(String mqttUrl) {
		benchProperties = new BenchProperties().readFromEnvironment();
		this.mqttUrl = mqttUrl;
	}

	public void updateSettings(JsonNode updatedProperties) throws MqttException {
		benchProperties.readFromJsonNode(updatedProperties);
		createProcessors();
	}

	public void terminate() {
		for (ProcessorWorker worker : workers) {
			worker.stop();
		}
	}

	private void createProcessors() throws MqttException {
		Thing benchmarkThing = BenchData.getBenchmarkThing();
		try {
			List<Datastream> datastreams = benchmarkThing
					.datastreams()
					.query()
					.select("@iot.id")
					.top(10000)
					.list()
					.toList();
			int datastreamCount = datastreams.size();
			int wantedCoverage = benchProperties.coverage;
			int wantedWorkers = wantedCoverage * datastreamCount / 100;
			int haveWorkers = workers.size();

			LOGGER.info("Requested coverage: {}.", wantedCoverage);

			if (wantedWorkers > haveWorkers) {
				LOGGER.info("Adding {} workers.", wantedWorkers - haveWorkers);
				String clientId = "BechmarkProcessor-" + System.currentTimeMillis();
				while (wantedWorkers > haveWorkers) {
					addProcessor(datastreams, clientId);
					haveWorkers++;
				}
			}

			if (wantedWorkers < haveWorkers) {
				LOGGER.info("Removing {} workers.", haveWorkers - wantedWorkers);
				while (wantedWorkers < haveWorkers) {
					ProcessorWorker worker = workers.remove(workers.size() - 1);
					worker.stop();
					haveWorkers--;
				}
			}
			int coverage = 100 * haveWorkers / datastreamCount;
			LOGGER.info("Coverage: {} / {} = {}%", haveWorkers, datastreamCount, coverage);

		} catch (ServiceFailureException ex) {
			LOGGER.error("Failed to load datastreams.");
		}
	}

	private void addProcessor(List<Datastream> datastreams, String clientId) throws MqttException {
		if (datastreams.isEmpty()) {
			LOGGER.error("No more datastreams to generate workers for.");
			return;
		}
		int nr = random.nextInt(datastreams.size());
		Datastream ds = datastreams.remove(nr);
		ProcessorWorker processor = new ProcessorWorker(
				mqttUrl,
				clientId + "-" + ds.getId().toString(),
				cleanSession);
		processor.setDataStreamTopic("v1.0/Datastreams(" + ds.getId().getUrl() + ")/Observations");
		processor.start();
		workers.add(processor);
	}

	public long resetNotificationCount() {
		long count = 0;
		for (ProcessorWorker worker : workers) {
			count += worker.resetNotificationsReceived();
		}
		return count;
	}

	public long getNotificationCount() {
		long count = 0;
		for (ProcessorWorker worker : workers) {
			count += worker.getNotificationsReceived();
		}
		return count;
	}
}
