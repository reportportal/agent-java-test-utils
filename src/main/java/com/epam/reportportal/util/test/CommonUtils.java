/*
 *  Copyright 2020 EPAM Systems
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.epam.reportportal.util.test;

import io.reactivex.Maybe;
import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

@SuppressWarnings("unused")
public class CommonUtils {

	// 20 milliseconds is enough to separate one test from another
	public static final long MINIMAL_TEST_PAUSE = 20L;

	public static class ExecutorService implements java.util.concurrent.ExecutorService, AutoCloseable {
		private final java.util.concurrent.ExecutorService delegate;

		public ExecutorService(java.util.concurrent.ExecutorService delegate) {
			this.delegate = delegate;
		}

		@Override
		public void shutdown() {
			delegate.shutdown();
		}

		@Override
		@Nonnull
		public List<Runnable> shutdownNow() {
			return delegate.shutdownNow();
		}

		@Override
		public boolean isShutdown() {
			return delegate.isShutdown();
		}

		@Override
		public boolean isTerminated() {
			return delegate.isTerminated();
		}

		@Override
		public boolean awaitTermination(long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
			return delegate.awaitTermination(timeout, unit);
		}

		@Override
		@Nonnull
		public <T> Future<T> submit(@Nonnull Callable<T> task) {
			return delegate.submit(task);
		}

		@Override
		@Nonnull
		public <T> Future<T> submit(@Nonnull Runnable task, T result) {
			return delegate.submit(task, result);
		}

		@Override
		@Nonnull
		public Future<?> submit(@Nonnull Runnable task) {
			return delegate.submit(task);
		}

		@Override
		@Nonnull
		public <T> List<Future<T>> invokeAll(@Nonnull Collection<? extends Callable<T>> tasks) throws InterruptedException {
			return delegate.invokeAll(tasks);
		}

		@Override
		@Nonnull
		public <T> List<Future<T>> invokeAll(@Nonnull Collection<? extends Callable<T>> tasks, long timeout, @Nonnull TimeUnit unit)
				throws InterruptedException {
			return delegate.invokeAll(tasks, timeout, unit);
		}

		@Override
		@Nonnull
		public <T> T invokeAny(@Nonnull Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
			return delegate.invokeAny(tasks);
		}

		@Override
		public <T> T invokeAny(@Nonnull Collection<? extends Callable<T>> tasks, long timeout, @Nonnull TimeUnit unit)
				throws InterruptedException, ExecutionException, TimeoutException {
			return delegate.invokeAny(tasks, timeout, unit);
		}

		@Override
		public void execute(@Nonnull Runnable command) {
			delegate.execute(command);
		}

		@Override
		public void close() {
			CommonUtils.shutdownExecutorService(delegate);
		}
	}

	private CommonUtils() {
	}

	public static ExecutorService testExecutor() {
		return new ExecutorService(Executors.newSingleThreadExecutor(r -> {
			Thread t = Executors.defaultThreadFactory().newThread(r);
			t.setDaemon(true);
			return t;
		}));
	}

	public static ExecutorService testExecutor(final int threadNum) {
		return new ExecutorService(Executors.newFixedThreadPool(
				threadNum, r -> {
					Thread t = Executors.defaultThreadFactory().newThread(r);
					t.setDaemon(true);
					return t;
				}
		));
	}

	public static <T extends java.util.concurrent.ExecutorService> void shutdownExecutorService(T executor) {
		if (executor == null || executor.isShutdown()) {
			return;
		}
		executor.shutdown();
		try {
			if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
				executor.shutdownNow();
			}
		} catch (InterruptedException ignore) {
			executor.shutdownNow();
		}
	}

	/**
	 * Generates a unique ID shorter than UUID based on current time in milliseconds and thread ID.
	 *
	 * @return a unique ID string
	 */
	public static String generateUniqueId() {
		return System.currentTimeMillis() + "-" + Thread.currentThread().getId() + "-" + ThreadLocalRandom.current().nextInt(9999);
	}

	public static String namedId(String name) {
		return name + generateUniqueId();
	}

	public static Maybe<String> createMaybeUuid() {
		return Maybe.just(UUID.randomUUID().toString());
	}
}
