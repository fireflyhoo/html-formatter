package net.kiigo.web.apps.utils.jstyle;

import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

class JSLineBreaker
{
  private static final int BEFORE = 0;
  private static final int AFTER = 1;
  private String[] prefs = { "().", "()", ").", "+=", "-=", "*=", "/=", "%=", "^=", "||", "&&", "==", "!=", ">=", "<=", "(", ")", "[", "]", "?", ":", ",", ";", "=", "<", ">", "+", "-", "*", "/", "&", "|", "^" };

  private static Hashtable prefTable = new Hashtable();
  private Vector brokenLineVector;
  private StringBuffer wsBuffer;
  private char quoteChar;
  private boolean isInQuote;
  private boolean isInComment;
  private boolean isNestedConnection = true;
  private boolean isCut;
  private boolean isLineComment;
  private int parenDepth;
  private int breakDepth;
  private int preferredLineLength = 70;
  private int lineLengthDeviation = 5;

  private LineBreak previousLineBreak = null;

  JSLineBreaker()
  {
    init();
  }

  void init() {
    this.brokenLineVector = new Vector();
    this.parenDepth = 0;
    this.breakDepth = 0;
    this.isInQuote = false;
    this.isInComment = false;
    this.isCut = false;
    this.isLineComment = false;
    this.wsBuffer = new StringBuffer();
  }

  void setPreferredLineLength(int length) {
    this.preferredLineLength = length;
  }

  void setLineLengthDeviation(int dev) {
    this.lineLengthDeviation = dev;
  }

  void setNestedConnection(boolean nest) {
    this.isNestedConnection = nest;
  }

