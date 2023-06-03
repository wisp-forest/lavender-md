package io.wispforest.lavendermd;

import io.wispforest.lavendermd.compiler.MarkdownCompiler;
import io.wispforest.lavendermd.util.ListNibbler;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

public interface MarkdownFeature {

    String name();

    boolean supportsCompiler(MarkdownCompiler<?> compiler);

    void registerTokens(TokenRegistrar registrar);

    void registerNodes(NodeRegistrar registrar);

    @FunctionalInterface
    interface TokenRegistrar {
        void registerToken(Lexer.LexFunction lexer, char trigger);

        default void registerToken(Lexer.LexFunction lexer, char... triggers) {
            for (char trigger : triggers) {
                this.registerToken(lexer, trigger);
            }
        }
    }

    @FunctionalInterface
    interface NodeRegistrar {
        <T extends Lexer.Token> void registerNode(Parser.ParseFunction<T> parser, BiFunction<Lexer.Token, ListNibbler<Lexer.Token>, @Nullable T> trigger);
    }

}
