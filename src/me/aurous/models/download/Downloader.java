package me.aurous.models.download;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import me.aurous.exceptions.ExceptionWidget;

/**
 *
 * @author Aero
 */
public class Downloader {
	public class Progress {
	
		private int total = 1;
		private int have = 0;
		private final float progress = 0f;
		private int rate = 0, rateincr = 0;
		private long start = 0, lastRateCheck;
		private int responsecode = 0;

		// from 0.0 to 1.0
		public float getProgress() {
			return have / ((float) total);
		}

		// in bytes/s
		public int getDownloadRate() {
			final long now = System.currentTimeMillis();
			final long t = now - lastRateCheck;
			if (t >= 1000) {
				final float rmod = t / 1000f;
				rate = (int) (rateincr / rmod);
				lastRateCheck = now;
				rateincr = 0;
			}
			return rate;
		}

		public int getTotalSize() {
			return total;
		}

		public long getDownloadStartSystemTime() {
			return start;
		}
	}

	public Progress download(final URL source, final File destination,
			final DownloadResult callback) {
		final Progress progress = new Progress();
		new Thread(
				() -> {
					final long start = System.currentTimeMillis(); // profiling
																	// could be
																	// done
																	// elsewhere
																	// but eh
					final File temp = new File(destination.toString() + ".temp");
					try {
						progress.start = start;
						final HttpURLConnection con = (HttpURLConnection) source
								.openConnection();
						 String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36";
						con.setRequestProperty("User-Agent", USER_AGENT);
						progress.responsecode = con.getResponseCode();
						progress.total = con.getContentLength();
						if (progress.total == 0) {
							progress.total = 1; // dont devide by zero D:
						}
						final InputStream in = con.getInputStream();
						final FileOutputStream out = new FileOutputStream(temp);

						// oldschool but efficient
						int r = 0;
						final byte[] b = new byte[0x7FFF]; // 32kb
						progress.lastRateCheck = System.currentTimeMillis();
						while ((r = in.read(b)) > -1) {
							out.write(b, 0, r);
							progress.have += r;
							progress.rateincr += r;
						}

						in.close();
						out.close();

						temp.renameTo(destination);

						callback.finished(System.currentTimeMillis() - start);

					} catch (final IOException ex) {
						ExceptionWidget widget = new ExceptionWidget(ex);
						widget.showWidget();
						temp.delete();
						callback.failed(System.currentTimeMillis() - start, ex,
								progress.responsecode);
					}
				}).start();
		return progress;
	}
}