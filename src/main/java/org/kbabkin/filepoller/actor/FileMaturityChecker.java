package org.kbabkin.filepoller.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.kbabkin.filepoller.file.FileInfo;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;

@AllArgsConstructor
@Slf4j
public class FileMaturityChecker {
    private final ActorContext<FileCommand> context;
    private final String name;
    private final ActorRef<FileCommand> senderRef;
    private final Duration interval;
    private final int maxTries;

    public FileMaturityChecker(ActorContext<FileCommand> context, ActorRef<FileCommand> senderRef, Duration interval, int maxTries) {
        this.context = context;
        this.name = context.getSelf().path().toStringWithoutAddress();
        this.senderRef = senderRef;
        this.interval = interval;
        this.maxTries = maxTries;
    }

    public static Behavior<FileCommand> create(ActorRef<FileCommand> senderRef, Duration interval, int maxTries) {
        return Behaviors.setup(context -> {
            context.setLoggerName(FileMaturityChecker.class);
            return new FileMaturityChecker(context, senderRef, interval, maxTries).createReceive();
        });
    }

    public Behavior<FileCommand> createReceive() {
        return Behaviors.receive(FileCommand.class)
                .onMessage(Scanner.NewFileFound.class, this::newFileFound)
                .onMessage(FileHash.class, this::refresh)
                .build();
    }

    Behavior<FileCommand> newFileFound(Scanner.NewFileFound t) {
        try {
            FileInfo fileInfo = t.getFileInfo();
            context.scheduleOnce(interval, context.getSelf(), FileHash.of(fileInfo, maxTries));
            log.info("Wait for file maturity [{}]: {}", name, fileInfo);
        } catch (IOException e) {
            log.error("Failed to retrieve file attributes, skip further processing [{}]: {}", name, t, e);
        }
        return Behaviors.same();
    }

    Behavior<FileCommand> refresh(FileHash prevFileHash) {
        try {
            FileHash fileHash = prevFileHash.refresh();
            if (fileHash.isMature()) {
                senderRef.tell(new FileMatured(fileHash.getFileInfo()));
                log.info("File matured [{}]: {}", name, fileHash.getFileInfo());
            } else {
                context.scheduleOnce(interval, context.getSelf(), fileHash);
            }
        } catch (IOException e) {
            log.error("Failed to retrieve file attributes, skip further processing [{}]: {}", name, prevFileHash, e);
        }
        return Behaviors.same();
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    static class FileHash extends FileCommand {
        long lastHash;
        int tries;

        public FileHash(FileInfo fileInfo, long lastHash, int tries) {
            super(fileInfo);
            this.lastHash = lastHash;
            this.tries = tries;
        }

        public static FileHash of(FileInfo fileInfo, int maxTries) throws IOException {
            return new FileHash(fileInfo, getFileInfoHash(fileInfo), maxTries);
        }

        public FileHash refresh() throws IOException {
            long newFileInfoHash = getFileInfoHash(getFileInfo());
            if (newFileInfoHash == getLastHash()) {
                return new FileHash(getFileInfo(), newFileInfoHash, 0);
            } else {
                FileHash newFileHash = new FileHash(getFileInfo(), newFileInfoHash, getTries() - 1);
                if (newFileHash.isMature()) {
                    log.warn("File change exceeded patience max tries: {}", getFileInfo());
                } else {
                    log.info("File change detected: {}", getFileInfo());
                }
                return newFileHash;
            }
        }

        public boolean isMature() {
            return getTries() <= 0;
        }

        static long getFileInfoHash(FileInfo fileInfo) throws IOException {
            FileInfo.Attributes attributes = fileInfo.getAttributes();
            return Objects.hash(attributes.getLastModified(), attributes.getSize());
        }

    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static class FileMatured extends FileCommand {
        public FileMatured(FileInfo fileInfo) {
            super(fileInfo);
        }
    }
}
