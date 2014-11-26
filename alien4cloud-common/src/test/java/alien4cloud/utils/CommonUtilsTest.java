package alien4cloud.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CommonUtilsTest {

    @Test
    public void putValueCommaSeparatedInPositionTest() {
        String sample1 = null;
        String sample2 = "";
        String sample3 = "a";
        String sample4 = "a,b,c,d,e";
        String sample5 = "a,,,d";

        assertEquals("a", AlienUtils.putValueCommaSeparatedInPosition(sample1, "a", 1));
        assertEquals(",a", AlienUtils.putValueCommaSeparatedInPosition(sample2, "a", 2));
        assertEquals("a,b", AlienUtils.putValueCommaSeparatedInPosition(sample3, "b", 2));
        assertEquals("a,,,b", AlienUtils.putValueCommaSeparatedInPosition(sample3, "b", 4));
        assertEquals("a,b,c,d,e,f", AlienUtils.putValueCommaSeparatedInPosition(sample4, "f", 6));
        assertEquals("a,,c,d", AlienUtils.putValueCommaSeparatedInPosition(sample5, "c", 3));
        assertEquals("a,,,d,c", AlienUtils.putValueCommaSeparatedInPosition(sample5, "c", 5));
    }
}
