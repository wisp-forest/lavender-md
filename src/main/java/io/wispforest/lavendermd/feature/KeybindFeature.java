package io.wispforest.lavendermd.feature;

import io.wispforest.lavendermd.Lexer;
import io.wispforest.lavendermd.MarkdownFeature;
import io.wispforest.lavendermd.Parser;
import io.wispforest.lavendermd.compiler.MarkdownCompiler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Arrays;

public class KeybindFeature implements MarkdownFeature {

    @Override
    public String name() {
        return "keybindings";
    }

    @Override
    public boolean supportsCompiler(MarkdownCompiler<?> compiler) {
        return true;
    }

    @Override
    public void registerTokens(TokenRegistrar registrar) {
        registrar.registerToken((nibbler, tokens) -> {
            if (!nibbler.tryConsume("<keybind;")) return false;

            var keybindKey = nibbler.consumeUntil('>');
            if (keybindKey == null) return false;

            var binding = Arrays.stream(MinecraftClient.getInstance().options.allKeys).filter($ -> $.getTranslationKey().equals(keybindKey)).findAny();
            if (binding.isEmpty()) return false;

            tokens.add(new KeybindToken(keybindKey, binding.get()));
            return true;
        }, '<');
    }

    @Override
    public void registerNodes(NodeRegistrar registrar) {
        registrar.registerNode(
                (parser, keybindToken, tokens) -> new KeybindNode(keybindToken.binding),
                (token, tokens) -> token instanceof KeybindToken keybind ? keybind : null
        );
    }

    private static class KeybindToken extends Lexer.Token {

        public final KeyBinding binding;

        public KeybindToken(String content, KeyBinding binding) {
            super(content);
            this.binding = binding;
        }
    }

    private static class KeybindNode extends Parser.Node {

        private final KeyBinding binding;

        public KeybindNode(KeyBinding binding) {
            this.binding = binding;
        }

        @Override
        public void visitStart(MarkdownCompiler<?> compiler) {
            compiler.visitStyle(style -> style.withColor(Formatting.GOLD).withHoverEvent(
                    new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable(
                            "text.lavender.keybind_tooltip",
                            Text.translatable(this.binding.getCategory()),
                            Text.translatable(this.binding.getTranslationKey())
                    ))
            ));
            compiler.visitText(I18n.translate(this.binding.getBoundKeyTranslationKey()));
        }

        @Override
        protected void visitEnd(MarkdownCompiler<?> compiler) {
            compiler.visitStyleEnd();
        }
    }
}
