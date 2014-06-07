package net.kiigo.web.apps.utils.jspformatter;

import java.util.EmptyStackException;
import java.util.Stack;

/***
 * int 型的堆栈
 * @author Coollf
 *
 */
public class IntStack {
	private Stack<Integer> stack;

	public IntStack() {
		this.stack = new Stack<Integer>();
	}

	private int getInt(Object o) {
		return ((Integer) o).intValue();
	}

	public boolean empty() {
		return this.stack.empty();
	}

	public int peek() {
		return getInt(this.stack.peek());
	}

	public int pop() throws EmptyStackException {
		return getInt(this.stack.pop());
	}

	public void push(int newInt) {
		this.stack.push(new Integer(newInt));
	}

	public int search(int searchInt) {
		return this.stack.search(new Integer(searchInt));
	}
}