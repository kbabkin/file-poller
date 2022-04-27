package org.kbabkin.filepoller.simple;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
@NoArgsConstructor
public class LastModifiedDatePoller {
    long stagingTimeout = 1000;

    public List<String> getFileList(String dir, String filter) {
        Pattern pattern = Pattern.compile(filter);

        File parentFile = new File(dir);
        File[] files = parentFile.listFiles((File file) -> {
            String fileName = file.getName();
            return pattern.matcher(fileName).find()
                    && isMatured(fileName, file.lastModified(), stagingTimeout);
        });
        if (files == null) {
            log.info("getFileList(): dir not found: {}", dir);
            return Collections.emptyList();
        }
        return Arrays.stream(files)
                .map(File::getName)
                .collect(Collectors.toList());
    }

    static boolean isMatured(String fileName, long lastModified, long timeout) {
        if (lastModified == 0) {
            log.info("File {} has no lastModified", fileName);
            return true;
        }
        if (lastModified + timeout > System.currentTimeMillis()) {
            log.info("File {}: waiting for write completion", fileName);
            return false;
        }
        return true;
    }

}
