package net.kiigo.web.apps.utils.jstyle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Stack;
/***
 * js 格式化
 * @author Coollf
 *
 */
public class JSFormatter {
	private static String[] headers = { "if", "else", "for", "while", "do",
			"try", "catch", "finally", "synchronized", "switch", "static" };

	private static String[] parenHeaders = { "if", "for", "while", "catch",
			"synchronized", "switch" };
	private static String[] nonParenHeaders = { "else", "do", "try", "static",
			"finally" };
	private static String[] statementHeaders = { "class", "interface", "throws" };
	private static String[] longOperators = { "==", "!=", ">=", "<=", "+=",
			"-=", "*=", "/=", "%=", "^=", "|=", "&=", "++", "--", "&&", "||",
			".*" };

	private static Hashtable closingHeaders = new Hashtable();
	public JSBeautifier beautifier;
	private JSLineBreaker lineBreaker;
	private StringBuffer outBuffer;
	private String tempLine = "";
	private Stack openingStack;
	private Stack parenDepthsStack;
	private Stack bracketBlockStateStack;
	private char quoteChar;
	private int parenDepth;
	private int leadingWhiteSpaces;
	private String currentHeader;
	private boolean isInHeader;
	private boolean isSpecialChar;
	private boolean isInQuote;
	private boolean isInComment;
	private boolean isBlockNeeded;
	private boolean isSpecialBlock;
	private boolean isCloseSpecialBlock;
	private boolean isParenNeeded;
	private boolean isNewLineNeeded;
	private boolean checkBlockOpen;
	private boolean checkBlockClose;
	private boolean checkIf;
	private boolean checkClosingHeader;
	private boolean foundOrigLineBreak;
	private boolean isInQuestion;
	private boolean isSummerized;
	private boolean isInBracketOpen;
	private boolean isInBracketClose;
	private boolean isInClassStatement;
	private boolean bracketBreak = false;
	private char prevNonSpaceCh;
	private char currentNonSpaceCh;

	public JSFormatter() {
		this.beautifier = new JSBeautifier();
		this.lineBreaker = new JSLineBreaker();
		init();
	}

	public void init() {
		this.beautifier.init();
		this.lineBreaker.init();
		this.outBuffer = new StringBuffer();
		this.openingStack = new Stack();
		this.parenDepthsStack = new Stack();
		this.bracketBlockStateStack = new Stack();
		this.bracketBlockStateStack.push(new Boolean(true));
		this.tempLine = "";
		this.parenDepth = 0;
		this.isSpecialChar = false;
		this.isInQuote = false;
		this.isInComment = false;
		this.isBlockNeeded = false;
		this.isParenNeeded = false;
		this.isSpecialBlock = false;
		this.isCloseSpecialBlock = false;
		this.isNewLineNeeded = false;
		this.checkIf = false;
		this.checkBlockOpen = false;
		this.checkClosingHeader = false;
		this.checkBlockClose = false;
		this.foundOrigLineBreak = false;
		this.isInQuestion = false;
		this.isSummerized = false;
		this.isInBracketOpen = false;
		this.isInBracketClose = false;
		this.leadingWhiteSpaces = 0;
		this.isInHeader = false;
		this.isInClassStatement = false;
		this.prevNonSpaceCh = '{';
		this.currentNonSpaceCh = '{';
	}

	public void format(BufferedReader inReader, PrintWriter outWriter)
			throws IOException {
		String line = null;
		init();
		try {
			while (!hasMoreFormattedLines()) {
				line = inReader.readLine();
				if (line == null)
					throw new NullPointerException();
				formatLine(line);
			}
			while (hasMoreFormattedLines())
				outWriter.println(nextFormattedLine());
		} catch (NullPointerException e) {
			summerize();
			while (hasMoreFormattedLines())
				outWriter.println(nextFormattedLine());
		}
	}

	public boolean hasMoreFormattedLines() {
		if (this.lineBreaker.hasMoreBrokenLines()) {
			return true;
		}
		while (((!this.isSummerized) && (!isNewLineRequested()))
				|| ((this.isSummerized) && (hasMoreSummerizedLines()))) {
			String formatResult = format(null);
			if (formatResult != null) {
				this.lineBreaker.breakLine(formatResult);
				return true;
			}
		}
		return false;
	}

	public void formatLine(String line) {
		String formatResult = format(line);
		if (formatResult != null)
			this.lineBreaker.breakLine(formatResult);
	}

