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

/**
 * A Markdown-processor models the pipeline required to lex, parse and compile
 * some Markdown input into a result of type {@code R}. For this purpose, it employs
 * a set of {@link MarkdownFeature}s installed into a {@link Lexer} and {@link Parser},
 * the resulting AST of which it compiles using a {@link MarkdownCompiler}.
 * <p>
 * To create a processor, either use one of the default factories and optionally customize
 * them using the provided copyWith functions, or invoke the constructor and supply
 * the desired compiler factory and feature-set
 */
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

    /**
     * Create a copy of this processor with {@code features} added
     * to the copy's feature-set
     */
    public MarkdownProcessor<R> copyWith(MarkdownFeature... features) {
        var newFeatures = new ArrayList<>(this.features);
        for (var feature : features) {
            if (this.hasFeature(feature.getClass())) continue;
            newFeatures.add(feature);
        }

        return new MarkdownProcessor<>(this.compilerFactory, newFeatures);
    }

    /**
     * Create a copy of this processor with its compiler factory
     * replaced by {@code compilerFactory}
     */
    public <R2> MarkdownProcessor<R2> copyWith(Supplier<MarkdownCompiler<R2>> compilerFactory) {
        return new MarkdownProcessor<>(compilerFactory, this.features);
    }

    // --- default factories ---

    /**
     * Create a new Markdown-processor with support for basic text formatting, that is:
     * <ul>
     *     <li>Bold & Italic Emphasis</li>
     *     <li>Discord-like underscore and strikethrough formatting</li>
     *     <li>Colors using <pre>{&lt;color name&gt;|#RRGGBB}content here{}</pre> syntax</li>
     * </ul>
     */
    public static MarkdownProcessor<Text> text() {
        return new MarkdownProcessor<>(TextCompiler::new, new BasicFormattingFeature(false), new ColorFeature());
    }

    /**
     * Create a new Markdown-processor with support for rich text formatting, that is:
     * <ul>
     *     <li>Bold & Italic Emphasis</li>
     *     <li>Discord-like underscore and strikethrough formatting</li>
     *     <li>Horizontal Rules</li>
     *     <li>Colors using <pre>{&lt;color name&gt;|#RRGGBB}content here{}</pre> syntax</li>
     *     <li>Hyperlinks</li>
     *     <li>Ordered & unordered lists</li>
     *     <li>Block quotes</li>
     * </ul>
     */
    public static MarkdownProcessor<Text> richText(int assumedOutputWidth) {
        return new MarkdownProcessor<>(() -> new TextCompiler(assumedOutputWidth), new BasicFormattingFeature(), new ColorFeature(), new LinkFeature(), new ListFeature(), new BlockQuoteFeature());
    }
}
