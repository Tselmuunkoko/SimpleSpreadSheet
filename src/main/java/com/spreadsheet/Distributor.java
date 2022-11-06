package com.spreadsheet;

import javax.servlet.AsyncContext;
import javax.servlet.ServletContext;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.BlockingQueue;
public class Distributor implements Runnable {
    
    private final BlockingQueue<Cell> newCellQueue;
    private final Map<String, AsyncContext> clientList;
    private final List<Cell> pastCellList;
    private final ServletContext ctx;
    private boolean running = false;
    Distributor(ServletContext ctx) {
        this.ctx = ctx;
        newCellQueue = (BlockingQueue<Cell>)ctx.getAttribute("queue");
        pastCellList = (List<Cell>)ctx.getAttribute("newsList");
        clientList = (Map<String, AsyncContext>)ctx.getAttribute("clients");
    }
    void start() {
        running=true;
        Thread t= new Thread(this);
        t.start();
        ctx.log("started distributor");
    }
    void stop() {
        running=false;
        ctx.log("stopped distributor");
    }    
    @Override
    public void run() {
        while (running) {
            try {
                Cell news_item = newCellQueue.take();
                ctx.log("got new message :"+news_item);
                pastCellList.add(news_item);
                Iterator<AsyncContext> iter=clientList.values().iterator();
                while (iter.hasNext()) {
                    AsyncContext client=iter.next();
                    try {
                        PrintWriter channel = client.getResponse().getWriter();
                        sendMessage(channel, news_item);
                    } catch (Exception e) {
                        iter.remove();
                    }
                }
            } catch (InterruptedException e) {/* Log exception, etc.*/}
        }
    }
    private void sendMessage(PrintWriter writer, Cell c) {
        writer.write("event: message\n");
        writer.write("data: {\"id\":\"" + c.getId() + "\", \"value\":\"" + c.getValue() + "\", \"formula\": \"" + c.getFormula() + "\"}\n\n");
        writer.flush();
    }
}


