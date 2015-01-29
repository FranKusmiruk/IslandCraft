package com.github.hoqhuuep.islandcraft.nms.v1_5_R3;

import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.v1_5_R3.block.CraftBlock;
import org.bukkit.craftbukkit.v1_5_R3.CraftWorld;

import net.minecraft.server.v1_5_R3.BiomeBase;
import net.minecraft.server.v1_5_R3.WorldProvider;

import com.github.hoqhuuep.islandcraft.nms.BiomeGenerator;
import com.github.hoqhuuep.islandcraft.nms.NmsWrapper;

public class NmsHandler extends NmsWrapper {
    @Override
    public boolean installBiomeGenerator(final World world, final BiomeGenerator biomeGenerator) {
        if (!(world instanceof CraftWorld)) {
            // Wrong version?
            return false;
        }
        final CraftWorld craftWorld = (CraftWorld) world;
        final WorldProvider worldProvider = craftWorld.getHandle().worldProvider;
        if (worldProvider.d instanceof CustomWorldChunkManager) {
            // Already installed
            return false;
        }
        worldProvider.d = new CustomWorldChunkManager(biomeGenerator);
        return true;
    }

    @Override
    public Integer biomeIdFromName(final String worldName, final String biomeName) {
        final Biome biome = Biome.valueOf(biomeName);
        final BiomeBase biomeBase = CraftBlock.biomeToBiomeBase(biome);
        if (biomeBase == null) {
            return null;
        }
        return biomeBase.id;
    }
}
