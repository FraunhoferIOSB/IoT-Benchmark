package de.fraunhofer.iosb.ilt.frostBenchmark;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iosb.ilt.frostBenchmark.BenchProperties.STATUS;
import static de.fraunhofer.iosb.ilt.frostBenchmark.BenchProperties.TAG_SESSION;
import de.fraunhofer.iosb.ilt.sta.ServiceFailureException;
import de.fraunhofer.iosb.ilt.sta.model.Datastream;
import de.fraunhofer.iosb.ilt.sta.model.Observation;
import de.fraunhofer.iosb.ilt.sta.model.Thing;
import de.fraunhofer.iosb.ilt.sta.model.ext.EntityList;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Random;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.LoggerFactory;

public class StreamProcessor extends MqttHelper {

	public static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(StreamProcessor.class);
	public static BenchData benchData = null;
	public static BenchData resultData = null;

	private static long startTime = 0;
	public static int qos = 2;
	private ObjectMapper parser;
	/**
	 * The name to use when reading properties.
	 */
	private String name = "properties";

	public StreamProcessor(String name, String brokerUrl, String clientId, boolean cleanSession) throws MqttException {
		super(brokerUrl, clientId, cleanSession);
		this.parser = new ObjectMapper();
		this.name = name;
	}

	public static void main(String[] args) throws IOException, URISyntaxException, ServiceFailureException {
		String clientId = "BechmarkProcessor-" + System.currentTimeMillis();
		boolean cleanSession = true; // Non durable subscriptions

		String baseUriStr = BenchProperties.getEnv(BenchData.BASE_URL, "http://localhost:8080/FROST-Server/v1.0/").trim();
		String resultUriStr = BenchProperties.getEnv(BenchData.TAG_RESULT_URL, baseUriStr).trim();
		LOGGER.info("Using SensorThings Service at {} for benchmark data", baseUriStr);
		benchData = new BenchData().initialize(baseUriStr);
		LOGGER.info("Using SensorThings Service at {} for result data", resultUriStr);
		resultData = new BenchData().initialize(resultUriStr);
		Thing benchmarkThing = benchData.getBenchmarkThing();

		BenchProperties benchProperties = new BenchProperties().readFromEnvironment();

		try {
			// create processors for Datastream according to coverage
			Random random = new Random();
			int nbProcessors = 0;
			EntityList<Datastream> datastreams = benchData.service.datastreams().query()
					.filter("properties/" + BenchData.TAG_SESSION + " eq '" + benchData.sessionId + "'")
					.select("@iot.id")
					.top(10000)
					.list();
			int dsCount = 0;
			for (Iterator<Datastream> it = datastreams.fullIterator(); it.hasNext();) {
				Datastream dataStream = it.next();
				dsCount++;
				if (random.nextInt(100) < benchProperties.coverage) {
					ProcessorWorker processor = new ProcessorWorker(benchData.broker, clientId + "-" + dataStream.getId().toString(),
							cleanSession);
					processor.setDataStreamTopic("v1.0/Datastreams(" + dataStream.getId().toString() + ")/Observations");
					new Thread(processor).start();
					nbProcessors++;
				}
			}
			if (dsCount > 0) {
				LOGGER.info(nbProcessors + " created out of " + dsCount + " Datastreams (coverage="
					+ 100 * nbProcessors / dsCount + "[" + benchProperties.coverage + "]");
			} else {
				LOGGER.info ("no datastreams found");
			}
					

			// subscribeAndWait for benchmark commands
			String topic = "v1.0/Things(" + benchmarkThing.getId().toString() + ")/properties";
			StreamProcessor processor = new StreamProcessor(benchData.name, benchData.broker, clientId, cleanSession);
			processor.subscribeAndWait(topic, qos);

		} catch (MqttException me) {
			LOGGER.error("MQTT exception", me);
		} catch (Throwable me) {
			LOGGER.error("Something bad happened.", me);
		}
	}

	@Override
	/**
	 * @throws URISyntaxException
	 * @throws ServiceFailureException
	 * @see MqttCallback#messageArrived(String, MqttMessage)
	 */
	public void messageArrived(String topic, MqttMessage message) throws MqttException, ServiceFailureException, URISyntaxException {
		JsonNode msg = null;
		try {
			msg = parser.readTree(new String(message.getPayload()));
		} catch (IOException e) {
			LOGGER.error("can not parse mqtt message", e);
			System.exit(1);
		}
		JsonNode properties = msg.get(name);
		BenchProperties benchProperties = new BenchProperties().readFromJsonNode(properties);

		STATUS benchState = STATUS.TERMINATE;
		String statusString = properties.get(BenchProperties.TAG_STATUS).asText();
		try {
			benchState = STATUS.valueOf(statusString.toUpperCase());
		} catch (IllegalArgumentException exc) {
			LOGGER.error("Received unknown status value: {}", statusString);
			LOGGER.trace("Exception: ", exc);
		}

		LOGGER.info("Entering {} mode", benchState);
		switch (benchState) {
			case RUNNING:
				// start the client
				LOGGER.info("Starting Processor Test");
				startTime = System.currentTimeMillis();
				ProcessorWorker.setNotificationsReceived(0);
				break;

			case FINISHED:
				// get the results
				long endTime = System.currentTimeMillis();

				Datastream ds = resultData.getDatastream("SubsriberCluster" + benchData.name);
				double rate = (1000 * ProcessorWorker.getNotificationsReceived()) / (endTime - startTime);
				try {
					resultData.service.create(new Observation(rate, ds));
				} catch (ServiceFailureException e) {
					LOGGER.trace("Exception: ", e);
				}

				LOGGER.info(ProcessorWorker.getNotificationsReceived() + " Notifications received");
				LOGGER.info((1000 * ProcessorWorker.getNotificationsReceived()) / (endTime - startTime) + " notifications per sec");
				break;

			case TERMINATE:
				LOGGER.info("Terminate Command received - exit process");
				setState(STATE.DISCONNECT);
				LOGGER.info("Terminate");
				break;

			default:
				LOGGER.error("Unhandled state: {}", benchState);
		}
	}

}
