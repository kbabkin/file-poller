package org.kbabkin.filepoller.actor;

import lombok.Value;
import lombok.experimental.NonFinal;
import org.kbabkin.filepoller.file.FileInfo;

/**
 * Base class for Akka event related to file.
 */
@Value
@NonFinal
public class FileCommand {
    FileInfo fileInfo;
}
