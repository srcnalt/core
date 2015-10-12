/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.aurous.jus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;

import me.aurous.local.settings.GSONHelper;
import me.aurous.models.download.DownloadResult;
import me.aurous.models.download.Downloader;

/**
 *
 * @author Aero
 */
public class DynamicUpdater {

	GSONHelper helper = new GSONHelper();
	ArrayList<File> tracked = new ArrayList<File>();
	ArrayList<String> nowrite = new ArrayList<String>();
        ArrayList<String> exclude = new ArrayList<String>();
        
	String root = "";

	Progress progress = new Progress();
	Downloader downloader = new Downloader();

	public static final String UPDATE_REMOTE_HASHLIST = "update.json";
	public static final String UPDATE_REMOTE_DIR = "data/";

	public void setUpdateDirectory(final File f) {
		root = f.toString();
		final char c = root.charAt(root.length() - 1);
		if (!((c == '/') || (c == '\\') || (c == File.separatorChar))) {
			root += File.separator;
		}
		addAll(f);
	}

	private void addAll(final File f) {
		if(exclude.contains(f.getName())) return;
		for (final File file : f.listFiles()) {
			if (file.isDirectory()) {
				addAll(file);
			} else {
				tracked.add(file);
			}
		}
	}
        
        public void reset(){
            tracked.clear();
            exclude.clear();
        }

	// add file to list of non-writable files, update will be written to
	// <name>.update
	public void addNonWritable(final String name) {
		nowrite.add(name);
	}

	private UpdateData getCurrentData() throws NoSuchAlgorithmException,
			IOException {
		final UpdateData data = new UpdateData();
		for (final File f : tracked) {
			data.hashes.put(f.toString().substring(root.length()), getHash(f));
		}
		return data;
	}

	void addExclude(String ... excludes) {
		for(String s : excludes) exclude.add(s);
	}

	class Progress {

		Downloader.Progress current;
		String currentFile;
		int count = 0;
		int at = 0;

		public float getProgress() { // does not account for file size
			return at / (float) count;
		}

		public String getCurrentFile() {
			return currentFile;
		}

		public Downloader.Progress getCurrentFileProgress() {
			return current;
		}
	}

	class UpdateData {
		HashMap<String, String> hashes = new HashMap<String, String>();
	}

	// apache commons has a better function for this
	public String getHash(final File f) throws NoSuchAlgorithmException,
			FileNotFoundException, IOException {
		final FileInputStream in = new FileInputStream(f);
		final MessageDigest md = MessageDigest.getInstance("MD5");

		int r = 0;
		final byte[] b = new byte[0x7FFF]; // 32kb

		while ((r = in.read(b)) > -1) {
			md.update(b, 0, r);
		}

		return new BigInteger(1, md.digest()).toString(16).toUpperCase();
	}

	// create current version json based on the current files
	public void rebase(final File hashlist) throws IOException,
			NoSuchAlgorithmException {
		final UpdateData data = getCurrentData();
		helper.saveClass(new FileOutputStream(hashlist), data, UpdateData.class);
	}

	final ArrayList<String> needUpdate = new ArrayList<String>();
	final DownloadResult updater = new DownloadResult() {

		int index = 0;

		@Override
		public void finished(final long time) {
			if (index < total) {
				try {
					final String file = needUpdate.get(index);
					final File to = nowrite.contains(file) ? new File(root
							+ file + ".update") : new File(root + file);
					progress.current = downloader.download(new URL(baseURL
							+ UPDATE_REMOTE_DIR + file.replace("\\", "/")), to,
							updater);
					progress.currentFile = file;
					progress.at = index;
				} catch (final MalformedURLException ex) {
					result.failed(time, ex);
				}
				index++;
			} else {
				result.finished(System.currentTimeMillis() - startTime, true);
			}
		}

		@Override
		public void failed(final long time, final IOException exception,
				final int responseCode) {
		//	System.out.println("Update failed!");
			result.failed(time, exception);
		}

	};

	int total = 0;
	String baseURL;
	UpdateResult result;
	long startTime = 0;

	public DynamicUpdater.Progress update(final String baseURL,
			final UpdateResult result) {
		try {
			startTime = System.currentTimeMillis();
			final UpdateData remote = (UpdateData) helper.loadClass(new URL(
					baseURL + UPDATE_REMOTE_HASHLIST).openConnection()
					.getInputStream(), UpdateData.class);
			final UpdateData current = getCurrentData();
			this.baseURL = baseURL;
			this.result = result;
			needUpdate.clear();
			for (final String file : remote.hashes.keySet()) {
				if (!remote.hashes.get(file).equals(current.hashes.get(file))) {
					needUpdate.add(file);
				}
			}
			total = needUpdate.size();
			progress.count = total;
			if (needUpdate.size() > 0) {
				result.started();
				final String file = needUpdate.get(0);
				final File to = nowrite.contains(file) ? new File(root + file
						+ ".update") : new File(root + file);
				progress.current = downloader.download(new URL(baseURL
						+ UPDATE_REMOTE_DIR + file.replace("\\", "/")), to,
						updater);
			} else {
				result.finished(0, false);
			}

		} catch (final IOException ex) {
			result.failed(0, ex);
		} catch (final NoSuchAlgorithmException ex) {
			result.failed(0, ex);
		}
		return progress;
	}
}