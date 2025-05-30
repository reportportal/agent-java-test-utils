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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static java.util.Optional.*;

public class SocketUtils {
	public static final String CONTENT_LENGTH_HEADER = "Content-Length: ";
	public static final String WEB_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z";

	private static final Logger LOGGER = LoggerFactory.getLogger(SocketUtils.class);

	private SocketUtils() {
	}

	public static final class ServerCallable implements Callable<List<String>> {

		private final ServerSocket ss;
		private final Map<String, Object> model;
		private final List<String> responseFiles;

		public ServerCallable(@Nonnull ServerSocket serverSocket, @Nonnull Map<String, Object> replacementValues,
				@Nonnull List<String> responseFilePaths) {
			ss = serverSocket;
			model = replacementValues;
			responseFiles = responseFilePaths;
		}

		public ServerCallable(@Nonnull ServerSocket serverSocket, @Nonnull Map<String, Object> replacementValues,
				@Nonnull String responseFilePath) {
			this(serverSocket, replacementValues, Collections.singletonList(responseFilePath));
		}

		@Override
		public List<String> call() throws Exception {
			final List<String> results = new ArrayList<>();
			try (Socket s = ss.accept(); InputStream is = s.getInputStream(); OutputStream os = s.getOutputStream()) {
				for (String responseFile : responseFiles) {
					BufferedReader in = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
					StringBuilder builder = new StringBuilder();
					while (true) {
						String line = in.readLine();
						if (line == null || line.isEmpty()) {
							break; // End of headers or connection closed
						}
						builder.append(line);
						builder.append(System.lineSeparator());
					}
					String headers = builder.toString();
					if (headers.isEmpty()) {
						throw new IOException("No headers received from client");
					}

					int contentLength = 0;
					int lengthIdx = headers.indexOf(CONTENT_LENGTH_HEADER);
					if (lengthIdx > 0) {
						lengthIdx += CONTENT_LENGTH_HEADER.length();
						contentLength = Integer.parseInt(headers.substring(lengthIdx, headers.indexOf(System.lineSeparator(), lengthIdx))
								.trim());
					}
					if (contentLength > 0) {
						char[] body = new char[contentLength];
						int actualRead = in.read(body, 0, contentLength);
						if (actualRead < contentLength) {
							throw new IOException("Expected " + contentLength + " bytes, but only read " + actualRead + " bytes");
						}
						builder.append(System.lineSeparator());
						builder.append(new String(body));
					}
					results.add(builder.toString());

					String rs = ofNullable(getClass().getClassLoader().getResourceAsStream(responseFile)).flatMap(stream -> {
						try {
							String responseStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
							for (String k : model.keySet()) {
								responseStr = responseStr.replace("{" + k + "}", model.get(k).toString());
							}
							return of(responseStr);
						} catch (IOException ignore) {
							return empty();
						}
					}).orElseThrow(() -> new IOException("Unable to read file: " + responseFile)).replaceAll("\r?\n", "\r\n");
					IOUtils.write(rs, os, StandardCharsets.UTF_8);
					os.flush();
				}
			}

			return results;
		}
	}

	public static ServerSocket getServerSocketOnFreePort() throws IOException {
		return new ServerSocket(0);
	}

	public static <T> Pair<List<String>, T> executeServerCallable(ServerCallable srvCall, Callable<T> clientCallable, long timeoutSeconds)
			throws Exception {
		ExecutorService clientExecutor = Executors.newSingleThreadExecutor();
		ExecutorService serverExecutor = Executors.newSingleThreadExecutor();
		Future<List<String>> serverFuture = serverExecutor.submit(srvCall);
		Future<T> clientFuture = clientExecutor.submit(clientCallable);

		Pair<List<String>, T> result;
		try {
			result = Pair.of(serverFuture.get(timeoutSeconds, TimeUnit.SECONDS), clientFuture.get(timeoutSeconds, TimeUnit.SECONDS));
		} catch (TimeoutException e) {
			CommonUtils.shutdownExecutorService(clientExecutor);
			CommonUtils.shutdownExecutorService(serverExecutor);
			throw e;
		}
		CommonUtils.shutdownExecutorService(clientExecutor);
		CommonUtils.shutdownExecutorService(serverExecutor);
		return result;
	}

	public static <T> Pair<List<String>, T> executeServerCallable(ServerCallable srvCall, Callable<T> clientCallable) throws Exception {
		return executeServerCallable(srvCall, clientCallable, 5L);
	}
}
