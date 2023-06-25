package io.wispforest.lavendermd.util;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * nom nom
 * <p>
 * Consumes a list element-by-element with
 * a client-mutable element pointer
 */
public class ListNibbler<T> {

    private final List<T> delegate;
    private int pointer = 0;

    public ListNibbler(List<T> delegate) {
        this.delegate = delegate;
    }

    /**
     * @return The next element of this nibbler's underlying
     * list, or {@code null} if the list is exhausted
     */
    public T nibble() {
        return this.pointer < this.delegate.size()
                ? this.delegate.get(this.pointer++)
                : null;
    }

    /**
     * Skip forward by {@code elements} in this nibbler's underlying list
     */
    public void skip(int elements) {
        this.pointer += elements;
        if (this.pointer > this.delegate.size()) throw new NoSuchElementException();
    }

    /**
     * Shorthand of {@link #peek(int)} with {@code 0} for {@code offset}
     */
    public T peek() {
        return this.peek(0);
    }

    /**
     * @return The element at this nibbler's element pointer + {@code offset},
     * or {@code null} if that index is outside the nibbler's underlying list
     */
    public T peek(int offset) {
        int index = this.pointer + offset;
        return index >= 0 && index < this.delegate.size()
                ? this.delegate.get(index)
                : null;
    }

    /**
     * @return {@code true} if this nibbler has more elements to consume
     */
    public boolean hasElements() {
        return this.pointer < this.delegate.size();
    }

    /**
     * @return The element pointer of this nibbler
     */
    public int pointer() {
        return this.pointer;
    }

    /**
     * Set the element pointer of this nibbler
     * to {@code pointer}, without any validation
     */
    public void setPointer(int pointer) {
        this.pointer = pointer;
    }

}
