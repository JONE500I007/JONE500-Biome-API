package net.biome.api.impl;

/**
 * Splits the world into region patches ("cells") and decides which region each patch uses.
 *
 * <p>Pure function of (seed, weights, size, x, z): no global state, no load-order dependency.
 * Coordinates are quart positions (block / 4), the same unit biome lookups use.
 *
 * <p>Internal class, not part of the API.
 */
public final class RegionLayout {
	/** Region index used for vanilla patches. */
	public static final int VANILLA = -1;

	private static final long SALT_REGION = 0x5DEECE66DL;
	private static final long SALT_JITTER_X = 0x2545F4914F6CDD1DL;
	private static final long SALT_JITTER_Z = 0x7FB5D329728EA185L;
	private static final long SALT_WARP_X = 0x1B873593L;
	private static final long SALT_WARP_Z = 0xCC9E2D51L;

	private final long seed;
	private final int cellSize;
	private final float[] cumulative;
	private final float totalWeight;
	private final ThreadLocal<CellCache> cache = ThreadLocal.withInitial(CellCache::new);

	/**
	 * @param seed          world seed
	 * @param regionSize    patch size as power of two in quarts (7 = 128 quarts = 512 blocks)
	 * @param vanillaWeight weight of vanilla patches
	 * @param regionWeights weights of the regions, in region index order (sorted by id)
	 */
	public RegionLayout(long seed, int regionSize, float vanillaWeight, float[] regionWeights) {
		this.seed = seed;
		this.cellSize = 1 << regionSize;
		this.cumulative = new float[regionWeights.length + 1];
		float sum = vanillaWeight;
		this.cumulative[0] = sum;
		for (int i = 0; i < regionWeights.length; i++) {
			sum += regionWeights[i];
			this.cumulative[i + 1] = sum;
		}
		this.totalWeight = sum;
	}

	public int regionCount() {
		return cumulative.length - 1;
	}

	/** The patch at a quart position. */
	public Cell cellAt(int quartX, int quartZ) {
		CellCache c = cache.get();
		if (c.cell != null && c.x == quartX && c.z == quartZ) {
			return c.cell;
		}
		Cell cell = computeCell(quartX, quartZ);
		c.x = quartX;
		c.z = quartZ;
		c.cell = cell;
		return cell;
	}

	private Cell computeCell(int quartX, int quartZ) {
		// Warp the coordinates so patch borders are wavy instead of straight lines.
		double wx = quartX + warp(SALT_WARP_X, quartX, quartZ);
		double wz = quartZ + warp(SALT_WARP_Z, quartX, quartZ);

		int baseX = (int) Math.floor(wx / cellSize);
		int baseZ = (int) Math.floor(wz / cellSize);

		// Jittered grid (Voronoi): every cell has one point at a random place; the nearest point wins.
		int bestX = baseX;
		int bestZ = baseZ;
		double bestDist = Double.MAX_VALUE;
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				int cx = baseX + dx;
				int cz = baseZ + dz;
				double px = ((double) cx + 0.15 + 0.7 * unit(hash(cx, cz, SALT_JITTER_X))) * cellSize;
				double pz = ((double) cz + 0.15 + 0.7 * unit(hash(cx, cz, SALT_JITTER_Z))) * cellSize;
				double d = (px - wx) * (px - wx) + (pz - wz) * (pz - wz);
				if (d < bestDist) {
					bestDist = d;
					bestX = cx;
					bestZ = cz;
				}
			}
		}
		return new Cell(bestX, bestZ, pickRegion(bestX, bestZ));
	}

	/** Which region (or {@link #VANILLA}) a cell uses. */
	public int pickRegion(int cellX, int cellZ) {
		if (cumulative.length == 1 || totalWeight <= 0.0F) {
			return VANILLA;
		}
		float roll = (float) (unit(hash(cellX, cellZ, SALT_REGION)) * totalWeight);
		if (roll < cumulative[0]) {
			return VANILLA;
		}
		for (int i = 1; i < cumulative.length; i++) {
			if (roll < cumulative[i]) {
				return i - 1;
			}
		}
		return cumulative.length - 2;
	}

	/** True in {@code chance} (0..1) of the cells, independently for every {@code salt}. */
	public boolean roll(Cell cell, long salt, float chance) {
		return chance >= 1.0F || unit(hash(cell.x(), cell.z(), salt)) < chance;
	}

	/** A number from 0 (inclusive) to 1 (exclusive) for a cell, independently for every {@code salt}. */
	public double random(Cell cell, long salt) {
		return unit(hash(cell.x(), cell.z(), salt));
	}

	/** Picks 0..size-1 for a cell, independently for every {@code salt}. */
	public int pick(Cell cell, long salt, int size) {
		return (int) Math.floorMod(hash(cell.x(), cell.z(), salt), (long) size);
	}

	// ------------------------------------------------------------------------------------------
	// Noise / hashing
	// ------------------------------------------------------------------------------------------

	/** Two octaves of value noise, in quarts. */
	private double warp(long salt, int x, int z) {
		double big = valueNoise(salt, x, z, cellSize / 2) * cellSize * 0.35;
		double small = valueNoise(salt ^ 0x9E3779B97F4A7C15L, x, z, Math.max(4, cellSize / 8)) * cellSize * 0.08;
		return big + small;
	}

	/** Smooth value noise in -1..1 with the given lattice step. */
	private double valueNoise(long salt, int x, int z, int step) {
		int gx = Math.floorDiv(x, step);
		int gz = Math.floorDiv(z, step);
		double fx = (double) (x - gx * step) / step;
		double fz = (double) (z - gz * step) / step;
		double sx = fx * fx * (3 - 2 * fx);
		double sz = fz * fz * (3 - 2 * fz);
		double v00 = signed(hash(gx, gz, salt));
		double v10 = signed(hash(gx + 1, gz, salt));
		double v01 = signed(hash(gx, gz + 1, salt));
		double v11 = signed(hash(gx + 1, gz + 1, salt));
		double a = v00 + (v10 - v00) * sx;
		double b = v01 + (v11 - v01) * sx;
		return a + (b - a) * sz;
	}

	private long hash(int x, int z, long salt) {
		long h = seed ^ salt;
		h = mix(h ^ (x * 0x9E3779B97F4A7C15L));
		h = mix(h ^ (z * 0xC2B2AE3D27D4EB4FL));
		return h;
	}

	/** SplitMix64 finalizer. */
	static long mix(long z) {
		z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
		z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
		return z ^ (z >>> 31);
	}

	/** 0..1 */
	private static double unit(long h) {
		return (h >>> 11) * 0x1.0p-53;
	}

	/** -1..1 */
	private static double signed(long h) {
		return unit(h) * 2.0 - 1.0;
	}

	/** Stable salt for a string (String.hashCode is the same on every JVM). */
	public static long salt(String s) {
		return mix(s.hashCode() * 0x632BE59BD9B4E019L + s.length());
	}

	/**
	 * One patch of the world.
	 *
	 * @param x      cell x (identifies the patch)
	 * @param z      cell z
	 * @param region region index, or {@link #VANILLA}
	 */
	public record Cell(int x, int z, int region) {
	}

	private static final class CellCache {
		int x;
		int z;
		Cell cell;
	}
}
