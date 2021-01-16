package pt.tecnico.sauron.silo.domain;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import pt.tecnico.sauron.silo.domain.exceptions.ErrorMessage;
import pt.tecnico.sauron.silo.domain.exceptions.SiloException;

public class Silo {

    private Map<String, PersistentObject> objects = new ConcurrentHashMap<>();
    private Map<String, PersistentCamera> cameras = new ConcurrentHashMap<>();

    /**
     * Getter for list of objects stored.
     * 
     * @return a collection of objects.
     */
    public Collection<PersistentObject> getObjects() {
        return objects.values();
    }

    /**
     * Getter for list of cameras stored.
     * 
     * @return a collection of cameras.
     */
    public Collection<PersistentCamera> getCameras() {
        return cameras.values();
    }

    /**
     * Checks if a camera already exists.
     * @param camName name of camera to be found.
     * @return if the camera exists.
     */
    public boolean hasCamera(String camName) {
        return cameras.containsKey(camName);
    }

    /**
     * Getter for specific camera.
     * @param camName name of the camera to be returned.
     * @return the camera.
     */
    public PersistentCamera getCamera(String camName) {
        return cameras.get(camName);
    }

    /**
     * Adds a camera, bypassing data integrity checks. Used for testing.
     * @param c camera to be added.
     */
    public void addCameraUnchecked(PersistentCamera c) {
        cameras.put(c.getName(), c);
    }

    /**
     * Adds a camera.
     * @param c camera to be added.
     * @return if the camera was sucessfully added or not.
     */
    public boolean addCamera(PersistentCamera c) {

        if (hasCamera(c.getName()) && ((c.getLatitude() != getCamera(c.getName()).getLatitude())
                || (c.getLongitude() != getCamera(c.getName()).getLongitude()))) {
            throw new SiloException(ErrorMessage.CAM_EXISTS, c.getName());
        } else{
            if(hasCamera(c.getName()))
                return false;
            cameras.put(c.getName(), c);
            return true;
        }
    }

    /**
     * Resets state. Clears all cameras and all objects.
     */
    public void clearState() {
        objects.clear();
        cameras.clear();
    }

    /**
     * Getter fot specific object, based on type and id.
     * @param type type of object to be found.
     * @param id id of object to be found.
     * @return the object.
     */
    public PersistentObject getObject(PersistentObject.ObjectType type, String id) {
        PersistentObject o = objects.get(id);
        if (o != null)
            if (type != o.getType()) {
                return null;
            }
        return o;
    }

    /**
     * Getter for all objects of a type that match a given id pattern.
     * @param type object type.
     * @param sIdPattern pattern to be matched.
     * @return a list of objects of specified type that match given pattern.
     */
    public List<PersistentObject> findByTypeAndId(PersistentObject.ObjectType type, String sIdPattern) {
        List<PersistentObject> matches = new ArrayList<>();

        Set<String> set = objects.keySet().stream().filter(s -> s.matches(sIdPattern)).collect(Collectors.toSet());
        if (type != null)
            for (String id : set) {
                if (objects.get(id).getType() == type) {
                    matches.add(objects.get(id));
                }
            }
        else
            for (String id : set) {
                matches.add(objects.get(id));
            }

        return matches;
    }

    /**
     * Adds a set of objects, mapped by id. For each object, if it already exists, 
     * merges it with existent one.
     * @param objs map of <id, object> to add.
     */
    public void addObjects(Map<String, PersistentObject> objs) {
        for (String s : objs.keySet()) {
            if (objects.containsKey(s))
                objects.get(s).addObservation(objs.get(s).getObservations());
            else
                objects.put(s, objs.get(s));
        }
    }

    /**
     * Adds a single object. If object already exists, merges it with existent one.
     * @param o object to be added.
     */
    public void addObject(PersistentObject o) {
        if (objects.containsKey(o.getId()))
            objects.get(o.getId()).addObservation(o.getObservations());
        else
            objects.put(o.getId(), o);
    }

}