/*
* Copyright 2014 Nakamura Masato
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package me.masahito.data_structure;

import java.util.List;
import java.util.Random;

import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BloomfilterBenchmark {
	static int ELEMENTS = 50000; // Number of elements to test
	static double CAPACITY = 0.0001;

	public static void timeCheck(String test, List<String> elements, Consumer<String> consumer) {
		System.out.print(test);
		long start = System.nanoTime();
		elements.stream().forEach(consumer);
		long end = System.nanoTime();
		printStat(start, end);
	}

	public static void printStat(long start, long end) {
		double diff = (end - start) / 1000000000.0;
		System.out.println(diff + "s, " + (ELEMENTS / diff) + " elements/s");
	}

	public static List<String> getElements() {
		final Random r = new Random();
		return IntStream.range(1, ELEMENTS).boxed().map(i -> {
			byte[] b = new byte[200];
			r.nextBytes(b);
			return new String(b);
		}).collect(Collectors.toList());
	}

	public static void main(String[] argv) {

		// Generate elements first
		final List<String> existingElements = getElements();
		final List<String> nonExistingElements = getElements();

		final BloomFilter<String> bf = new BloomFilter<>(ELEMENTS, CAPACITY);

		System.out.println("Testing " + ELEMENTS + " elements");
		System.out.println("Testing " + bf.getBitSets().length + " bitsets");
		System.out.println("Testing " + bf.getK() + " hashes");

		// Add elements
		timeCheck("add(): ", existingElements, bf::add);

		// Check for existing elements with check()
		timeCheck("check(), existing: ", existingElements, bf::contains);

		// Check for nonexisting elements with check()
		timeCheck("check(), nonexisting: ", nonExistingElements, bf::contains);

		// Delete elements
		timeCheck("delete(): ", existingElements, bf::delete);

	}
}
