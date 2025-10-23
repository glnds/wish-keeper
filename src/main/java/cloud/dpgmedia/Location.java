package cloud.dpgmedia;

public class Location {
    private double latitude;
    private double longitude;

    public Location(double latitude, double longitude) {
        setLatitude(latitude);
        setLongitude(longitude);
    }

    // Getters for latitude and longitude
    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    private void setLatitude(double latitude) {
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90 degrees.");
        }
        this.latitude = latitude;
    }

    private void setLongitude(double longitude) {
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180 degrees.");
        }
        this.longitude = longitude;
    }

    // Method to display the location
    @Override
    public String toString() {
        return "Location [Latitude: " + latitude + ", Longitude: " + longitude + "]";
    }

    public double distanceToNorthPole() {
        // Haversine formula to calculate distance to the North Pole (90, 0)
        final int R = 6371; // Radius of the Earth in kilometers
        double latDistance = Math.toRadians(90 - this.latitude);
        double lonDistance = Math.toRadians(0 - this.longitude);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(this.latitude)) * Math.cos(Math.toRadians(90))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // Distance in kilometers

        // londistance is
    }
}