	public String nextFormattedLine() {
		return this.lineBreaker.nextBrokenLine();
	}

	public void summerize() {
		formatLine("");
		this.isSummerized = true;
	}

	public void setBracketBreak(boolean br) {
		this.bracketBreak = br;
	}

	public void setBracketIndent(boolean state) {
		this.beautifier.setBracketIndent(state);
	}

	public void setSwitchIndent(boolean state) {
		this.beautifier.setSwitchIndent(state);
	}

	public void setPreferredLineLength(int length) {
		this.lineBreaker.setPreferredLineLength(length);
	}

	public void setLineLengthDeviation(int dev) {
		this.lineBreaker.setLineLengthDeviation(dev);
	}

	public void setNestedConnection(boolean nest) {
		this.lineBreaker.setNestedConnection(nest);
	}

	private boolean isNewLineRequested() {
		return (this.tempLine.indexOf("//") == -1)
				&& (this.tempLine.indexOf("/*") == -1)
				&& (this.tempLine.indexOf("*/") == -1);
	}

	private boolean hasMoreSummerizedLines() {
		return (this.tempLine.length() != 0)
				&& ((this.tempLine.length() != 2)
						|| (this.tempLine.charAt(0) != '\r') || (this.tempLine
						.charAt(1) != '\n'));
	}

