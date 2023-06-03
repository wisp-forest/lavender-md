package io.wispforest.lavendermd.feature;

import io.wispforest.lavendermd.Lexer;
import io.wispforest.lavendermd.MarkdownFeature;
import io.wispforest.lavendermd.Parser;
import io.wispforest.lavendermd.compiler.MarkdownCompiler;
import io.wispforest.lavendermd.util.StringNibbler;

import java.util.OptionalInt;

public class ListFeature implements MarkdownFeature {
    @Override
    public String name() {
        return "lists";
    }

    @Override
    public boolean supportsCompiler(MarkdownCompiler<?> compiler) {
        return true;
    }

    @Override
    public void registerTokens(TokenRegistrar registrar) {
        // unordered
        registrar.registerToken((nibbler, tokens) -> {
            int whitespace = whitespaceSinceLineBreak(nibbler);
            if (whitespace < 0) return false;

            nibbler.skip();
            if (!nibbler.tryConsume(' ')) return false;

            tokens.add(new ListToken(whitespace, OptionalInt.empty()));
            return true;
        }, '-');

        // ordered
        registrar.registerToken((nibbler, tokens) -> {
            int whitespace = whitespaceSinceLineBreak(nibbler);
            if (whitespace < 0) return false;

            var ordinal = nibbler.consumeUntilEndOr(c -> c < '0' || c > '9');
            if (!ordinal.matches("[0-9]+") || !nibbler.tryConsume(". ")) {
                return false;
            }

            tokens.add(new ListToken(whitespace, OptionalInt.of(Integer.parseInt(ordinal))));
            return true;
        }, '0', '1', '2', '3', '4', '5', '6', '7', '8', '9');
    }

    @Override
    public void registerNodes(NodeRegistrar registrar) {
        registrar.registerNode(
                (parser, current, tokens) -> new ListNode(current.ordinal).addChild(parser.parseUntil(tokens, $ -> $.isBoundary() && !($ instanceof ListToken list && list.depth > current.depth), $ -> false)),
                (token, tokens) -> token instanceof ListToken list ? list : null
        );
    }

    private static int whitespaceSinceLineBreak(StringNibbler nibbler) {
        int offset = 1;
        int whitespace = 0;

        while (nibbler.cursor() - offset >= 0) {
            //noinspection DataFlowIssue
            char current = nibbler.peekOffset(-offset);
            if (current == '\n') return whitespace;

            if (Character.isWhitespace(current)) {
                whitespace++;
            } else {
                return -1;
            }

            offset++;
        }

        return whitespace;
    }

    // --- token ---

    private static final class ListToken extends Lexer.Token {

        public final int depth;
        public final OptionalInt ordinal;

        public ListToken(int depth, OptionalInt ordinal) {
            super("- ");
            this.depth = depth;
            this.ordinal = ordinal;
        }

        @Override
        public boolean isBoundary() {
            return true;
        }
    }

    // --- node ---

    private static class ListNode extends Parser.Node {

        private final OptionalInt ordinal;

        public ListNode(OptionalInt ordinal) {
            this.ordinal = ordinal;
        }

        @Override
        protected void visitStart(MarkdownCompiler<?> compiler) {
            compiler.visitListItem(this.ordinal);
        }

        @Override
        protected void visitEnd(MarkdownCompiler<?> compiler) {
            compiler.visitListItemEnd();
        }
    }
}
