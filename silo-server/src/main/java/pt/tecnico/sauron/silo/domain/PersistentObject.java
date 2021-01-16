package pt.tecnico.sauron.silo.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class PersistentObject implements GossipTarget {
    public enum ObjectType {
        PERSON, CAR
    }

    private List<PersistentObservation> observations = new ArrayList<>();
    private final String id;
    private final ObjectType type;

    public PersistentObject(String id, ObjectType type) {
        this.id = id;
        this.type = type;
    }

    public PersistentObject(String id, ObjectType type, PersistentObservation obs) {
        this.id = id;
        this.type = type;
        addObservation(obs);
    }

    public PersistentObject(String id, ObjectType type, List<PersistentObservation> obs) {
        this.id = id;
        this.type = type;
        addObservation(obs);
    }

    public synchronized void addObservation(PersistentObservation obs) {
        observations.add(obs);
    }

    public synchronized void addObservation(List<PersistentObservation> obs) {
        observations.addAll(obs);
    }

    public String getId() {
        return this.id;
    }

    public ObjectType getType() {
        return this.type;
    }

    public synchronized List<PersistentObservation> getObservations() {
        Collections.sort(this.observations);
        return this.observations;
    }

    public synchronized PersistentObservation getLatestObservation() {
        if (this.observations.isEmpty()) {
            return null;
        }
        Collections.sort(this.observations);
        return this.observations.get(0);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PersistentObject))
            return false;
        else {
            PersistentObject obj = (PersistentObject) o;
            return obj.id.equals(this.id) && obj.getType() == this.getType();
        }

    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.getType());
    }

    @Override
    public String toString() {
        String s = id + "," + this.type;
        synchronized (this) {
            for (PersistentObservation o : observations) {
                s += "," + o.toString();
            }

            return s;
        }
    }
}