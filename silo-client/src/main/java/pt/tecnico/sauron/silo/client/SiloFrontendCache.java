package pt.tecnico.sauron.silo.client;

import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.Message;

public class SiloFrontendCache {

  public class SiloFrontendCacheEntry {
    private String request;
    private Message data;

    public SiloFrontendCacheEntry(String request, Message data){
      this.request = request;
      this.data = data;
    }

    public void setRequest(String request){
      this.request= request;
    }

    public void setData(Message data){
      this.data = data;
    }

    public String getRequest(){
      return request;
    }

    public Message getData(){
      return data;
    }

    @Override
    public boolean equals(Object o){
      if( o instanceof SiloFrontendCacheEntry){
        return ((SiloFrontendCacheEntry) o).getRequest() == this.request;
      }
      else
        return false;
    }
  }

  private int maxSize;
  private List<SiloFrontendCacheEntry> cache = new ArrayList<>();

  public SiloFrontendCache(int size){
    maxSize = size;
  }

  public void addData(String request, Message data){
    SiloFrontendCacheEntry entry = new SiloFrontendCacheEntry(request, data);
    if(cache.contains(entry))
      cache.remove(entry);
    else if(cache.size() == maxSize)
        cache.remove(0);
    cache.add(entry);
  }

  public Message getData(String request){
    for(SiloFrontendCacheEntry e: cache){
      if(e.getRequest().equals(request)){
        return e.getData();
      }
    }
    return null;
  }

  public boolean hasData(String request){
    for(SiloFrontendCacheEntry e: cache){
      if(e.getRequest().equals(request)){
        return true;
      }
    }
    return false;
  }
}