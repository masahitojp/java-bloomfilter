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

import java.util.stream.IntStream;

/**
 * 最良のビットセット数を計算するためのコンテナクラス
 */
class BloomFilterForBestBitSetsContainer implements Comparable{
    public final int hashes;
    public final int bitSetSize;
    public BloomFilterForBestBitSetsContainer(final int hashes, final int bitSetSize) {
        this.hashes = hashes;
        this.bitSetSize = bitSetSize;
    }

    public static BloomFilterForBestBitSetsContainer getBestBitSetSize(final int capacity, final double errorRate) {
        return IntStream
                .range(1, 100)
                .boxed()
                .map(k -> {
                    int m = (int) ((-1 * k * capacity) /
                                (Math.log(1 - Math.pow(errorRate, 1.0 / k))));
                    return new BloomFilterForBestBitSetsContainer(k, m);
                })
                .min((s1, s2) -> s1.bitSetSize - s2.bitSetSize)
                .get();
    }

    @Override
    public int compareTo(final Object o) {
        BloomFilterForBestBitSetsContainer otherPair = (BloomFilterForBestBitSetsContainer) o;
        return this.bitSetSize - otherPair.bitSetSize;
    }
}
