package org.kbabkin.filepoller.simple;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

class LastModifiedDatePollerTest {

    @Test
    void getFileList() throws IOException {
        Path folder = Paths.get("target", "test-data", "LastModifiedDatePollerTest");
        Files.createDirectories(folder);
        Path tempDirectory = Files.createTempDirectory(folder, null);
        Path tempFile = Files.createTempFile(tempDirectory, null, ".txt");

        {
            LastModifiedDatePoller lastModifiedDatePoller = new LastModifiedDatePoller(5000);
            List<String> fileList = lastModifiedDatePoller.getFileList(tempDirectory.toString(), ".*\\.txt");
            Assertions.assertEquals(0, fileList.size());
        }
        {
            LastModifiedDatePoller lastModifiedDatePoller = new LastModifiedDatePoller(1);
            List<String> fileList = lastModifiedDatePoller.getFileList(tempDirectory.toString(), ".*\\.txt");
            Assertions.assertEquals(1, fileList.size());
        }
    }
}