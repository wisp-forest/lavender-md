package io.wispforest.lavendermd.util;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.UnaryOperator;

/**
 * A utility for building a Minecraft {@link Text} component
 * from a hierarchical style structure and text
 */
public class TextBuilder {

    private final Deque<Style> styles;

    private MutableText text = Text.empty();
    private boolean empty = true;

    public TextBuilder() {
        this.styles = new ArrayDeque<>();
        this.styles.push(Style.EMPTY);
    }

    /**
     * Append {@code text} to this builder's result
     */
    public void append(MutableText text) {
        this.text.append(text.styled(style -> style.withParent(this.styles.peek())));
        this.empty = false;
    }

    /**
     * Push {@code style} onto this builder's stack
     */
    public void pushStyle(UnaryOperator<Style> style) {
        this.styles.push(style.apply(this.styles.peek()));
    }

    /**
     * Pop the current style from this builder's stack
     */
    public void popStyle() {
        this.styles.pop();
    }

    /**
     * Return this builder's current result and clear
     * all internal state, ready to build a fresh text
     */
    public MutableText build() {
        var result = this.text;
        if (result.getString().equals("\n")) {
            result = Text.literal(" ");
        }

        this.text = Text.empty();
        this.empty = true;

        return result;
    }

    /**
     * @return {@code true} if this builder is in an empty state - that is,
     * either it is new or cleared by a call to {@link #build()}
     */
    public boolean empty() {
        return this.empty;
    }
}
