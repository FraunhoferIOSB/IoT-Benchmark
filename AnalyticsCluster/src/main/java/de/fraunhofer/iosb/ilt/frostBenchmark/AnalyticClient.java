package de.fraunhofer.iosb.ilt.frostBenchmark;

import java.net.URISyntaxException;
import java.util.concurrent.ScheduledFuture;

import org.slf4j.LoggerFactory;

import de.fraunhofer.iosb.ilt.sta.ServiceFailureException;
import de.fraunhofer.iosb.ilt.sta.model.Datastream;
import de.fraunhofer.iosb.ilt.sta.model.Observation;
import de.fraunhofer.iosb.ilt.sta.model.ext.EntityList;

public class AnalyticClient implements Runnable {

	public static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AnalyticClient.class);

	private BenchData benchData;
	private String myName;
	private int queryCount = 0;
	private long startTime;
	private long lastTime;
	private int analyticCyles;
	private double someFancyResult;

	private Datastream datastream;

	public Thread myThread = null;
	private ScheduledFuture<?> schedulerHandle;

	public AnalyticClient(BenchData benchDataForAnalytics) {
		benchData = benchDataForAnalytics;
	}

	/**
	 * find or create the datastream for given name
	 *
	 * @param name The name
	 * @return this
	 * @throws ServiceFailureException if something goes wrong
	 * @throws URISyntaxException      if something goes wrong
	 */
	public AnalyticClient intialize(String name, int cylces) throws ServiceFailureException, URISyntaxException {
		myName = name;
		analyticCyles = cylces;
		datastream = benchData.getDatastream(myName);
		return this;
	}

	private double calculateQueryRate() {
		return (double) queryCount * 1000.0 / ((lastTime > startTime) ? lastTime - startTime : 1);
	}

	@Override
	public void run() {
		if (startTime == 0) {
			startTime = System.currentTimeMillis();
		}
		lastTime = System.currentTimeMillis();
//		double queryRate = calculateQueryRate();

		queryCount++;
		try {
			EntityList<Observation> obs = datastream.observations().query().select("phenomenonTime", "result").top(10)
					.list();
			doSomeAnalytics(obs);
		} catch (ServiceFailureException exc) {
			LOGGER.error("Failed to create observation.", exc);
		}
	}

	private void doSomeAnalytics(EntityList<Observation> obs) {
		// do some fancy stuff here to generate some cpu load
		someFancyResult = 0.0;
		for (int i = 0; i < analyticCyles; i++) {
			someFancyResult *= 0.123456789;
			for (Observation o : obs) {
				someFancyResult += Double.parseDouble(o.getResult().toString());
				someFancyResult += Math.sqrt(someFancyResult * i);
				someFancyResult = Math.log(someFancyResult*i);
				someFancyResult = Math.sin(someFancyResult*i);
				someFancyResult = Math.cos(someFancyResult*i);
				someFancyResult = Math.tan(someFancyResult*i);
				someFancyResult = Math.asin(someFancyResult*i);
				someFancyResult = Math.acos(someFancyResult*i);
				someFancyResult = Math.atan(someFancyResult*i);
			}	 
		}
	}

	public int reset() {
		double observateRate = calculateQueryRate();
		LOGGER.debug("{} created {} entries at a rate of {}/s", myName, queryCount,
				String.format("%.2f", observateRate));
		startTime = 0;
		int obsCount = queryCount;
		queryCount = 0;
		return obsCount;
	}

	public void cancel() {
		if (schedulerHandle != null) {
			schedulerHandle.cancel(false);
		}
		schedulerHandle = null;
	}

	public int getCreatedObsCount() {
		return queryCount;
	}
	
	public int getAnalyticCycles() {
		return analyticCyles;
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

}
