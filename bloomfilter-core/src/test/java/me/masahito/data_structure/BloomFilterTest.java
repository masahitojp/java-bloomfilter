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

import org.junit.Before;
import org.junit.Test;

import java.io.*;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;

public class BloomFilterTest {
    BloomFilter<String> bf;

    @Before
    public void setUp() {
        bf = new BloomFilter<>();
    }

    @Test
    public void noData() throws Exception {
        assertThat(bf.contains("test"), is(false));
    }
    @Test
    public void checkTrue() throws Exception {
        bf.add("test");
        assertThat(bf.contains("test"), is(true));
    }
    @Test
    public void checkFalse() throws Exception {
        bf.add("test");
        assertThat(bf.contains("test2"), is(false));
    }

    @Test
    public void addToDelete() throws Exception {
        bf.add("test");
        assertThat(bf.contains("test"), is(true));

        bf.delete("test");
        assertThat(bf.contains("test"), is(not(true)));
    }

    @Test
    public void deleteToAdd() throws Exception {
        bf.delete("test");
        assertThat(bf.contains("test"), is(false));

        bf.add("test");
        assertThat(bf.contains("test"), is(true));
    }

    @Test
    public void equals() {
        bf.add("test");
        final BloomFilter<String> bf2 = new BloomFilter<>();
        bf2.add("test");


        final BloomFilter<String> bf3 = new BloomFilter<>(0.001);
        bf3.add("test");

        assertThat(bf.equals(null), is(false));
        assertThat(bf.equals("test"), is(false));

        assertThat(bf.hashCode(), is(bf2.hashCode()));
        assertThat(bf, is(bf2));

        bf2.add("test2");
        assertThat(bf, is(not(bf2)));

        assertThat(bf, is(not(bf3)));

    }

    @Test
    public void serialize() throws Exception {
        bf.add("test");

        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(bout));
        oos.writeObject(bf);
        oos.close();
        bout.close();
        final ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(bin));
        final BloomFilter<String> newBf = (BloomFilter<String>)ois.readObject();
        ois.close();
        bin.close();


        assertThat(bf.getBitSets(), is(newBf.getBitSets()));
        assertThat(bf.getK(), is(newBf.getK()));
        assertThat(bf.hashCode(), is(newBf.hashCode()));
        assertThat(bf, is(newBf));
        assertThat(newBf.contains("test"), is(true));
    }
}
