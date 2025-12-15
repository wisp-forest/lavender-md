package io.wispforest.lavendermdtest;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.wispforest.lavendermd.MarkdownProcessor;
import io.wispforest.owo.braid.core.BraidScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class LavenderMdTest implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(literal("parse-md").then(argument("md", StringArgumentType.greedyString()).executes(context -> {
                context.getSource().sendFeedback(MarkdownProcessor.text().process(StringArgumentType.getString(context, "md")));
                return 0;
            })));

            dispatcher.register(literal("edit-md").executes(context -> {
                Minecraft.getInstance().setScreen(new EditMdScreen());
                return 0;
            }));

            dispatcher.register(literal("edit-md-braid").executes(context -> {
                Minecraft.getInstance().schedule(() -> {
                    Minecraft.getInstance().setScreen(new BraidScreen(new BraidEditMdWidget()));
                });
                return 0;
            }));
        });
    }
}
