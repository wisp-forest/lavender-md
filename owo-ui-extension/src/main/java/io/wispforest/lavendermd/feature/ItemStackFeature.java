package io.wispforest.lavendermd.feature;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.wispforest.lavendermd.Lexer;
import io.wispforest.lavendermd.MarkdownFeature;
import io.wispforest.lavendermd.Parser;
import io.wispforest.lavendermd.compiler.BraidCompiler;
import io.wispforest.lavendermd.compiler.MarkdownCompiler;
import io.wispforest.lavendermd.compiler.OwoUICompiler;
import io.wispforest.owo.braid.widgets.basic.Tooltip;
import io.wispforest.owo.braid.widgets.object.ItemStackWidget;
import io.wispforest.owo.ui.component.ItemComponent;
import io.wispforest.owo.ui.component.UIComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemStackFeature implements MarkdownFeature {

    private final HolderLookup.Provider registries;
    public ItemStackFeature(HolderLookup.Provider registries) {
        this.registries = registries;
    }

    @Override
    public String name() {
        return "item_stacks";
    }

    @Override
    public boolean supportsCompiler(MarkdownCompiler<?> compiler) {
        return compiler instanceof OwoUICompiler || compiler instanceof BraidCompiler;
    }

    @Override
    public void registerTokens(TokenRegistrar registrar) {
        registrar.registerToken((nibbler, tokens) -> {
            if (!nibbler.tryConsume("<item;")) return false;

            var itemStackString = nibbler.consumeUntil('>');
            if (itemStackString == null) return false;

            try {
                var result = new ItemParser(this.registries).parse(new StringReader(itemStackString));

                var stack = result.item().value().getDefaultInstance();
                stack.applyComponents(result.components());

                tokens.add(new ItemStackToken(itemStackString, stack));
                return true;
            } catch (CommandSyntaxException e) {
                return false;
            }
        }, '<');
    }

    @Override
    public void registerNodes(NodeRegistrar registrar) {
        registrar.registerNode(
                (parser, stackToken, tokens) -> new ItemStackNode(stackToken.stack),
                (token, tokens) -> token instanceof ItemStackToken itemStack ? itemStack : null
        );
    }

    private static class ItemStackToken extends Lexer.Token {

        public final ItemStack stack;

        public ItemStackToken(String content, ItemStack stack) {
            super(content);
            this.stack = stack;
        }
    }

    private static class ItemStackNode extends Parser.Node {

        private final ItemStack stack;

        public ItemStackNode(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        protected void visitStart(MarkdownCompiler<?> compiler) {
            if (compiler instanceof OwoUICompiler owoCompiler) {
                owoCompiler.visitComponent(UIComponents.item(this.stack).setTooltipFromStack(true));
            } else if (compiler instanceof BraidCompiler braidCompiler) {
                var client = Minecraft.getInstance();
                braidCompiler.visitWidget(new Tooltip(
                    ItemComponent.tooltipFromItem(this.stack, Item.TooltipContext.of(client.level), client.player, null),
                    new ItemStackWidget(this.stack)
                ));
            }
        }

        @Override
        protected void visitEnd(MarkdownCompiler<?> compiler) {}
    }
}
