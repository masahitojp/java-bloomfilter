package me.masahito.data_structure;

import me.masahito.util.ArrayUtils;
import me.masahito.util.Hash;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
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

    private int[] bs;
    private int k;

    private final StampedLock lock = new StampedLock();
    private final int digestLength;
    private final List<Function<T, Integer>> hashingFunctions;

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
        this.bs = new int[bitSetSize];
        Arrays.fill(this.bs, 0);
        this.k = hashes;
        this.hashingFunctions = IntStream.range(0, this.k).boxed().map(s -> {
                final ByteBuffer b = ByteBuffer.allocate(BYTES_FOR_SALT);
                b.putInt(s);
                return (Function<T, Integer>) (T x) -> getHash(x, b.array());
            }).collect(Collectors.toList());
        this.digestLength = Hash.getSha1().getDigestLength();
    }
    public BloomFilter(final int capacity, final double errorRate) {
        this(BloomFilterForBestBitSetsContainer.getBestBitSetSize(capacity, errorRate));
    }

    public int[] getBitSets() {
        return this.bs;
    }

    public int getK() {
        return this.k;
    }

    private Integer getHash(T o, byte[] salt) {
        final MessageDigest sha1 = Hash.getSha1();
        sha1.update(ArrayUtils.concat(o.toString().getBytes(), salt));
        ByteBuffer wrapped = ByteBuffer.wrap(sha1.digest());
        return Math.abs(wrapped.getInt());
    }

    /**
     * add an element to the BloomFilter
     * @param o instance of {@link T}
     */
    public void add(T o) {
        hashingFunctions.parallelStream().forEach(salt -> {
            final int idx = salt.apply(o) % this.digestLength;
            long stamp = lock.writeLock();
            try {
                if (bs[idx] < Integer.MAX_VALUE) {
                    bs[idx] += 1;
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
    public void delete(T o) {
        hashingFunctions.parallelStream().forEach(salt -> {
            final int idx = salt.apply(o) % this.digestLength;
            long stamp = lock.writeLock();
            try {
                if (bs[idx] > 0) {
                    bs[idx] -= 1;
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
    public boolean contains(T o) {
        return hashingFunctions.parallelStream().allMatch(salt -> {
            long stamp = lock.tryOptimisticRead();
            final int idx = salt.apply(o) % this.digestLength;
            int result = bs[idx];
            if (!lock.validate(stamp)) {
                stamp = lock.readLock();
                try {
                    result = bs[idx];
                } finally {
                    lock.unlockRead(stamp);
                }
            }
            return result > 0;
        });
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.writeInt(k);
        stream.writeInt(this.bs.length);
        for (final int b : this.bs) {
            stream.writeInt(b);
        }
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {

        final int k = stream.readInt();
        final int bitsetLength = stream.readInt();
        final int[] bb = new int[bitsetLength];
        for(int i =0; i < bitsetLength; i++) {
            bb[i] = stream.readInt();
        }

        this.k = k;
        this.bs = bb;
    }
}
