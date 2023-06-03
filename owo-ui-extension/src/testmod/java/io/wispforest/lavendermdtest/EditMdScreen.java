package io.wispforest.lavendermdtest;

import io.wispforest.lavendermd.MarkdownProcessor;
import io.wispforest.lavendermd.compiler.OwoUICompiler;
import io.wispforest.lavendermd.feature.BlockStateFeature;
import io.wispforest.lavendermd.feature.EntityFeature;
import io.wispforest.lavendermd.feature.ItemStackFeature;
import io.wispforest.lavendermd.feature.OwoUITemplateFeature;
import io.wispforest.lavendermd.feature.ImageFeature;
import io.wispforest.owo.ui.base.BaseUIModelScreen;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextAreaComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.util.CommandOpenedScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.PrintWriter;
import java.io.StringWriter;

public class EditMdScreen extends BaseUIModelScreen<FlowLayout> implements CommandOpenedScreen {

    public EditMdScreen() {
        super(FlowLayout.class, new Identifier("lavender-md-test", "edit-md"));
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        var output = rootComponent.childById(LabelComponent.class, "output");

        var anchor = rootComponent.childById(FlowLayout.class, "output-anchor");
        rootComponent.childById(TextAreaComponent.class, "input").onChanged().subscribe(value -> {
            try {
                anchor.<FlowLayout>configure(layout -> {
                    var processor = MarkdownProcessor.richText(0).copyWith(OwoUICompiler::new).copyWith(new ImageFeature(), new BlockStateFeature(), new ItemStackFeature(), new EntityFeature(), new OwoUITemplateFeature());

                    layout.clearChildren();
                    layout.child(processor.process(value));
                });

                output.text(MarkdownProcessor.richText(40).process(value));
            } catch (Exception e) {
                var trace = new StringWriter();
                var traceWriter = new PrintWriter(trace);
                e.printStackTrace(traceWriter);

                output.text(Text.literal(trace.toString()));
            }
        });
    }
}
