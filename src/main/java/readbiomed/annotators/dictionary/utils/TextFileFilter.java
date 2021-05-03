package readbiomed.annotators.dictionary.utils;

import java.io.File;

import org.apache.commons.io.filefilter.IOFileFilter;

public final class TextFileFilter implements IOFileFilter {
	public boolean accept(File file) {
		return file.getPath().endsWith(".txt");
	}

	public boolean accept(File dir, String name) {
		return name.endsWith(".txt");
	}
}