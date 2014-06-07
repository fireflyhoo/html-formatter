package net.kiigo.web.apps.utils.jspformatter;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.EmptyStackException;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/***
 * 格式化 jsp html 格式化工具
 * @author Coollf
 *
 */
public class JSPFormatter {
	public static final int TEXT_MODE = 0;
	public static final int TAG_MODE = 1;
	public static final int HTML_START_TAG_MODE = 2;
	public static final int HTML_END_TAG_MODE = 3;
	public static final int HTML_COMMENT_MODE = 4;
	public static final int JSP_COMMENT_MODE = 5;
	public static final int JAVA_MODE = 6;
	public static final int JAVA_INLINE_MODE = 7;
	public static final int JAVA_DECL_MODE = 8;
	public static final int JAVASCRIPT_MODE = 9;
	private static final String escOpAngle = "\\<";
	private static final String escSlash = "\\/";
	private static final char TAB = '\t';
	private StringBuffer writer = new StringBuffer();
	private Options opts;
	private String inFilename;
	private String outFilename;
	private String inText;
	private String outText;

	public JSPFormatter(String inFilename, String outFilename, Options opts) {
		this.inFilename = inFilename;
		this.outFilename = outFilename;
		this.opts = opts;
	}

	public JSPFormatter(String inText, int ident, int LineLength) {
		this.inText = inText;
		this.opts = new Options();
		this.opts.numSpaces = ident;
		this.opts.lineLength = LineLength;
	}

	private StringBuffer indent(int indentLevel) {
		StringBuffer retVal = new StringBuffer(0);
		if (this.opts.spaces) {
			for (int i = 0; i < this.opts.numSpaces * indentLevel; i++)
				retVal.append(" ");
		} else {
			for (int i = 0; i < indentLevel; i++) {
				retVal.append('\t');
			}
		}

		return retVal;
	}

	private void formatText(int indentLevel, StringBuffer text)
			throws IOException {
		formatText(indentLevel, text.toString());
	}

	private int indentLength(int indentLevel) {
		if (this.opts.spaces) {
			return indentLevel * this.opts.numSpaces;
		}
		return indentLevel * this.opts.tabSize;
	}

	private int indentLength(int indentLevel, String text) {
		return indentLength(indentLevel) + text.length();
	}

	private void formatText(int indentLevel, String text) throws IOException {
		if (text.trim().length() == 0) {
			return;
		}
		StringTokenizer tokenizer = new StringTokenizer(text, "\t \r\n", false);
		StringBuffer output = indent(indentLevel);
		String last = "";

		boolean firstToken = true;

		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();

			if (!firstToken) {
				output.append(" ");
			}
			output.append(token);
			firstToken = false;

			boolean overLineLength = false;
			if ((this.opts.spaces) && (output.length() > this.opts.lineLength)) {
				overLineLength = true;
			}
			if ((!this.opts.spaces)
					&& (output.length() + indentLevel * (this.opts.tabSize - 1) > this.opts.lineLength)) {
				overLineLength = true;
			}
			if (overLineLength) {
				this.writer.append(last);
				this.writer.append("\n");
				output = new StringBuffer(indent(indentLevel) + token);
			}
			last = output.toString();
		}

