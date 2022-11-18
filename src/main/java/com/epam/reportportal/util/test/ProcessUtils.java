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

import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.joinWith;

public class ProcessUtils {
	private ProcessUtils() {
	}

	public static class ExecutableNotFoundException extends RuntimeException {
		public ExecutableNotFoundException(String message) {
			super(message);
		}
	}

	private static String getPathToClass(@Nonnull Class<?> mainClass) {
		return mainClass.getCanonicalName();
	}

	public static Process buildProcess(boolean inheritOutput, @Nonnull Class<?> mainClass,
			@Nullable Map<String, String> additionalEnvironmentVariables,
			@Nullable Map<String, String> additionSystemVariables, String... params) throws IOException {
		String fileSeparator = System.getProperty("file.separator");
		String javaHome = System.getProperty("java.home");
		String executablePath = joinWith(fileSeparator, javaHome, "bin", "java");
		File executableFile = new File(executablePath);
		if (!executableFile.exists()) {
			executablePath = executablePath + ".exe";
			executableFile = new File(executablePath);
			if (!executableFile.exists()) {
				throw new ExecutableNotFoundException("Unable to find java executable file.");
			}
		}
		List<String> paramList = new ArrayList<>();
		paramList.add(executablePath);
		paramList.add("-classpath");
		paramList.add(System.getProperty("java.class.path"));
		paramList.addAll(ofNullable(additionSystemVariables).map(p -> p.entrySet()
				.stream()
				.map(e -> "-D" + e.getKey() + "=" + e.getValue())
				.collect(Collectors.toList())).orElse(Collections.emptyList()));
		paramList.add(getPathToClass(mainClass));
		paramList.addAll(Arrays.asList(params));
		ProcessBuilder pb = new ProcessBuilder(paramList);
		if (inheritOutput) {
			pb.inheritIO();
		}
		ofNullable(additionalEnvironmentVariables).ifPresent(v -> pb.environment().putAll(v));
		return pb.start();
	}

	public static Process buildProcess(boolean inheritOutput, @Nonnull Class<?> mainClass,
			@Nullable Map<String, String> additionalEnvironmentVariables, String... params) throws IOException {
		return buildProcess(inheritOutput, mainClass, additionalEnvironmentVariables, null, params);
	}

	public static Process buildProcess(Class<?> mainClass, String... params) throws IOException {
		return buildProcess(false, mainClass, null, null, params);
	}

	public static Process buildProcess(boolean inheritOutput, Class<?> mainClass, String... params) throws IOException {
		return buildProcess(inheritOutput, mainClass, null, null, params);
	}

	public static Triple<OutputStreamWriter, BufferedReader, BufferedReader> getProcessIos(Process process) {
		return ImmutableTriple.of(
				new OutputStreamWriter(process.getOutputStream()),
				new BufferedReader(new InputStreamReader(process.getInputStream())),
				new BufferedReader(new InputStreamReader(process.getErrorStream()))
		);
	}
}
