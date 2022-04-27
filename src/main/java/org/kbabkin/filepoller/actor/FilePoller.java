package org.kbabkin.filepoller.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import com.typesafe.config.Config;
import lombok.Builder;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.kbabkin.filepoller.file.FileInfo;
import org.kbabkin.filepoller.file.NIOFileInfo;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;


//@AllArgsConstructor
@Builder
//@Getter
@ToString
@Slf4j
public class FilePoller {

    private final String name;
    private final FileInfo folder;
    private final String fileNamePattern;
    private final Duration scanInterval;
    private final Duration maturityCheckInterval;
    private final int maturityCheckMaxTries;
    private final Duration cleanupInterval;
    private final Duration cleanupAge;

    public static Behavior<Start> create(Config config) {
        FilePoller filePoller = builder()
                .name(config.getString("name"))
                .folder(new NIOFileInfo(Paths.get(config.getString("folder"))))
                .fileNamePattern(config.getString("fileNamePattern"))
                .scanInterval(config.getDuration("scanInterval"))
                .maturityCheckInterval(config.getDuration("maturityCheckInterval"))
                .maturityCheckMaxTries(config.getInt("maturityCheckMaxTries"))
                .cleanupInterval(config.hasPath("cleanupInterval") ? config.getDuration("cleanupInterval") : null)
                .cleanupAge(config.hasPath("cleanupAge") ? config.getDuration("cleanupAge") : null)
                .build();
        return Behaviors.setup(context -> filePoller.createPoller());
    }

    Behavior<Start> createPoller() {
        log.info("Create [{}]", name);
        return Behaviors.setup(context -> {
            log.info("Setup [{}]", name);

            List<ActorRef<Scanner.Scan>> scanners = new ArrayList<>();

            ActorRef<FileCommand> sender = context.spawn(Sender.create(), "Sender");
            ActorRef<FileCommand> fileMaturityChecker = context.spawn(FileMaturityChecker.create(
                    sender, maturityCheckInterval, maturityCheckMaxTries), "FileMaturityChecker");
            ActorRef<Scanner.Scan> scanner = context.spawn(Scanner.create(
                    folder, fileMaturityChecker, scanInterval, Scanner.filterByName(fileNamePattern)), "Scanner");
            scanners.add(scanner);

            if (cleanupAge != null && cleanupInterval != null) {
                ActorRef<FileCommand> remover = context.spawn(Remover.create(), "Remover");
                ActorRef<Scanner.Scan> cleanupScanner = context.spawn(Scanner.create(
                        folder, remover, cleanupInterval, Scanner.filterByName("done\\." + fileNamePattern)
                                .and(Scanner.filterOlderThan(cleanupAge))), "CleanupScanner");
                scanners.add(cleanupScanner);
            }

            return Behaviors.receiveMessage(s -> {
                for (ActorRef<Scanner.Scan> scanActorRef : scanners) {
                    scanActorRef.tell(new Scanner.Scan());
                }
                return Behaviors.empty();
            });
        });
    }

    public static void start(ActorRef<FilePoller.Start> pollerRef) {
        pollerRef.tell(Start.INSTANCE);
    }

    enum Start {
        INSTANCE
    }

}
