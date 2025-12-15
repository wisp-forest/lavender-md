package io.wispforest.lavendermd.compiler;

import io.wispforest.lavendermd.util.TextBuilder;
import io.wispforest.owo.Owo;
import io.wispforest.owo.braid.core.Alignment;
import io.wispforest.owo.braid.core.Color;
import io.wispforest.owo.braid.core.Insets;
import io.wispforest.owo.braid.core.Size;
import io.wispforest.owo.braid.framework.widget.Widget;
import io.wispforest.owo.braid.widgets.basic.*;
import io.wispforest.owo.braid.widgets.flex.Column;
import io.wispforest.owo.braid.widgets.flex.Row;
import io.wispforest.owo.braid.widgets.label.Label;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class BraidCompiler implements MarkdownCompiler<Widget> {

    protected final Deque<WidgetBuilder> widgets = new ArrayDeque<>();
    protected final TextBuilder textBuilder = new TextBuilder();

    public BraidCompiler() {
        this.widgets.push(new WidgetBuilder(Column::new));
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

        this.push(new WidgetBuilder(children -> {
            return new IntrinsicHeight(
                new Row(
                    new Sized(
                        2.0, null,
                        new Box(Color.rgb(0x777777))
                    ),
                    new Padding(
                        Insets.of(5, 5, 5, 5),
                        new Column(
                            children
                        )
                    )
                )
            );
        }));
    }

    @Override
    public void visitBlockQuoteEnd() {
        this.textBuilder.popStyle();
        this.pop();
    }

    @Override
    public void visitHorizontalRule() {
        this.append(() -> new Sized(
            Double.POSITIVE_INFINITY, 2,
            new Box(Color.rgb(0x777777))
        ));
    }

    @Override
    public void visitImage(Identifier image, String description, boolean fit) {
        Widget widget = new Tooltip(
            Component.literal(description),
            new TextureWidget(
                image,
                TextureWidget.Wrap.STRETCH,
                Color.WHITE
            )
        );

        if (fit) {
            widget = new Align(
                Alignment.CENTER, null, 1.0,
                new Sized(
                    Size.square(100),
                    widget
                )
            );
        }

        var javaMoment = widget;
        this.append(() -> javaMoment);
    }

    @Override
    public void visitListItem(OptionalInt ordinal) {
        this.push(new WidgetBuilder(children -> {
            return new Padding(
                Insets.left(11),
                new Row(
                    new Padding(
                        Insets.of(1, 1, -11, 0),
                        new Label(
                            Component.literal(ordinal.isPresent() ? " " + ordinal.getAsInt() + ". " : " • ").withStyle(ChatFormatting.GRAY)
                        )
                    ),
                    new Column(children)
                )
            );
        }));
    }

    @Override
    public void visitListItemEnd() {
        this.pop();
    }

    /**
     * Append {@code widget} to this compiler's result
     */
    public void visitWidget(Widget widget) {
        this.append(() -> widget);
    }

    protected void append(Supplier<Widget> widget) {
        this.flushText();
        this.widgets.peek().child(widget);
    }

    protected void push(WidgetBuilder builder) {
        this.append(builder);
        this.widgets.push(builder);
    }

    protected void pop() {
        this.flushText();
        this.widgets.pop();
    }

    protected void flushText() {
        if (this.textBuilder.empty()) return;
        var text = this.textBuilder.build();
        this.widgets.peek().child(() -> new Label(text));
    }

    @Override
    public Widget compile() {
        this.flushText();
        if (Owo.DEBUG && this.widgets.size() != 1) {
            throw new IllegalStateException("unclosed node in BraidCompiler");
        }

        return this.widgets.getFirst().get();
    }

    @Override
    public String name() {
        return "lavender_builtin_braid";
    }

    protected static class WidgetBuilder implements Supplier<Widget> {
        private final List<Supplier<Widget>> children = new ArrayList<>();
        private final Function<List<Widget>, Widget> builder;

        public WidgetBuilder(Function<List<Widget>, Widget> builder) {
            this.builder = builder;
        }

        public void child(Supplier<Widget> child) {
            this.children.add(child);
        }

        @Override
        public Widget get() {
            return this.builder.apply(this.children.stream().map(Supplier::get).toList());
        }
    }
}
