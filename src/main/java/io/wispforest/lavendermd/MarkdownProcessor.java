package io.wispforest.lavendermd;

import com.google.common.collect.ImmutableList;
import io.wispforest.lavendermd.compiler.MarkdownCompiler;
import io.wispforest.lavendermd.compiler.TextCompiler;
import io.wispforest.lavendermd.feature.*;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public class MarkdownProcessor<R> {

    private final Supplier<MarkdownCompiler<R>> compilerFactory;

    private final List<MarkdownFeature> features;

    private final Lexer lexer;
    private final Parser parser;

    public MarkdownProcessor(Supplier<MarkdownCompiler<R>> compilerFactory, MarkdownFeature... features) {
        this(compilerFactory, Arrays.asList(features));
    }

    public MarkdownProcessor(Supplier<MarkdownCompiler<R>> compilerFactory, List<MarkdownFeature> features) {
        this.compilerFactory = compilerFactory;
        this.features = ImmutableList.copyOf(features);

        var testCompiler = this.compilerFactory.get();
        for (var feature : this.features) {
            if (!feature.supportsCompiler(testCompiler)) {
                throw new IllegalStateException("Feature '" + feature.name() + "' is incompatible with compiler '" + testCompiler.name() + "'");
            }
        }

        this.lexer = new Lexer();
        this.parser = new Parser();

        for (var extension : features) {
            extension.registerTokens(this.lexer);
            extension.registerNodes(this.parser);
        }
    }

    public Collection<MarkdownFeature> installedFeatures() {
        return this.features;
    }

    public boolean hasFeature(Class<?> featureClass) {
        for (var extension : this.features) {
            if (featureClass.isInstance(extension)) {
                return true;
            }
        }

        return false;
    }

    public R process(String markdown) {
        var compiler = this.compilerFactory.get();

        this.parser.parse(this.lexer.lex(markdown)).visit(compiler);
        return compiler.compile();
    }

    // --- copy constructors ---

    public MarkdownProcessor<R> copyWith(MarkdownFeature... features) {
        var newFeatures = new ArrayList<>(this.features);
        for (var feature : features) {
            if (this.hasFeature(feature.getClass())) continue;
            newFeatures.add(feature);
        }

        return new MarkdownProcessor<>(this.compilerFactory, newFeatures);
    }

    public <R2> MarkdownProcessor<R2> copyWith(Supplier<MarkdownCompiler<R2>> compilerFactory) {
        return new MarkdownProcessor<>(compilerFactory, this.features);
    }

    // --- default factories ---

    public static MarkdownProcessor<Text> text() {
        return new MarkdownProcessor<>(TextCompiler::new, new BasicFormattingFeature(false), new ColorFeature());
    }

    public static MarkdownProcessor<Text> richText(int assumedOutputWidth) {
        return new MarkdownProcessor<>(() -> new TextCompiler(assumedOutputWidth), new BasicFormattingFeature(), new ColorFeature(), new LinkFeature(), new ListFeature(), new BlockQuoteFeature());
    }
}
