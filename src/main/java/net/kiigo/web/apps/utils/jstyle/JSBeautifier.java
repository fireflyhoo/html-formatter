package net.kiigo.web.apps.utils.jstyle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Stack;

/**
 * 美化js文本工具
 * @author Coollf
 *
 */
public class JSBeautifier {
	
	private static String[] headers = { "if", "else", "for", "while", "do",
			"try", "catch", "finally", "synchronized", "switch", "case",
			"default", "static" };

	private static String[] nonParenHeaders = { "else", "do", "try", "static",
			"finally" };

	private static String[] preBlockStatements = { "class", "interface",
			"throws" };

	private static String[] assignmentOperators = { "=", "+=", "-=", "*=",
			"/=", "%=", "|=", "&=", "return" };

	private static String[] nonAssignmentOperators = { "==", "++", "--", "!=" };
	private Stack headerStack;
	private Stack tempStacks;
	private Stack blockParenDepthStack;
	private Stack blockStatementStack;
	private Stack parenStatementStack;
	private Stack inStatementIndentStack;
	private Stack inStatementIndentStackSizeStack;
	private Stack parenIndentStack;
	private Stack bracketBlockStateStack;
	private boolean isSpecialChar;
	private boolean isInQuote;
	private boolean isInComment;
	private boolean isInCase;
	private boolean isInQuestion;
	private boolean isInStatement;
	private boolean isInClassHeader;
	private boolean isInClassHeaderTab;
	private boolean switchIndent;
	private boolean bracketIndent;
	private char quoteChar;
	private int commentIndent = 1;
	private int parenDepth;
	private String indentString;
	private int indentLength;
	private int blockTabCount;
	private int statementTabCount;
	private int leadingWhiteSpaces;
	private int maxInStatementIndent;
	private char prevNonSpaceCh;
	private char currentNonSpaceCh;
	private String currentHeader;
	private boolean isInHeader;
	private String immediatelyPreviousAssignmentOp;

	public void beautifyReader(BufferedReader inReader, PrintWriter outWriter)
			throws IOException {
		String line = null;
		try {
			while (true) {
				line = inReader.readLine();
				if (line == null)
					break;
				outWriter.println(beautify(line));
			}
		} catch (IOException e) {
		}
	}

	public JSBeautifier() {
		init();
		setSpaceIndentation(4);
		setMaxInStatementIndetation(40);
		setBracketIndent(false);
		setSwitchIndent(true);
	}

	public void init() {
		this.headerStack = new Stack();
		this.tempStacks = new Stack();
		this.tempStacks.push(new Stack());
		this.blockParenDepthStack = new Stack();
		this.blockStatementStack = new Stack();
		this.parenStatementStack = new Stack();
		this.bracketBlockStateStack = new Stack();
		this.bracketBlockStateStack.push(new Boolean(true));
		this.inStatementIndentStack = new Stack();
		this.inStatementIndentStackSizeStack = new Stack();
		this.inStatementIndentStackSizeStack.push(new Integer(0));
		this.parenIndentStack = new Stack();
		this.isSpecialChar = false;
		this.isInQuote = false;
		this.isInComment = false;
		this.isInStatement = false;
		this.isInCase = false;
		this.isInQuestion = false;
		this.isInClassHeader = false;
		this.isInClassHeaderTab = false;
		this.isInHeader = false;
		this.immediatelyPreviousAssignmentOp = null;
		this.parenDepth = 0;
		this.blockTabCount = 0;
		this.statementTabCount = -1;
		this.leadingWhiteSpaces = 0;
		this.prevNonSpaceCh = '{';
		this.currentNonSpaceCh = '{';
	}

	public void setTabIndentation() {
		this.indentString = "\t";
		this.indentLength = 4;
	}

	public void setSpaceIndentation(int length) {
		char[] spaces = new char[length];
		for (int i = 0; i < length; i++)
			spaces[i] = ' ';
		this.indentString = new String(spaces);
		this.indentLength = length;
	}

