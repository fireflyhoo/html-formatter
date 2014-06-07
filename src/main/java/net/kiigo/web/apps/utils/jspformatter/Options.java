package net.kiigo.web.apps.utils.jspformatter;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Vector;

public class Options {
	/***
	 * 输入文件名
	 */
	private Vector<String> inputFiles;
	
	/**
	 * 可用的标签
	 */
	private HashSet<String> tagNames;
	
	/***
	 * 默认行长度
	 */
	public int lineLength = 300;
	
	public boolean spaces = true;
	public int numSpaces = 2;
	public int tabSize = 8;
	public boolean testMode = false;

	public Options() {
		this.inputFiles = new Vector<String>();

		this.tagNames = new HashSet<String>();
		this.tagNames.add("!doctype");
		this.tagNames.add("base");
		this.tagNames.add("basefont");
		this.tagNames.add("bgsound");
		this.tagNames.add("br");
		this.tagNames.add("dd");
		this.tagNames.add("dt");
		this.tagNames.add("frame");
		this.tagNames.add("img");
		this.tagNames.add("hr");
		this.tagNames.add("input");
		this.tagNames.add("isindex");
		this.tagNames.add("li");
		this.tagNames.add("link");
		this.tagNames.add("meta");
		this.tagNames.add("nextid");
		this.tagNames.add("param");
		this.tagNames.add("plaintext");
		this.tagNames.add("wbr");
	}

	public void addFile(String file) {
		this.inputFiles.add(file);
	}

	public Enumeration<String> getFiles() {
		return this.inputFiles.elements();
	}

	public boolean hasEndTag(String tagName) {
		return !this.tagNames.contains(tagName);
	}
}