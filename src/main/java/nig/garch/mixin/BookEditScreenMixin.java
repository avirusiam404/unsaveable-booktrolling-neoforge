package nig.garch.mixin;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(BookEditScreen.class)
public abstract class BookEditScreenMixin extends Screen {

    @Shadow(remap = false) @Final private List<String> pages;
    @Shadow(remap = false) private String title;
    @Shadow(remap = false) private boolean isModified;
    @Shadow(remap = false) @Final private InteractionHand hand;

    @Shadow(remap = false) protected abstract void saveChanges(boolean publish);

    protected BookEditScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"), remap = false)
    private void booktrolling$addUnsaveableButtons(CallbackInfo ci) {
        // "unsaveable" button — signs the book with a 33-char title, stays in inventory
        this.addRenderableWidget(
            Button.builder(
                Component.literal("inv"),
                (button) -> {
                    this.pages.clear();
                    this.pages.add("");
                    this.title = "123456789012345678901234567890123";
                    this.isModified = true;
                    this.saveChanges(true);
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(null);
                    }
                }
            ).bounds(4, 4, 120, 20).build()
        );

        // "unsaveable + drop" button — same, but drops the signed book instead of keeping it
        this.addRenderableWidget(
            Button.builder(
                Component.literal("drop"),
                (button) -> {
                    this.pages.clear();
                    this.pages.add("");
                    this.title = "123456789012345678901234567890123";
                    this.isModified = true;
                    this.saveChanges(true);

                    // Now drop the newly-signed book from the player's inventory.
                    // The server processes the sign packet on the next tick, so we
                    // schedule the drop one tick later.
                    if (this.minecraft != null && this.minecraft.player != null) {
                        final var player = this.minecraft.player;
                        final var connection = this.minecraft.getConnection();

                        // Wait 1 tick for the server to swap the writable book for a written one
                        net.minecraft.Util.ioPool().execute(() -> {}); // no-op to keep imports clean
                        this.minecraft.execute(() -> {
                            // Send a drop-item packet for the current hotbar slot
                            // (the book was in main hand when the screen opened)
                            int slot = player.getInventory().selected;
                            connection.send(new net.minecraft.network.protocol.game.ServerboundPlayerActionPacket(
                                net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.DROP_ITEM,
                                net.minecraft.core.BlockPos.ZERO,
                                net.minecraft.core.Direction.DOWN
                            ));
                        });
                    }

                    if (this.minecraft != null) {
                        this.minecraft.setScreen(null);
                    }
                }
            ).bounds(128, 4, 120, 20).build()
        );
    }
}
