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

        assertEquals("a", CommonUtils.putValueCommaSeparatedInPosition(sample1, "a", 1));
        assertEquals(",a", CommonUtils.putValueCommaSeparatedInPosition(sample2, "a", 2));
        assertEquals("a,b", CommonUtils.putValueCommaSeparatedInPosition(sample3, "b", 2));
        assertEquals("a,,,b", CommonUtils.putValueCommaSeparatedInPosition(sample3, "b", 4));
        assertEquals("a,b,c,d,e,f", CommonUtils.putValueCommaSeparatedInPosition(sample4, "f", 6));
        assertEquals("a,,c,d", CommonUtils.putValueCommaSeparatedInPosition(sample5, "c", 3));
        assertEquals("a,,,d,c", CommonUtils.putValueCommaSeparatedInPosition(sample5, "c", 5));
    }
}
