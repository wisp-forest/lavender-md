package io.wispforest.lavendermd.feature;

import com.mojang.logging.LogUtils;
import dev.kdl.parse.Kdl2Parser;
import io.wispforest.endec.SerializationAttributes;
import io.wispforest.endec.SerializationContext;
import io.wispforest.lavendermd.Lexer;
import io.wispforest.lavendermd.MarkdownFeature;
import io.wispforest.lavendermd.Parser;
import io.wispforest.lavendermd.compiler.BraidCompiler;
import io.wispforest.lavendermd.compiler.MarkdownCompiler;
import io.wispforest.owo.braid.util.kdl.BraidKdlEndecs;
import io.wispforest.owo.braid.util.kdl.KdlDeserializer;
import io.wispforest.owo.braid.util.kdl.KdlMapper;
import io.wispforest.owo.braid.util.kdl.WidgetEndec;
import io.wispforest.owo.braid.widgets.label.Label;
import io.wispforest.owo.braid.widgets.label.LabelStyle;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class InlineBraidFeature implements MarkdownFeature {

    private final Map<String, Consumer<@Nullable Object>> kdlHandlers;

    public InlineBraidFeature(Map<String, Consumer<@Nullable Object>> kdlHandlers) {
        this.kdlHandlers = kdlHandlers;
    }

    @Override
    public String name() {
        return "inline_braid";
    }

    @Override
    public boolean supportsCompiler(MarkdownCompiler<?> compiler) {
        return compiler instanceof BraidCompiler;
    }

    @Override
    public void registerTokens(TokenRegistrar registrar) {
        registrar.registerToken((nibbler, tokens) -> {
            if (!nibbler.tryConsume("```kdl braid")) return false;

            var kdlContent = nibbler.consumeUntil('`');
            if (kdlContent == null || !nibbler.tryConsume("``")) return false;

            tokens.add(new InlineBraidToken(kdlContent));
            return true;
        }, '`');
    }

    @Override
    public void registerNodes(NodeRegistrar registrar) {
        registrar.<@NotNull InlineBraidToken>registerNode(
            (parser, braidToken, tokens) -> new InlineBraidNode(braidToken.kdlContent),
            (token, tokens) -> token instanceof InlineBraidToken braidToken ? braidToken : null
        );
    }

    private static final Logger LOGGER = LogUtils.getLogger();

    private static class InlineBraidToken extends Lexer.Token {

        public final String kdlContent;

        protected InlineBraidToken(String kdlContent) {
            super("```kdl braid" + kdlContent + "```");
            this.kdlContent = kdlContent;
        }
    }

    private class InlineBraidNode extends Parser.Node {

        private final String kdlContent;

        public InlineBraidNode(String kdlContent) {
            this.kdlContent = kdlContent;
        }

        @Override
        protected void visitStart(MarkdownCompiler<?> compiler) {
            try {
                var parsedKdl = new Kdl2Parser().parse(this.kdlContent);
                var rootNode = parsedKdl.nodes().getFirst();

                var deserializer = new KdlDeserializer(rootNode, KdlMapper.DEFAULT_MAPPERS);
                var ctx = deserializer.setupContext(SerializationContext.attributes(
                    SerializationAttributes.HUMAN_READABLE,
                    BraidKdlEndecs.HANDLERS.instance(InlineBraidFeature.this.kdlHandlers)
                ));

                var parsedWidget = WidgetEndec.ROOT.decode(ctx, deserializer);
                ((BraidCompiler) compiler).visitWidget(parsedWidget);
            } catch (Exception e) {
                LOGGER.warn("Failed to build inline braid markdown element", e);

                Throwable cause = e;
                while (cause.getCause() != null) {
                    cause = cause.getCause();
                }

                ((BraidCompiler) compiler).visitWidget(
                    new Label(LabelStyle.SHADOW, true, Component.literal(Objects.requireNonNullElse(cause.getMessage(), "no message")).withStyle(ChatFormatting.RED))
                );
            }
        }

        @Override
        protected void visitEnd(MarkdownCompiler<?> compiler) {}
    }
}
