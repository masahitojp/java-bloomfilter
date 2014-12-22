package me.masahito.data_structure;


import me.masahito.util.ArrayUtils;
import me.masahito.util.Hash;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
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

    public BloomFilter() {
        this(DEFAULT_CAPACITY, DEFAULT_ERROR_RATE);
    }

    public BloomFilter(final double errorRate) {
        this(DEFAULT_CAPACITY, errorRate);
    }

    public BloomFilter(final int capacity, final double errorRate) {

        // generate BitSet
        Integer lowest_m = null;
        int best_k = 1;

        for (int k = 1; k < 100; k++) {
            int m = (int) ((-1 * k * capacity) /
                    (Math.log(1 - Math.pow(errorRate, 1.0 / k))));

            if (lowest_m == null || m < lowest_m) {
                lowest_m = m;
                best_k = k;
            }
        }
        bs = new int[lowest_m];
        Arrays.fill(bs, 0);

        final Random rd = new Random();
        hashingFunctions = IntStream.range(0, best_k).boxed().map(s -> {
            final byte[] b = new byte[BYTES_FOR_SALT];
            rd.nextBytes(b);
            return (Function<T, Integer>) (T x) -> getHash(x, b);
        }).collect(Collectors.toList());
    }


    private Integer getHash(T o, byte[] salt) {
        final MessageDigest sha1 = Hash.getSha1();
        sha1.update(ArrayUtils.concat(o.toString().getBytes(), salt));
        ByteBuffer wrapped = ByteBuffer.wrap(sha1.digest());
        return Math.abs(wrapped.getInt());
    }

    public void add(T o) {
        hashingFunctions.stream().parallel().forEach(salt -> {
            final MessageDigest sha1 = Hash.getSha1();
            final int idx = salt.apply(o) % sha1.getDigestLength();
            long stamp = lock.writeLock();
            bs[idx] += 1;
            lock.unlockWrite(stamp);
        });
    }

    public void delete(T o) {
        hashingFunctions.stream().parallel().forEach(salt -> {
            final MessageDigest sha1 =  Hash.getSha1();
            final int idx = salt.apply(o) %  sha1.getDigestLength();
            long stamp = lock.writeLock();
            if (bs[idx] > 0) {
                bs[idx] -= 1;
            }
            lock.unlockWrite(stamp);
        });
    }

    public boolean check(T o) {
        return hashingFunctions.stream().parallel().allMatch(salt -> {
            long stamp = lock.tryOptimisticRead();
            final int idx = salt.apply(o) % Hash.getSha1().getDigestLength();
            int a = bs[idx];
            // 他のスレッドから値が更新されていないことを確認
            if (!lock.validate(stamp)) {
                // 他のスレッドから値が更新されていた場合は読み取りロックを取得して読み取り
                stamp = lock.readLock();
                a = bs[idx];
                // 読み取りロックを解放
                lock.unlockRead(stamp);
            }
            return a > 0;
        });
    }
}
