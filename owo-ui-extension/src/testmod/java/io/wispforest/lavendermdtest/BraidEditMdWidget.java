package io.wispforest.lavendermdtest;

import io.wispforest.lavendermd.MarkdownProcessor;
import io.wispforest.lavendermd.compiler.BraidCompiler;
import io.wispforest.lavendermd.feature.*;
import io.wispforest.owo.braid.core.Alignment;
import io.wispforest.owo.braid.core.AppState;
import io.wispforest.owo.braid.core.Color;
import io.wispforest.owo.braid.core.Insets;
import io.wispforest.owo.braid.framework.BuildContext;
import io.wispforest.owo.braid.framework.proxy.WidgetState;
import io.wispforest.owo.braid.framework.widget.StatefulWidget;
import io.wispforest.owo.braid.framework.widget.Widget;
import io.wispforest.owo.braid.widgets.basic.*;
import io.wispforest.owo.braid.widgets.flex.Column;
import io.wispforest.owo.braid.widgets.flex.Flexible;
import io.wispforest.owo.braid.widgets.textinput.TextBox;
import io.wispforest.owo.braid.widgets.textinput.TextEditingController;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Map;

public class BraidEditMdWidget extends StatefulWidget {

    @Override
    public WidgetState<BraidEditMdWidget> createState() {
        return new State();
    }

    public static class State extends WidgetState<BraidEditMdWidget> {

        public TextEditingController textController;
        public Widget compiled = EmptyWidget.INSTANCE;

        @Override
        public void init() {
            this.textController = new TextEditingController();
            this.textController.addListener(() -> {
                var client = AppState.of(this.context()).client();
                var processor = MarkdownProcessor.richText(0)
                    .copyWith(BraidCompiler::new)
                    .copyWith(
                        new ImageFeature(),
                        new BlockStateFeature(),
                        new ItemStackFeature(client.level.registryAccess()),
                        new EntityFeature(),
                        new InlineBraidFeature(Map.of("the_handler", (arg) -> Minecraft.getInstance().gui.getChat().addClientSystemMessage(Component.literal("button: handled with argument " + arg)))),
                        new KeybindFeature(),
                        new TranslationsFeature()
                    );

                this.setState(() -> {
                    this.compiled = processor.process(this.textController.value().text());
                });
            });
        }

        @Override
        public Widget build(BuildContext context) {
            return new Center(
                new Column(
                    new Flexible(
                        3,
                        new Padding(
                            Insets.all(15),
                            new Sized(
                                400, null,
                                new TextBox(
                                    this.textController,
                                    widget -> widget.autoFocus()
                                )
                            )
                        )
                    ),
                    new Flexible(
                        4,
                        new Padding(
                            Insets.all(15),
                            new Sized(
                                400, null,
                                new Box(
                                    Color.BLACK.withA(.25),
                                    new Box(
                                        Color.WHITE,
                                        true,
                                        new Padding(
                                            Insets.all(3),
                                            new Align(
                                                Alignment.TOP_LEFT,
                                                this.compiled
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            );
        }
    }
}
