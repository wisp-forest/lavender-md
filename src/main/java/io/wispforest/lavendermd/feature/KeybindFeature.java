package io.wispforest.lavendermd.feature;

import io.wispforest.lavendermd.Lexer;
import io.wispforest.lavendermd.MarkdownFeature;
import io.wispforest.lavendermd.Parser;
import io.wispforest.lavendermd.compiler.MarkdownCompiler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

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

            var binding = Arrays.stream(Minecraft.getInstance().options.keyMappings).filter($ -> $.getName().equals(keybindKey)).findAny();
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

        public final KeyMapping binding;

        public KeybindToken(String content, KeyMapping binding) {
            super(content);
            this.binding = binding;
        }
    }

    private static class KeybindNode extends Parser.Node {

        private final KeyMapping binding;

        public KeybindNode(KeyMapping binding) {
            this.binding = binding;
        }

        @Override
        public void visitStart(MarkdownCompiler<?> compiler) {
            compiler.visitStyle(style -> style.withColor(ChatFormatting.GOLD).withHoverEvent(
                new HoverEvent.ShowText(Component.translatable(
                    "text.lavender.keybind_tooltip",
                    this.binding.getCategory().label(),
                    Component.translatable(this.binding.getName())
                ))
            ));
            compiler.visitText(this.binding.getTranslatedKeyMessage().getString());
        }

        @Override
        protected void visitEnd(MarkdownCompiler<?> compiler) {
            compiler.visitStyleEnd();
        }
    }
}
