package io.wispforest.lavendermd.feature;

import io.wispforest.lavendermd.Lexer;
import io.wispforest.lavendermd.MarkdownFeature;
import io.wispforest.lavendermd.Parser;
import io.wispforest.lavendermd.compiler.MarkdownCompiler;
import net.minecraft.util.Identifier;

public class ImageFeature implements MarkdownFeature {

    @Override
    public String name() {
        return "images";
    }

    @Override
    public boolean supportsCompiler(MarkdownCompiler<?> compiler) {
        return true;
    }

    @Override
    public void registerTokens(TokenRegistrar registrar) {
        registrar.registerToken((nibbler, tokens) -> {
            nibbler.skip();
            if (!nibbler.tryConsume('[')) return false;

            var description = nibbler.consumeUntil(']');
            if (description == null || !nibbler.tryConsume('(')) return false;

            var identifier = nibbler.consumeUntil(')');
            if (identifier == null) return false;

            boolean fit = identifier.endsWith(",fit");
            if (fit) identifier = identifier.substring(0, identifier.length() - 4);
            if (Identifier.tryParse(identifier) == null) return false;

            tokens.add(new ImageToken(description, identifier, fit));
            return true;
        }, '!');

    }

    @Override
    public void registerNodes(NodeRegistrar registrar) {
        registrar.registerNode(
                (parser, image, tokens) -> new ImageNode(image.identifier, image.description, image.fit),
                (token, tokens) -> token instanceof ImageToken image ? image : null
        );
    }

    // --- token ---

    private static final class ImageToken extends Lexer.Token {

        public final String description, identifier;
        public final boolean fit;

        public ImageToken(String description, String identifier, boolean fit) {
            super("![" + description + "](" + identifier + ")");
            this.description = description;
            this.identifier = identifier;
            this.fit = fit;
        }
    }

    // --- node ---

    private static class ImageNode extends Parser.Node {

        private final String identifier, description;
        private final boolean fit;

        public ImageNode(String identifier, String description, boolean fit) {
            this.identifier = identifier;
            this.description = description;
            this.fit = fit;
        }

        @Override
        protected void visitStart(MarkdownCompiler<?> compiler) {
            compiler.visitImage(new Identifier(this.identifier), this.description, this.fit);
        }

        @Override
        protected void visitEnd(MarkdownCompiler<?> compiler) {}
    }
}
