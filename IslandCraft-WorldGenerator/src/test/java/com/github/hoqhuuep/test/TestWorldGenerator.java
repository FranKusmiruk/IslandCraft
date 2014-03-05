package com.github.hoqhuuep.test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.github.hoqhuuep.islandcraft.worldgenerator.Biome;
import com.github.hoqhuuep.islandcraft.worldgenerator.WorldGenerator;
import com.github.hoqhuuep.islandcraft.worldgenerator.hack.CustomWorldChunkManager;

import net.minecraft.server.v1_7_R1.WorldChunkManager;

public class TestWorldGenerator {
	private static final int WIDTH = 1920 * 4;
	private static final int HEIGHT = 1080 * 4;

	public static void main(final String[] args) throws IOException {
		final WorldChunkManager worldChunkManager = new CustomWorldChunkManager(new WorldGenerator(0, 288, 320, Biome.DEEP_OCEAN.getId()));
		final BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
		for (int j = 0; j < HEIGHT; ++j) {
			for (int i = 0; i < WIDTH; ++i) {
				image.setRGB(i, j, Biome.fromId[worldChunkManager.getBiome(i - WIDTH / 2, j - HEIGHT / 2).id].getColor().getRGB());
			}
			if (j % 120 == 0) {
				System.out.println("LINE: " + j);
			}
		}
		ImageIO.write(image, "png", new File("target/world.png"));
		System.out.println("DONE");
	}
}
