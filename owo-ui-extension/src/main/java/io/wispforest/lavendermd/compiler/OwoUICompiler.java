package io.wispforest.lavendermd.compiler;

import io.wispforest.lavendermd.util.TextBuilder;
import io.wispforest.owo.ui.component.BoxComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.OptionalInt;
import java.util.function.UnaryOperator;

/**
 * A secondary default compiler implementation which generates rich, formatted
 * and structured output containing images and other arbitrary UI elements
 * by compiling to an owo-ui component tree
 */
public class OwoUICompiler implements MarkdownCompiler<ParentUIComponent> {

    protected final Deque<FlowLayout> components = new ArrayDeque<>();
    protected final TextBuilder textBuilder = new TextBuilder();

    public OwoUICompiler() {
        this.components.push(UIContainers.verticalFlow(Sizing.content(), Sizing.content()));
    }

    @Override
    public void visitText(String text) {
        this.textBuilder.append(Component.literal(text));
    }

    @Override
    public void visitStyle(UnaryOperator<Style> style) {
        this.textBuilder.pushStyle(style);
    }

    @Override
    public void visitStyleEnd() {
        this.textBuilder.popStyle();
    }

    @Override
    public void visitBlockQuote() {
        this.textBuilder.pushStyle(style -> style.withColor(ChatFormatting.GRAY));

        var quotation = UIContainers.verticalFlow(Sizing.content(), Sizing.content());
        quotation.padding(Insets.of(5, 5, 7, 5)).surface((context, component) -> {
            context.fill(component.x(), component.y() + 3, component.x() + 2, component.y() + component.height() - 3, 0xFF777777);
        });

        this.push(quotation);
    }

    @Override
    public void visitBlockQuoteEnd() {
        this.textBuilder.popStyle();
        this.pop();
    }

    @Override
    public void visitHorizontalRule() {
        this.append(new BoxComponent(Sizing.fill(100), Sizing.fixed(2)).color(Color.ofRgb(0x777777)).fill(true));
    }

    @Override
    public void visitImage(Identifier image, String description, boolean fit) {
        if (fit) {
            this.append(UIContainers.stack(Sizing.fill(100), Sizing.content())
                .child(UIComponents.texture(image, 0, 0, 256, 256, 256, 256).blend(true).tooltip(Component.literal(description)).sizing(Sizing.fixed(100)))
                .horizontalAlignment(HorizontalAlignment.CENTER));
        } else {
            var texture = Minecraft.getInstance().getTextureManager().getTexture(image);
            var textureSize = Size.of(texture.getTexture().getWidth(0), texture.getTexture().getHeight(64));

            this.append(UIComponents.texture(image, 0, 0, textureSize.width(), textureSize.height(), textureSize.width(), textureSize.height()).blend(true).tooltip(Component.literal(description)));
        }
    }

    @Override
    public void visitListItem(OptionalInt ordinal) {
        var element = UIContainers.horizontalFlow(Sizing.content(), Sizing.content());
        element.child(this.makeLabel(Component.literal(ordinal.isPresent() ? " " + ordinal.getAsInt() + ". " : " • ").withStyle(ChatFormatting.GRAY)).margins(Insets.left(-11))).margins(Insets.vertical(1));
        element.padding(Insets.left(11)).allowOverflow(true);

        var container = UIContainers.verticalFlow(Sizing.content(), Sizing.content());
        element.child(container);

        this.push(element, container);
    }

    @Override
    public void visitListItemEnd() {
        this.pop();
    }

    /**
     * Append {@code component} to this compiler's result
     */
    public void visitComponent(UIComponent component) {
        this.append(component);
    }

    protected void append(UIComponent component) {
        this.flushText();
        this.components.peek().child(component);
    }

    protected void push(FlowLayout component) {
        this.push(component, component);
    }

    protected void push(UIComponent element, FlowLayout contentPanel) {
        this.append(element);
        this.components.push(contentPanel);
    }

    protected void pop() {
        this.flushText();
        this.components.pop();
    }

    protected LabelComponent makeLabel(MutableComponent text) {
        return UIComponents.label(text);
    }

    protected void flushText() {
        if (this.textBuilder.empty()) return;
        this.components.peek().child(this.makeLabel(this.textBuilder.build()).horizontalSizing(Sizing.fill(100)));
    }

    @Override
    public ParentUIComponent compile() {
        this.flushText();
        return this.components.getLast();
    }

    @Override
    public String name() {
        return "lavender_builtin_owo_ui";
    }
}
