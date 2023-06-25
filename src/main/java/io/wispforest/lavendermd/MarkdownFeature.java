package io.wispforest.lavendermd;

import io.wispforest.lavendermd.compiler.MarkdownCompiler;
import io.wispforest.lavendermd.util.ListNibbler;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

/**
 * A Markdown feature implements a set of tokens and AST nodes
 * for parsing & generating certain Markdown syntax
 */
public interface MarkdownFeature {

    /**
     * @return A name for this feature, to be used in logging messages
     */
    String name();

    /**
     * Return {@code true} if this feature knows how to invoke {@code compiler}.
     * <p>
     * All nodes created by this feature must assume that the compiler they are
     * invoked on has passed the check in this method. Notably - if this method does
     * a type-check, all compilers used on this feature's nodes are guaranteed
     * to be of said type
     */
    boolean supportsCompiler(MarkdownCompiler<?> compiler);

    /**
     * Add this feature's set of tokens to {@code registrar}
     */
    void registerTokens(TokenRegistrar registrar);

    /**
     * Add this feature's set of nodes to {@code registrar}
     */
    void registerNodes(NodeRegistrar registrar);

    @FunctionalInterface
    interface TokenRegistrar {
        /**
         * Register {@code lexer} to be invoked when {@code trigger}
         * is encountered while lexing the Markdown input
         */
        void registerToken(Lexer.LexFunction lexer, char trigger);

        /**
         * Register {@code lexer} to be invoked when any of
         * {@code triggers} is encountered while lexing the Markdown input
         */
        default void registerToken(Lexer.LexFunction lexer, char... triggers) {
            for (char trigger : triggers) {
                this.registerToken(lexer, trigger);
            }
        }
    }

    @FunctionalInterface
    interface NodeRegistrar {
        /**
         * Register {@code parser} to be invoked when {@code trigger} matches and
         * returns a token of type {@code T} from the incoming stream
         */
        <T extends Lexer.Token> void registerNode(Parser.ParseFunction<T> parser, BiFunction<Lexer.Token, ListNibbler<Lexer.Token>, @Nullable T> trigger);
    }

}
