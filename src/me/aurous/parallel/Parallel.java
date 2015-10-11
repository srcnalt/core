package me.aurous.parallel;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import me.aurous.exceptions.ExceptionWidget;

/**
 * @author Andrew
 *
 */

public class Parallel {

	private static final int TOTAL_CORES = Runtime.getRuntime()
			.availableProcessors();

	private static final ForkJoinPool forkPool = new ForkJoinPool(TOTAL_CORES,
			ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);

	public static <T> void awaitFor(final Iterable<? extends T> elements,
			final Operation<T> operation) {
		awaitFor(2 * TOTAL_CORES, elements, operation);
	}

	public static <T> void awaitFor(final int totalThreads,
			final Iterable<? extends T> elements, final Operation<T> operation) {
		For(totalThreads, new ParallelThreadFactory("Parallel.For"), elements,
				operation, Integer.MAX_VALUE, TimeUnit.DAYS);
	}

	public static <T> void For(final Iterable<? extends T> elements,
			final Operation<T> operation) {
		For(2 * TOTAL_CORES, elements, operation);
	}

	public static <T> void For(final int totalThreads,
			final Iterable<? extends T> elements, final Operation<T> operation) {
		For(totalThreads, new ParallelThreadFactory("Parallel.For"), elements,
				operation, null, null);
	}

	public static <S extends T, T> void For(final int totalThreads,
			final ParallelThreadFactory threadFactory,
			final Iterable<S> elements, final Operation<T> operation,
			final Integer wait, final TimeUnit waitUnit) {

		final ThreadPoolExecutor threadPoolWorker = new ThreadPoolExecutor(
				totalThreads, totalThreads, 0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>());
		final ThreadSafeIterator<S> itr = new ThreadSafeIterator<S>(
				elements.iterator());

		for (int i = 0; i < threadPoolWorker.getMaximumPoolSize(); i++) {
			threadPoolWorker.submit(() -> {
				T element;
				while ((element = itr.next()) != null) {
					try {
						operation.perform(element);
					} catch (final Exception e) {

						ExceptionWidget widget = new ExceptionWidget(e);
						widget.showWidget();
						continue;
					}
				}
				return null;
			});
		}

		threadPoolWorker.shutdown();

		if (wait != null) {
			try {
				threadPoolWorker.awaitTermination(wait, waitUnit);
			} catch (final InterruptedException e) {
				ExceptionWidget widget = new ExceptionWidget(e);
				widget.showWidget();
			}
		}
	}

	private static class ThreadSafeIterator<T> {

		private final Iterator<T> itr;

		public ThreadSafeIterator(final Iterator<T> itr) {
			this.itr = itr;
		}

		public synchronized T next() {
			return itr.hasNext() ? itr.next() : null;
		}
	}

	public static <T> void ForJoin(final Iterable<T> elements,
			final Operation<T> operation) {
		forkPool.invokeAll(createCallables(elements, operation));
	}

	public static <T> Collection<Callable<Void>> createCallables(
			final Iterable<T> elements, final Operation<T> operation) {
		final List<Callable<Void>> callables = new LinkedList<Callable<Void>>();
		for (final T elem : elements) {
			callables.add(() -> {
				operation.perform(elem);
				return null;
			});
		}

		return callables;
	}

	public static interface Operation<T> {
		public void perform(T pParameter);
	}

}