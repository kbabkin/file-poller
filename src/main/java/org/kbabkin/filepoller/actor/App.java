package org.kbabkin.filepoller.actor;


import akka.actor.typed.ActorSystem;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class App {
    public static void main(String[] args) {
        Config config = ConfigFactory.load();
        log.info("Starting ActorSystem");
        ActorSystem.create(Pollers.create(config.getConfigList("pollers")), "Pollers");
        log.info("Created ActorSystem");
    }
}
