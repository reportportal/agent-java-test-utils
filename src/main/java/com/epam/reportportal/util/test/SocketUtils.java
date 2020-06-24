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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.*;

import static java.util.Optional.*;

public class SocketUtils {
	public static final String WEB_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z";

	private SocketUtils() {
	}

	public static final class ServerCallable implements Callable<String> {

		private final ServerSocket ss;
		private final Map<String, Object> model;
		private final String responseFile;
		private Socket s;

		public ServerCallable(ServerSocket serverSocket, Map<String, Object> replacementValues, String responseFilePath) {
			ss = serverSocket;
			model = replacementValues;
			responseFile = responseFilePath;
		}

		@Override
		public String call() throws Exception {
			if (s == null) {
				s = ss.accept();
			}
			BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
			StringBuilder builder = new StringBuilder();
			String line;
			while ((line = in.readLine()) != null) {
				if (line.equals("")) {
					break;
				}
				builder.append(line);
				builder.append(System.lineSeparator());
			}
			String rq = builder.toString();
			String rs = ofNullable(getClass().getClassLoader().getResourceAsStream(responseFile)).flatMap(s -> {
				try {
					String responseStr = IOUtils.toString(s);
					for (String k : model.keySet()) {
						responseStr = responseStr.replace("{" + k + "}", model.get(k).toString());
					}
					return of(responseStr);
				} catch (IOException ignore) {
					return empty();
				}
			}).orElseThrow(() -> new IOException("Unable to read file: " + responseFile));
			IOUtils.write(rs, s.getOutputStream());
			return rq;
		}
	}

	public static ServerSocket getServerSocketOnFreePort() throws IOException {
		return new ServerSocket(0);
	}

	public static <T> Pair<String, T> executeServerCallable(ServerCallable srvCall, Callable<T> clientCallable) throws Exception {
		ExecutorService serverExecutor = Executors.newSingleThreadExecutor();
		Future<String> future = serverExecutor.submit(srvCall);
		T rs = clientCallable.call();
		try {
			return Pair.of(future.get(5, TimeUnit.SECONDS), rs);
		} finally {
			CommonUtils.shutdownExecutorService(serverExecutor);
		}
	}
}
