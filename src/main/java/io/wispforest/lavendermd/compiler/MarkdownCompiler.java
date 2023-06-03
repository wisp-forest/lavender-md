package io.wispforest.lavendermd.compiler;

import net.minecraft.text.Style;
import net.minecraft.util.Identifier;

import java.util.OptionalInt;
import java.util.function.UnaryOperator;

public interface MarkdownCompiler<R> {

    void visitText(String text);

    void visitStyle(UnaryOperator<Style> style);

    void visitStyleEnd();

    void visitBlockQuote();

    void visitBlockQuoteEnd();

    void visitHorizontalRule();

    void visitImage(Identifier image, String description, boolean fit);

    void visitListItem(OptionalInt ordinal);

    void visitListItemEnd();

    R compile();

    String name();
}
