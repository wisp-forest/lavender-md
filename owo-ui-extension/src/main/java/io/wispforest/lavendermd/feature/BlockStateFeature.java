package io.wispforest.lavendermd.feature;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.wispforest.lavendermd.Lexer;
import io.wispforest.lavendermd.MarkdownFeature;
import io.wispforest.lavendermd.Parser;
import io.wispforest.lavendermd.compiler.MarkdownCompiler;
import io.wispforest.lavendermd.compiler.OwoUICompiler;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.registry.Registries;

public class BlockStateFeature implements MarkdownFeature {

    @Override
    public String name() {
        return "block_states";
    }

    @Override
    public boolean supportsCompiler(MarkdownCompiler<?> compiler) {
        return compiler instanceof OwoUICompiler;
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
                        BlockArgumentParser.block(Registries.BLOCK.getReadOnlyWrapper(), blockStateString, true)
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

        public final BlockArgumentParser.BlockResult state;

        public BlockStateToken(String content, BlockArgumentParser.BlockResult state) {
            super(content);
            this.state = state;
        }
    }

    private static class BlockStateNode extends Parser.Node {

        private final BlockArgumentParser.BlockResult state;

        public BlockStateNode(BlockArgumentParser.BlockResult state) {
            this.state = state;
        }

        @Override
        protected void visitStart(MarkdownCompiler<?> compiler) {
            ((OwoUICompiler) compiler).visitComponent(
                    Containers.stack(Sizing.fill(100), Sizing.content())
                            .child(Components.block(this.state.blockState(), this.state.nbt()).sizing(Sizing.fixed(48)))
                            .horizontalAlignment(HorizontalAlignment.CENTER)
            );
        }

        @Override
        protected void visitEnd(MarkdownCompiler<?> compiler) {}
    }
}
