package me.aurous.local.media.watcher;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import me.aurous.exceptions.ExceptionWidget;

/**
 *
 * @author Aero
 */
public class SimpleWatcher {

	CopyOnWriteArrayList < FileChangeWatcher > watchers = new CopyOnWriteArrayList < FileChangeWatcher > ();
	CopyOnWriteArrayList < File > watchedFiles = new CopyOnWriteArrayList < File > (); // because it needs to fire removed for every deleted file if a directory is deleted
	ArrayList < String > extensions = new ArrayList < String > ();
	WatchService watcher;

	final long MOVEHACK_MAX_TIME = 64; // maximum amount of time (miliseconds) between ENTRY_DELETED and ENTRY_CREATED with the same filename

	public SimpleWatcher(final String[] directories) throws IOException {
		this();
		for (final String s: directories) {
			watchDir(Paths.get(s));
		}
	}
	public SimpleWatcher(final Path[] directories) throws IOException {
		this();
		for (final Path s: directories) {
			watchDir(s);
		}
	}

	public void watchDir(final Path p) throws IOException {
		final File f = p.toFile();
		watchDir(f);
	}

	public void watchDir(final File f) throws IOException {
		registerForWatch(f);
		for (final File s: f.listFiles()) {
			if (s.isDirectory()) {
				watchDir(s);
			} else {
				added(s);
			}
		}
	}

	private void registerForWatch(final File f) throws IOException {
		Paths.get(f.toURI()).register(watcher, ENTRY_CREATE, ENTRY_DELETE);
	}

	public void addWatcher(final FileChangeWatcher watcher) {
		watchers.add(watcher);
	}

	public void addExtensions(final String...extensions) {
		for (String s: extensions) {
			final int l = s.length();
			if (l > 3) {
				s = s.substring(l - 3);
			} else if (l == 2)
			{
				s = "." + s; // ugly little bit of code but it makes lookups much faster (does not work with file formats that are only 1 chracter long)
			}
			this.extensions.add(s);
		}
	}

	private boolean isValid(final File f) {
		final String name = f.getName();
		return !f.isDirectory() && name.length() > 3 && extensions.contains(name.substring(name.length() - 3));
	}

	private void process(final File f, final Kind <? > kind) throws IOException {
		if (kind == ENTRY_CREATE) {
			if (f.isDirectory()) {
				watchDir(f);
			} else {
				added(f);
			}
		} else {
			removed(f);
		}
	}

	private void added(final File f) throws IOException {
		if (isValid(f)) {
			watchedFiles.add(f);
			for (final FileChangeWatcher w: watchers) {
				w.added(f);
			}
		}
	}

	private void removed(final File f) throws IOException {
		if (f.isDirectory()) {
			final String root = f.toString();
			for (final File s: watchedFiles) {
				if (s.toString().startsWith(root)) {
					removed(s);
				}
			}
		} else {
			if (isValid(f)) {
				watchedFiles.remove(f);
				for (final FileChangeWatcher w: watchers) {
					w.removed(f);
				}
			}
		}
	}

	private void processMove(final File from, final File to) {
		if (to.isDirectory()) {
			final String baseFrom = from.toString() + File.separator;
		//	System.out.println(baseFrom);
			for (final File f: to.listFiles()) {
				for (final FileChangeWatcher w: watchers) {
					w.moved(new File(baseFrom + f.getName()), f);
				}
			}
		} else {
			for (final FileChangeWatcher w: watchers) {
				w.moved(from, to);
			}
		}
	}

	public SimpleWatcher() throws IOException {
		watcher = FileSystems.getDefault().newWatchService();
		final Thread t = new Thread((Runnable) () -> {
			WatchKey k = null; // moved hack
			try {
				while (true) { // not a fan
					k = watcher.take();
					boolean removed = false;
					File lastRemoved = null;
					final int cnt = 0;
					for (final WatchEvent <? > e1: k.pollEvents()) {
						final WatchEvent < Path > path1 = (WatchEvent < Path > ) e1;
						final Kind <? > kind1 = e1.kind();
						removed = kind1 == ENTRY_DELETE;
						final Path dir1 = (Path) k.watchable();
						final Path p1 = dir1.resolve(path1.context()).normalize();
						final File f1 = p1.toFile();
						if (!removed && (lastRemoved != null)) { // renamed
							processMove(lastRemoved, f1);
						} else if (removed) {
							lastRemoved = f1;
						} else {
							try {
								process(f1, kind1);
							} catch (final IOException ex1) {
								ExceptionWidget widget = new ExceptionWidget(ex1);
								widget.showWidget();
							}
						}
					}
					k.reset();
					if (removed) {
						k = watcher.poll(MOVEHACK_MAX_TIME, TimeUnit.MILLISECONDS);
						if (k != null) {
							for (final WatchEvent <? > e2: k.pollEvents()) {
								final Kind <? > kind2 = e2.kind();
								final WatchEvent < Path > path2 = (WatchEvent < Path > ) e2;
								final Path dir2 = (Path) k.watchable();
								final Path p2 = dir2.resolve(path2.context()).normalize();
								final File f2 = p2.toFile();
								if (kind2 == ENTRY_CREATE) {
									processMove(lastRemoved, f2);
									k.reset();
								} else {
									try {
										process(lastRemoved, ENTRY_DELETE);
										process(f2, ENTRY_DELETE);
									} catch (final IOException ex2) {
										ExceptionWidget widget = new ExceptionWidget(ex2);
										widget.showWidget();
									}
								}
							}
							k.reset();
						} else {
							try {
								process(lastRemoved, ENTRY_DELETE);
							} catch (final IOException ex3) {
								ExceptionWidget widget = new ExceptionWidget(ex3);
								widget.showWidget();
							}
						}
					}
				}
			} catch (final InterruptedException e3) {
				ExceptionWidget widget = new ExceptionWidget(e3);
				widget.showWidget();
			}
		}, "Directory Watcher");
		t.setDaemon(true);
		t.start();
	}

}
