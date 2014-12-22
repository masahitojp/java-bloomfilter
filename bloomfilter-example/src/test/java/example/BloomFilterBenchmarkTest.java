package example;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.is;

public class BloomFilterBenchmarkTest {

    @Test
    public void testGetElements() throws Exception {
        final List<String> test = BloomfilterBenchmark.getElements();
        assertThat(test.size(), is(BloomfilterBenchmark.ELEMENTS));
    }
}