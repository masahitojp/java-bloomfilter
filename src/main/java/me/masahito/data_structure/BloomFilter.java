package me.masahito.data_structure;

import lombok.Synchronized;
import me.masahito.util.ArrayUtils;
import me.masahito.util.Hash;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class BloomFilter<T> {
    private static final int BYTES_FOR_SALT = 20;
    private static final int DEFAULT_CAPACITY = 1024;
    private static final double DEFAULT_ERROR_RATE = 0.1;

    private final int[] bs;
    private final MessageDigest md = Hash.getSha1();
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
        java.util.Arrays.fill(bs, 0);

        final Random rd = new Random();
        hashingFunctions = IntStream.range(0, best_k).boxed().map(s -> {
            final byte[] b = new byte[BYTES_FOR_SALT];
            rd.nextBytes(b);
            return (Function<T, Integer>) (T x) -> getHash(x, b);
        }).collect(Collectors.toList());
    }


    private Integer getHash(T o, byte[] salt) {
        md.update(ArrayUtils.concat(o.toString().getBytes(), salt));
        ByteBuffer wrapped = ByteBuffer.wrap(md.digest());
        return Math.abs(wrapped.getInt());
    }

    @Synchronized
    public void add(T o) {
        hashingFunctions.stream().forEach(salt -> {
            final int idx = salt.apply(o) % md.getDigestLength();
            bs[idx] += 1;
        });
    }

    @Synchronized
    public void delete(T o) {
        hashingFunctions.stream().forEach(salt -> {
            final int idx = salt.apply(o) % md.getDigestLength();
            if (bs[idx] > 0) {
                bs[idx] -= 1;
            }
        });
    }

    @Synchronized
    public boolean check(T o) {
        return hashingFunctions.stream().allMatch(salt -> bs[salt.apply(o) % md.getDigestLength()] > 0);
    }
}
