package com.sh1r0.noveldroid;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.os.Environment;

public class NovelUtils {
	public static final int MAX_THREAD_NUM = 4;
	public static final String APP_DIR = Environment.getExternalStorageDirectory()
			.getAbsolutePath() + "/NovelDroid/";
	public static final String TEMP_DIR = APP_DIR + "temp/";
	private static NovelUtils novelUtils;

	private final Map<Character, Character> S2T_MAP;

	private NovelUtils() {
		S2T_MAP = new HashMap<Character, Character>();
		try {
			InputStream input = ApplicationContextProvider.getContext().getAssets()
					.open("table_s2t.txt");
			byte[] buffer = new byte[input.available()];
			input.read(buffer);
			input.close();
			char[] S2T_TABLE = (new String(buffer)).toCharArray();
			for (int i = 0; i < S2T_TABLE.length; i += 2) {
				final Character cS = Character.valueOf(S2T_TABLE[i]);
				final Character cT = Character.valueOf(S2T_TABLE[i + 1]);
				S2T_MAP.put(cS, cT);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String s2t(String input) {
		final char[] sChars = input.toCharArray();
		for (int i = 0, n = sChars.length; i < n; i++) {
			final Character tChar = S2T_MAP.get(sChars[i]);
			if (tChar != null)
				sChars[i] = tChar;
		}
		return String.valueOf(sChars);
	}

	public static NovelUtils getInstance() {
		if (novelUtils == null) {
			novelUtils = new NovelUtils();
		}
		return novelUtils;
	}

	public static String genTxtName(String novelName, String authorName, int namingRule) {
		String filename = "";
		switch (namingRule) {
			case 0:
				filename = novelName + ".txt";
				break;
			case 1:
				filename = novelName + "_" + authorName + ".txt";
				break;
			case 2:
				filename = authorName + "_" + novelName + ".txt";
				break;
			default:
				filename = novelName + ".txt";
				break;
		}

		return filename;
	}

	public static OutputStreamWriter newNovelWriter(String filepath, String encoding) throws IOException {
		OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(filepath), encoding);
		if (encoding.equals("UTF-16LE")) { // inject BOM
			writer.write("\uFEFF");
		}

		return writer;
	}

	public static void unZip(String zipName, String target) throws IOException {
		ZipFile zf = new ZipFile(NovelUtils.TEMP_DIR + zipName);
		ZipEntry ze = zf.getEntry(target);

		InputStream inputStream = zf.getInputStream(ze);
		FileOutputStream fout = new FileOutputStream(NovelUtils.TEMP_DIR + ze.getName());

		int length = 0;
		byte[] buffer = new byte[2048];
		while ((length = inputStream.read(buffer)) > 0)
			fout.write(buffer, 0, length);

		inputStream.close();
		fout.close();
		zf.close();
	}

	public static String replace(String str, String target, String replacement) {
		StringBuilder sb = new StringBuilder(str);

		int index = sb.length();
		int lenTarget = target.length();
		while ((index = sb.lastIndexOf(target, index)) != -1) {
			sb.replace(index, index + lenTarget, replacement);
			index -= lenTarget;
		}

		return sb.toString();
	}

	public static String replace(String str, String[] targets, String[] replacements) {
		StringBuilder sb = new StringBuilder(str);

		int index, lenTarget;
		for (int i = 0; i < targets.length; i++) {
			index = sb.length();
			lenTarget = targets[i].length();
			while ((index = sb.lastIndexOf(targets[i], index)) != -1) {
				sb.replace(index, index + lenTarget, replacements[i]);
				index -= lenTarget;
			}
		}

		return sb.toString();
	}

	public static void deleteTempFiles(File tempDir) {
		File[] files = tempDir.listFiles();
		if (files != null) {
			for (File f : files) {
				f.delete();
			}
		}
	}
}
