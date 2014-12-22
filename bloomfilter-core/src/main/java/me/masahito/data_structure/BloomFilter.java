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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BloomFilter<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final int BYTES_FOR_SALT = 20;
    private static final int DEFAULT_CAPACITY = 1024;
    private static final double DEFAULT_ERROR_RATE = 0.01; //default: 1%

    private static final Charset charset = Charset.forName("UTF-8");

    private int[] bitset;
    private int k;

    private StampedLock lock;
    private int digestLength;
    private List<Function<T, Integer>> hashingFunctions;


    /* Constructor */
    public BloomFilter() {
        this(DEFAULT_CAPACITY, DEFAULT_ERROR_RATE);
    }

    public BloomFilter(final double errorRate) {
        this(DEFAULT_CAPACITY, errorRate);
    }

    public BloomFilter(final BloomFilterForBestBitSetsContainer pair) {
        this(pair.bitSetSize, pair.hashes);
    }
    public BloomFilter(final int bitSetSize, final int hashes) {
        final int[] bs = new int[bitSetSize];
        Arrays.fill(bs, 0);
        initialize(bs, hashes);
    }
    public BloomFilter(final int capacity, final double errorRate) {
        this(BloomFilterForBestBitSetsContainer.getBestBitSetSize(capacity, errorRate));
    }

    public void initialize(final int[] bitSets, final int hashes) {
        this.bitset = bitSets;
        this.k = hashes;
        this.hashingFunctions = IntStream.range(0, this.k).boxed().map(s -> {
            final ByteBuffer b = ByteBuffer.allocate(BYTES_FOR_SALT);
            b.putInt(s);
            return (Function<T, Integer>) (T x) -> getHash(x, b.array());
        }).collect(Collectors.toList());
        this.digestLength = this.getSha1().getDigestLength();
        this.lock = new StampedLock();
    }

    /* public methods */

    public int[] getBitSets() {
        return this.bitset;
    }

    public int getK() {
        return this.k;
    }

    /**
     * add an element to the BloomFilter
     * @param o instance of {@link T}
     */
    public void add(T o) {
        hashingFunctions.parallelStream().forEach(salt -> {
            final int idx = salt.apply(o) % this.digestLength;
            final long stamp = lock.writeLock();
            try {
                if (bitset[idx] < Integer.MAX_VALUE) {
                    bitset[idx] += 1;
                }
            }
            finally {
                lock.unlockWrite(stamp);
            }
        });
    }

    /**
     * remove an element from the BloomFilter
     * @param o instance of {@link T}
     */
    public void delete(final T o) {
        hashingFunctions.parallelStream().forEach(salt -> {
            final int idx = salt.apply(o) % this.digestLength;
            final long stamp = lock.writeLock();
            try {
                if (bitset[idx] > 0) {
                    bitset[idx] -= 1;
                }
            } finally {
                lock.unlockWrite(stamp);
            }
        });
    }

    /**
     * check contains an element from the BloomFilter
     * @param o instance of {@link T}
     */
    public final boolean contains(final T o) {
        return hashingFunctions.parallelStream().allMatch(salt -> {
            long stamp = lock.tryOptimisticRead();
            final int idx = salt.apply(o) % this.digestLength;
            int result = bitset[idx];
            if (!lock.validate(stamp)) {
                stamp = lock.readLock();
                try {
                    result = bitset[idx];
                } finally {
                    lock.unlockRead(stamp);
                }
            }
            return result > 0;
        });
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final BloomFilter<T> other = (BloomFilter<T>) obj;
        if (this.k != other.k) {
            return false;
        }
        if (this.bitset != other.bitset && (!Arrays.equals(this.bitset, other.bitset))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 61 * hash + (this.bitset != null ? Arrays.hashCode(this.bitset) : 0);
        hash = 61 * hash + this.k;
        return hash;
    }

    /** private methods **/

    private byte[] byteArrayConcat(byte[] first, byte[] second) {
        byte[] result = java.util.Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    private MessageDigest getSha1() {
        try {
            return MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Integer getHash(T o, byte[] salt) {
        final MessageDigest sha1 = this.getSha1();
        sha1.update(this.byteArrayConcat(o.toString().getBytes(charset), salt));
        ByteBuffer wrapped = ByteBuffer.wrap(sha1.digest());
        return Math.abs(wrapped.getInt());
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.writeInt(k);
        stream.writeInt(this.bitset.length);
        for (final int b : this.bitset) {
            stream.writeInt(b);
        }
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {

        final int k = stream.readInt();
        final int bitSetLength = stream.readInt();
        final int[] bb = new int[bitSetLength];
        for(int i =0; i < bitSetLength; i++) {
            bb[i] = stream.readInt();
        }

        initialize(bb, k);
    }
}
