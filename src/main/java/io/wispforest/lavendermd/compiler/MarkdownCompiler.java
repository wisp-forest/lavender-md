package io.wispforest.lavendermd.compiler;

import net.minecraft.text.Style;
import net.minecraft.util.Identifier;

import java.util.OptionalInt;
import java.util.function.UnaryOperator;

/**
 * All functions defined on this interface are required for supporting
 * every default Markdown feature implemented in lavender-md. A compiler
 * accepts these commands while visiting an AST and compiles into a result
 * type specific to each compiler implementation.
 * <p>
 * Compilers are stateful objects which build their result object internally
 * while visiting the AST and must be re-created for each new result to be
 * generated
 *
 * @param <R> The result type of this specific compiler implementation
 * @see io.wispforest.lavendermd.util.TextBuilder
 */
public interface MarkdownCompiler<R> {

    /**
     * Append {@code text} to this compiler's result, using
     * the current style as dictated by {@link #visitStyle(UnaryOperator)}
     * and {@link #visitStyleEnd()}
     */
    void visitText(String text);

    /**
     * Push {@code style} onto this compiler's stack
     */
    void visitStyle(UnaryOperator<Style> style);

    /**
     * Pop the current style from this compiler's stack
     */
    void visitStyleEnd();

    /**
     * Begin a new level of block quote
     */
    void visitBlockQuote();

    /**
     * End the current level of block quote
     */
    void visitBlockQuoteEnd();

    /**
     * Append a horizontal rule to this compiler's result
     */
    void visitHorizontalRule();

    /**
     * Append {@code image} into this compiler's result, using
     * {@code description} as alt-text, stretching the image to the
     * width of the compiler's result if {@code fit} is {@code true}
     */
    void visitImage(Identifier image, String description, boolean fit);

    /**
     * Begin a new list item, potentially nesting inside a
     * previous one
     */
    void visitListItem(OptionalInt ordinal);

    /**
     * End the current list item
     */
    void visitListItemEnd();

    /**
     * @return This compiler's result - the state of this compiler
     * and the result of calling this function again is not defined
     */
    R compile();

    /**
     * @return A name for this compiler, to be used in logging messages
     */
    String name();
}
