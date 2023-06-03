package io.wispforest.lavendermd.util;

import net.minecraft.util.function.CharPredicate;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class StringNibbler {

    public final String string;
    private int cursor;

    public StringNibbler(String string) {
        this.string = string;
    }

    public int cursor() {
        return this.cursor;
    }

    public char peek() {
        return this.string.charAt(this.cursor);
    }

    public void skip() {
        this.skip(1);
    }

    public void skip(int count) {
        this.cursor += count;
    }

    public @Nullable Character next() {
        return this.cursor < this.string.length() ? this.string.charAt(this.cursor++) : null;
    }

    public String consumed() {
        return this.string.substring(0, this.cursor);
    }

    public boolean hasNext() {
        return this.cursor < this.string.length();
    }

    public @Nullable Character peekOffset(int offset) {
        int charIndex = this.cursor + offset;
        return charIndex >= 0 && charIndex < this.string.length() ? this.string.charAt(charIndex) : null;
    }

    public @Nullable String consumeEscapedString(char terminator, boolean allowUnterminated) {
        var read = new StringBuilder();
        while (this.hasNext()) {
            char next = this.next();
            if (next == '\\' && this.expect(0, terminator)) {
                this.skip();
                read.append(terminator);
            } else if (next == terminator) {
                return read.toString();
            } else {
                read.append(next);
            }
        }

        return allowUnterminated ? read.toString() : null;
    }

    public @Nullable String consumeUntil(char delimiter) {
        return this.consumeUntil(delimiter, true);
    }

    public @Nullable String consumeUntil(char delimiter, boolean skipDelimiter) {
        var read = new StringBuilder();
        if (this.tryMatch($ -> {
            while (this.hasNext()) {
                if (this.peek() == delimiter) {
                    if (skipDelimiter) this.skip();
                    return true;
                }

                read.append(this.next());
            }

            return false;
        })) {
            return read.toString();
        } else {
            return null;
        }
    }

    public String consumeUntilEndOr(CharPredicate until) {
        var read = new StringBuilder();
        while (this.hasNext() && !until.test(this.peek())) {
            read.append(this.next());
        }

        return read.toString();
    }

    public boolean expect(int offset, char expect) {
        int charIndex = this.cursor + offset;
        return charIndex >= 0 && charIndex < this.string.length() && this.string.charAt(charIndex) == expect;
    }

    public boolean tryConsume(char consume) {
        return tryMatch($ -> {
            var next = next();
            return next != null && next == consume;
        });
    }

    public boolean tryConsume(String consume) {
        return this.tryMatch(stringNibbler -> {
            for (int i = 0; i < consume.length(); i++) {
                var next = this.next();
                if (next == null || next != consume.charAt(i)) return false;
            }

            return true;
        });
    }

    public boolean tryMatch(Predicate<StringNibbler> matcher) {
        int cursorPos = this.cursor;
        if (!matcher.test(this)) {
            this.cursor = cursorPos;
            return false;
        } else {
            return true;
        }
    }
}
