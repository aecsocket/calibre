package me.aecsocket.calibre.defaults.service.bukkit.spread;

import org.bukkit.util.Vector;

import java.util.Random;

/**
 * Default implementation of {@link CalibreSpreadService}.
 */
public class CalibreSpreadProvider implements CalibreSpreadService {
    private final Random rng = new Random();

    public Random getRng() { return rng; }

    @Override
    public Vector applySpread(Vector direction, double spread) {
        direction.rotateAroundX(Math.toRadians(getValue(spread)));
        direction.rotateAroundY(Math.toRadians(getValue(spread)));
        direction.rotateAroundZ(Math.toRadians(getValue(spread)));
        return direction;
    }

    public double getValue(double spread) { return rng.nextGaussian() * spread; }
}
