package me.aurous.local.media.watcher;

import java.io.File;
import java.nio.file.Path;

/**
 *
 * @author Aero
 */
public interface FileChangeWatcher {
    abstract void added(File file);
    abstract void removed(File file);
    abstract void moved(File from, File to);
}