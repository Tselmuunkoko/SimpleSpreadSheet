package com.spreadsheet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;

@WebServlet(name = "sheetServlet", value = "/sheet-servlet")
public class SheetServlet extends HttpServlet {
    ServletContext ctx;
    private AtomicLong counter = new AtomicLong();
    private Map<String, AsyncContext> clientList;
    private BlockingQueue<Cell> newCellQueue;
    private List<Cell> pastCellList;
    private Distributor distributor;
    SSEngine engine = SSEngine.getSSEngine();
    void addReader(HttpServletRequest request, HttpServletResponse response) {
        clientList = (Map<String, AsyncContext>) ctx.getAttribute("clients");
        final String id = UUID.randomUUID().toString();
        final AsyncContext ac = request.startAsync(request, response);
        ac.addListener(new AsyncListener() {

            @Override
            public void onStartAsync(AsyncEvent event) throws IOException {
            }

            @Override
            public void onComplete(AsyncEvent event) throws IOException {
                clientList.remove(id);
            }

            @Override
            public void onError(AsyncEvent event) throws IOException {
                clientList.remove(id);
            }

            @Override
            public void onTimeout(AsyncEvent event) throws IOException {
                clientList.remove(id);
            }
        });
        clientList.put(id, ac);
        log("added new client");
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        log(" INIT =============================");
        ctx = getServletContext();
        startEvent();
    }

    private void startEvent(){
        counter.set(0);
        newCellQueue =new LinkedBlockingQueue<Cell>();
        ctx.setAttribute("queue", newCellQueue);
        clientList=new ConcurrentHashMap<String, AsyncContext>();
        ctx.setAttribute("clients", clientList);
        pastCellList =new CopyOnWriteArrayList<Cell>();
        ctx.setAttribute("newsList", pastCellList);
        distributor=new Distributor(ctx);
        ctx.setAttribute("distributor", distributor);
        distributor.start();
    }
    private void endEvent(){
        distributor.stop();
        clientList.clear();
        newCellQueue.clear();
        pastCellList.clear();
        ctx.removeAttribute("queue");
        ctx.removeAttribute("clients");
        ctx.removeAttribute("distributor");
        newCellQueue =null;
        clientList=null;
        distributor=null;
    }
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (request.getHeader("Accept").equals("text/event-stream")) {
            response.setContentType("text/event-stream");
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("Connection", "keep-alive");
            response.setCharacterEncoding("UTF-8");
            request.setAttribute("org.apache.catalina.ASYNC_SUPPORTED", true);
            addReader(request,response);
        } else {
            PrintWriter out = response.getWriter();
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            HashMap<String, Cell> map = this.engine.cellMap;
            GsonBuilder builder = new GsonBuilder();
            builder.registerTypeAdapter(Cell.class, new CellAdapter());
            builder.setPrettyPrinting();
            Gson gson1 = builder.create();
            ArrayList<Cell> cells = new ArrayList<>(map.values());
            String jsonObject = gson1.toJson(cells);
            out.print(jsonObject);
            out.flush();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (ctx.getAttribute("queue")==null) startEvent();
        try {
            String data = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            Gson gson = new Gson();
            Formula formula = gson.fromJson(data, Formula.class);
            engine.modifyCell(formula.id, formula.value );
            newCellQueue.put(engine.cellMap.get(formula.id));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void destroy() {
        endEvent();
    }
}
