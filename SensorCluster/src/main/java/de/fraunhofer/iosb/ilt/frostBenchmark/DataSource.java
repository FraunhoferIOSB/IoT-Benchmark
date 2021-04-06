package de.fraunhofer.iosb.ilt.frostBenchmark;

import de.fraunhofer.iosb.ilt.sta.ServiceFailureException;
import de.fraunhofer.iosb.ilt.sta.model.Datastream;
import de.fraunhofer.iosb.ilt.sta.model.FeatureOfInterest;
import de.fraunhofer.iosb.ilt.sta.model.Observation;
import de.fraunhofer.iosb.ilt.sta.service.SensorThingsService;
import java.net.URISyntaxException;
import java.util.concurrent.ScheduledFuture;
import org.slf4j.LoggerFactory;

public class DataSource implements Runnable {

	public static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DataSource.class);

	private SensorThingsService service;
	private String myName;
	private int createdObsCount = 0;
	private long startTime = 0;
	private long lastTime = 0;

	private Datastream datastream;
	private FeatureOfInterest foi;

	public Thread myThread = null;
	private ScheduledFuture<?> schedulerHandle;
	private boolean cacheFoi = false;

	public DataSource(SensorThingsService sensorThingsService) {
		service = sensorThingsService;
	}

	/**
	 * find or create the datastream for given name
	 *
	 * @param name The name
	 * @return this
	 * @throws ServiceFailureException if something goes wrong
	 * @throws URISyntaxException if something goes wrong
	 */
	public DataSource intialize(String name) throws ServiceFailureException, URISyntaxException {
		myName = name;
		datastream = SensorCluster.benchData.getDatastream(myName);
		return this;
	}

	private double calculateRate() {
		return (double) createdObsCount * 1000.0 / ((lastTime > startTime) ? lastTime - startTime : 1);
	}

	@Override
	public void run() {
		if (startTime == 0) {
			startTime = System.currentTimeMillis();
		}
		lastTime = System.currentTimeMillis();
		double observateRate = calculateRate();
		Observation o = new Observation(observateRate, datastream);
		if (cacheFoi) {
			o.setFeatureOfInterest(foi);
		}
		createdObsCount++;
		try {
			service.create(o);
			if (cacheFoi && foi == null) {
				foi = o.getFeatureOfInterest();
				if (foi == null) {
					LOGGER.error("Failed to retrieve FoI");
					cacheFoi = false;
				} else {
					foi = foi.withOnlyId();
				}
			}
		} catch (ServiceFailureException exc) {
			LOGGER.error("Failed to create observation.", exc);
		}
	}

	public int reset() {
		double observateRate = calculateRate();
		LOGGER.debug("{} created {} entries at a rate of {}/s", myName, createdObsCount, String.format("%.2f", observateRate));
		startTime = 0;
		int obsCount = createdObsCount;
		createdObsCount = 0;
		return obsCount;
	}

	public void cancel() {
		if (schedulerHandle != null) {
			schedulerHandle.cancel(false);
		}
		schedulerHandle = null;
	}

	public int getCreatedObsCount() {
		return createdObsCount;
	}

	public ScheduledFuture<?> getSchedulerHandle() {
		return schedulerHandle;
	}

	public void setSchedulerHandle(ScheduledFuture<?> schedulerHandle) {
		if (this.schedulerHandle != null) {
			LOGGER.warn("Sensor is scheduled twice without cancelling first!");
			this.cancel();
		}
		this.schedulerHandle = schedulerHandle;
	}

	/**
	 * @return the cacheFoi
	 */
	public boolean isCacheFoi() {
		return cacheFoi;
	}

	/**
	 * @param cacheFoi the cacheFoi to set
	 */
	public void setCacheFoi(boolean cacheFoi) {
		this.cacheFoi = cacheFoi;
	}

}
