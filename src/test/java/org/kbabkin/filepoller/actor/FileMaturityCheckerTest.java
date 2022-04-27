package org.kbabkin.filepoller.actor;

import akka.actor.testkit.typed.Effect;
import akka.actor.testkit.typed.javadsl.BehaviorTestKit;
import akka.actor.testkit.typed.javadsl.TestInbox;
import akka.actor.typed.Behavior;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kbabkin.filepoller.file.FileInfo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FileMaturityCheckerTest {

    FileInfo file = mock(FileInfo.class);
    FileInfo.Attributes attributes = mock(FileInfo.Attributes.class);
    int maxTries = 3;
    TestInbox<FileCommand> outbox = TestInbox.create();
    Behavior<FileCommand> behavior = FileMaturityChecker.create(outbox.getRef(), Duration.ofSeconds(10), maxTries);
    BehaviorTestKit<FileCommand> syncTestKit = BehaviorTestKit.create(behavior);

    @BeforeEach
    void setup() throws IOException {
        when(file.getAttributes()).thenReturn(attributes);
        when(attributes.getLastModified()).thenReturn(System.currentTimeMillis());
        when(attributes.getSize()).thenReturn(100L);
    }

    @Test
    void newFileFound() throws IOException {
        syncTestKit.run(new Scanner.NewFileFound(file));
        assertFalse(outbox.hasMessages(), String.valueOf(outbox.getAllReceived()));
        assertEquals(FileMaturityChecker.FileHash.of(file, maxTries), syncTestKit.expectEffectClass(Effect.Scheduled.class).message());
    }

    @Test
    void newFileFoundWithException() throws IOException {
        when(file.getAttributes()).thenThrow(new FileNotFoundException());
        syncTestKit.run(new Scanner.NewFileFound(file));
        assertFalse(outbox.hasMessages(), String.valueOf(outbox.getAllReceived()));
        assertFalse(syncTestKit.hasEffects(), String.valueOf(syncTestKit.getAllEffects()));
    }

    @Test
    void refreshMatured() throws IOException {
        syncTestKit.run(FileMaturityChecker.FileHash.of(file, maxTries));
        outbox.expectMessage(new FileMaturityChecker.FileMatured(file));
        assertFalse(syncTestKit.hasEffects(), String.valueOf(syncTestKit.getAllEffects()));
    }

    @Test
    void refreshChanged() throws IOException {
        FileMaturityChecker.FileHash message = FileMaturityChecker.FileHash.of(file, maxTries);
        when(attributes.getSize()).thenReturn(200L);
        syncTestKit.run(message);
        assertFalse(outbox.hasMessages(), String.valueOf(outbox.getAllReceived()));
        assertEquals(FileMaturityChecker.FileHash.of(file, maxTries - 1), syncTestKit.expectEffectClass(Effect.Scheduled.class).message());
    }

    @Test
    void refreshChangedButExpired() throws IOException {
        FileMaturityChecker.FileHash message = FileMaturityChecker.FileHash.of(file, 1);
        when(attributes.getSize()).thenReturn(200L);
        syncTestKit.run(message);
        outbox.expectMessage(new FileMaturityChecker.FileMatured(file));
        assertFalse(syncTestKit.hasEffects(), String.valueOf(syncTestKit.getAllEffects()));
    }

    @Test
    void refreshWithException() throws IOException {
        FileMaturityChecker.FileHash message = FileMaturityChecker.FileHash.of(file, maxTries);
        when(file.getAttributes()).thenThrow(new FileNotFoundException());
        syncTestKit.run(message);
        assertFalse(outbox.hasMessages(), String.valueOf(outbox.getAllReceived()));
        assertFalse(syncTestKit.hasEffects(), String.valueOf(syncTestKit.getAllEffects()));
    }
}
