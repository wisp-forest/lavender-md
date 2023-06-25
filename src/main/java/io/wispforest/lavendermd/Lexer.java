package io.wispforest.lavendermd;

import io.wispforest.lavendermd.util.StringNibbler;
import it.unimi.dsi.fastutil.chars.Char2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectMap;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class Lexer implements MarkdownFeature.TokenRegistrar {

    private final Char2ObjectMap<List<LexFunction>> lexFunctions = new Char2ObjectLinkedOpenHashMap<>();

    public Lexer() {
        // newlines
        this.registerToken((nibbler, tokens) -> {
            var newlines = nibbler.consumeUntilEndOr(c -> c != '\n');
            if (newlines.length() > 1) {
                tokens.add(new NewlineToken("\n".repeat(newlines.length() - 1), true));
            } else {
                tokens.add(new NewlineToken(" ", false));
            }

            return true;
        }, '\n');

        // token escapes
        this.registerToken((nibbler, tokens) -> {
            nibbler.skip();
            var escaped = nibbler.next();
            if (escaped == null || !this.lexFunctions.containsKey(escaped.charValue())) return false;

            if (escaped == '\n') {
                tokens.add(new NewlineToken("\n", false));
            } else {
                appendText(tokens, escaped);
            }

            return true;
        }, '\\');
    }

    @Override
    public void registerToken(LexFunction lexer, char trigger) {
        this.lexFunctions.computeIfAbsent(trigger, character -> new ArrayList<>()).add(0, lexer);
    }

    /**
     * A lex-function is responsible for consuming a string of characters
     * in {@code nibbler} and appending the corresponding token(s) to {@code tokens}
     */
    @FunctionalInterface
    public interface LexFunction {
        boolean lex(StringNibbler nibbler, List<Token> tokens);
    }

    public List<Token> lex(String input) {
        var tokens = new ArrayList<Token>();
        var nibbler = new StringNibbler(input.strip());

        while (nibbler.hasNext()) {
            char current = nibbler.peek();
            if (this.lexFunctions.containsKey(current)) {

                boolean matched = false;
                for (var function : this.lexFunctions.get(current)) {
                    if (!nibbler.tryMatch($ -> function.lex(nibbler, tokens))) continue;

                    matched = true;
                    break;
                }

                if (!matched) {
                    nibbler.skip();
                    appendText(tokens, String.valueOf(current));
                }
            } else {
                appendText(tokens, nibbler.consumeUntilEndOr(this.lexFunctions.keySet()::contains));
            }
        }

        return tokens;
    }

    private static void appendText(List<Token> tokens, String text) {
        if (!tokens.isEmpty() && tokens.get(tokens.size() - 1) instanceof TextToken textToken) {
            textToken.append(text);
        } else {
            tokens.add(new TextToken(text));
        }
    }

    private static void appendText(List<Token> tokens, char text) {
        if (!tokens.isEmpty() && tokens.get(tokens.size() - 1) instanceof TextToken textToken) {
            textToken.append(text);
        } else {
            tokens.add(new TextToken(String.valueOf(text)));
        }
    }

    // --- basic tokens required for simple text lexing ---

    public abstract static class Token {
        protected final String content;

        protected Token(String content) {
            this.content = content;
        }

        public String content() {
            return this.content;
        }

        public boolean isBoundary() {
            return false;
        }

        public static LexFunction lexFromChar(Supplier<Token> factory) {
            return (nibbler, tokens) -> {
                nibbler.skip();
                tokens.add(factory.get());

                return true;
            };
        }
    }

    public static final class TextToken extends Token {

        private final StringBuilder contentBuilder;

        private String contentCache = "";
        private boolean dirty = true;

        public TextToken(String content) {
            super("");
            this.contentBuilder = new StringBuilder(content);
        }

        public void append(String content) {
            this.contentBuilder.append(content);
            this.dirty = true;
        }

        public void append(char content) {
            this.contentBuilder.append(content);
            this.dirty = true;
        }

        @Override
        public String content() {
            if (this.dirty) {
                this.contentCache = this.contentBuilder.toString();
                this.dirty = false;
            }

            return this.contentCache;
        }
    }

    public static final class NewlineToken extends Token {

        private final boolean isBoundary;

        public NewlineToken(String content, boolean isBoundary) {
            super(content);
            this.isBoundary = isBoundary;
        }

        @Override
        public boolean isBoundary() {
            return this.isBoundary;
        }
    }
}