  void breakLine(String line) {
    StringBuffer outBuffer = new StringBuffer();
    Stack lineBreakStack = new Stack();
    String previousAfterCut = "";
    boolean isSpecialChar = false;
    char ch = ' ';
    char prevCh = '\000';

    int bufferStart = 0;
    if (line.trim().length() == 0) {
      this.brokenLineVector.addElement("");
      return;
    }
    ch = line.charAt(0);
    int ws = 0;
    if (!this.isLineComment)
      this.isCut = false;
    this.isLineComment = false;
    if (!this.isCut) {
      this.wsBuffer = new StringBuffer();
      while (((ch == ' ') || (ch == '\t')) && (ws < line.length() - 1)) {
        this.wsBuffer.append(ch);
        ch = line.charAt(++ws);
      }
    }

    for (int i = ws; i < line.length(); i++) {
      if ((ch != ' ') && (ch != '\t'))
        prevCh = ch;
      ch = line.charAt(i);

      if (isSpecialChar) {
        outBuffer.append(ch);
        isSpecialChar = false;
      }
      else if ((!this.isInComment) && (!this.isLineComment) && (line.regionMatches(false, i, "\\\\", 0, 2))) {
        outBuffer.append("\\\\");
        i++;
      }
      else if ((!this.isInComment) && (!this.isLineComment) && (ch == '\\')) {
        outBuffer.append(ch);
        isSpecialChar = true;
      }
      else if ((!this.isInQuote) && (!this.isInComment) && (!this.isLineComment) && (line.regionMatches(false, i, "//", 0, 2))) {
        this.isLineComment = true;
        outBuffer.append("//");
        i++;
      }
      else if ((!this.isInQuote) && (!this.isInComment) && (!this.isLineComment) && (line.regionMatches(false, i, "/*", 0, 2))) {
        this.isInComment = true;
        outBuffer.append("/*");
        i++;
      }
      else if ((!this.isInQuote) && ((this.isInComment) || (this.isLineComment)) && (line.regionMatches(false, i, "*/", 0, 2))) {
        this.isInComment = false;
        outBuffer.append("*/");
        i++;
      }
      else if ((this.isInComment) || (this.isLineComment)) {
        outBuffer.append(ch);
      }
      else
      {
        if ((ch == '"') || (ch == '\''))
          if (!this.isInQuote) {
            this.quoteChar = ch;
            this.isInQuote = true;
          } else if (this.quoteChar == ch) {
            this.isInQuote = false;
            outBuffer.append(ch);
            continue;
          }
        if (this.isInQuote) {
          outBuffer.append(ch);
        }
        else {
          outBuffer.append(ch);
          for (int p = 0; p < this.prefs.length; p++) {
            String key = this.prefs[p];
            if (line.regionMatches(false, i, key, 0, key.length())) {
              int breakType = 1;
              if ((ch == '(') || (ch == '[') || (ch == ')') || (ch == ']')) {
                if (("(".equals(key)) || (ch == '['))
                  this.parenDepth += 1;
                else if ((ch == ')') || (ch == ']'))
                  this.parenDepth -= 1;
                this.breakDepth = this.parenDepth;
                if ((ch == ')') || (ch == ']') || (key.startsWith("()")))
                  this.breakDepth += 1;
                if ((ch == '(') || (ch == '[')) {
                  if (((prevCh >= 'a') && (prevCh <= 'z')) || ((prevCh >= 'A') && (prevCh <= 'Z')) || ((prevCh >= '0') && (prevCh <= '9')) || (prevCh == '.'))
                    breakType = 1;
                  else
                    breakType = 0;
                }
                else breakType = 1;
              }
              if (key.length() > 1) {
                outBuffer.append(key.substring(1));
                i += key.length() - 1;
              }
              registerLineBreak(lineBreakStack, new LineBreak(key, outBuffer.length() + bufferStart, this.breakDepth, breakType));
              this.breakDepth = this.parenDepth;
              break;
            }
          }
          int bufLength = outBuffer.length() + this.wsBuffer.length() + previousAfterCut.length() + (this.isCut ? 8 : 0);
          LineBreak curBreak = null;
          if ((bufLength > this.preferredLineLength) && (i < line.length() - this.lineLengthDeviation)) {
            while (!lineBreakStack.isEmpty()) {
              curBreak = (LineBreak)lineBreakStack.elementAt(0);
              if (curBreak.breakWhere - bufferStart >= 1)
                break;
              curBreak = null;
              lineBreakStack.removeElementAt(0);
            }

            if (curBreak != null)
              lineBreakStack.removeElementAt(0);
          }
          if (curBreak != null)
          {
            int cutWhere = curBreak.breakWhere - bufferStart - (curBreak.breakType == 0 ? curBreak.breakStr.length() : 0);
            if (cutWhere >= 8)
            {
              StringBuffer brokenLineBuffer = new StringBuffer();
              String outString = outBuffer.toString();
              String beforeCut = outString.substring(0, cutWhere);

              brokenLineBuffer.append(beforeCut);
              addBrokenLine(this.wsBuffer.toString(), brokenLineBuffer.toString(), curBreak, this.breakDepth, this.isCut);

              bufferStart += cutWhere;
              outBuffer = new StringBuffer(outString.substring(cutWhere));

              this.isCut = true;
            }
          }
        }
      }
    }
    StringBuffer brokenLineBuffer = new StringBuffer();

    brokenLineBuffer.append(outBuffer);
    addBrokenLine(this.wsBuffer.toString(), brokenLineBuffer.toString(), null, this.breakDepth, this.isCut);
  }

  private void registerLineBreak(Stack lineBreakStack, LineBreak newBreak)
  {
    while (!lineBreakStack.isEmpty()) {
      LineBreak lastBreak = (LineBreak)lineBreakStack.peek();
      if (compare(lastBreak, newBreak) >= 0) break;
      lineBreakStack.pop();
    }

    lineBreakStack.push(newBreak);
  }

