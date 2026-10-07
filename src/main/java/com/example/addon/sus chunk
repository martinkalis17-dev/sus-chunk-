package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.entity.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Sus Chunk Finder : repère les chunks chargés qui contiennent beaucoup de
 * blocs de stockage (coffres, shulkers, barils, entonnoirs...), ce qui est
 * anormal dans un terrain naturel et typique d'une base ou d'un stash.
 */
public class SusChunkFinder extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Rendu");

    private final Setting<Integer> minStorage = sgGeneral.add(new IntSetting.Builder()
        .name("min-stockage")
        .description("Nombre minimum de blocs de stockage dans un chunk pour le marquer suspect.")
        .defaultValue(4).min(1).sliderMax(30).build());

    private final Setting<Integer> radius = sgGeneral.add(new IntSetting.Builder()
        .name("rayon-chunks")
        .description("Rayon de scan autour de vous, en chunks.")
        .defaultValue(12).min(2).sliderMax(32).build());

    private final Setting<Integer> interval = sgGeneral.add(new IntSetting.Builder()
        .name("intervalle-ticks")
        .description("Délai entre deux scans (20 ticks = 1 seconde).")
        .defaultValue(40).min(5).sliderMax(200).build());

    private final Setting<Boolean> chat = sgGeneral.add(new BoolSetting.Builder()
        .name("message-chat")
        .description("Écrit les coordonnées dans le chat quand un nouveau chunk suspect est trouvé.")
        .defaultValue(true).build());

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("mode-forme").defaultValue(ShapeMode.Both).build());

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("couleur-faces").defaultValue(new SettingColor(255, 80, 80, 40)).build());

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("couleur-lignes").defaultValue(new SettingColor(255, 80, 80, 255)).build());

    private record Sus(int count, int minY, int maxY) {}

    private final Map<ChunkPos, Sus> flagged = new HashMap<>();
    private final Set<ChunkPos> notified = new HashSet<>();
    private int timer = 0;

    public SusChunkFinder() {
        super(AddonTemplate.CATEGORY, "sus-chunk-finder",
            "Repère les chunks suspects (beaucoup de coffres, shulkers, etc.).");
    }

    @Override
    public void onDeactivate() {
        flagged.clear();
        notified.clear();
        timer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.world == null || mc.player == null) return;
        if (++timer < interval.get()) return;
        timer = 0;

        flagged.clear();
        int pcx = mc.player.getChunkPos().x;
        int pcz = mc.player.getChunkPos().z;
        int r = radius.get();

        for (int cx = pcx - r; cx <= pcx + r; cx++) {
            for (int cz = pcz - r; cz <= pcz + r; cz++) {
                Chunk c = mc.world.getChunk(cx, cz, ChunkStatus.FULL, false);
                if (!(c instanceof WorldChunk chunk)) continue;

                int count = 0;
                int minY = Integer.MAX_VALUE;
                int maxY = Integer.MIN_VALUE;

                for (Map.Entry<BlockPos, BlockEntity> e : chunk.getBlockEntities().entrySet()) {
                    if (!isStorage(e.getValue())) continue;
                    count++;
                    minY = Math.min(minY, e.getKey().getY());
                    maxY = Math.max(maxY, e.getKey().getY());
                }

                if (count >= minStorage.get()) {
                    ChunkPos cp = chunk.getPos();
                    flagged.put(cp, new Sus(count, minY, maxY));
                    if (chat.get() && notified.add(cp)) {
                        ChatUtils.info("Chunk suspect (%d stockages) vers x=%d z=%d",
                            count, cp.getStartX(), cp.getStartZ());
                    }
                }
            }
        }
    }

    private boolean isStorage(BlockEntity be) {
        return be instanceof ChestBlockEntity
            || be instanceof EnderChestBlockEntity
            || be instanceof BarrelBlockEntity
            || be instanceof ShulkerBoxBlockEntity
            || be instanceof HopperBlockEntity
            || be instanceof DispenserBlockEntity
            || be instanceof FurnaceBlockEntity;
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        for (Map.Entry<ChunkPos, Sus> e : flagged.entrySet()) {
            ChunkPos cp = e.getKey();
            Sus s = e.getValue();
            event.renderer.box(
                cp.getStartX(), s.minY(), cp.getStartZ(),
                cp.getStartX() + 16, s.maxY() + 1, cp.getStartZ() + 16,
                sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }
    }
}
