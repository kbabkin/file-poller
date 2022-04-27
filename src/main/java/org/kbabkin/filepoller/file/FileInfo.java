package org.kbabkin.filepoller.file;

import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;

/**
 * Interface for File System, easy switch of blocking (java.io), non-blocking (java.nio), test mocks implementation etc.
 * See also <a href="https://commons.apache.org/proper/commons-vfs/api.html">Apache Commons VFS (Virtual File System) Project</a>
 */
public interface FileInfo {
    String getName();

    //todo methods to get content

    Attributes getAttributes() throws IOException;

    boolean delete() throws IOException;

    void rename(String newName) throws IOException;

    List<FileInfo> list(Predicate<FileInfo> filter) throws IOException;

    interface Attributes {
        long getLastModified();

        long getSize();
    }
}
