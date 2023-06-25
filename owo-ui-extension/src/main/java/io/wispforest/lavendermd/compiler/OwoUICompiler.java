package io.wispforest.lavendermd.compiler;

import io.wispforest.lavendermd.util.TextBuilder;
import io.wispforest.lavendermd.util.TextureSizeLookup;
import io.wispforest.owo.ui.component.BoxComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.OptionalInt;
import java.util.function.UnaryOperator;

/**
 * A secondary default compiler implementation which generates rich, formatted
 * and structured output containing images and other arbitrary UI elements
 * by compiling to an owo-ui component tree
 */
public class OwoUICompiler implements MarkdownCompiler<ParentComponent> {

    protected final Deque<FlowLayout> components = new ArrayDeque<>();
    protected final TextBuilder textBuilder = new TextBuilder();

    public OwoUICompiler() {
        this.components.push(Containers.verticalFlow(Sizing.content(), Sizing.content()));
    }

    @Override
    public void visitText(String text) {
        this.textBuilder.append(Text.literal(text));
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
        this.textBuilder.pushStyle(style -> style.withFormatting(Formatting.GRAY));

        var quotation = Containers.verticalFlow(Sizing.content(), Sizing.content());
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
            this.append(Containers.stack(Sizing.fill(100), Sizing.content())
                    .child(Components.texture(image, 0, 0, 256, 256, 256, 256).blend(true).tooltip(Text.literal(description)).sizing(Sizing.fixed(100)))
                    .horizontalAlignment(HorizontalAlignment.CENTER));
        } else {
            var textureSize = TextureSizeLookup.sizeOf(image);
            if (textureSize == null) textureSize = new TextureSizeLookup.Size(64, 64);

            this.append(Components.texture(image, 0, 0, textureSize.width(), textureSize.height(), textureSize.width(), textureSize.height()).blend(true).tooltip(Text.literal(description)));
        }
    }

    @Override
    public void visitListItem(OptionalInt ordinal) {
        var element = Containers.horizontalFlow(Sizing.content(), Sizing.content());
        element.child(this.makeLabel(Text.literal(ordinal.isPresent() ? " " + ordinal.getAsInt() + ". " : " • ").formatted(Formatting.GRAY)).margins(Insets.left(-11))).margins(Insets.vertical(1));
        element.padding(Insets.left(11)).allowOverflow(true);

        var container = Containers.verticalFlow(Sizing.content(), Sizing.content());
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
    public void visitComponent(Component component) {
        this.append(component);
    }

    protected void append(Component component) {
        this.flushText();
        this.components.peek().child(component);
    }

    protected void push(FlowLayout component) {
        this.push(component, component);
    }

    protected void push(Component element, FlowLayout contentPanel) {
        this.append(element);
        this.components.push(contentPanel);
    }

    protected void pop() {
        this.flushText();
        this.components.pop();
    }

    protected LabelComponent makeLabel(MutableText text) {
        return Components.label(text);
    }

    protected void flushText() {
        if (this.textBuilder.empty()) return;
        this.components.peek().child(this.makeLabel(this.textBuilder.build()).horizontalSizing(Sizing.fill(100)));
    }

    @Override
    public ParentComponent compile() {
        this.flushText();
        return this.components.getLast();
    }

    @Override
    public String name() {
        return "lavender_builtin_owo_ui";
    }
}
