package io.wispforest.lavendermd.feature;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.wispforest.lavendermd.Lexer;
import io.wispforest.lavendermd.MarkdownFeature;
import io.wispforest.lavendermd.Parser;
import io.wispforest.lavendermd.compiler.MarkdownCompiler;
import io.wispforest.lavendermd.compiler.OwoUICompiler;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;

public class EntityFeature implements MarkdownFeature {

    @Override
    public String name() {
        return "entities";
    }

    @Override
    public boolean supportsCompiler(MarkdownCompiler<?> compiler) {
        return compiler instanceof OwoUICompiler;
    }

    @Override
    public void registerTokens(TokenRegistrar registrar) {
        registrar.registerToken((nibbler, tokens) -> {
            if (!nibbler.tryConsume("<entity;")) return false;

            var entityString = nibbler.consumeUntil('>');
            if (entityString == null) return false;

            try {
                CompoundTag nbt = null;

                int nbtIndex = entityString.indexOf('{');
                if (nbtIndex != -1) {

                    nbt = TagParser.parseCompoundAsArgument(new StringReader(entityString.substring(nbtIndex)));
                    entityString = entityString.substring(0, nbtIndex);
                }

                var entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(entityString)).orElseThrow();
                tokens.add(new EntityToken(entityString, entityType, nbt));
                return true;
            } catch (CommandSyntaxException | NoSuchElementException e) {
                return false;
            }
        }, '<');
    }

    @Override
    public void registerNodes(NodeRegistrar registrar) {
        registrar.registerNode(
                (parser, entityToken, tokens) -> new EntityNode(entityToken.type, entityToken.nbt),
                (token, tokens) -> token instanceof EntityToken entity ? entity : null
        );
    }

    private static class EntityToken extends Lexer.Token {

        public final EntityType<?> type;
        public final @Nullable CompoundTag nbt;

        public EntityToken(String content, EntityType<?> type, @Nullable CompoundTag nbt) {
            super(content);
            this.type = type;
            this.nbt = nbt;
        }
    }

    private static class EntityNode extends Parser.Node {

        public final EntityType<?> type;
        public final @Nullable CompoundTag nbt;

        public EntityNode(EntityType<?> type, @Nullable CompoundTag nbt) {
            this.type = type;
            this.nbt = nbt;
        }

        @Override
        protected void visitStart(MarkdownCompiler<?> compiler) {
            ((OwoUICompiler) compiler).visitComponent(UIComponents.entity(Sizing.fixed(32), this.type, this.nbt).scaleToFit(true));
        }

        @Override
        protected void visitEnd(MarkdownCompiler<?> compiler) {}
    }
}
