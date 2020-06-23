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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.joinWith;

public class ProcessUtils {
	private ProcessUtils() {
	}

	public static class ExecutableNotFoundException extends RuntimeException {
		public ExecutableNotFoundException(String message) {
			super(message);
		}
	}

	private static String getPathToClass(Class<?> mainClass) {
		return mainClass.getCanonicalName();
	}

	public static Process buildProcess(boolean inheritOutput, Class<?> mainClass, String... params) throws IOException {
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
		paramList.add(getPathToClass(mainClass));
		paramList.addAll(Arrays.asList(params));
		ProcessBuilder pb = new ProcessBuilder(paramList);
		if (inheritOutput) {
			pb.inheritIO();
		}
		return pb.start();
	}

	public static Process buildProcess(Class<?> mainClass, String... params) throws IOException {
		return buildProcess(false, mainClass, params);
	}
}
