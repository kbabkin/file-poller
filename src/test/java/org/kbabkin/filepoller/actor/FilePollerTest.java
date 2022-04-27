package org.kbabkin.filepoller.actor;

import akka.actor.testkit.typed.javadsl.ActorTestKit;
import akka.actor.testkit.typed.javadsl.ManualTime;
import akka.actor.typed.ActorRef;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.kbabkin.filepoller.file.FileInfo;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FilePollerTest {

    ActorTestKit asyncTestKit = ActorTestKit.create(ManualTime.config());
    ManualTime manualTime = ManualTime.get(asyncTestKit.system());

    @AfterEach
    void shutdown() {
        asyncTestKit.shutdownTestKit();
    }

    @Test
    void scan() throws Exception {
        FileInfo folder = mock(FileInfo.class);
        FileInfo file = mock(FileInfo.class);
        when(folder.list(any())).thenReturn(List.of(file));
        when(file.getAttributes()).thenReturn(mock(FileInfo.Attributes.class));

        ActorRef<FilePoller.Start> pollerRef = asyncTestKit.spawn(FilePoller.builder()
                .name("tst")
                .folder(folder)
                .fileNamePattern(".*")
                .scanInterval(Duration.ofSeconds(10))
                .maturityCheckInterval(Duration.ofSeconds(5))
                .maturityCheckMaxTries(10)
                .build()
                .createPoller(), "FilePollerTest");
        FilePoller.start(pollerRef);
        manualTime.timePasses(Duration.ofSeconds(4));
        verify(file, times(0)).rename(anyString());
        manualTime.timePasses(Duration.ofSeconds(1));
        manualTime.timePasses(Duration.ofSeconds(1));
        verify(file).rename(anyString());
    }

    @Test
    void cleanup() throws Exception {
        List<FileInfo> files = new ArrayList<>();
        {
            FileInfo file = mock(FileInfo.class);
            FileInfo.Attributes attributes = mock(FileInfo.Attributes.class);
            when(file.getName()).thenReturn("done.US.0.txt");
            when(file.getAttributes()).thenReturn(attributes);
            when(attributes.getLastModified()).thenReturn(System.currentTimeMillis() - 48 * 3600 * 1000);
            files.add(file);
        }
        {
            FileInfo file = mock(FileInfo.class);
            FileInfo.Attributes attributes = mock(FileInfo.Attributes.class);
            when(file.getName()).thenReturn("done.US.1.txt");
            when(file.getAttributes()).thenReturn(attributes);
            when(attributes.getLastModified()).thenReturn(System.currentTimeMillis());
            files.add(file);
        }
        FileInfo folder = mock(FileInfo.class);
        when(folder.list(any())).thenAnswer(a -> files.stream()
                .filter(a.getArgument(0))
                .collect(Collectors.toList()));

        ActorRef<FilePoller.Start> pollerRef = asyncTestKit.spawn(FilePoller.builder()
                .name("tst")
                .folder(folder)
                .fileNamePattern("US.*\\.txt")
                .scanInterval(Duration.ofSeconds(10))
                .maturityCheckInterval(Duration.ofSeconds(5))
                .maturityCheckMaxTries(10)
                .cleanupAge(Duration.ofDays(1))
                .cleanupInterval(Duration.ofHours(1))
                .build()
                .createPoller(), "FilePollerTest");
        FilePoller.start(pollerRef);
        verify(files.get(0), times(0)).delete();
        verify(files.get(1), times(0)).delete();
        manualTime.timePasses(Duration.ofSeconds(1));
        verify(files.get(0)).delete();
        verify(files.get(1), times(0)).delete();
    }
}