package pt.tecnico.sauron.silo.domain;

import java.time.Instant;
import java.util.Objects;

public class PersistentObservation implements Comparable<PersistentObservation> {
    private final Instant timestamp;
    private final PersistentCamera camera;

    public PersistentObservation(Instant timestamp, PersistentCamera camera) {
        this.timestamp = timestamp;
        this.camera = camera;
    }

    public Instant getTimestamp() {
        return this.timestamp;
    }

    public PersistentCamera getCamera() {
        return this.camera;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PersistentObservation)) {
            return false;
        }
        PersistentObservation ob = (PersistentObservation) o;
        return this.getCamera() == ob.getCamera() && this.getTimestamp() == ob.getTimestamp();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getCamera(), this.getTimestamp());
    }

    @Override
    public int compareTo(PersistentObservation o) {
        return o.getTimestamp().compareTo(this.getTimestamp());
    }

    @Override
    public String toString() {
        return this.camera.toString() + "," + this.timestamp.toString();
    }
}