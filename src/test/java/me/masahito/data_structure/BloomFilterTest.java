package me.masahito.data_structure;


import org.junit.Test;

import java.io.*;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;

public class BloomFilterTest {

    @Test
    public void noData() throws Exception {
        final BloomFilter<String> bf = new BloomFilter<>();
        assertThat(bf.contains("test"), is(false));
    }
    @Test
    public void checkTrue() throws Exception {
        final BloomFilter<String> bf = new BloomFilter<>();
        bf.add("test");
        assertThat(bf.contains("test"), is(true));
    }
    @Test
    public void checkFalse() throws Exception {
        final BloomFilter<String> bf = new BloomFilter<>();
        bf.add("test");
        assertThat(bf.contains("test2"), is(false));
    }

    @Test
    public void addToDelete() throws Exception {
        final BloomFilter<String> bf = new BloomFilter<>();
        bf.add("test");
        assertThat(bf.contains("test"), is(true));

        bf.delete("test");
        assertThat(bf.contains("test"), is(not(true)));
    }

    @Test
    public void deleteToAdd() throws Exception {
        final BloomFilter<String> bf = new BloomFilter<>();
        bf.delete("test");
        assertThat(bf.contains("test"), is(not(true)));
        bf.add("test");
        assertThat(bf.contains("test"), is(true));
    }

    @Test
    public void serialize() throws Exception {
        final BloomFilter<String> bf = new BloomFilter<>();
        bf.add("test");
        ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream("data")));
        oos.writeObject(bf);
        oos.close();
        ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream("data")));
        final BloomFilter<String> a = (BloomFilter<String>)in.readObject();

        assertThat(a.getBitSets(), is(bf.getBitSets()));
    }
}
