package utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class ResourceFileUtils {

    public <T> T deserializeJsonResourceFileIntoObject(String resourceFilePath, Class<T> deserializingClass) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(getResourceFile(resourceFilePath),deserializingClass);
    }

    private File getResourceFile(String resourceFilePath) throws IOException {
        InputStream inputStream = ResourceFileUtils.class.getResourceAsStream(resourceFilePath);
        File tempFile = File.createTempFile("tempResourceFile", ".json");
        FileUtils.copyInputStreamToFile(inputStream, tempFile);
        tempFile.deleteOnExit();
        return tempFile;
    }

}
