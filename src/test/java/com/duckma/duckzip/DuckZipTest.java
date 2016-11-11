package com.duckma.duckzip;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class DuckZipTest {

    private DuckZip duckZip;

    @Before
    public void setUp() throws Exception {
        duckZip = new DuckZip();
    }

    @Test
    public void test() throws Exception {
        assertEquals(2, duckZip.exampleMethod(1, 1));
    }

}
