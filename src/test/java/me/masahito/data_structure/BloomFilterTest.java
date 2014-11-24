package me.masahito.data_structure;


import org.junit.Test;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.CoreMatchers.is;


public class BloomFilterTest {

    @Test
    public void testNoData() throws Exception {
        final BloomFilter<String> bf = new BloomFilter<>();
        assertThat(bf.check("test"), is(false));
    }
    @Test
    public void testCheck() throws Exception {
        final BloomFilter<String> bf = new BloomFilter<>();
        bf.add("test");
        assertThat(bf.check("test"), is(true));
    }
    @Test
    public void testCheckFalse() throws Exception {
        final BloomFilter<String> bf = new BloomFilter<>();
        bf.add("test");
        assertThat(bf.check("test2"), is(false));
    }
}