package org.kbabkin.filepoller.actor;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Sender {

    private final String name;

    public static Behavior<FileCommand> create() {
        return Behaviors.setup(context -> new Sender(context).send());
    }

    public Sender(ActorContext<FileCommand> context) {
        this.name = context.getSelf().path().toStringWithoutAddress();
    }

    public Behavior<FileCommand> send() {
        return Behaviors.receiveMessage(f -> {
            log.info("SEND file (and RENAME) [{}]: {}", name, f);
            //todo: perform actual processing
            f.getFileInfo().rename("done." + f.getFileInfo().getName());
            return Behaviors.same();
        });
    }

}
