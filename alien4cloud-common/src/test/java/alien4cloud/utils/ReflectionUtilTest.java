package alien4cloud.utils;

import lombok.Getter;
import lombok.Setter;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * @author Minh Khang VU
 */
public class ReflectionUtilTest {

    @Getter
    @Setter
    private static class MergedObject {

        private String id;

        private String text;

        private Integer number;

        private String notUpdated;
    }

    @Getter
    @Setter
    private static class MergeRequest {

        private String text;

        private String number;

        private String notUpdated;

        private String badField;
    }

    @Test
    public void mergeObjectTest() throws JsonProcessingException {
        MergedObject mergedObject = new MergedObject();
        mergedObject.setId("an id");
        mergedObject.setText("text");
        mergedObject.setNumber(4);

        MergeRequest mergeRequest = new MergeRequest();
        mergeRequest.setText("another text");
        mergeRequest.setNumber("5");

        ReflectionUtil.mergeObject(mergeRequest, mergedObject);

        Assert.assertEquals(5, mergedObject.getNumber().intValue());
        Assert.assertEquals("another text", mergedObject.getText());
        Assert.assertEquals("an id", mergedObject.getId());
    }

    public void mergeObjectTestWithUnknownField() throws JsonProcessingException {
        MergedObject mergedObject = new MergedObject();
        mergedObject.setId("an id");
        mergedObject.setText("text");
        mergedObject.setNumber(4);

        MergeRequest mergeRequest = new MergeRequest();
        mergeRequest.setText("another text");
        mergeRequest.setNumber("5");
        // this property will be ignored
        mergeRequest.setBadField("bad");
        ReflectionUtil.mergeObject(mergeRequest, mergedObject);
    }

}
