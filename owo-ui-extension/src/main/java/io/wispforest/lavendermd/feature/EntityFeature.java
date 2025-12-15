package io.wispforest.lavendermd.feature;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.wispforest.lavendermd.Lexer;
import io.wispforest.lavendermd.MarkdownFeature;
import io.wispforest.lavendermd.Parser;
import io.wispforest.lavendermd.compiler.BraidCompiler;
import io.wispforest.lavendermd.compiler.MarkdownCompiler;
import io.wispforest.lavendermd.compiler.OwoUICompiler;
import io.wispforest.owo.Owo;
import io.wispforest.owo.braid.core.Alignment;
import io.wispforest.owo.braid.core.Size;
import io.wispforest.owo.braid.framework.BuildContext;
import io.wispforest.owo.braid.framework.proxy.WidgetState;
import io.wispforest.owo.braid.framework.widget.StatefulWidget;
import io.wispforest.owo.braid.framework.widget.Widget;
import io.wispforest.owo.braid.widgets.basic.Align;
import io.wispforest.owo.braid.widgets.basic.Sized;
import io.wispforest.owo.braid.widgets.object.EntityWidget;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.IdentifierException;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.TagValueInput;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;

public class EntityFeature implements MarkdownFeature {

    @Override
    public String name() {
        return "entities";
    }

    @Override
    public boolean supportsCompiler(MarkdownCompiler<?> compiler) {
        return compiler instanceof OwoUICompiler || compiler instanceof BraidCompiler;
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
            } catch (CommandSyntaxException | NoSuchElementException | IdentifierException e) {
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
            if (compiler instanceof OwoUICompiler owoCompiler) {
                owoCompiler.visitComponent(UIComponents.entity(Sizing.fixed(32), this.type, this.nbt).scaleToFit(true));
            } else if (compiler instanceof BraidCompiler braidCompiler) {
                braidCompiler.visitWidget(new EntityFeatureWidget(this.type, this.nbt));
            }
        }

        @Override
        protected void visitEnd(MarkdownCompiler<?> compiler) {}
    }

    public static class EntityFeatureWidget extends StatefulWidget {

        public final EntityType<?> type;
        public final @Nullable CompoundTag nbt;

        public EntityFeatureWidget(EntityType<?> type, @Nullable CompoundTag nbt) {
            this.type = type;
            this.nbt = nbt;
        }

        @Override
        public WidgetState<EntityFeatureWidget> createState() {
            return new State();
        }

        public static class State extends WidgetState<EntityFeatureWidget> {

            private Entity entity;

            @Override
            public void init() {
                this.resetEntity();
            }

            @Override
            public void didUpdateWidget(EntityFeatureWidget oldWidget) {
                this.resetEntity();
            }

            private void resetEntity() {
                var level = Minecraft.getInstance().level;

                this.entity = this.widget().type.create(level, EntitySpawnReason.NATURAL);
                if (this.widget().nbt != null) {
                    this.entity.load(TagValueInput.create(new ProblemReporter.ScopedCollector(Owo.LOGGER), level.registryAccess(), this.widget().nbt));
                }
            }

            @Override
            public Widget build(BuildContext context) {
                return new Sized(
                    Size.square(32),
                    new EntityWidget(
                        1,
                        this.entity,
                        widget -> widget.displayMode(EntityWidget.DisplayMode.CURSOR)
                    )
                );
            }
        }
    }
}
