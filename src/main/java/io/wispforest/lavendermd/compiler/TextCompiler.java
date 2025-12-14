package io.wispforest.lavendermd.compiler;

import io.wispforest.lavendermd.util.TextBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.util.OptionalInt;
import java.util.function.UnaryOperator;

/**
 * lavender-md's default compiler implementation which compiles to a
 * single Minecraft {@link Component} - depending on the input
 * AST it might contain multiple lines
 */
public class TextCompiler implements MarkdownCompiler<Component> {

    private final TextBuilder builder = new TextBuilder();
    private final int assumedOutputWidth;

    private int quoteDepth = 0;
    private int listDepth = 0;

    public TextCompiler() {
        this(50);
    }

    public TextCompiler(int assumedOutputWidth) {
        this.assumedOutputWidth = assumedOutputWidth;
    }

    @Override
    public void visitText(String text) {
        if (this.quoteDepth != 0 && text.contains("\n")) {
            if (text.equals("\n")) {
                this.builder.append(this.quoteMarker());
            } else {
                for (var line : text.split("\n")) {
                    this.builder.append(this.quoteMarker().append(Component.literal(line)));
                }
            }
        } else if (this.listDepth != 0 && text.contains("\n")) {
            if (text.equals("\n")) {
                this.builder.append(Component.literal("\n   " + "  ".repeat(this.listDepth - 1)));
            } else {
                var lines = text.split("\n");
                for (int i = 0; i < lines.length; i++) {
                    this.builder.append(Component.literal((i > 0 ? "\n   " : "   ") + "  ".repeat(this.listDepth - 1)).append(Component.literal(lines[i])));
                }
            }
        } else {
            this.builder.append(Component.literal(text));
        }
    }

    @Override
    public void visitStyle(UnaryOperator<Style> style) {
        this.builder.pushStyle(style);
    }

    @Override
    public void visitStyleEnd() {
        this.builder.popStyle();
    }

    @Override
    public void visitBlockQuote() {
        this.quoteDepth++;
        this.builder.append(this.quoteMarker());
        this.builder.pushStyle(style -> style.withColor(ChatFormatting.GRAY).withItalic(true));
    }

    @Override
    public void visitBlockQuoteEnd() {
        this.builder.popStyle();
        this.quoteDepth--;

        if (this.quoteDepth > 0) {
            this.builder.append(this.quoteMarker());
        } else {
            this.builder.append(Component.literal("\n"));
        }
    }

    private MutableComponent quoteMarker() {
        return Component.literal("\n >" + ">".repeat(this.quoteDepth) + " ").withStyle(ChatFormatting.DARK_GRAY);
    }

    @Override
    public void visitHorizontalRule() {
        this.builder.append(Component.literal("-".repeat(this.assumedOutputWidth)).withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public void visitImage(Identifier image, String description, boolean fit) {
        this.builder.append(Component.literal("[" + description + "]").withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public void visitListItem(OptionalInt ordinal) {
        var listPrefix = ordinal.isPresent() ? " " + ordinal.getAsInt() + ". " : " • ";

        if (this.listDepth > 0) {
            this.builder.append(Component.literal("\n" + "   ".repeat(this.listDepth) + listPrefix));
        } else {
            this.builder.append(Component.literal(listPrefix));
        }

        this.listDepth++;
    }

    @Override
    public void visitListItemEnd() {
        this.listDepth--;

        if (this.listDepth > 0) {
            this.builder.append(Component.literal("   ".repeat(this.listDepth)));
        } else {
            this.builder.append(Component.literal("\n"));
        }
    }

    @Override
    public Component compile() {
        return this.builder.build();
    }

    @Override
    public String name() {
        return "lavender_builtin_text";
    }
}