		if (output.length() > 0) {
			this.writer.append(output.toString());
			this.writer.append("\n");
		}
	}

	private void formatHTMLComment(int indentLevel, String comment)
			throws IOException {
		formatLine(indentLevel, "<!--");
		formatText(indentLevel + 1, comment.substring(4, comment.length() - 3));
		formatLine(indentLevel, "-->");
	}

	private void formatTag(int indentLevel, String tag) throws IOException {
		if (indentLength(indentLevel, tag) <= this.opts.lineLength) {
			formatLine(indentLevel, tag);
		} else {
			int currCharNum;
			for (currCharNum = 0; (currCharNum < tag.length())
					&& (!Character.isWhitespace(tag.charAt(currCharNum))); currCharNum++)
				;
			currCharNum++;
			if (currCharNum >= tag.length()) {
				return;
			}
			formatLine(indentLevel, tag.substring(0, currCharNum));
			StringBuffer attributeAndValue = new StringBuffer();
			char insideQuote = '\000';
			for (; currCharNum < tag.length(); currCharNum++) {
				char currChar = tag.charAt(currCharNum);
				if ((Character.isWhitespace(currChar)) && (insideQuote == 0)
						&& (attributeAndValue.length() != 0)) {
					formatLine(indentLevel + 2, attributeAndValue.toString());
					attributeAndValue = new StringBuffer();
				} else if (((currChar == '\'') || (currChar == '"'))
						&& (insideQuote == 0)) {
					insideQuote = currChar;
					attributeAndValue.append(currChar);
				} else if (currChar == insideQuote) {
					attributeAndValue.append(currChar);
					insideQuote = '\000';
				} else {
					attributeAndValue.append(currChar);
				}
			}
			String lastAttribute = attributeAndValue.toString();
			if (lastAttribute.length() > 0) {
				if ((lastAttribute.equals(">")) || (lastAttribute.equals("/>")))
					formatLine(indentLevel, lastAttribute);
				else {
					formatLine(indentLevel + 2, lastAttribute);
				}
			}
			if (!lastAttribute.endsWith("/>"))
				this.writer.append("\n");
		}
	}

	private void formatLine(int indentLevel, String line) throws IOException {
		if (line.trim().length() != 0) {
			StringBuffer output = indent(indentLevel);
			output.append(line);
			this.writer.append(output.toString());
		}
		this.writer.append("\n");
	}

	private int formatCode(int mode, int inIndentLevel, String inJavaCode)
			throws IOException {
		int indentLevel = inIndentLevel;
		String javaCode;

		if (mode == 8) {
			formatText(inIndentLevel, "<%!");
			javaCode = inJavaCode.substring(3, inJavaCode.length() - 2).trim();
		} else {

			if (mode == 6) {
				formatText(inIndentLevel, "<%");
				javaCode = inJavaCode.substring(2, inJavaCode.length() - 2)
						.trim();
			} else {
				javaCode = inJavaCode.trim();
			}
		}
		while (javaCode.length() > 0) {
			int index = javaCode.indexOf('\n');
			String line;

			if (index == -1)
				line = javaCode.trim();
			else {
				line = javaCode.substring(0, index).trim();
			}
			if (line.equals("// -->")) {
				formatLine(indentLevel - 1, line);
			} else {
				if (line.endsWith("}")) {
					indentLevel--;
				}
				if ((line.startsWith("}")) && (line.endsWith("{"))) {
					formatLine(indentLevel - 1, line);
					indentLevel--;
				} else {
					formatLine(indentLevel, line);
				}
				if (line.endsWith("{")) {
					indentLevel++;
				}
			}
			if (index == -1) {
				break;
			}
			javaCode = javaCode.substring(index + 1);
		}

		if ((mode == 8) || (mode == 6)) {
			formatText(indentLevel, "%>");
		}
		return indentLevel;
	}

	public int findClosingAngle(String line) {
		int balance = 1;
		if (line.startsWith("<")) {
			for (int i = 1; i < line.length(); i++) {
				char c = line.charAt(i);
				if (c == '<')
					balance++;
				else if (c == '>') {
					balance--;
				}
				if (balance == 0)
					return i;
			}
		} else {
			throw new IllegalArgumentException(line);
		}

		return -1;
	}

	public int findClosingHTMLComment(String line) {
		if (line.startsWith("<!--")) {
			return line.indexOf("-->");
		}
		throw new IllegalArgumentException(line);
	}

	public int findClosingJSPComment(String line) {
		return line.indexOf("--%>");
	}

	public int findClosingJavaAngle(String line) {
		if (line.equals("<%")) {
			line = "<% ";
		}
		if (line.startsWith("<%")) {
			char insideQuote = '\000';
			for (int i = 0; i < line.length(); i++) {
				char currChar = line.charAt(i);
				if ((currChar == '%') && (insideQuote == 0)) {
					if (line.charAt(i + 1) == '>')
						return i;
				} else if (((currChar == '\'') || (currChar == '"'))
						&& (insideQuote == 0))
					insideQuote = currChar;
				else if (currChar == '\\')
					i += 2;
				else if (currChar == insideQuote) {
					insideQuote = '\000';
				}
			}
			return -1;
		}
		throw new IllegalArgumentException(line);
	}

	private int safePop(IntStack modeStack) {
		try {
			return modeStack.pop();
		} catch (EmptyStackException ese) {
		}
		return 0;
	}

	private String safePop(TagStack tagStack) {
		try {
			return tagStack.pop();
		} catch (EmptyStackException ese) {
		}
		return "";
	}

	public String format() throws FileNotFoundException, IOException {
		Pattern scriptPattern = Pattern.compile("^\\<SCRIPT.*", 2);
		Pattern endScriptPattern = Pattern.compile(".*\\<\\/SCRIPT>.*", 2);
		BufferedReader reader = null;
		reader = new BufferedReader(new StringReader(this.inText));

		String startTag = "";
		String endTag = "";
		String line = "";
		String javaCode = "";
		String htmlComment = "";
		String tagName = "";
		int lines = 0;
		int mode = 0;
		int indentLevel = 0;
		IntStack modeStack = new IntStack();
		TagStack tagStack = new TagStack();
		String lineSep = System.getProperty("line.separator");

		StringBuffer text = new StringBuffer(0);
		while (true) {
			if (line.length() == 0) {
				String newLine = reader.readLine();
				lines++;
				if (newLine == null)
					break;
				if ((newLine.trim().length() == 0) && (mode == 0)) {
					this.writer.append("\n");
				} else
					line = newLine;
			} else {

				int index;
				switch (mode) {
				case 0:

					index = line.indexOf("<");
					if (index > -1) {
						modeStack.push(mode);
						mode = 1;
						text.append(line.substring(0, index));
						formatText(indentLevel, text);
						text = new StringBuffer(0);
						line = line.substring(index);
					} else {
						text.append(line + " ");
						line = "";
					}
					break;
				case 1:
					if ((line.startsWith("<% ")) || (line.startsWith("<%\t"))
							|| (line.trim().equals("<%"))) {
						mode = 6;
					} else if (line.startsWith("<!--")) {
						if (modeStack.peek() != 9) {
							mode = 4;
						} else {
							formatLine(indentLevel - 1, line);
							line = "";
							mode = modeStack.pop();
						}
					} else if (line.startsWith("<%--")) {
						mode = 5;
					} else if ((line.startsWith("<%! "))
							|| (line.endsWith("<%!"))) {
						mode = 8;
					} else if ((line.startsWith("<%="))
							|| (line.startsWith("<%@"))) {
						mode = 7;
					} else if (line.startsWith("</")) {
						mode = 3;
					} else
						mode = 2;
					break;
				case 2:
					if (startTag.length() == 0) {
						startTag = line.trim();
						index = findClosingAngle(startTag);
						int index2 = startTag.indexOf(" ");
						if ((index > -1) && (index2 > -1)) {
							if (index < index2)
								tagName = startTag.substring(1, index)
										.toLowerCase();
							else
								tagName = startTag.substring(1, index2)
										.toLowerCase();
						} else if (index > -1)
							tagName = startTag.substring(1, index)
									.toLowerCase();
						else if (index2 > -1)
							tagName = startTag.substring(1, index2)
									.toLowerCase();
						else
							tagName = startTag.substring(1).toLowerCase();
					} else {
						startTag = startTag + " " + line.trim();
					}
					index = findClosingAngle(startTag);
					if (index > -1) {
						formatTag(indentLevel, startTag.substring(0, index + 1));
						if ((!startTag.startsWith("<?"))
								&& (!startTag.substring(0, index + 1).endsWith(
										"/>"))
								&& (this.opts.hasEndTag(tagName))) {
							tagStack.push(tagName);
							indentLevel++;
						}

						if (scriptPattern.matcher(startTag).matches())
							mode = 9;
						else {
							mode = safePop(modeStack);
						}
						line = startTag.substring(index + 1);
						startTag = "";
						tagName = "";
					} else {
						line = "";
					}
					break;
				case 3:
					endTag = endTag + line + " ";
					index = findClosingAngle(endTag);
					if (index > -1) {
						indentLevel--;
						if (indentLevel < 0) {
							indentLevel = 0;
						}
						endTag = endTag.substring(0, index + 1);
						formatTag(indentLevel, endTag);
						endTag = endTag.substring(2, endTag.length() - 1);
						startTag = "";
						while (true) {
							startTag = safePop(tagStack);
							if (!startTag.equals("")) {
								if (startTag.equalsIgnoreCase(endTag)) {
									break;
								}
							}
						}
						startTag = "";
						endTag = "";
						mode = safePop(modeStack);
						if ((mode == 9)
								&& (endScriptPattern.matcher(line.substring(0,
										index + 1)).matches())) {
							mode = safePop(modeStack);
						}

						line = line.substring(index + 1);
					} else {
						line = "";
					}
					break;
				case 4:
					if (htmlComment.length() == 0) {
						index = findClosingHTMLComment(line);
						if (index > -1) {
							htmlComment = line.substring(0, index + 3);

							if (indentLength(indentLevel, htmlComment) <= this.opts.lineLength)
								formatLine(indentLevel, htmlComment);
							else {
								formatHTMLComment(indentLevel, htmlComment);
							}
							htmlComment = "";
							line = line.substring(index + 3);
							mode = safePop(modeStack);
						} else {
							htmlComment = line;
							line = "";
						}
					} else {
						htmlComment = htmlComment + line + lineSep;
						index = findClosingHTMLComment(htmlComment);
						if (index > -1) {
							htmlComment = htmlComment.substring(0,
									htmlComment.length() - lineSep.length());
							formatHTMLComment(indentLevel, htmlComment);
							line = htmlComment.substring(index + 3);
							htmlComment = "";
							mode = safePop(modeStack);
						} else {
							line = "";
						}
					}
					break;
				case 5:
					index = findClosingJSPComment(line);

					if (index > -1) {
						formatLine(indentLevel, line.substring(0, index + 4));

						mode = safePop(modeStack);
						line = line.substring(index + 4);
					} else {
						this.writer.append(line);
						this.writer.append("\n");
						line = "";
					}
					break;
				case 6:
				case 8:
					if (javaCode.length() == 0)
						javaCode = line;
					else {
						javaCode = javaCode + lineSep + line;
					}
					index = findClosingJavaAngle(javaCode);
					if (index > -1) {
						indentLevel = formatCode(mode, indentLevel,
								javaCode.substring(0, index + 2));
						line = line.substring(line.length()
								- (javaCode.length() - index) + 2);
						mode = safePop(modeStack);
						javaCode = "";
					} else {
						line = "";
					}
					break;
				case 9:
					if (javaCode.length() == 0)
						javaCode = line;
					else {
						javaCode = javaCode + lineSep + line;
					}
					if (line.equalsIgnoreCase("</script>")) {
						line = "</script>";
					}
					index = line.indexOf("</script>");
					if (index > -1) {
						int javaCodeIndex = javaCode.length()
								- (line.length() - index);
						int indexSpace = javaCode.indexOf(" ",
								javaCodeIndex + 1);
						int indexColon = javaCode.indexOf(":",
								javaCodeIndex + 1);
						if (((line.indexOf("alert") == -1) && (indexColon > -1)
								&& (indexSpace > -1) && (indexColon < indexSpace))
								|| (javaCode.substring(javaCodeIndex)
										.toLowerCase().startsWith("</"))
								|| (javaCode.substring(javaCodeIndex)
										.startsWith("<!--"))
								|| (javaCode.substring(javaCodeIndex)
										.startsWith("<%"))) {
							indentLevel = formatCode(mode, indentLevel,
									javaCode.substring(0, javaCodeIndex));
							line = javaCode.substring(javaCodeIndex);
							modeStack.push(mode);
							mode = 1;
							javaCode = "";
						} else {
							int index3 = line.indexOf("<", index + 1);
							if (index3 > -1) {
								int javaCodeIndex2 = javaCode.length()
										- (line.length() - index3);
								javaCode = javaCode
										.substring(0, javaCodeIndex2);
								line = line.substring(index3);
							} else {
								line = "";
							}
						}
					} else {
						line = "";
					}
					break;
				case 7:
					if (javaCode.length() == 0)
						javaCode = line.trim();
					else {
						javaCode = javaCode + " " + line.trim();
					}
					index = findClosingJavaAngle(javaCode);
					if (index > -1) {
						formatLine(indentLevel,
								javaCode.substring(0, index + 2));
						line = line.substring(line.length()
								- (javaCode.length() - index) + 2);
						mode = safePop(modeStack);
						javaCode = "";
					} else {
						line = "";
					}
					break;
				}
			}
		}
		String newLine;
		switch (mode) {
		case 0:
			if (text.toString().trim().length() != 0)
				formatText(indentLevel, text);
			break;
		case 1:
			break;
		case 2:
			this.writer.append(startTag);
			this.writer.append("\n");
			break;
		case 3:
			this.writer.append(endTag);
			this.writer.append("\n");
			break;
		case 4:
			this.writer.append(htmlComment.trim());
			this.writer.append("\n");
			break;
		case 5:
			break;
		case 6:
		case 7:
		case 8:
			this.writer.append(javaCode);
			this.writer.append("\n");
			break;
		case 9:
			this.writer.append(javaCode);
			this.writer.append("\n");
		}

		return this.writer.toString();
	}
}