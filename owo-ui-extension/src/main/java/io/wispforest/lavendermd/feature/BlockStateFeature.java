package io.wispforest.lavendermd.feature;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.wispforest.lavendermd.Lexer;
import io.wispforest.lavendermd.MarkdownFeature;
import io.wispforest.lavendermd.Parser;
import io.wispforest.lavendermd.compiler.BraidCompiler;
import io.wispforest.lavendermd.compiler.MarkdownCompiler;
import io.wispforest.lavendermd.compiler.OwoUICompiler;
import io.wispforest.owo.braid.core.Alignment;
import io.wispforest.owo.braid.core.Size;
import io.wispforest.owo.braid.widgets.basic.Align;
import io.wispforest.owo.braid.widgets.basic.Sized;
import io.wispforest.owo.braid.widgets.object.BlockWidget;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.registries.BuiltInRegistries;

public class BlockStateFeature implements MarkdownFeature {

    @Override
    public String name() {
        return "block_states";
    }

    @Override
    public boolean supportsCompiler(MarkdownCompiler<?> compiler) {
        return compiler instanceof OwoUICompiler || compiler instanceof BraidCompiler;
    }

    @Override
    public void registerTokens(TokenRegistrar registrar) {
        registrar.registerToken((nibbler, tokens) -> {
            if (!nibbler.tryConsume("<block;")) return false;

            var blockStateString = nibbler.consumeUntil('>');
            if (blockStateString == null) return false;

            try {
                tokens.add(new BlockStateToken(
                        blockStateString,
                        BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK, blockStateString, true)
                ));
                return true;
            } catch (CommandSyntaxException e) {
                return false;
            }
        }, '<');
    }

    @Override
    public void registerNodes(NodeRegistrar registrar) {
        registrar.registerNode(
                (parser, stateToken, tokens) -> new BlockStateNode(stateToken.state),
                (token, tokens) -> token instanceof BlockStateToken blockState ? blockState : null
        );
    }

    private static class BlockStateToken extends Lexer.Token {

        public final BlockStateParser.BlockResult state;

        public BlockStateToken(String content, BlockStateParser.BlockResult state) {
            super(content);
            this.state = state;
        }
    }

    private static class BlockStateNode extends Parser.Node {

        private final BlockStateParser.BlockResult state;

        public BlockStateNode(BlockStateParser.BlockResult state) {
            this.state = state;
        }

        @Override
        protected void visitStart(MarkdownCompiler<?> compiler) {
            if (compiler instanceof OwoUICompiler owoCompiler) {
                owoCompiler.visitComponent(UIContainers.stack(Sizing.fill(100), Sizing.content())
                    .child(UIComponents.block(this.state.blockState(), this.state.nbt()).sizing(Sizing.fixed(48)))
                    .horizontalAlignment(HorizontalAlignment.CENTER));
            } else if (compiler instanceof BraidCompiler braidCompiler) {
                braidCompiler.visitWidget(new Align(
                    Alignment.CENTER, null, 1.0,
                    new Sized(
                        Size.square(48),
                        new BlockWidget(this.state.blockState(), this.state.nbt())
                    )
                ));
            }
        }

        @Override
        protected void visitEnd(MarkdownCompiler<?> compiler) {}
    }
}