  private void addBrokenLine(String whiteSpace, String brokenLine, LineBreak lineBreak, int breakDepth, boolean isCut)
  {
    boolean isLineAppended = false;
    brokenLine = brokenLine.trim();
    if (this.previousLineBreak != null)
    {
      String previousBrokenLine = (String)this.brokenLineVector.lastElement();
      if (((brokenLine.length() + previousBrokenLine.length() <= this.preferredLineLength + this.lineLengthDeviation) || (brokenLine.startsWith("{"))) && (
        (lineBreak == null) || ((this.isNestedConnection) && (!",".equals(this.previousLineBreak.breakStr))) || (lineBreak.breakDepth < this.previousLineBreak.breakDepth) || ((lineBreak.breakDepth == this.previousLineBreak.breakDepth) && (((!this.isNestedConnection) && (!",".equals(this.previousLineBreak.breakStr))) || (",".equals(lineBreak.breakStr)) || (";".equals(lineBreak.breakStr)) || (")".equals(lineBreak.breakStr)) || ("]".equals(lineBreak.breakStr))))))
      {
        this.brokenLineVector.setElementAt(previousBrokenLine + " " + brokenLine, this.brokenLineVector.size() - 1);
        isLineAppended = true;
      }
    }

    if (!isLineAppended) {
      if ((isCut) && ((this.previousLineBreak == null) || (!",".equals(this.previousLineBreak.breakStr)) || (this.previousLineBreak.breakDepth != 0)))
        brokenLine = "        " + brokenLine;
      brokenLine = whiteSpace + brokenLine;
      this.brokenLineVector.addElement(brokenLine);
    }
    this.previousLineBreak = lineBreak;
  }

  private int compare(LineBreak br1, LineBreak br2) {
    if (br1.breakDepth < br2.breakDepth)
      return 1;
    if (br1.breakDepth > br2.breakDepth)
      return -1;
    int ord1 = ((Integer)prefTable.get(br1.breakStr)).intValue();
    int ord2 = ((Integer)prefTable.get(br2.breakStr)).intValue();
    if (ord1 < ord2) {
      return 1;
    }
    return -1;
  }

  boolean hasMoreBrokenLines()
  {
    return this.brokenLineVector.size() > 0;
  }

  String nextBrokenLine()
  {
    String nextLine;
    if (hasMoreBrokenLines()) {
      nextLine = (String)this.brokenLineVector.firstElement();
      this.brokenLineVector.removeElementAt(0);
    } else {
      return nextLine = "";
    }return nextLine;
  }

  static
  {
    prefTable.put("()", new Integer(80));
    prefTable.put("().", new Integer(90));
    prefTable.put(").", new Integer(90));
    prefTable.put("(", new Integer(80));
    prefTable.put(")", new Integer(80));
    prefTable.put("[", new Integer(80));
    prefTable.put("]", new Integer(80));
    prefTable.put(",", new Integer(10));
    prefTable.put(";", new Integer(5));
    prefTable.put("=", new Integer(20));
    prefTable.put("+=", new Integer(20));
    prefTable.put("-=", new Integer(20));
    prefTable.put("*=", new Integer(20));
    prefTable.put("/=", new Integer(20));
    prefTable.put("|=", new Integer(20));
    prefTable.put("&=", new Integer(20));
    prefTable.put("^=", new Integer(20));
    prefTable.put("?", new Integer(25));
    prefTable.put(":", new Integer(25));
    prefTable.put("||", new Integer(30));
    prefTable.put("&&", new Integer(30));
    prefTable.put("==", new Integer(40));
    prefTable.put("!=", new Integer(40));
    prefTable.put(">=", new Integer(40));
    prefTable.put("<=", new Integer(40));
    prefTable.put(">", new Integer(40));
    prefTable.put("<", new Integer(40));
    prefTable.put("+", new Integer(50));
    prefTable.put("-", new Integer(50));
    prefTable.put("*", new Integer(60));
    prefTable.put("/", new Integer(60));
    prefTable.put("%", new Integer(60));

    prefTable.put("&", new Integer(70));
    prefTable.put("|", new Integer(70));
    prefTable.put("^", new Integer(70));
  }

  class LineBreak
  {
    String breakStr;
    int breakWhere;
    int breakDepth;
    int breakType;

    LineBreak(String str, int wh, int dp, int tp)
    {
      this.breakStr = str;
      this.breakWhere = wh;
      this.breakDepth = dp;
      this.breakType = tp;
    }
  }
}