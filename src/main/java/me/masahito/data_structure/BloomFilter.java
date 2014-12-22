package me.masahito.data_structure;

import me.masahito.util.ArrayUtils;
import me.masahito.util.Hash;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BloomFilter<T> {
    private static final int BYTES_FOR_SALT = 20;
    private static final int DEFAULT_CAPACITY = 1024;
    private static final double DEFAULT_ERROR_RATE = 0.01; //default: 1%

    private final StampedLock lock = new StampedLock();
    private final int[] bs;
    private final List<Function<T, Integer>> hashingFunctions;
    private final int k;

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
     * BloomFilterに要素を追加します。
     * @param o instance of {@link T}
     */
    public void add(T o) {
        hashingFunctions.stream().parallel().forEach(salt -> {
            final MessageDigest sha1 = Hash.getSha1();
            final int idx = salt.apply(o) % sha1.getDigestLength();
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
     * BloomFilterから要素を削除します。
     * @param o instance of {@link T}
     */
    public void delete(T o) {
        hashingFunctions.stream().parallel().forEach(salt -> {
            final MessageDigest sha1 =  Hash.getSha1();
            final int idx = salt.apply(o) %  sha1.getDigestLength();
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
     * BloomFilterに要素が追加済みか確認します。
     * @param o instance of {@link T}
     */
    public boolean contains(T o) {
        return hashingFunctions.stream().parallel().allMatch(salt -> {
            long stamp = lock.tryOptimisticRead();
            final int idx = salt.apply(o) % Hash.getSha1().getDigestLength();
            int a = bs[idx];
            // 他のスレッドから値が更新されていないことを確認
            if (!lock.validate(stamp)) {
                // 他のスレッドから値が更新されていた場合は読み取りロックを取得して読み取り
                stamp = lock.readLock();
                try {
                    a = bs[idx];
                } finally {
                    // 読み取りロックを解放
                    lock.unlockRead(stamp);
                }
            }
            return a > 0;
        });
    }
}