	public void setMaxInStatementIndetation(int max) {
		this.maxInStatementIndent = max;
	}

	public void setBracketIndent(boolean state) {
		this.bracketIndent = state;
	}

	public void setSwitchIndent(boolean state) {
		this.switchIndent = state;
	}

	public String beautify(String line) {
		boolean isInLineComment = false;
		boolean isInSwitch = false;
		char ch = ' ';

		StringBuffer outBuffer = new StringBuffer();
		int tabCount = 0;
		String lastLineHeader = null;
		boolean closingBracketReached = false;
		int spaceTabCount = 0;
		int headerStackSize = this.headerStack.size();
		boolean isLineInStatement = this.isInStatement;
		boolean shouldIndentBrackettedLine = true;
		this.currentHeader = null;

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
		if (line.length() == 0)
			return line;
		if (!this.inStatementIndentStack.isEmpty()) {
			spaceTabCount = ((Integer) this.inStatementIndentStack.peek())
					.intValue();
		}
		for (int i = 0; i < headerStackSize; i++) {
			if ((i <= 0) || ("{".equals(this.headerStack.elementAt(i - 1)))
					|| (!"{".equals(this.headerStack.elementAt(i)))) {
				tabCount++;
			}
			if ((this.switchIndent) && (i > 1)
					&& ("switch".equals(this.headerStack.elementAt(i - 1)))
					&& ("{".equals(this.headerStack.elementAt(i)))) {
				tabCount++;
				isInSwitch = true;
			}
		}
		if ((isInSwitch)
				&& (this.switchIndent)
				&& (headerStackSize >= 2)
				&& ("switch".equals(this.headerStack
						.elementAt(headerStackSize - 2)))
				&& ("{".equals(this.headerStack.elementAt(headerStackSize - 1)))
				&& (line.charAt(0) == '}'))
			tabCount--;
		if (this.isInClassHeader) {
			this.isInClassHeaderTab = true;
			tabCount += 2;
		}

		for (int i = 0; i < line.length(); i++) {
			char prevCh = ch;
			ch = line.charAt(i);
			if ((ch != '\n') && (ch != '\r')) {
				outBuffer.append(ch);
				if ((ch != ' ') && (ch != '\t')) {
					if (this.isSpecialChar) {
						this.isSpecialChar = false;
					} else if ((!this.isInComment) && (!isInLineComment)
							&& (line.regionMatches(false, i, "\\\\", 0, 2))) {
						outBuffer.append('\\');
						i++;
					} else if ((!this.isInComment) && (!isInLineComment)
							&& (ch == '\\')) {
						this.isSpecialChar = true;
					} else {
						if ((!this.isInComment) && (!isInLineComment)
								&& ((ch == '"') || (ch == '\'')))
							if (!this.isInQuote) {
								this.quoteChar = ch;
								this.isInQuote = true;
							} else if (this.quoteChar == ch) {
								this.isInQuote = false;
								this.isInStatement = true;
								continue;
							}
						if (!this.isInQuote) {
							if ((!this.isInComment)
									&& (!isInLineComment)
									&& (line.regionMatches(false, i, "//", 0, 2))) {
								isInLineComment = true;
								outBuffer.append("/");
								i++;
							} else if ((!this.isInComment)
									&& (!isInLineComment)
									&& (line.regionMatches(false, i, "/*", 0, 2))) {
								this.isInComment = true;
								outBuffer.append("*");
								i++;
							} else if (((this.isInComment) || (isInLineComment))
									&& (line.regionMatches(false, i, "*/", 0, 2))) {
								this.isInComment = false;
								outBuffer.append("/");
								i++;
							} else if ((!this.isInComment)
									&& (!isInLineComment)) {
								this.prevNonSpaceCh = this.currentNonSpaceCh;
								this.currentNonSpaceCh = ch;
								if (this.isInHeader) {
									this.isInHeader = false;
									this.currentHeader = ((String) this.headerStack
											.peek());
								} else {
									this.currentHeader = null;
								}
								if ((ch == '(') || (ch == '[') || (ch == ')')
										|| (ch == ']')) {
									if ((ch == '(') || (ch == '[')) {
										if (this.parenDepth == 0) {
											this.parenStatementStack
													.push(new Boolean(
															this.isInStatement));
											this.isInStatement = true;
										}
										this.parenDepth += 1;
										this.inStatementIndentStackSizeStack
												.push(new Integer(
														this.inStatementIndentStack
																.size()));
										if (this.currentHeader != null) {
											this.inStatementIndentStack
													.push(new Integer(
															this.indentLength
																	* 2
																	+ spaceTabCount));
											this.parenIndentStack
													.push(new Integer(
															this.indentLength
																	* 2
																	+ spaceTabCount));
										} else {
											registerInStatementIndent(line, i,
													spaceTabCount,
													isLineInStatement, true);
										}
									} else if ((ch == ')') || (ch == ']')) {
										this.parenDepth -= 1;
										if (this.parenDepth == 0) {
											this.isInStatement = ((Boolean) this.parenStatementStack
													.pop()).booleanValue();
											ch = ' ';
										}
										if (!this.inStatementIndentStackSizeStack
												.isEmpty()) {
											int previousIndentStackSize = ((Integer) this.inStatementIndentStackSizeStack
													.pop()).intValue();
											while (previousIndentStackSize < this.inStatementIndentStack
													.size())
												this.inStatementIndentStack
														.pop();
											if (!this.parenIndentStack
													.isEmpty()) {
												Object poppedIndent = this.parenIndentStack
														.pop();
												if (i == 0) {
													spaceTabCount = ((Integer) poppedIndent)
															.intValue();
												}
											}
										}
									}
								} else if (ch == '{') {
									boolean isBlockOpener = false;

									isBlockOpener |= ((this.prevNonSpaceCh == '{') && (((Boolean) this.bracketBlockStateStack
											.peek()).booleanValue()));
									isBlockOpener |= ((this.prevNonSpaceCh == ')') || (this.prevNonSpaceCh == ';'));
									isBlockOpener |= this.isInClassHeader;
									this.isInClassHeader = false;
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
										if ((line.length() - i == getNextProgramCharDistance(
												line, i))
												&& (this.immediatelyPreviousAssignmentOp != null))
											this.inStatementIndentStack.pop();
										this.inStatementIndentStackSizeStack
												.push(new Integer(
														this.inStatementIndentStack
																.size()));
										registerInStatementIndent(line, i,
												spaceTabCount,
												isLineInStatement, true);

										this.parenDepth += 1;
										if (i == 0)
											shouldIndentBrackettedLine = false;
									} else {
										if (this.isInClassHeader)
											this.isInClassHeader = false;
										if (this.isInClassHeaderTab) {
											this.isInClassHeaderTab = false;
											tabCount -= 2;
										}
										this.blockParenDepthStack
												.push(new Integer(
														this.parenDepth));
										this.blockStatementStack
												.push(new Boolean(
														this.isInStatement));
										this.inStatementIndentStackSizeStack
												.push(new Integer(
														this.inStatementIndentStack
																.size()));
										this.blockTabCount += (this.isInStatement ? 1
												: 0);
										this.parenDepth = 0;
										this.isInStatement = false;
										this.tempStacks.push(new Stack());
										this.headerStack.push("{");
										lastLineHeader = "{";
									}
								} else {
									if (prevCh == ' ') {
										boolean isDoubleHeader = false;
										int h = findLegalHeader(line, i,
												headers);
										if (h > -1) {
											this.isInHeader = true;
											Stack lastTempStack = (Stack) this.tempStacks
													.peek();

											if (("if".equals(headers[h]))
													&& ("else"
															.equals(lastLineHeader))) {
												this.headerStack.pop();
											} else if ("else"
													.equals(headers[h])) {
												if (lastTempStack != null) {
													int indexOfIf = lastTempStack
															.indexOf("if");
													if (indexOfIf != -1) {
														int restackSize = lastTempStack
																.size()
																- indexOfIf - 1;
														for (int r = 0; r < restackSize; r++)
															this.headerStack
																	.push(lastTempStack
																			.pop());
														if (!closingBracketReached) {
															tabCount += restackSize;
														}

													}

												}

											} else if ("while"
													.equals(headers[h])) {
												if (lastTempStack != null) {
													int indexOfDo = lastTempStack
															.indexOf("do");
													if (indexOfDo != -1) {
														int restackSize = lastTempStack
																.size()
																- indexOfDo - 1;
														for (int r = 0; r < restackSize; r++)
															this.headerStack
																	.push(lastTempStack
																			.pop());
														if (!closingBracketReached) {
															tabCount += restackSize;
														}
													}
												}
											} else if ("catch"
													.equals(headers[h])) {
												if (lastTempStack != null) {
													int indexOfTry = lastTempStack
															.indexOf("try");
													if (indexOfTry == -1)
														indexOfTry = lastTempStack
																.indexOf("catch");
													if (indexOfTry != -1) {
														int restackSize = lastTempStack
																.size()
																- indexOfTry
																- 1;
														for (int r = 0; r < restackSize; r++) {
															this.headerStack
																	.push(lastTempStack
																			.pop());
														}

														if (!closingBracketReached) {
															tabCount += restackSize;
														}
													}
												}
											} else if ("finally"
													.equals(headers[h])) {
												if (lastTempStack != null) {
													int indexOfTry = lastTempStack
															.indexOf("try");
													if (indexOfTry == -1)
														indexOfTry = lastTempStack
																.indexOf("catch");
													if (indexOfTry == -1)
														indexOfTry = lastTempStack
																.indexOf("finally");
													if (indexOfTry != -1) {
														int restackSize = lastTempStack
																.size()
																- indexOfTry
																- 1;
														for (int r = 0; r < restackSize; r++) {
															this.headerStack
																	.push(lastTempStack
																			.pop());
														}

														if (!closingBracketReached)
															tabCount += restackSize;
													}
												}
											} else if (("case"
													.equals(headers[h]))
													|| ("default"
															.equals(headers[h]))) {
												this.isInCase = true;
												tabCount--;
											} else if ((("static"
													.equals(headers[h])) || ("synchronized"
													.equals(headers[h])))
													&& (!this.headerStack
															.isEmpty())
													&& (("static"
															.equals(this.headerStack
																	.lastElement())) || ("synchronized"
															.equals(this.headerStack
																	.lastElement())))) {
												isDoubleHeader = true;
											}
											if (!isDoubleHeader) {
												spaceTabCount -= this.indentLength;
												this.headerStack
														.push(headers[h]);
											}
											lastLineHeader = headers[h];
											outBuffer.append(headers[h]
													.substring(1));
											i += headers[h].length() - 1;

											this.isInStatement = false;
										}
									}
									if (ch == '?') {
										this.isInQuestion = true;
									}
									if (ch == ':') {
										if (this.isInQuestion) {
											this.isInQuestion = false;
										} else {
											this.currentNonSpaceCh = ';';
											if (this.isInCase) {
												this.isInCase = false;
												ch = ';';
											}
										}
									}
									if (((ch == ';') || (ch == ','))
											&& (!this.inStatementIndentStackSizeStack
													.isEmpty())) {
										while (((Integer) this.inStatementIndentStackSizeStack
												.peek()).intValue()
												+ (this.parenDepth > 0 ? 1 : 0) < this.inStatementIndentStack
													.size())
											this.inStatementIndentStack.pop();
									}
									if (((ch == ';') && (this.parenDepth == 0))
											|| (ch == '}')
											|| ((ch == ',') && (this.parenDepth == 0))) {
										if (ch == '}') {
											if ((!this.bracketBlockStateStack
													.isEmpty())
													&& (!((Boolean) this.bracketBlockStateStack
															.pop())
															.booleanValue())) {
												if (!this.inStatementIndentStackSizeStack
														.isEmpty()) {
													int previousIndentStackSize = ((Integer) this.inStatementIndentStackSizeStack
															.pop()).intValue();
													while (previousIndentStackSize < this.inStatementIndentStack
															.size())
														this.inStatementIndentStack
																.pop();
													this.parenDepth -= 1;
													if (i == 0)
														shouldIndentBrackettedLine = false;
													if (!this.parenIndentStack
															.isEmpty()) {
														Object poppedIndent = this.parenIndentStack
																.pop();
														if (i == 0)
															spaceTabCount = ((Integer) poppedIndent)
																	.intValue();
													}
												}
											} else {
												if (!this.inStatementIndentStackSizeStack
														.isEmpty())
													this.inStatementIndentStackSizeStack
															.pop();
												if (!this.blockParenDepthStack
														.isEmpty()) {
													this.parenDepth = ((Integer) this.blockParenDepthStack
															.pop()).intValue();
													this.isInStatement = ((Boolean) this.blockStatementStack
															.pop())
															.booleanValue();
													if (this.isInStatement)
														this.blockTabCount -= 1;
												}
												closingBracketReached = true;
												int headerPlace = this.headerStack
														.search("{");
												if (headerPlace != -1) {
													while (!"{"
															.equals(this.headerStack
																	.pop()))
														;
													if (!this.tempStacks
															.isEmpty())
														this.tempStacks.pop();
												}
												ch = ' ';
											}

										} else {
											if (!((Stack) this.tempStacks
													.peek()).isEmpty())
												((Stack) this.tempStacks.peek())
														.removeAllElements();
											while ((!this.headerStack.isEmpty())
													&& (!"{".equals(this.headerStack
															.peek())))
												((Stack) this.tempStacks.peek())
														.push(this.headerStack
																.pop());
											if ((this.parenDepth == 0)
													&& (ch == ';'))
												this.isInStatement = false;
										}
									} else {
										if (prevCh == ' ') {
											int headerNum = findLegalHeader(
													line, i, preBlockStatements);
											if (headerNum > -1) {
												this.isInClassHeader = true;
												outBuffer
														.append(preBlockStatements[headerNum]
																.substring(1));
												i += preBlockStatements[headerNum]
														.length() - 1;
											}

										}

										this.immediatelyPreviousAssignmentOp = null;
										boolean isNonAssingmentOperator = false;
										for (int n = 0; n < nonAssignmentOperators.length; n++)
											if (line.regionMatches(false, i,
													nonAssignmentOperators[n],
													0,
													nonAssignmentOperators[n]
															.length())) {
												outBuffer
														.append(nonAssignmentOperators[n]
																.substring(1));
												i++;

												isNonAssingmentOperator = true;
												break;
											}
										if (!isNonAssingmentOperator) {
											for (int a = 0; a < assignmentOperators.length; a++)
												if (line.regionMatches(false,
														i,
														assignmentOperators[a],
														0,
														assignmentOperators[a]
																.length())) {
													if (assignmentOperators[a]
															.length() > 1) {
														outBuffer
																.append(assignmentOperators[a]
																		.substring(1));
														i += assignmentOperators[a]
																.length() - 1;
													}
													registerInStatementIndent(
															line, i,
															spaceTabCount,
															isLineInStatement,
															false);
													this.immediatelyPreviousAssignmentOp = assignmentOperators[a];
													break;
												}
										}
										if ((this.parenDepth > 0)
												|| ((!isLegalNameChar(ch)) && (ch != ':')))
											this.isInStatement = true;
									}
								}
							}
						}
					}
				}
			}
		}
		if ((outBuffer.length() > 0)
				&& (outBuffer.charAt(0) == '{')
				&& ((this.headerStack.size() <= 1) || (!"{"
						.equals(this.headerStack.elementAt(this.headerStack
								.size() - 2)))) && (shouldIndentBrackettedLine))
			tabCount--;
		else if ((outBuffer.length() > 0) && (outBuffer.charAt(0) == '}')
				&& (shouldIndentBrackettedLine))
			tabCount--;
		if (tabCount < 0) {
			tabCount = 0;
		}
		if ((this.bracketIndent)
				&& (outBuffer.length() > 0)
				&& (shouldIndentBrackettedLine)
				&& ((outBuffer.charAt(0) == '{') || (outBuffer.charAt(0) == '}'))) {
			tabCount++;
		}
		for (int i = 0; i < tabCount; i++)
			outBuffer.insert(0, this.indentString);
		while (spaceTabCount-- > 0)
			outBuffer.insert(0, ' ');
		if (!this.inStatementIndentStack.isEmpty()) {
			if (this.statementTabCount < 0)
				this.statementTabCount = tabCount;
		} else
			this.statementTabCount = -1;
		return outBuffer.toString();
	}

	private void registerInStatementIndent(String line, int i,
			int spaceTabCount, boolean isLineInStatement,
			boolean updateParenStack) {
		int remainingCharNum = line.length() - i;
		int nextNonWSChar = 1;

		nextNonWSChar = getNextProgramCharDistance(line, i);

		if (nextNonWSChar == remainingCharNum) {
			int previousIndent = spaceTabCount;
			if (!this.inStatementIndentStack.isEmpty())
				previousIndent = ((Integer) this.inStatementIndentStack.peek())
						.intValue();
			this.inStatementIndentStack.push(new Integer(2 + previousIndent));
			if (updateParenStack)
				this.parenIndentStack.push(new Integer(previousIndent));
			return;
		}
		if (updateParenStack)
			this.parenIndentStack.push(new Integer(i + spaceTabCount));
		int inStatementIndent = i + nextNonWSChar + spaceTabCount;
		if (i + nextNonWSChar > this.maxInStatementIndent)
			inStatementIndent = this.indentLength * 2 + spaceTabCount;
		if ((!this.inStatementIndentStack.isEmpty())
				&& (inStatementIndent < ((Integer) this.inStatementIndentStack
						.peek()).intValue())) {
			inStatementIndent = ((Integer) this.inStatementIndentStack.peek())
					.intValue();
		}

		this.inStatementIndentStack.push(new Integer(inStatementIndent));
	}

	private int getNextProgramCharDistance(String line, int i) {
		boolean inComment = false;
		int remainingCharNum = line.length() - i;
		int charDistance = 1;

		for (charDistance = 1; charDistance < remainingCharNum; charDistance++) {
			int ch = line.charAt(i + charDistance);
			if (inComment) {
				if (line.regionMatches(false, i + charDistance, "*/", 0, 2)) {
					charDistance++;
					inComment = false;
				}
			} else if ((ch != 32) && (ch != 9)) {
				if (ch == 47) {
					if (line.regionMatches(false, i + charDistance, "//", 0, 2))
						return remainingCharNum;
					if (line.regionMatches(false, i + charDistance, "/*", 0, 2)) {
						charDistance++;
						inComment = true;
					}
				} else {
					return charDistance;
				}
			}
		}
		return charDistance;
	}

	private boolean isLegalNameChar(char ch) {
		return ((ch >= 'a') && (ch <= 'z')) || ((ch >= 'A') && (ch <= 'Z'))
				|| ((ch >= '0') && (ch <= '9')) || (ch == '.') || (ch == '_')
				|| (ch == '$');
	}

	private int findLegalHeader(String line, int i, String[] possibleHeaders) {
		int maxHeaders = possibleHeaders.length;

		for (int p = 0; p < maxHeaders; p++)
			if (line.regionMatches(false, i, possibleHeaders[p], 0,
					possibleHeaders[p].length())) {
				int lineLength = line.length();
				int headerEnd = i + possibleHeaders[p].length();
				char endCh = '\000';
				if (headerEnd < lineLength)
					endCh = line.charAt(headerEnd);
				if ((headerEnd >= lineLength) || (!isLegalNameChar(endCh))) {
					return p;
				}
				return -1;
			}
		return -1;
	}
}