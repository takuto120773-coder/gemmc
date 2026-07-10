/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.comphenix.protocol.PacketType$Play$Server
 *  com.comphenix.protocol.ProtocolLibrary
 *  com.comphenix.protocol.events.PacketContainer
 *  com.comphenix.protocol.wrappers.WrappedChatComponent
 *  org.bukkit.entity.Player
 */
package com.example.smp;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Optional;
import org.bukkit.entity.Player;

public class TeamPacketHelper {
    private static Constructor<?> playerTeamConstructor;
    private static Constructor<?> parametersConstructor;
    private static Object dummyScoreboard;
    private static Method setDisplayName;
    private static Method setPlayerPrefix;
    private static Method setPlayerSuffix;
    private static Method setAllowFriendlyFire;
    private static Method setSeeFriendlyInvisibles;

    public static void sendTeamPacket(Player player, String teamName, String prefix, int mode, Collection<String> entries) {
        try {
            PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SCOREBOARD_TEAM);
            packet.getStrings().write(0, teamName);
            packet.getIntegers().write(0, mode);
            if (mode == 0 || mode == 2) {
                Object team = playerTeamConstructor.newInstance(dummyScoreboard, teamName);
                
                net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer legacy = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection();
                net.kyori.adventure.text.serializer.gson.GsonComponentSerializer gson = net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson();
                
                String displayNameJson = gson.serialize(legacy.deserialize(teamName));
                String prefixJson = gson.serialize(legacy.deserialize(prefix));
                String suffixJson = gson.serialize(legacy.deserialize(""));
                
                setDisplayName.invoke(team, WrappedChatComponent.fromJson(displayNameJson).getHandle());
                setPlayerPrefix.invoke(team, WrappedChatComponent.fromJson(prefixJson).getHandle());
                setPlayerSuffix.invoke(team, WrappedChatComponent.fromJson(suffixJson).getHandle());
                setAllowFriendlyFire.invoke(team, false);
                setSeeFriendlyInvisibles.invoke(team, false);
                Object parameters = parametersConstructor.newInstance(team);
                // parameters は Optional<Parameters> 型のフィールド。型を指定して書き込む
                packet.getModifier().withType(Optional.class).write(0, Optional.of(parameters));
            }
            // エントリ(プレイヤー名)は Collection<String> 型のフィールド。
            // size()-1 で書くと parameters(Optional) に当たってしまうので、型で指定する
            if (entries != null && !entries.isEmpty()) {
                packet.getModifier().withType(Collection.class).write(0, entries);
            }
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    static {
        try {
            Class<?> componentClass = Class.forName("net.minecraft.network.chat.Component");
            Class<?> scoreboardClass = Class.forName("net.minecraft.world.scores.Scoreboard");
            Class<?> playerTeamClass = Class.forName("net.minecraft.world.scores.PlayerTeam");
            Class<?> parametersClass = Class.forName("net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket$Parameters");
            dummyScoreboard = scoreboardClass.getConstructor(new Class[0]).newInstance(new Object[0]);
            playerTeamConstructor = playerTeamClass.getConstructor(scoreboardClass, String.class);
            parametersConstructor = parametersClass.getConstructor(playerTeamClass);
            setDisplayName = playerTeamClass.getMethod("setDisplayName", componentClass);
            setPlayerPrefix = playerTeamClass.getMethod("setPlayerPrefix", componentClass);
            setPlayerSuffix = playerTeamClass.getMethod("setPlayerSuffix", componentClass);
            setAllowFriendlyFire = playerTeamClass.getMethod("setAllowFriendlyFire", Boolean.TYPE);
            setSeeFriendlyInvisibles = playerTeamClass.getMethod("setSeeFriendlyInvisibles", Boolean.TYPE);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

