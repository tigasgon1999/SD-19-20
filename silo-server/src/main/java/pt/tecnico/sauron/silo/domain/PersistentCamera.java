package pt.tecnico.sauron.silo.domain;

import java.util.Objects;

public class PersistentCamera  implements GossipTarget{
    private final String name;
    private final float latitude;
    private final float longitude;

    public PersistentCamera(String name, float latitude, float longitude) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getName() {
        return this.name;
    }

    public float getLatitude() {
        return this.latitude;
    }

    public float getLongitude() {
        return this.longitude;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PersistentCamera)) {
            return false;
        }
        return ((PersistentCamera) o).getName().equals(this.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return this.name + "," + String.valueOf(this.latitude) + "," + String.valueOf(this.latitude);
    }
}