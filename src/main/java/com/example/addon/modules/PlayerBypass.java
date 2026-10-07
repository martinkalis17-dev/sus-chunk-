package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Player Bypass : détecte les joueurs situés sous une certaine hauteur
 * (par défaut sous la couche 0) et vous prévient.
 *
 * Limite : ça ne détecte que les joueurs que le serveur vous envoie
 * (ceux dans votre distance d'entités). Ça ne révèle rien de plus.
 */
public class PlayerBypass extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Rendu");

    private final Setting<Integer> belowY = sgGeneral.add(new IntSetting.Builder()
        .name("sous-y")
        .description("Un joueur dont le Y est strictement inférieur à cette valeur est détecté.")
        .defaultValue(0).sliderRange(-64, 64).build());

    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("ignorer-amis")
        .description("Ne détecte pas les joueurs de votre liste d'amis Meteor.")
        .defaultValue(true).build());

    private final Setting<Boolean> chat = sgGeneral.add(new BoolSetting.Builder()
        .name("message-chat")
        .description("Écrit un message dans le chat à la détection.")
        .defaultValue(true).build());

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("surbrillance")
        .description("Entoure les joueurs détectés d'une boîte.")
        .defaultValue(true).build());

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("mode-forme").defaultValue(ShapeMode.Both).build());

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("couleur-faces").defaultValue(new SettingColor(255, 170, 0, 40)).build());

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("couleur-lignes").defaultValue(new SettingColor(255, 170, 0, 255)).build());

    private final Set<UUID> alerted = new HashSet<>();
    private final List<Player> detected = new ArrayList<>();

    public PlayerBypass() {
        super(AddonTemplate.CATEGORY, "player-bypass",
            "Détecte les joueurs situés sous la couche 0 (ou une hauteur choisie).");
    }

    @Override
    public void onDeactivate() {
        alerted.clear();
        detected.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.level == null || mc.player == null) return;

        detected.clear();
        Set<UUID> stillBelow = new HashSet<>();

        for (Player p : mc.level.players()) {
            if (p == mc.player) continue;
            if (ignoreFriends.get() && Friends.get().isFriend(p)) continue;
            if (p.getY() >= belowY.get()) continue;

            detected.add(p);
            stillBelow.add(p.getUUID());

            if (alerted.add(p.getUUID()) && chat.get()) {
                ChatUtils.warning("%s est sous la couche %d (Y=%d)",
                    p.getName().getString(), belowY.get(), p.getBlockY());
            }
        }

        // Permet de ré-alerter si le joueur remonte puis redescend.
        alerted.retainAll(stillBelow);
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get()) return;
        for (Player p : detected) {
            AABB b = p.getBoundingBox();
            event.renderer.box(b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ,
                sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }
    }
}
