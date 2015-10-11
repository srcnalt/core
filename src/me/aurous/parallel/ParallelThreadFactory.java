package me.aurous.parallel;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Andrew
 *
 */
public class ParallelThreadFactory implements ThreadFactory {
	private static final AtomicLong WORKER_POOL_SIZE = new AtomicLong(0);
	private final AtomicLong workerNumber = new AtomicLong(0);
	private final String workerPrefix;
	private final boolean isDaemon;
	private final long workerPoolNumber;

	public ParallelThreadFactory(final String prefex) {
		this(prefex, true);
	}

	public ParallelThreadFactory(final String prefix, final boolean isDaemon) {
		this.isDaemon = isDaemon;
		workerPrefix = prefix;
		workerPoolNumber = WORKER_POOL_SIZE.incrementAndGet();
	}

	@Override
	public Thread newThread(final Runnable r) {
		final Thread factoryThread = new Thread(r, workerPrefix + "-"
				+ workerPoolNumber + "-Thread-"
				+ workerNumber.incrementAndGet());
		if (factoryThread.isDaemon() != isDaemon) {
			factoryThread.setDaemon(isDaemon);
		}

		return factoryThread;
	}
}