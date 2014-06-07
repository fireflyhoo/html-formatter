package net.kiigo.web.apps.utils.jspformatter;

import java.util.EmptyStackException;
import java.util.Stack;

public class TagStack
{
  private Stack<Object> stack;

  public TagStack()
  {
    this.stack = new Stack<Object>();
  }

  private String getString(Object o) {
    return (String)o;
  }

  public boolean empty() {
    return this.stack.empty();
  }

  public String peek() throws EmptyStackException {
    return getString(this.stack.peek());
  }

  public String pop() throws EmptyStackException {
    return getString(this.stack.pop());
  }

  public void push(String newStr) {
    this.stack.push(newStr);
  }

  public int search(String searchStr) {
    return this.stack.search(searchStr);
  }
}