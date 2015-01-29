package com.github.hoqhuuep.islandcraft.core;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import org.apache.commons.lang.StringUtils;
import org.bukkit.util.noise.OctaveGenerator;
import org.bukkit.util.noise.SimplexOctaveGenerator;

import com.github.hoqhuuep.islandcraft.api.IslandGenerator;
import com.github.hoqhuuep.islandcraft.core.mosaic.Poisson;
import com.github.hoqhuuep.islandcraft.core.mosaic.Site;

public class IslandGeneratorAlpha implements IslandGenerator {
    private static final double MIN_DISTANCE = 8;
    private static final double NOISE = 2.7;
    private static final double CIRCLE = 2;
    private static final double SQUARE = 0;
    private static final double THRESHOLD = 2;
    private final Color ocean;
    private final Color normal;
    private final Color mountains;
    private final Color hills;
    private final Color hillsMountains;
    private final Color forest;
    private final Color forestMountains;
    private final Color outerCoast;
    private final Color innerCoast;

    // private final Color river; // unused for now

    public IslandGeneratorAlpha(final String worldName, final String[] args) {
        ICLogger.logger.info("Creating IslandGeneratorAlpha with args: " + StringUtils.join(args, " "));
        if (args.length != 9) {
            ICLogger.logger.severe("IslandGeneratorAlpha requrires 9 parameters, " + args.length + " given");
            throw new IllegalArgumentException("IslandGeneratorAlpha requrires 9 parameters");
        }
        ocean = new Color(-1, true);
        normal = biomeColor(worldName, args[0], ocean);
        mountains = biomeColor(worldName, args[1], normal);
        hills = biomeColor(worldName, args[2], normal);
        hillsMountains = biomeColor(worldName, args[3], hills);
        forest = biomeColor(worldName, args[4], normal);
        forestMountains = biomeColor(worldName, args[5], forest);
        outerCoast = biomeColor(worldName, args[6], normal);
        innerCoast = biomeColor(worldName, args[7], normal);
        // river = biomeColor(args[8], normal); // unused for now
    }

    @Override
    public int[] generate(final int xSize, final int zSize, final long islandSeed) {
        ICLogger.logger.info(String.format("Generating island from IslandGeneratorAlpha with xSize: %d, zSize: %d, islandSeed: %d, biome: %d", xSize, zSize, islandSeed, normal.getRGB()));
        final Poisson poisson = new Poisson(xSize, zSize, MIN_DISTANCE);
        final List<Site> sites = poisson.generate(new Random(islandSeed));
        final SimplexOctaveGenerator shapeNoise = new SimplexOctaveGenerator(islandSeed, 2);
        final SimplexOctaveGenerator hillsNoise = new SimplexOctaveGenerator(islandSeed + 1, 2);
        final SimplexOctaveGenerator forestNoise = new SimplexOctaveGenerator(islandSeed + 2, 2);
        final SimplexOctaveGenerator mountainsNoise = new SimplexOctaveGenerator(islandSeed + 3, 2);
        // Find borders
        final Queue<Site> oceanSites = new LinkedList<Site>();
        for (final Site site : sites) {
            if (site.polygon == null) {
                site.isOcean = true;
                oceanSites.add(site);
            }
        }
        final List<Site> suspectCoast = new ArrayList<Site>();
        final List<Site> coast = new ArrayList<Site>();
        // Find oceans and coasts
        while (!oceanSites.isEmpty()) {
            final Site site = oceanSites.remove();
            for (final Site neighbor : site.neighbors) {
                if (site.polygon == null) {
                    if (!neighbor.isOcean) {
                        neighbor.isOcean = true;
                        oceanSites.add(neighbor);
                    }
                } else {
                    final double dx = (double) (neighbor.x - (xSize / 2)) / (double) (xSize / 2);
                    final double dz = (double) (neighbor.z - (zSize / 2)) / (double) (zSize / 2);
                    if (NOISE * noise(dx, dz, shapeNoise) + CIRCLE * circle(dx, dz) + SQUARE * square(dx, dz) > THRESHOLD) {
                        if (!neighbor.isOcean) {
                            neighbor.isOcean = true;
                            oceanSites.add(neighbor);
                        }
                    } else {
                        neighbor.isInnerCoast = true;
                        suspectCoast.add(neighbor);
                    }
                }
            }
        }
        // Create coast
        SITE: for (final Site site : suspectCoast) {
            for (final Site neighbor : site.neighbors) {
                if (!neighbor.isOcean && !neighbor.isInnerCoast) {
                    coast.add(site);
                    continue SITE;
                }
            }
            site.isInnerCoast = false;
            site.isOcean = true;
        }
        // Create shallow ocean
        for (final Site site : coast) {
            for (final Site neighbor : site.neighbors) {
                if (neighbor.isOcean) {
                    neighbor.isOcean = false;
                    neighbor.isOuterCoast = true;
                }
            }
        }
        // Create blank image
        final BufferedImage image = new BufferedImage(xSize, zSize, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D graphics = image.createGraphics();
        graphics.setComposite(AlphaComposite.Src);
        graphics.setBackground(ocean);
        graphics.clearRect(0, 0, xSize, zSize);
        // Render island
        for (final Site site : sites) {
            if (site.isOcean) {
                continue;
            } else if (site.isOuterCoast) {
                graphics.setColor(outerCoast);
            } else if (site.isInnerCoast) {
                graphics.setColor(innerCoast);
            } else if (noise(site, 0.375, 160.0, mountainsNoise)) {
                if (noise(site, 0.375, 80.0, hillsNoise)) {
                    graphics.setColor(hillsMountains);
                } else if (noise(site, 0.375, 160.0, forestNoise)) {
                    graphics.setColor(forestMountains);
                } else {
                    graphics.setColor(mountains);
                }
            } else {
                if (noise(site, 0.375, 80.0, hillsNoise)) {
                    graphics.setColor(hills);
                } else if (noise(site, 0.375, 160.0, forestNoise)) {
                    graphics.setColor(forest);
                } else {
                    graphics.setColor(normal);
                }
            }
            graphics.fillPolygon(site.polygon);
            graphics.drawPolygon(site.polygon);
        }
        // Save result
        graphics.dispose();
        final int[] result = new int[xSize * zSize];
        for (int i = 0; i < result.length; ++i) {
            final int x = i % xSize;
            final int z = i / xSize;
            result[i] = image.getRGB(x, z);
        }
        return result;
    }

    private static Color biomeColor(final String worldName, final String name, final Color backup) {
        if (name.equals("~")) {
            return backup;
        }
        return new Color(ICBiome.biomeIdFromName(worldName, name), true);
    }

    private static boolean noise(final Site site, final double threshold, final double period, final OctaveGenerator octaveGenerator) {
        return octaveGenerator.noise(site.x / period, site.z / period, 2.0, 0.5, true) / 2.0 + 0.5 < threshold;
    }

    private static double noise(final double dx, final double dz, final OctaveGenerator octaveGenerator) {
        return octaveGenerator.noise(dx, dz, 2.0, 0.5, true) / 2.0 + 0.5;
    }

    private static double circle(final double dx, final double dz) {
        return (dx * dx + dz * dz) / 2;
    }

    private static double square(final double dx, final double dz) {
        return Math.max(Math.abs(dx), Math.abs(dz));
    }
}
