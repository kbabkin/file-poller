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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@Slf4j
public class Scanner {
    private final ActorContext<Scan> context;
    private final String name;
    private final FileInfo folder;
    private final ActorRef<FileCommand> newFileHandlerRef;
    private final Duration interval;
    private final Predicate<FileInfo> filter;

    public static Behavior<Scan> create(FileInfo folder, ActorRef<FileCommand> newFileHandlerRef, Duration interval, Predicate<FileInfo> filter) {
        return Behaviors.setup(context -> new Scanner(context, folder, newFileHandlerRef, interval, filter).scanAndReschedule());
    }

    public Scanner(ActorContext<Scan> context, FileInfo folder, ActorRef<FileCommand> newFileHandlerRef, Duration interval, Predicate<FileInfo> filter) {
        this.context = context;
        this.name = context.getSelf().path().toStringWithoutAddress();
        this.folder = folder;
        this.newFileHandlerRef = newFileHandlerRef;
        this.interval = interval;
        this.filter = filter;
    }

    public Behavior<Scan> scanAndReschedule() {
        return Behaviors.receiveMessage(t -> {
            Scan newScan = scan(t);
            // todo timer.startSingleTimer uses key to replace previous timer.
            // If this is not required for stop/restart sequence, can be scheduled without key:
            // context.scheduleOnce(interval, context.getSelf(), newScan);
            return Behaviors.withTimers(timer -> {
                log.debug("Reschedule [{}]", name);
                timer.startSingleTimer("Scan-" + name, newScan, interval);
                return Behaviors.same();
            });
        });
    }

    Scan scan(Scan t) {
        Set<FileInfo> handledFileInfos = new HashSet<>(t.getPrevFileInfos());
        try {
            Set<FileInfo> fileInfos = new HashSet<>(folder.list(filter));

            // cleanup cached but removed files
            handledFileInfos.retainAll(fileInfos);

            fileInfos.stream()
                    .filter(Predicate.not(handledFileInfos::contains))
                    .forEach(f -> {
                        newFileHandlerRef.tell(new NewFileFound(f));
                        handledFileInfos.add(f);
                        log.info("New File found [{}]: {}", name, f);
                    });
        } catch (Throwable e) {
            log.error("Scan failed", e);
        }
        return new Scan(handledFileInfos);
    }

    public static Predicate<FileInfo> filterByName(String fileNamePattern) {
        Pattern pattern = Pattern.compile(fileNamePattern);
        return n -> pattern.matcher(n.getName()).matches();
    }

    public static Predicate<FileInfo> filterOlderThan(Duration age) {
        return n -> {
            try {
                return Duration.ofMillis(System.currentTimeMillis() - n.getAttributes().getLastModified()).compareTo(age) >= 0;
            } catch (IOException e) {
                return true;
            }
        };
    }

    @Value
    @AllArgsConstructor
    public static class Scan {
        Set<FileInfo> prevFileInfos;

        public Scan() {
            prevFileInfos = Collections.emptySet();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static class NewFileFound extends FileCommand {
        public NewFileFound(FileInfo fileInfo) {
            super(fileInfo);
        }
    }
}
