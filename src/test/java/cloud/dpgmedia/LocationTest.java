package cloud.dpgmedia;

import junit.framework.TestCase;

public class LocationTest extends TestCase {
    public void testDistanceToNorthPole() {
        Location location = new Location(45.0, 0.0); // Example location
        double distance = location.distanceToNorthPole();
        // The expected distance is approximately 5003 km
        assertEquals(5003, Math.round(distance), 1);
    }

    public void testDistanceToNorthPoleLongitudeIsIrrelevant() {
        Location location1 = new Location(45.0, 10.0); // Example location
        Location location2 = new Location(45.0, 25.0); // Same latitude, different longitude
        double distance1 = location1.distanceToNorthPole();
        double distance2 = location2.distanceToNorthPole();
        assertEquals(Math.round(distance1), Math.round(distance2));
    }
}
