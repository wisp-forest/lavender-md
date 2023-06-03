package io.wispforest.lavendermd.feature;

import io.wispforest.lavendermd.Lexer;
import io.wispforest.lavendermd.MarkdownFeature;
import io.wispforest.lavendermd.Parser;
import io.wispforest.lavendermd.compiler.MarkdownCompiler;

public class BlockQuoteFeature implements MarkdownFeature {
    @Override
    public String name() {
        return "block_quotes";
    }

    @Override
    public boolean supportsCompiler(MarkdownCompiler<?> compiler) {
        return true;
    }

    @Override
    public void registerTokens(TokenRegistrar registrar) {
        registrar.registerToken((nibbler, tokens) -> {
            var brackets = nibbler.consumeUntilEndOr(c -> c != '>');
            if (!nibbler.tryConsume(' ')) return false;

            tokens.add(new QuotationToken(brackets.length()));
            return true;
        }, '>');
    }

    @Override
    public void registerNodes(NodeRegistrar registrar) {
        registrar.registerNode(
                (parser, current, tokens) -> new QuotationNode().addChild(parser.parseUntil(tokens, $ -> $.isBoundary() && (!($ instanceof QuotationToken) || ((QuotationToken) $).depth < current.depth), $ -> $ instanceof QuotationToken quote && quote.depth == current.depth)),
                (token, tokens) -> token instanceof QuotationToken current && (tokens.peek(-2) == null || tokens.peek(-2) instanceof Lexer.NewlineToken) ? current : null
        );
    }

    // --- token ---

    private static final class QuotationToken extends Lexer.Token {

        public final int depth;

        public QuotationToken(int depth) {
            super(">".repeat(depth) + " ");
            this.depth = depth;
        }

        @Override
        public boolean isBoundary() {
            return true;
        }
    }

    // --- node ---

    private static class QuotationNode extends Parser.Node {
        @Override
        protected void visitStart(MarkdownCompiler<?> compiler) {
            compiler.visitBlockQuote();
        }

        @Override
        protected void visitEnd(MarkdownCompiler<?> compiler) {
            compiler.visitBlockQuoteEnd();
        }
    }
}
