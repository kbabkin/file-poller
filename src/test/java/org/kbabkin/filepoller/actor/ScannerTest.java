package org.kbabkin.filepoller.actor;

import akka.actor.testkit.typed.Effect;
import akka.actor.testkit.typed.javadsl.BehaviorTestKit;
import akka.actor.testkit.typed.javadsl.TestInbox;
import akka.actor.typed.Behavior;
import org.junit.jupiter.api.Test;
import org.kbabkin.filepoller.file.FileInfo;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScannerTest {
    FileInfo folder = mock(FileInfo.class);
    FileInfo file = mock(FileInfo.class);
    TestInbox<FileCommand> outbox = TestInbox.create();
    Behavior<Scanner.Scan> scanBehavior = Scanner.create(folder, outbox.getRef(), Duration.ofSeconds(1), Scanner.filterByName(".*"));
    BehaviorTestKit<Scanner.Scan> syncTestKit = BehaviorTestKit.create(scanBehavior);

    @Test
    void scanAndReschedule() throws IOException {
        when(folder.list(any())).thenReturn(List.of(file));
        syncTestKit.run(new Scanner.Scan());
        outbox.expectMessage(new Scanner.NewFileFound(file));
        assertEquals(new Scanner.Scan(Set.of(file)), syncTestKit.expectEffectClass(Effect.TimerScheduled.class).msg());
    }

    @Test
    void fileListException() throws IOException {
        when(folder.list(any())).thenThrow(new IOException("test"));
        syncTestKit.run(new Scanner.Scan(Set.of(file)));
        assertFalse(outbox.hasMessages(), String.valueOf(outbox.getAllReceived()));
        assertEquals(new Scanner.Scan(Set.of(file)), syncTestKit.expectEffectClass(Effect.TimerScheduled.class).msg());
    }
}