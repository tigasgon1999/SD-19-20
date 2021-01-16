package pt.tecnico.sauron.silo;

import java.util.TimerTask;
import io.grpc.BindableService;


public class MyTimerTask extends TimerTask {

    private SiloServerImpl impl;

    public MyTimerTask(BindableService impl) {
        this.impl = (SiloServerImpl)impl;
    }

    @Override
    public void run() {
        impl.sendGossip();
    }
}