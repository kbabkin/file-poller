package org.kbabkin.filepoller.actor;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Remover {

    private final String name;

    public static Behavior<FileCommand> create() {
        return Behaviors.setup(context -> new Remover(context).remove());
    }

    public Remover(akka.actor.typed.javadsl.ActorContext<FileCommand> context) {
        this.name = context.getSelf().path().toStringWithoutAddress();
    }

    public Behavior<FileCommand> remove() {
        return Behaviors.receiveMessage(f -> {
            log.info("Delete [{}]: {}", name, f);
            f.getFileInfo().delete();
            return Behaviors.same();
        });
    }

}
