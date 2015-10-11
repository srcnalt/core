package me.aurous.local.settings;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;

import com.google.gson.Gson;

public class GSONHelper {
	Gson son = new Gson();

	public void saveClass(final OutputStream out, final Object clas,
			final Type t) throws IOException {
		final OutputStreamWriter w = new OutputStreamWriter(out);
		w.write(son.toJson(clas, t));
		w.close();
	}

	public void saveClass(final OutputStream out, final Object clas)
			throws IOException {
		final OutputStreamWriter w = new OutputStreamWriter(out);
		w.write(son.toJson(clas));
		w.close();
	}

	public Object loadClass(final InputStream in, final Type t)
			throws IOException {
		return son.fromJson(new InputStreamReader(in), t);
	}
}