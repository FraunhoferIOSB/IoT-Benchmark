package de.fraunhofer.iosb.ilt.frostBenchmark;

import com.fasterxml.jackson.databind.JsonNode;
import de.fraunhofer.iosb.ilt.sta.ServiceFailureException;
import de.fraunhofer.iosb.ilt.sta.model.Datastream;
import de.fraunhofer.iosb.ilt.sta.model.Observation;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.LoggerFactory;

public class SensorScheduler {

	public static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SensorScheduler.class);

	private ScheduledExecutorService sensorScheduler;
	private ScheduledExecutorService outputScheduler;
	private ScheduledFuture<?> outputTask;

	/**
	 * How many seconds between stats outputs.
	 */
	private int outputPeriod = 1;

	private List<DataSource> dsList = new ArrayList<>();
	private long startTime = 0;
	private long stopTime = 0;

	BenchProperties settings;
	private boolean running = false;

	/**
	 * TODO pass in settings object instead of using static BenchProperties
	 */
	public SensorScheduler() {
		//BenchData.initialize();
		settings = new BenchProperties().readFromEnvironment();
		sensorScheduler = Executors.newScheduledThreadPool(settings.workers);
		outputScheduler = Executors.newSingleThreadScheduledExecutor();
	}

	private int logUpdates(String name, int oldVal, int newVal) {
		if (oldVal != newVal) {
			LOGGER.info("Updating value of {} from {} to {}.", name, oldVal, newVal);
		}
		return newVal;
	}

	private void warnIfChanged(String name, int oldVal, int newVal) {
		if (oldVal != newVal) {
			LOGGER.warn("Changing parameter {} is not supported, using old value {} instead of new value {}.", name, oldVal, newVal);
		}
	}

	private void sendRateObservation(double rate) {
		try {
			Datastream ds = SensorCluster.resultData.getDatastream(SensorCluster.resultData.getEnv(BenchData.TAG_NAME, "SensorCluster"));
			SensorCluster.resultData.service.create(new Observation(rate, ds));
		} catch (ServiceFailureException exc) {
			LOGGER.error("Failed to send current observation rate.", exc);
		}
	}

	public synchronized void initWorkLoad(JsonNode updatedProperties) throws ServiceFailureException, URISyntaxException {
		if (running) {
			stopWorkLoad();
		}
		int oldWorkerCount = settings.workers;
		int oldSensors = settings.sensors;
		int oldPeriod = settings.period;
		int oldJitter = settings.jitter;
		settings.readFromJsonNode(updatedProperties);

		LOGGER.debug("Benchmark initializing, starting workers");
		logUpdates(BenchProperties.TAG_SENSORS, oldSensors, settings.sensors);
		logUpdates(BenchProperties.TAG_PERIOD, oldPeriod, settings.period);
		logUpdates(BenchProperties.TAG_JITTER, oldJitter, settings.jitter);
		logUpdates(BenchProperties.TAG_WORKERS, oldWorkerCount, settings.workers);

		if (oldWorkerCount != settings.workers) {
			cleanupScheduler(false);
			sensorScheduler = Executors.newScheduledThreadPool(settings.workers);
		}

		int haveCount = dsList.size();
		if (settings.sensors != haveCount) {
			if (settings.sensors > haveCount) {
				int toAdd = settings.sensors - haveCount;
				LOGGER.info("Setting up {} sensors...", toAdd);
				for (int i = haveCount; i < settings.sensors; i++) {
					String name = "Benchmark." + i;
					DataSource sensor = new DataSource(SensorCluster.benchData.service).intialize(name);
					dsList.add(sensor);
					if ((i - haveCount) % 100 == 0) {
						LOGGER.info("... {}", i - haveCount);
					}
				}
			}
			if (settings.sensors < haveCount) {
				int toRemove = haveCount - settings.sensors;
				LOGGER.info("Taking down {} sensors...", toRemove);
				while (dsList.size() > settings.sensors) {
					DataSource ds = dsList.remove(dsList.size() - 1);
					ds.cancel();
				}
			}
			LOGGER.info("Done.");
		}

		LOGGER.trace("Benchmark initialized");
	}

	public synchronized void startWorkLoad(JsonNode properties) throws ServiceFailureException, URISyntaxException {
		if (running) {
			stopWorkLoad();
		}

		int oldPeriod = settings.period;
		int oldWorkerCount = settings.workers;
		int oldSensors = settings.sensors;
		settings.readFromJsonNode(properties);

		if (oldWorkerCount != settings.workers || oldSensors != settings.sensors) {
			logUpdates(BenchProperties.TAG_WORKERS, oldWorkerCount, settings.workers);
			logUpdates(BenchProperties.TAG_SENSORS, oldSensors, settings.sensors);
			LOGGER.info("Re-initialising sensors.");
			for (DataSource sensor : dsList) {
				sensor.cancel();
			}
			initWorkLoad(properties);
		}
		running = true;
		startTime = System.currentTimeMillis();

		if (properties != null) {
			logUpdates(BenchProperties.TAG_PERIOD, oldPeriod, settings.period);
		}

		LOGGER.info("Starting workload: {} workers, {} sensors, {} delay, {} jitter.", settings.workers, settings.sensors, settings.period, settings.jitter);
		double delayPerSensor = ((double) settings.period) / settings.sensors;
		double currentDelay = 0;
		for (DataSource sensor : dsList) {
			ScheduledFuture<?> handle = sensorScheduler.scheduleAtFixedRate(sensor, (long) currentDelay, settings.period - settings.jitter / 2, TimeUnit.MILLISECONDS);
			sensor.setSchedulerHandle(handle);
			currentDelay += delayPerSensor;
		}

		if ((outputTask == null) && (outputPeriod > 0)) {
			LOGGER.info("starting reporter with {} sec initial delay and {} report period", outputPeriod, outputPeriod);
			outputTask = outputScheduler.scheduleAtFixedRate(this::printStats, outputPeriod, outputPeriod, TimeUnit.SECONDS);
		}
	}

	public synchronized void stopWorkLoad() {
		LOGGER.trace("Benchmark finishing");

		if (outputTask != null) {
			outputTask.cancel(true);
			outputTask = null;
		}

		for (DataSource sensor : dsList) {
			sensor.cancel();
		}

		stopTime = System.currentTimeMillis();

		int entries = 0;
		for (DataSource sensor : dsList) {
			entries += sensor.reset();
		}

		double rate = 1000.0 * entries / (stopTime - startTime);
		LOGGER.info("==> {} entries created per sec", String.format("%.2f", rate));

		sendRateObservation(rate);

		LOGGER.info("Benchmark finished");
		running = false;
	}

	public void printStats() {
		long curTime = System.currentTimeMillis();
		int entries = 0;
		for (DataSource sensor : dsList) {
			entries += sensor.getCreatedObsCount();
		}

		double rate = 1000.0 * entries / (curTime - startTime);
		LOGGER.info("==> {} per sec", String.format("%.2f", rate));

		sendRateObservation(rate);
	}

	private void cleanupScheduler(boolean all) {
		if (all) {
			outputScheduler.shutdown();
		}
		sensorScheduler.shutdown();
		boolean allOk = true;
		try {
			allOk = sensorScheduler.awaitTermination(2, TimeUnit.SECONDS);
		} catch (InterruptedException ex) {
			LOGGER.trace("Woken up, wait done.", ex);
		}
		if (!allOk) {
			sensorScheduler.shutdownNow();
		}
	}

	public synchronized void terminate() {
		stopWorkLoad();
		cleanupScheduler(true);
	}

	/**
	 * How many seconds between stats outputs.
	 *
	 * @return the outputPeriod
	 */
	public int getOutputPeriod() {
		return outputPeriod;
	}

	/**
	 * How many seconds between stats outputs.
	 *
	 * @param outputRate the outputPeriod to set
	 */
	public void setOutputPeriod(int outputRate) {
		this.outputPeriod = outputRate;
	}

}
