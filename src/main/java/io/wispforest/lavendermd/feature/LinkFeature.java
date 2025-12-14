package io.wispforest.lavendermd.feature;

import io.wispforest.lavendermd.Lexer;
import io.wispforest.lavendermd.MarkdownFeature;
import io.wispforest.lavendermd.Parser;
import io.wispforest.lavendermd.compiler.MarkdownCompiler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import org.jetbrains.annotations.NotNull;

import java.net.URI;

public class LinkFeature implements MarkdownFeature {

    @Override
    public String name() {
        return "links";
    }

    @Override
    public boolean supportsCompiler(MarkdownCompiler<?> compiler) {
        return true;
    }

    @Override
    public void registerTokens(TokenRegistrar registrar) {
        registrar.registerToken(Lexer.Token.lexFromChar(OpenLinkToken::new), '[');
        registrar.registerToken((nibbler, tokens) -> {
            nibbler.skip();
            if (!nibbler.tryConsume('(')) return false;

            var link = nibbler.consumeUntil(')');
            if (link == null) return false;

            tokens.add(new CloseLinkToken(link));
            return true;
        }, ']');
    }

    @Override
    public void registerNodes(NodeRegistrar registrar) {
        registrar.registerNode((parser, left, tokens) -> {
            int pointer = tokens.pointer();
            var content = parser.parseUntil(tokens, CloseLinkToken.class);

            if (tokens.peek() instanceof CloseLinkToken right) {
                tokens.nibble();
                return new Parser.FormattingNode(style -> style.withClickEvent(
                        new ClickEvent.OpenUrl(URI.create(right.link))
                ).withHoverEvent(
                        new HoverEvent.ShowText(Component.literal(right.link))
                ).withColor(ChatFormatting.BLUE)).addChild(content);
            } else {
                tokens.setPointer(pointer);
                return new Parser.TextNode(left.content());
            }
        }, (token, tokens) -> token instanceof OpenLinkToken link ? link : null);
    }

    // --- tokens ---

    private static final class OpenLinkToken extends Lexer.Token {
        public OpenLinkToken() {
            super("[");
        }
    }

    private static final class CloseLinkToken extends Lexer.Token {

        public final @NotNull String link;

        public CloseLinkToken(@NotNull String link) {
            super("](" + link + ")");
            this.link = link;
        }
    }
}
