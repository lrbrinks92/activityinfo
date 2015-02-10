package org.activityinfo.test.webdriver;


import org.hamcrest.CoreMatchers;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class VersionTest {
    
    @Test
    public void test() {
        assertThat(new Version("10.9").compareTo(new Version("10.10")), equalTo(-1));
    }

}