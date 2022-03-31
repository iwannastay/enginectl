package com.iws.enginectl;


import com.alibaba.fastjson.JSONObject;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.net.URIBuilder;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Enginectl {
    List<String> input;
    JSONObject pairs;
    Map<String, String> urls;
    String base;
    String user;

    static SimpleDateFormat ft = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");

    public List<String> getInput() {
        return input;
    }

    public void setInput(List<String> input) {
        this.input = input;
    }

    public Map<String, String> getUrls() {
        return urls;
    }

    public void setUrls(Map<String, String> urls) {
        this.urls = urls;
    }

    public Enginectl(String[] args) {
        //TODO: user
        user = "iws";
        //args
        input = new ArrayList<>(Arrays.asList(args));
        pairs = new JSONObject();
        //load urls
        urls = new HashMap<>();
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader("/etc/enginectl/urls.yml"));
            String str;
            int index;
            while ((str = bufferedReader.readLine()) != null) {
                if (str.equals("")) continue;
                index = str.indexOf(":");
                urls.put(str.substring(0, index), str.substring(index + 1).strip());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        base = urls.get("pro");

    }

    public static void main(String[] args) throws FileNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        /*
        * example 1: development
        * create --name dev2 --image 10.16.17.92:8433/library/ubuntu16.04-ssh:v1 --command /bin/bash
        * list dev
        * get dev dev1
        * connect dev1
        * stop dev dev1 dev2 --save true
        * start dev dev1 dev2
        * save dev1
        * drop dev1 --image 10.16.17.92:8433/tmp/dev1:2021.08.08-00.07.00 10.16.17.92:8433/tmp/dev1:2021.08.08-00.16.42
        * setdefault dev2 --image 10.16.17.92:8433/tmp/dev2:2021.08.07-23.57.55
        * rm dev dev1 dev2
        */

        /*
        * example 2: deployment
        * export --name test --images 10.16.17.92:8433/public/es-frontend:v1 10.16.17.92:8433/public/es-backend:v1
        * upload --name test --config dev.json
        * get app --deploy test --app app1
        * list deploy
        * list app
        * run --deploy test --app app1 --config dev.json
        * stop app --deploy test --app app1
        * start app --deploy test --app app1
        * rm app --deploy test --app app1
        * rm config --deploy test --config dev.json
        * rm deploy --deploy test
        * */

        /*
        * TODO:
        * help
        * login/out
        * */

        /*
        * example 3: user
        * login --username test --password 123456
        * logout
        * adduser --username test --password 123456 --email 123@qq.com --role 1
        * rmuser --username test
        * */

        /*
         * example 4: image
         * pull busybox:latest
         * rm image 10.16.17.92:8433/public/busybox:latest
         * save image --containerID qwe --name busybox:latesttt
         * get image 10.16.17.92:8433/public/busybox:latest
         * list image --project library --page 1 --page_size 10
         * */


        Enginectl enginectl = new Enginectl(args);
        enginectl.handleInput();
    }

    public void handleInput(){
        if (getInput().size() == 0) {
            help();
        }else {
            String operator=getInput().remove(0);
            if(operator.equals("help")){
                help();
            }else if(operator.equals("login")||authorization()){
                classify(operator);
            }else{
                System.out.println("Please login first.");
            }
        }
    }

    public void classify(String s) {
        Method method = null;
        try {
            method = Enginectl.class.getMethod(s, null);
            method.invoke(this);
        } catch (NoSuchMethodException e) {
            printInputError(s);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

    }

    public void readArgs(){
        Queue<String> keys = new LinkedList<>();
        List<String> values = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (String next : input) {
            if (next.startsWith("--")) {
                if (!keys.isEmpty()) {
                    if(!values.isEmpty()){
                        pairs.put(keys.poll(), values);
                        values= new ArrayList<>();
                    }
                    else errors.add(keys.poll());
                }else if(!values.isEmpty()){
                    pairs.put("objects",values);
                    values= new ArrayList<>();
                }
                keys.offer(next.substring(2));
            } else {
                values.add(next);
            }
        }

        String last = keys.poll();
        if(last!=null&&values.isEmpty())  errors.add(last);

        if(!values.isEmpty())
            pairs.put(last==null? "objects":last, values);

//        System.out.println(pairs);

        if(!errors.isEmpty())
            printArgsError(errors.toString());
    }

    public void get() {
        if(input.size()==0) printInputError("get");
        String remove = input.remove(0);

        switch (remove) {
            case "app":
                getApplication();
                break;
            case "dev":
                getDevelopment();
                break;
            case "image":
                getImage();
                break;
            default:
                printInputError(remove);
                break;
        }
    }

    public void start() {
        if(input.size()==0) printInputError("start");
        String remove = input.remove(0);

        switch (remove) {
            case "app":
                startApplication();
                break;
            case "dev":
                startDevelopment();
                break;
            default:
                printInputError(remove);
                break;
        }
    }

    public void stop() {
        if(input.size()==0) printInputError("stop");
        String remove = input.remove(0);

        switch (remove) {
            case "app":
                stopApplication();
                break;
            case "dev":
                stopDevelopment();
                break;
            default:
                printInputError(remove);
                break;
        }

    }

    public void save() {
        if(input.size()==0) printInputError("save");
        String remove = input.remove(0);

        switch (remove) {
            case "image":
                saveImage();
                break;
            case "dev":
                saveDevelopment();
                break;
            default:
                printInputError(remove);
                break;
        }

    }

    public void saveDevelopment() {
        System.out.println("Please wait ... ");

        String url = base + urls.get("development") + "/save";

        checkArgs("objects");
        String name = pairs.getObject("objects",ArrayList.class).get(0).toString();


        JSONObject jsonObject = new JSONObject();
        jsonObject.put("user", user);
        jsonObject.put("name", name);

        String result = sendPOST(url, jsonObject);
        System.out.println(result);
    }

    public void drop() {
        String url = base + urls.get("development") + "/drop";


        checkArgs("objects","image");
        String name = pairs.getObject("objects",ArrayList.class).get(0).toString();
        List<String> images= pairs.getObject("image",ArrayList.class);



        JSONObject jsonObject = new JSONObject();
        jsonObject.put("user", user);
        jsonObject.put("name", name);

        for(String image: images){
            jsonObject.put("image", image);
            String result = sendPOST(url, jsonObject);
            System.out.println(image + ":" + result);
        }

    }

    public void setdefault() {
        String url = base + urls.get("development") + "/setdefault";


        checkArgs("objects","image");
        String name = pairs.getObject("objects",ArrayList.class).get(0).toString();
        List<String> images= pairs.getObject("image",ArrayList.class);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("user", user);
        jsonObject.put("name", name);

        for(String image: images){
            jsonObject.put("image", image);
            String result = sendPOST(url, jsonObject);
            System.out.println(image + ":" + result);
        }
    }

    public void rm() {
        if(input.size()==0) printInputError("rm");
        String remove = input.remove(0);

        switch (remove) {
            case "app":
                removeApplication();
                break;
            case "dev":
                removeDevelopment();
                break;
            case "config":
                removeConfig();
                break;
            case "deploy":
                removeDeployment();
                break;
            case "image":
                removeImage();
                break;
            default:
                printInputError(remove);
                break;
        }
    }

    public void list() {
        if(input.size()==0) printInputError("list");
        String remove = input.remove(0);

        switch (remove) {
            case "app":
                listApplication();
                break;
            case "dev":
                listDevelopment();
                break;
            case "deploy":
                listDeployment();
                break;
            case "image":
                listImage();
                break;
            default:
                printInputError(remove);
                break;
        }
    }

    public void connect() {
        String url = base + urls.get("development") + "/connect";

        checkArgs("objects");
        String name = pairs.getObject("objects",ArrayList.class).get(0).toString();


        JSONObject jsonObject = new JSONObject();
        jsonObject.put("user", user);
        jsonObject.put("name", name);

        String result = sendGET(url, jsonObject);
        VisualTool.showList(Collections.singletonList(JSONObject.parseObject(result))
                , new ArrayList<>(Arrays.asList("sshLink","password","username"))
        );
    }

    public void create() {
        String url = base + urls.get("development") + "/create";

        checkArgs("name","image","command");
        String name = pairs.getObject("name",ArrayList.class).get(0).toString();
        String baseImage = pairs.getObject("image",ArrayList.class).get(0).toString();
        String command = pairs.getObject("command",ArrayList.class).get(0).toString();


        JSONObject jsonObject = new JSONObject();
        jsonObject.put("user", user);
        jsonObject.put("name", name);
        jsonObject.put("baseImage", baseImage);
        jsonObject.put("command", command);


        if (pairs.containsKey("servicePort")) {
            jsonObject.put("servicePort", Integer.parseInt(pairs.getObject("servicePort",ArrayList.class).get(0).toString()));
        } else {
            jsonObject.put("servicePort", 80);
        }
        if (pairs.containsKey("containerPort")) {
            jsonObject.put("containerPort", Integer.parseInt(pairs.getObject("containerPort",ArrayList.class).get(0).toString()));
        } else {
            jsonObject.put("containerPort", 80);
        }
        if (pairs.containsKey("gpuNumbers")) {
            jsonObject.put("gpuNumbers", Integer.parseInt(pairs.getObject("gpuNumbers",ArrayList.class).get(0).toString()));
        } else {
            jsonObject.put("gpuNumbers", 0);
        }
        if (!pairs.containsKey("cpuNumbers")) {
            jsonObject.put("cpuNumbers", "200m");
        } else {
            //TODO: check format
//            String cpuNumbers = jsonObject.getString("cpuNumbers");
        }
        if (!pairs.containsKey("memory")) {
            jsonObject.put("memory", "300Mi");
        } else {
            //TODO: check format
        }

        String result = sendPOST(url, jsonObject);
        System.out.println(result);
    }

    public void help() {
        Scanner in = null;
        try {
            in = new Scanner(new File("/etc/enginectl/help.txt"));
            while (in.hasNextLine()) {
                System.out.println(in.nextLine());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    public boolean authorization() {
        String userPath = "/etc/enginectl/user";
        HashMap<String, String> userInfo = new HashMap<>();
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(userPath));
            String str;
            int index;
            while ((str = bufferedReader.readLine()) != null) {
                if (str.equals("")) continue;
                index = str.indexOf(":");
                userInfo.put(str.substring(0, index), str.substring(index + 1).strip());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //TODO: login record exp
        if (userInfo.containsKey("username")&&userInfo.containsKey("last_time_login")) {
            try {
                long last = ft.parse(userInfo.get("last_time_login")).getTime();
                long now = new Date().getTime();
                int hours = (int) ((now - last) / (1000 * 60 * 60));

                if(hours<24){
                    user = userInfo.get("username");
                    return true;
                }

            } catch (java.text.ParseException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public void login() {
        String url = base + urls.get("user") + "/signin";

        checkArgs("username","password");
        String name = pairs.getObject("username",ArrayList.class).get(0).toString();
        String password = pairs.getObject("password",ArrayList.class).get(0).toString();


        JSONObject jsonObject = new JSONObject();
        jsonObject.put("user_name", name);
        jsonObject.put("password", password);

        String result = sendPOST(url, jsonObject);
        System.out.println(result);

        JSONObject jsonObject1 = JSONObject.parseObject(result);
        if (jsonObject1.get("code").equals(800)) {
            String userPath = "/etc/enginectl/user";


            FileWriter writer = null;
            try {
                writer = new FileWriter(userPath);
                writer.write("username: " + jsonObject.getString("user_name") + "\n");
                writer.write("last_time_login: " + ft.format(new Date()) + "\n");
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }


    /////////////////

    //export --images image1 image2 image3 --name test
    public void export(){
        String url = base + urls.get("deployment") + "/export";

        checkArgs("name","images");
        String name = pairs.getObject("name",ArrayList.class).get(0).toString();
        List<String> images= pairs.getObject("images",ArrayList.class);


        JSONObject jsonObject = new JSONObject();
        jsonObject.put("user", user);
        jsonObject.put("name", name);
        jsonObject.put("images", images);

        String result = sendPOST(url, jsonObject);
        System.out.println(result);

    }

    public void removeDeployment(){
        String url = base + urls.get("deployment") + "/deleteDeployment";

        checkArgs("deploy");
        String name = pairs.getObject("deploy",ArrayList.class).get(0).toString();

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("user", user);
        jsonObject.put("name", name);

        String result = sendPOST(url, jsonObject);
        System.out.println(result);
    }

    public void removeConfig(){
        String url = base + urls.get("deployment") + "/deleteConfig";

        checkArgs("deploy","config");
        String name = pairs.getObject("deploy",ArrayList.class).get(0).toString();
        String jsonName = pairs.getObject("config",ArrayList.class).get(0).toString();


        JSONObject jsonObject = new JSONObject();
        jsonObject.put("user", user);
        jsonObject.put("name", name);
        jsonObject.put("jsonName", jsonName);

        String result = sendPOST(url, jsonObject);
        System.out.println(result);
    }

    public void removeApplication(){
        String url = base + urls.get("deployment") + "/deleteApplication";

        checkArgs("deploy","app");
        String deployName = pairs.getObject("deploy",ArrayList.class).get(0).toString();
        String appName = pairs.getObject("app",ArrayList.class).get(0).toString();


        JSONObject jsonObject = new JSONObject();
        jsonObject.put("user", user);
        jsonObject.put("deployName", deployName);
        jsonObject.put("appName", appName);

        String result = sendPOST(url, jsonObject);
        System.out.println(result);
    }

    public void removeDevelopment(){
        String url = base + urls.get("development") + "/remove";

        checkArgs("objects");
        List<String> objects= pairs.getObject("objects",ArrayList.class);


        JSONObject jsonObject = new JSONObject();
        jsonObject.put("user", user);

        for(String name: objects){
            jsonObject.put("name", name);
            String result = sendPOST(url, jsonObject);
            System.out.println(name + ": " + result);
        }
    }

    public void upload(){
        String url = base + urls.get("deployment") + "/upload";

        checkArgs("name","config");
        String name = pairs.getObject("name",ArrayList.class).get(0).toString();
        String jsonName = pairs.getObject("config",ArrayList.class).get(0).toString();


        JSONObject jsonFile = ToolBox.jsonFileReader(jsonName);
        if(jsonFile==null){
            printArgsError("config");
            return;
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("user", user);
        jsonObject.put("name", name);
        jsonObject.put("jsonName", jsonName);
        jsonObject.put("jsonFile", jsonFile);

        String result = sendPOST(url, jsonObject);
        System.out.println(result);

    }

    public void listDevelopment(){
        String url = base + urls.get("development") + "/list";

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("user", user);
        jsonObject.put("page", 1);
        jsonObject.put("pageSize", 10);

        String result = sendGET(url, jsonObject);

        VisualTool.showList((List<JSONObject>)JSONObject.parseObject(result,ArrayList.class)
                , new ArrayList<>(Arrays.asList("name","node","containerId","baseImage","user","status"))
        );
    }

    public void listDeployment(){
        String url = base + urls.get("deployment") + "/listDeployment";

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("user", user);

        String result = sendGET(url, jsonObject);
        List<JSONObject> data = JSONObject.parseObject(result).getObject("data", ArrayList.class);
        for (JSONObject deploy : data) {
            deploy.put("images",deploy.getObject("images",ArrayList.class).size());
            deploy.put("configs",deploy.getObject("configs",JSONObject.class).keySet());
        }
        VisualTool.showList(data
                , new ArrayList<>(Arrays.asList("name","user","images","configs","applications"))
        );
    }

    public void listApplication(){
        String url = base + urls.get("deployment") + "/listApplication";

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("user", user);

        String result = sendGET(url, jsonObject);
        System.out.println(result);
        List<JSONObject> data = JSONObject.parseObject(result).getObject("data", ArrayList.class);
        VisualTool.showList(data
                , new ArrayList<>(Arrays.asList("name","appID","taskID","jsonName","deployName","state"))
        );
    }

    public void run(){
        String url = base + urls.get("deployment") + "/run";

        checkArgs("app","deploy","config");
        String appName = pairs.getObject("app",ArrayList.class).get(0).toString();
        String deployName = pairs.getObject("deploy",ArrayList.class).get(0).toString();
        String jsonName = pairs.getObject("config",ArrayList.class).get(0).toString();


        JSONObject jsonObject = new JSONObject();
        jsonObject.put("user", user);
        jsonObject.put("deployName", deployName);
        jsonObject.put("jsonName", jsonName);
        jsonObject.put("appName", appName);

        String result = sendPOST(url, jsonObject);
        System.out.println(result);

    }

    public void startDevelopment(){
        String url = base + urls.get("development") + "/start";

        checkArgs("objects");
        List<String> objects = pairs.getObject("objects",ArrayList.class);


        JSONObject jsonObject = new JSONObject();
        jsonObject.put("user", user);

        for(String name: objects){
            jsonObject.put("name", name);
            String result = sendPOST(url, jsonObject);
            System.out.println(name + ": " + (Boolean.valueOf(result)? "Start":"Failed"));
        }
    }

    public void startApplication(){
        String url = base + urls.get("deployment") + "/start";

        checkArgs("app","deploy");
        String appName = pairs.getObject("app",ArrayList.class).get(0).toString();
        String deployName = pairs.getObject("deploy",ArrayList.class).get(0).toString();


        JSONObject jsonObject = new JSONObject();
        jsonObject.put("user", user);
        jsonObject.put("deployName", deployName);
        jsonObject.put("appName", appName);

        String result = sendPOST(url, jsonObject);
        System.out.println(result);
    }

    public void stopDevelopment(){
        String url = base + urls.get("development") + "/stop";

        checkArgs("objects");
        List<String> objects = pairs.getObject("objects",ArrayList.class);


        JSONObject jsonObject = new JSONObject();
        jsonObject.put("user", user);
        if (pairs.containsKey("save")) {
            jsonObject.put("save", Boolean.valueOf(pairs.getObject("save",ArrayList.class).get(0).toString()));
        } else jsonObject.put("save", false);

        for(String name: objects){
            jsonObject.put("name", name);
            String result = sendPOST(url, jsonObject);
            System.out.println(name + ": " + result);
        }
    }


    public void stopApplication(){
        String url = base + urls.get("deployment") + "/stop";

        checkArgs("app","deploy");
        String appName = pairs.getObject("app",ArrayList.class).get(0).toString();
        String deployName = pairs.getObject("deploy",ArrayList.class).get(0).toString();


        JSONObject jsonObject = new JSONObject();
        jsonObject.put("user", user);
        jsonObject.put("deployName", deployName);
        jsonObject.put("appName", appName);

        String result = sendPOST(url, jsonObject);
        System.out.println(result);
    }

    public void getDevelopment(){
        String url = base + urls.get("development") + "/get";

        checkArgs("objects");
        String name = pairs.getObject("objects",ArrayList.class).get(0).toString();


        JSONObject jsonObject = new JSONObject();
        jsonObject.put("user", user);
        jsonObject.put("name", name);

        String result = sendGET(url, jsonObject);
        VisualTool.showJSON(JSONObject.parseObject(result));
    }

    public void getApplication(){
        String url = base + urls.get("deployment") + "/runState";

        checkArgs("app","deploy");
        String appName = pairs.getObject("app",ArrayList.class).get(0).toString();
        String deployName = pairs.getObject("deploy",ArrayList.class).get(0).toString();


        JSONObject jsonObject = new JSONObject();
        jsonObject.put("user", user);
        jsonObject.put("appName", appName);
        jsonObject.put("deployName", deployName);

        String result = sendGET(url, jsonObject);
        VisualTool.showJSON(JSONObject.parseObject(result));
    }

    public void logout(){
        String url = base + urls.get("user") + "/signout";
        JSONObject jsonObject = new JSONObject();
        String result = sendPOST(url, jsonObject);
        System.out.println(result);

        ToolBox.deleteFile("/etc/enginectl/user");

    }

    public void adduser(){
        String url = base + urls.get("user") + "/add";

        checkArgs("username","password","email","role");
        String user_name = pairs.getObject("username",ArrayList.class).get(0).toString();
        String password = pairs.getObject("password",ArrayList.class).get(0).toString();
        String email = pairs.getObject("email",ArrayList.class).get(0).toString();
        Integer role = Integer.parseInt(pairs.getObject("role",ArrayList.class).get(0).toString());


        JSONObject jsonObject = new JSONObject();
        jsonObject.put("user_name", user_name);
        jsonObject.put("password", password);
        jsonObject.put("email", email);
        jsonObject.put("role", role);

        String result = sendPOST(url, jsonObject);
        System.out.println(result);
    }

    public void rmuser(){
        String url = base + urls.get("user") + "/remove";

        checkArgs("username");
        String user_name = pairs.getObject("username",ArrayList.class).get(0).toString();

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("user_name", user_name);

        String result = sendPOST(url, jsonObject);
        System.out.println(result);
    }

    public void removeImage(){
        String url = base + urls.get("image") + "/remove";

        checkArgs("objects");
        String name = pairs.getObject("objects",ArrayList.class).get(0).toString();

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", name);

        String result = sendPOST(url, jsonObject);
        System.out.println(result);
    }

    public void pull(){
        String url = base + urls.get("image") + "/pull";

        checkArgs("name");
        String name = pairs.getObject("name",ArrayList.class).get(0).toString();

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", name);

        String result = sendPOST(url, jsonObject);
        System.out.println(result);
    }

    public void saveImage(){
        String url = base + urls.get("image") + "/save";

        checkArgs("name","containerID");
        String name = pairs.getObject("name",ArrayList.class).get(0).toString();
        String containerID = pairs.getObject("containerID",ArrayList.class).get(0).toString();

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", name);
        jsonObject.put("containerID", containerID);

        String result = sendPOST(url, jsonObject);
        System.out.println(result);
    }

    public void getImage(){
        String url = base + urls.get("image") + "/get";

        checkArgs("objects");
        String name = pairs.getObject("objects",ArrayList.class).get(0).toString();

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", name);

        String result = sendGET(url, jsonObject);
        System.out.println(result);
    }

    public void listImage(){
        String url = base + urls.get("image") + "/list";

        checkArgs("project");
        String project = pairs.getObject("project",ArrayList.class).get(0).toString();

        String page=pairs.containsKey("page")?pairs.getObject("page",ArrayList.class).get(0).toString():"1";
        String page_size=pairs.containsKey("page_size")?pairs.getObject("page_size",ArrayList.class).get(0).toString():"10";

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("project", project);
        jsonObject.put("page", page);
        jsonObject.put("page_size", page_size);

        String result = sendGET(url, jsonObject);

        VisualTool.showList((List<JSONObject>)JSONObject.parseObject(result,ArrayList.class)
                , new ArrayList<>(Arrays.asList("name","tag","imageID","created","size"))
        );

    }

    public void printArgsError(String str) {
        System.out.println("Please check your args : [" + str + "]");
        System.exit(0);
    }

    public void printInputError(String str) {
        System.out.println("Please check your input at [" + str + "]");
        System.exit(0);
    }

    public boolean checkArgs(String ...strings){
        readArgs();
        for(String str: strings){

            if(!pairs.containsKey(str)
//                    ||pairs.getObject(str,ArrayList.class).isEmpty()
//                    there is no need to judge because readArgs() ensures that the cndition  {"objects":[]} will never occur.
            ){
                printArgsError(str);
            }
        }
        return true;
    }

    public String sendPOST(String url, JSONObject data) {
        HttpPost post = new HttpPost(url);

        // send a JSON data
        post.setEntity(new StringEntity(data.toJSONString()));
        post.addHeader("content-type", "application/json");
        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            CloseableHttpResponse response = httpClient.execute(post);

            return EntityUtils.toString(response.getEntity());
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String sendGET(String url, JSONObject data) {
        try {
            URIBuilder uriBuilder = new URIBuilder(url);
            for (String key : data.keySet()) {
                uriBuilder.addParameter(key, data.getString(key));
            }
            HttpGet get = new HttpGet(uriBuilder.build());
            CloseableHttpClient httpClient = HttpClients.createDefault();
            CloseableHttpResponse response = httpClient.execute(get);
            return EntityUtils.toString(response.getEntity());
        } catch (URISyntaxException | IOException | ParseException e) {
            e.printStackTrace();
        }

        return null;
    }
}
