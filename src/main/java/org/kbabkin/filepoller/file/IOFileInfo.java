package org.kbabkin.filepoller.file;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Value
@Slf4j
public class IOFileInfo implements FileInfo {
    File file;

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public Attributes getAttributes() {
        return new IOFileInfoAttributes(file);
    }

    @Override
    public boolean delete() {
        return file.delete();
    }

    @Override
    public void rename(String newName) throws IOException {
        if (!file.renameTo(new File(file.getParentFile(), newName))) {
            throw new IOException("Failed to rename " + file + " to " + newName);
        }
    }

    @Override
    public List<FileInfo> list(Predicate<FileInfo> filter) {
        File[] files = file.listFiles(f -> filter.test(new IOFileInfo(f)));
        if (files == null) {
            log.info("list(): folder not found: {}", file);
            return Collections.emptyList();
        }
        return Arrays.stream(files)
                .map(IOFileInfo::new)
                .collect(Collectors.toList());
    }

    @Value
    static class IOFileInfoAttributes implements FileInfo.Attributes {
        File file;

        @Override
        public long getLastModified() {
            return file.lastModified();
        }

        @Override
        public long getSize() {
            return file.length();
        }
    }
}
