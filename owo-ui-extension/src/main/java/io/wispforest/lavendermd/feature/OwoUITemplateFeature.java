package io.wispforest.lavendermd.feature;

import com.mojang.logging.LogUtils;
import io.wispforest.lavendermd.Lexer;
import io.wispforest.lavendermd.MarkdownFeature;
import io.wispforest.lavendermd.Parser;
import io.wispforest.lavendermd.compiler.MarkdownCompiler;
import io.wispforest.lavendermd.compiler.OwoUICompiler;
import io.wispforest.lavendermd.util.StringNibbler;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.parsing.IncompatibleUIModelException;
import io.wispforest.owo.ui.parsing.UIModelLoader;
import io.wispforest.owo.ui.parsing.UIModelParsingException;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class OwoUITemplateFeature implements MarkdownFeature {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final TemplateProvider templateSource;

    public OwoUITemplateFeature(TemplateProvider templateProvider) {
        this.templateSource = templateProvider;
    }

    public OwoUITemplateFeature() {
        this(new TemplateProvider() {
            @Override
            public <C extends Component> C template(Identifier model, Class<C> expectedClass, String templateName, Map<String, String> templateParams) {
                var uiModel = UIModelLoader.get(model);
                if (uiModel == null) {
                    throw new UIModelParsingException("No UI model with id '" + model + " is currently loaded");
                }

                return uiModel.expandTemplate(expectedClass, templateName, templateParams);
            }
        });
    }

    @Override
    public String name() {
        return "owo_ui_templates";
    }

    @Override
    public boolean supportsCompiler(MarkdownCompiler<?> compiler) {
        return compiler instanceof OwoUICompiler;
    }

    @Override
    public void registerTokens(TokenRegistrar registrar) {
        registrar.registerToken((nibbler, tokens) -> {
            nibbler.skip();
            if (!nibbler.tryConsume('|')) return false;

            var templateLocation = nibbler.consumeUntil('|');
            if (templateLocation == null) return false;

            var splitLocation = templateLocation.split("@");
            if (splitLocation.length != 2) return false;

            var modelId = Identifier.tryParse(splitLocation[1]);
            if (modelId == null || !UIModelLoader.allLoadedModels().contains(modelId)) return false;

            String templateParams = "";
            if (!nibbler.tryConsume('>')) {
                templateParams = nibbler.consumeUntil('|');
                if (templateParams == null || !nibbler.tryConsume('>')) return false;
            } else {
                nibbler.skip();
            }

            tokens.add(new TemplateToken(modelId, splitLocation[0], templateParams));
            return true;
        }, '<');
    }

    @Override
    public void registerNodes(NodeRegistrar registrar) {
        registrar.registerNode(
                (parser, templateToken, tokens) -> new TemplateNode(templateToken.modelId, templateToken.templateName, templateToken.params),
                (token, tokens) -> token instanceof TemplateToken template ? template : null
        );
    }

    private static class TemplateToken extends Lexer.Token {

        public final Identifier modelId;
        public final String templateName;
        public final String params;

        public TemplateToken(Identifier modelId, String templateName, String params) {
            super("<|" + modelId + "|" + params + "|>");
            this.modelId = modelId;
            this.templateName = templateName;
            this.params = params;
        }

        @Override
        public boolean isBoundary() {
            return true;
        }
    }

    private class TemplateNode extends Parser.Node {

        private final Identifier modelId;
        private final String templateName;
        private final String params;

        public TemplateNode(Identifier modelId, String templateName, String params) {
            this.modelId = modelId;
            this.templateName = templateName;
            this.params = params;
        }

        @Override
        protected void visitStart(MarkdownCompiler<?> compiler) {
            try {
                var paramReader = new StringNibbler(params);
                var builtParams = new HashMap<String, String>();

                while (paramReader.hasNext()) {
                    var paramName = paramReader.consumeUntil('=');
                    var paramValue = paramReader.consumeEscapedString(',', true);
                    paramReader.skip();

                    builtParams.put(paramName, paramValue);
                }

                ((OwoUICompiler) compiler).visitComponent(OwoUITemplateFeature.this.templateSource.template(modelId, Component.class, this.templateName, builtParams));
            } catch (UIModelParsingException | IncompatibleUIModelException e) {
                LOGGER.warn("Failed to build owo-ui template markdown element", e);
                ((OwoUICompiler) compiler).visitComponent(
                        Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                                .child(Components.label(Text.literal(e.getMessage())).horizontalSizing(Sizing.fill(100)))
                                .padding(Insets.of(10))
                                .surface(Surface.flat(0x77A00000).and(Surface.outline(0x77FF0000)))
                );
            }
        }

        @Override
        protected void visitEnd(MarkdownCompiler<?> compiler) {}
    }

    @FunctionalInterface
    public interface TemplateProvider {
        <C extends Component> C template(Identifier model, Class<C> expectedClass, String templateName, Map<String, String> templateParams);
    }
}
