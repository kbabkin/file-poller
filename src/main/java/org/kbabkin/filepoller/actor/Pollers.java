package org.kbabkin.filepoller.actor;

import akka.NotUsed;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import com.typesafe.config.Config;

import java.util.List;

public class Pollers {

    public static Behavior<NotUsed> create(List<? extends Config> configs) {
        return Behaviors.setup(context -> {
            for (Config config : configs) {
                String name = config.getString("name");
                ActorRef<FilePoller.Start> pollerRef = context.spawn(FilePoller.create(config), name);
                FilePoller.start(pollerRef);
            }
            return Behaviors.empty();
        });
    }

}
