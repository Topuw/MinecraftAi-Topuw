package com.minecraftai.core;

import java.util.Random;

public class PerlinNoise {
    private final int[] p = new int[512];

    public PerlinNoise(long seed) {
        Random rand = new Random(seed);
        int[] permutation = new int[256];
        for (int i = 0; i < 256; i++) {
            permutation[i] = i;
        }
        for (int i = 255; i > 0; i--) {
            int j = rand.nextInt(i + 1);
            int temp = permutation[i];
            permutation[i] = permutation[j];
            permutation[j] = temp;
        }
        for (int i = 0; i < 256; i++) {
            p[i] = permutation[i];
            p[256 + i] = permutation[i];
        }
    }

    public double noise(double x, double y) {
        return noise(x, y, 0.0);
    }

    public double noise(double x, double y, double z) {
        int X = (int) Math.floor(x) & 255;
        int Y = (int) Math.floor(y) & 255;
        int Z = (int) Math.floor(z) & 255;

        x -= Math.floor(x);
        y -= Math.floor(y);
        z -= Math.floor(z);

        double u = fade(x);
        double v = fade(y);
        double w = fade(z);

        int A  = p[X] + Y;
        int AA = p[A] + Z;
        int AB = p[A + 1] + Z;
        int B  = p[X + 1] + Y;
        int BA = p[B] + Z;
        int BB = p[B + 1] + Z;

        double lerp1 = lerp(u, grad(p[AA], x, y, z), grad(p[BA], x - 1, y, z));
        double lerp2 = lerp(u, grad(p[AB], x, y - 1, z), grad(p[BB], x - 1, y - 1, z));
        double lerp3 = lerp(v, lerp1, lerp2);

        double lerp4 = lerp(u, grad(p[AA + 1], x, y, z - 1), grad(p[BA + 1], x - 1, y, z - 1));
        double lerp5 = lerp(u, grad(p[AB + 1], x, y - 1, z - 1), grad(p[BB + 1], x - 1, y - 1, z - 1));
        double lerp6 = lerp(v, lerp4, lerp5);

        return lerp(w, lerp3, lerp6);
    }

    private double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    private double grad(int hash, double x, double y, double z) {
        int h = hash & 15;
        double u = h < 8 ? x : y;
        double v = h < 4 ? y : h == 12 || h == 14 ? x : z;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }
}