	public String format(String line) {
		boolean isLineComment = false;
		char ch = ' ';
		char prevCh = ' ';
		String outString = null;

		boolean shouldPublish = false;
		boolean isBreakCalled = false;
		this.currentHeader = null;

		if (line == null) {
			line = "";
		} else {
			if (!this.isInComment) {
				this.leadingWhiteSpaces = 0;
				while ((this.leadingWhiteSpaces < line.length())
						&& ((line.charAt(this.leadingWhiteSpaces) == ' ') || (line
								.charAt(this.leadingWhiteSpaces) == '\t')))
					this.leadingWhiteSpaces += 1;
				line = line.trim();
			} else {
				int trimSize;
				for (trimSize = 0; (trimSize < line.length())
						&& (trimSize < this.leadingWhiteSpaces)
						&& ((line.charAt(trimSize) == ' ') || (line
								.charAt(trimSize) == '\t')); trimSize++)
					;
				line = line.substring(trimSize);
			}

			if ("".equals(line))
				line = "\n";
		}
		line = this.tempLine + " \r" + line;
		int i;
		for (i = 0; i < line.length(); i++) {
			prevCh = ch;
			ch = line.charAt(i);

			if ((!this.isInComment) && (!isLineComment) && (ch == '\t')) {
				ch = ' ';
			}
			if (ch == '\n') {
				isBreakCalled = true;
				break;
			}

			if (ch == '\r') {
				ch = ' ';
				if (isBreakCalled)
					break;
				if (this.checkBlockClose) {
					this.checkBlockClose = false;
					isBreakCalled = true;
					break;
				}
				this.foundOrigLineBreak = true;
			} else {
				if ((ch != ' ') && (ch != '\t') && (!this.isInComment)
						&& (!isLineComment) && (!this.isInQuote)
						&& (!line.regionMatches(false, i, "//", 0, 2))
						&& (!line.regionMatches(false, i, "/*", 0, 2))) {
					this.prevNonSpaceCh = this.currentNonSpaceCh;
					this.currentNonSpaceCh = ch;
				}

				if ((!this.isInComment) && (!isLineComment)
						&& (!this.isInQuote) && (ch == ' ')) {
					if ((this.currentNonSpaceCh != '(')
							&& (this.currentNonSpaceCh != ')')
							&& (this.currentNonSpaceCh != '[')
							&& (this.currentNonSpaceCh != ']')) {
						appendSpace(this.outBuffer);
					}

				} else {
					shouldPublish = false;
					if (this.checkBlockClose) {
						this.checkBlockClose = false;
						if (ch != '}') {
							isBreakCalled = true;
						}
					}
					if ((!this.isInQuote) && (!this.isInComment)
							&& (!isLineComment)
							&& (line.regionMatches(false, i, "//", 0, 2))) {
						if (this.foundOrigLineBreak) {
							this.foundOrigLineBreak = false;
							if (this.checkClosingHeader) {
								this.checkClosingHeader = false;
								i--;
								isBreakCalled = true;
								break;
							}
						}
						isLineComment = true;
						this.checkClosingHeader = false;
						this.outBuffer.append("//");
						i++;
					} else if ((!this.isInQuote) && (!this.isInComment)
							&& (!isLineComment)
							&& (line.regionMatches(false, i, "/*", 0, 2))) {
						if (this.foundOrigLineBreak) {
							this.foundOrigLineBreak = false;
							if (this.checkClosingHeader) {
								this.checkClosingHeader = false;
								i--;
								isBreakCalled = true;
								break;
							}
						}
						this.isInComment = true;
						this.outBuffer.append("/*");
						i++;
					} else if ((!this.isInQuote)
							&& ((this.isInComment) || (isLineComment))
							&& (line.regionMatches(false, i, "*/", 0, 2))) {
						this.isInComment = false;
						this.outBuffer.append("*/");
						shouldPublish = true;
						i++;
					} else if ((this.isInComment) || (isLineComment)) {
						this.outBuffer.append(ch);
					} else {
						if (this.isInHeader) {
							this.isInHeader = false;
							this.currentHeader = ((String) this.openingStack
									.peek());
						} else {
							this.currentHeader = null;
						}
						this.foundOrigLineBreak = false;
						if (isBreakCalled) {
							i--;
							break;
						}

						if (this.checkClosingHeader) {
							this.checkClosingHeader = false;
							if (this.bracketBreak) {
								if (ch != ';') {
									i--;
									isBreakCalled = true;
									break;
								}
								i--;
								continue;
							}
							while (!this.openingStack.isEmpty() && !"{".equals(this.openingStack.pop()));
							if (!this.openingStack.isEmpty()) {
								String openingHeader = (String) this.openingStack
										.peek();
								String closingHeader = (String) closingHeaders
										.get(openingHeader);
								i--;
								if ((closingHeader == null)
										|| (!line.regionMatches(false, i + 1,
												closingHeader, 0,
												closingHeader.length()))) {
									if (ch != ';') {
										outString = this.outBuffer.toString();
										this.outBuffer.setLength(0);
										break;
									}
									i++;
								} else {
									int lastBufCharPlace = this.outBuffer
											.length() - 1;
									if ((lastBufCharPlace >= 0)
											&& (this.outBuffer
													.charAt(lastBufCharPlace) != ' '))
										appendSpace(this.outBuffer);
									ch = ' ';
									this.openingStack.pop();
									continue;
								}
							}
						}

						if (this.checkIf) {
							this.checkIf = false;
							if (line.regionMatches(false, i, "if", 0, 2))
								this.isNewLineNeeded = false;
						}
						if ((!this.isParenNeeded) && (this.checkBlockOpen)) {
							this.checkBlockOpen = false;
							if ((ch == '{')
									|| ("static".equals(this.currentHeader)))
								this.isNewLineNeeded = false;
						}
						if ((this.isNewLineNeeded) && (!this.isParenNeeded)) {
							this.isNewLineNeeded = false;

							i--;
							isBreakCalled = true;
						} else if (this.isSpecialChar) {
							this.outBuffer.append(ch);
							this.isSpecialChar = false;
						} else if ((!this.isInComment) && (!isLineComment)
								&& (line.regionMatches(false, i, "\\\\", 0, 2))) {
							this.outBuffer.append("\\\\");
							i++;
						} else if ((!this.isInComment) && (!isLineComment)
								&& (ch == '\\')) {
							this.isSpecialChar = true;
							this.outBuffer.append(ch);
						} else {
							if ((ch == '"') || (ch == '\''))
								if (!this.isInQuote) {
									this.quoteChar = ch;
									this.isInQuote = true;
								} else if (this.quoteChar == ch) {
									this.isInQuote = false;
									this.outBuffer.append(ch);
									continue;
								}
							if (this.isInQuote) {
								this.outBuffer.append(ch);
							} else {
								if ((ch == '(') || (ch == '[') || (ch == ')')
										|| (ch == ']')) {
									if ((ch == '(') || (ch == '['))
										this.parenDepth += 1;
									else if ((ch == ')') || (ch == ']'))
										this.parenDepth -= 1;
									if ((this.parenDepth == 0)
											&& (this.isParenNeeded)) {
										this.isParenNeeded = false;
										this.checkBlockOpen = true;
									}

								}

								if (prevCh == ' ') {
									boolean foundHeader = false;
									for (int h = 0; h < headers.length; h++) {
										if (line.regionMatches(false, i,
												headers[h], 0,
												headers[h].length())) {
											int lineLength = line.length();
											int headerEnd = i
													+ headers[h].length();
											char endCh = '\000';
											if (headerEnd < lineLength)
												endCh = line.charAt(headerEnd);
											if ((headerEnd > lineLength)
													|| ((endCh >= 'a') && (endCh <= 'z'))
													|| ((endCh >= 'A') && (endCh <= 'Z'))
													|| ((endCh >= '0') && (endCh <= '9')))
												break;
											foundHeader = true;
											this.outBuffer.append(headers[h]);
											i += headers[h].length() - 1;
											if ("else".equals(headers[h]))
												this.checkIf = true;
											this.checkBlockOpen = true;
											this.isNewLineNeeded = true;
											this.isBlockNeeded = false;
											this.openingStack.push(headers[h]);
											appendSpace(this.outBuffer);

											ch = ' ';

											for (int p = 0; p < parenHeaders.length; p++) {
												if (headers[h]
														.equals(parenHeaders[p])) {
													this.isParenNeeded = true;
													break;
												}
											}
										}
									}
									if (foundHeader) {
										this.isInHeader = true;
										continue;
									}
								}
								if (ch == '?')
									this.isInQuestion = true;
								if (ch == ':') {
									if (this.isInQuestion) {
										this.isInQuestion = false;
									} else {
										this.outBuffer.append(ch);
										isBreakCalled = true;
										continue;
									}
								}
								if ((ch == ';') && (this.parenDepth == 0)) {
									this.outBuffer.append(ch);
									isBreakCalled = true;
								} else if (ch == '{') {
									if ((!this.bracketBreak)
											|| (!this.isInBracketOpen)) {
										boolean isBlockOpener = false;

										isBlockOpener |= ((this.prevNonSpaceCh == '{') && (((Boolean) this.bracketBlockStateStack
												.peek()).booleanValue()));
										isBlockOpener |= ((this.prevNonSpaceCh == ')') || (this.prevNonSpaceCh == ';'));
										isBlockOpener |= this.isInClassStatement;
										isBlockOpener |= ((this.prevNonSpaceCh == ':') && (!this.isInQuestion));
										this.isInClassStatement = false;
										if ((!isBlockOpener)
												&& (this.currentHeader != null)) {
											for (int n = 0; n < nonParenHeaders.length; n++)
												if (this.currentHeader
														.equals(nonParenHeaders[n])) {
													isBlockOpener = true;
													break;
												}
										}
										this.bracketBlockStateStack
												.push(new Boolean(isBlockOpener));
										if (!isBlockOpener) {
											this.outBuffer.append('{');
											continue;
										}
									}

									if (this.bracketBreak) {
										if (this.isInBracketOpen) {
											this.isInBracketOpen = false;
										} else {
											this.isInBracketOpen = true;
											isBreakCalled = true;
											i--;
											break;
										}
									}
									this.checkBlockClose = true;
									int lastBufCharPlace = this.outBuffer
											.length() - 1;
									if ((lastBufCharPlace >= 0)
											&& (this.outBuffer
													.charAt(lastBufCharPlace) != ' '))
										appendSpace(this.outBuffer);
									this.outBuffer.append('{');
									this.openingStack.push("{");

									this.parenDepthsStack.push(new Integer(
											this.parenDepth));
									this.parenDepth = 0;
								} else if (ch == '}') {
									if (!this.bracketBlockStateStack.isEmpty() && !((Boolean) this.bracketBlockStateStack
											.pop()).booleanValue()) {
										this.outBuffer.append(ch);
									} else {
										if (!this.parenDepthsStack.isEmpty())
											this.parenDepth = ((Integer) this.parenDepthsStack
													.pop()).intValue();
										this.outBuffer.append(ch);
										this.checkClosingHeader = true;
									}
								} else {
									if (prevCh == ' ') {
										for (int h = 0; h < statementHeaders.length; h++) {
											if (line.regionMatches(false, i,
													statementHeaders[h], 0,
													statementHeaders[h]
															.length())) {
												int lineLength = line.length();
												int headerEnd = i
														+ statementHeaders[h]
																.length();
												char endCh = '\000';
												if (headerEnd < lineLength)
													endCh = line
															.charAt(headerEnd);
												if ((headerEnd > lineLength)
														|| ((endCh >= 'a') && (endCh <= 'z'))
														|| ((endCh >= 'A') && (endCh <= 'Z'))
														|| ((endCh >= '0') && (endCh <= '9')))
													break;
												this.isInClassStatement = true;
												break;
											}
										}
									}
									if ((prevCh == ' ')
											&& (line.regionMatches(false, i,
													"return", 0, 6))) {
										int lineLength = line.length();
										int headerEnd = i + 6;
										char endCh = '\000';
										if (headerEnd < lineLength)
											endCh = line.charAt(headerEnd);
										if ((headerEnd <= lineLength)
												&& ((endCh < 'a') || (endCh > 'z'))
												&& ((endCh < 'A') || (endCh > 'Z'))
												&& ((endCh < '0') || (endCh > '9'))) {
											this.outBuffer.append("return");
											i += 5;
											this.currentNonSpaceCh = '-';
											continue;
										}
									}

									if (((this.prevNonSpaceCh == ')') || (this.prevNonSpaceCh == ']'))
											&& (Character.isLetterOrDigit(ch))
											&& (ch != '.')
											&& (ch != '_')
											&& (ch != '$')
											&& (ch != '(')
											&& (ch != '[')
											&& (ch != ')')
											&& (ch != ']'))
										appendSpace(this.outBuffer);
									if ((!Character.isLetterOrDigit(ch))
											&& (ch != '.')
											&& (ch != '_')
											&& (ch != '$')
											&& (ch != '(')
											&& (ch != '[')
											&& (ch != ')')
											&& (ch != ']')
											&& ((Character
													.isLetterOrDigit(this.prevNonSpaceCh))
													|| (this.prevNonSpaceCh == '.')
													|| (this.prevNonSpaceCh == '_')
													|| (this.prevNonSpaceCh == '$')
													|| (this.prevNonSpaceCh == ')') || (this.prevNonSpaceCh == ']'))) {
										boolean isLongOperator = false;
										String longOperator = null;
										for (int l = 0; l < longOperators.length; l++) {
											if (line.regionMatches(false, i,
													longOperators[l], 0,
													longOperators[l].length())) {
												isLongOperator = true;
												longOperator = longOperators[l];
												break;
											}
										}
										if (isLongOperator) {
											if ((!"--".equals(longOperator))
													&& (!"++"
															.equals(longOperator))
													&& (!".*"
															.equals(longOperator))) {
												appendSpace(this.outBuffer);
												this.outBuffer
														.append(longOperator);
												appendSpace(this.outBuffer);
												ch = ' ';
											} else {
												this.outBuffer
														.append(longOperator);
												this.currentNonSpaceCh = '0';
											}
											i++;
										} else if ((ch != '*')
												|| (this.prevNonSpaceCh != '.')) {
											if ((ch != ',') && (ch != ';'))
												appendSpace(this.outBuffer);
											this.outBuffer.append(ch);
											appendSpace(this.outBuffer);
											ch = ' ';
										} else {
											this.outBuffer.append(ch);
										}
									} else {
										if ((ch == ')') || (ch == ']')) {
											clearPaddingSpace(this.outBuffer);
										}
										this.outBuffer.append(ch);
									}
								}
							}
						}
					}
				}
			}
		}
		try {
			this.tempLine = line.substring(i + (i < line.length() ? 1 : 0));
		} catch (Exception e) {
			this.tempLine = "";
		}
		if ((isBreakCalled) || (this.isInComment) || (isLineComment)
				|| (shouldPublish)) {
			outString = this.outBuffer.toString();
			this.outBuffer.setLength(0);
		}
		if ((outString != null) && (!"".equals(outString)))
			outString = this.beautifier.beautify(outString);
		else if ((ch != '[') && (ch != ']') && (ch != '(') && (ch != ')')
				&& (ch != '.') && (ch != '_') && (ch != '$'))
			appendSpace(this.outBuffer);
		return outString;
	}

	private void appendSpace(StringBuffer buf) {
		if ((buf.length() == 0) || (buf.charAt(buf.length() - 1) != ' '))
			buf.append(' ');
	}

	private void clearPaddingSpace(StringBuffer buf) {
		int bufLength = buf.length();
		if ((bufLength != 0) && (buf.charAt(bufLength - 1) == ' '))
			buf.setLength(bufLength - 1);
	}

	static {
		closingHeaders.put("if", "else");
		closingHeaders.put("do", "while");
		closingHeaders.put("try", "catch");
		closingHeaders.put("catch", "finally");
	}
}