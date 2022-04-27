package org.kbabkin.filepoller.file;

import lombok.Value;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Value
public class NIOFileInfo implements FileInfo {
    Path path;

    @Override
    public String getName() {
        return path.toFile().getName();
    }

    @Override
    public Attributes getAttributes() throws IOException {
        return new NIOFileInfoAttributes(Files.readAttributes(path, BasicFileAttributes.class));
    }

    @Override
    public boolean delete() throws IOException {
        return Files.deleteIfExists(path);
    }

    @Override
    public void rename(String newName) throws IOException {
        Files.move(path, path.resolveSibling(newName));
    }

    @Override
    public List<FileInfo> list(Predicate<FileInfo> filter) throws IOException {
        // Files.find(path, 1, (p, attrs) -> filter.test(new NIOFileInfo(p, attrs)))
        try (Stream<Path> stream = Files.list(path)) {
            return stream
                    .map(NIOFileInfo::new)
                    .filter(filter)
                    .collect(Collectors.toList());
        }
    }

    @Value
    static class NIOFileInfoAttributes implements FileInfo.Attributes {
        BasicFileAttributes attributes;

        @Override
        public long getLastModified() {
            return attributes.lastModifiedTime().toMillis();
        }

        @Override
        public long getSize() {
            return attributes.size();
        }
    }
}
