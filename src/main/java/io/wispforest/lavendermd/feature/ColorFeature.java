package io.wispforest.lavendermd.feature;

import com.google.common.collect.ImmutableMap;
import io.wispforest.lavendermd.Lexer;
import io.wispforest.lavendermd.MarkdownFeature;
import io.wispforest.lavendermd.Parser;
import io.wispforest.lavendermd.compiler.MarkdownCompiler;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public class ColorFeature implements MarkdownFeature {

    private static final Map<String, Formatting> FORMATTING_COLORS = Stream.of(Formatting.values())
            .filter(Formatting::isColor)
            .collect(ImmutableMap.toImmutableMap(formatting -> formatting.getName().toLowerCase(Locale.ROOT), Function.identity()));

    @Override
    public String name() {
        return "colors";
    }

    @Override
    public boolean supportsCompiler(MarkdownCompiler<?> compiler) {
        return true;
    }

    @Override
    public void registerTokens(TokenRegistrar registrar) {
        registrar.registerToken((nibbler, tokens) -> {
            nibbler.skip();
            if (!nibbler.hasNext()) return false;

            if (nibbler.peek() == '}') {
                nibbler.skip();
                tokens.add(new CloseColorToken());
            } else {
                if (nibbler.peek() == '#') {
                    nibbler.skip();

                    var color = nibbler.consumeUntil('}');
                    if (color == null) return false;

                    if (!color.matches("[0-9a-fA-F]{6}")) return false;
                    tokens.add(new OpenColorToken("{#" + color + "}", style -> style.withColor(Integer.parseInt(color, 16))));
                } else {
                    var color = nibbler.consumeUntil('}');
                    if (color == null) return false;

                    if (!FORMATTING_COLORS.containsKey(color)) return false;
                    tokens.add(new OpenColorToken("{" + color + "}", style -> style.withFormatting(FORMATTING_COLORS.get(color))));
                }
            }

            return true;
        }, '{');

    }

    @Override
    public void registerNodes(NodeRegistrar registrar) {
        registrar.registerNode((parser, left, tokens) -> {
            int pointer = tokens.pointer();
            var content = parser.parseUntil(tokens, CloseColorToken.class);

            if (tokens.peek() instanceof CloseColorToken) {
                tokens.nibble();
                return new Parser.FormattingNode(left.style).addChild(content);
            } else {
                tokens.setPointer(pointer);
                return new Parser.TextNode(left.content());
            }
        }, (token, tokens) -> token instanceof OpenColorToken color ? color : null);
    }

    // --- tokens ---

    private static final class OpenColorToken extends Lexer.Token {

        public final @NotNull UnaryOperator<Style> style;

        public OpenColorToken(String content, @NotNull UnaryOperator<Style> style) {
            super(content);
            this.style = style;
        }
    }

    private static final class CloseColorToken extends Lexer.Token {
        private CloseColorToken() {
            super("{}");
        }
    }
}
