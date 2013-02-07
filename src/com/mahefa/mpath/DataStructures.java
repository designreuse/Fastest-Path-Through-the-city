package com.mahefa.mpath;

import java.awt.Color;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import javax.swing.JOptionPane;


class Parameter{
    String param,value;
    
    public Parameter(String p, String v) throws UnsupportedEncodingException{
        this.param = p;
        this.value = URLEncoder.encode(v, "ISO-8859-1");
    }
    
    public String toString(){
        return param+"="+value;
    }
}

class Address{
    int street_number;
    String street_name, city, country, zip_code, formatted_address;
    double lng, lat;
    
    public Address(	int street_number,
            String street_name,
            String city,
            String country,
            String zip_code,
            String formatted_address,
            double lng,
            double lat){
        this.street_name = street_name;
        this.street_number = street_number;
        this.city = city;
        this.country = country;
        this.zip_code = zip_code;
        this.formatted_address = formatted_address;
        System.out.println("Adress "+formatted_address+" : "+lat+","+lng);
        this.lat = lat;
        this.lng = lng;
    }
    
    public String toString(){
        return formatted_address;
    }
}

class Direction{
    long distance;
    long time;
    double latA=0,latB=0,lngB=0,lngA=0,start_lat=0,start_lng=0,end_lat=0,end_lng=0;
    String start_address = "<start_address>", end_address = "<end_address>";
    String travel_mode = "DRIVING";
    ArrayList<Direction> steps = null;
    
    public Direction(
            long distance,
            long time,
            String start_address,
            String end_address,
            String travel_mode,
            double start_lat,
            double start_lng,
            double end_lat,
            double end_lng){
        this.distance = distance;
        this.time = time;
        this.start_address = start_address;
        this.end_address = end_address;
        this.travel_mode = travel_mode;
        this.start_lat = start_lat;
        this.start_lng = start_lng;
        this.end_lat = end_lat;
        this.end_lng = end_lng;
    }
    
    public Direction(
            long distance,
            long time,
            double latA,
            double lngA,
            double latB,
            double lngB){
        this.distance = distance;
        this.time = time;
        this.latA = latA;
        this.latB = latB;
        this.lngA = lngA;
        this.lngB = lngB;
    }
    
    public void addStep(Direction stp){
        if(stp==null) return;
        if(steps==null)
            steps = new ArrayList<Direction>();
        steps.add(stp);
    }
    
    public Direction invert(){
        Direction d;
        if(latA==0&&latB==0&&lngA==0&&lngB==0){
            d = new Direction(distance,time,end_address,start_address,travel_mode,end_lat,end_lng,start_lat,start_lng);
            for(int i=steps.size()-1;i>=0;i--)
                d.addStep(steps.get(i).invert());
        }
        else{
            d = new Direction(distance,time,latB,lngB,latA,lngA);
        }
        return d;
    }
    
    public String formUrlParameter(){
        String res = Double.toString(start_lat)+","+Double.toString(start_lng);
        for(Direction d : steps)
            res += "|"+d.latB+","+d.lngB;
        return res;
    }
    
    public Double[] centerCoordinates(){
        Double[] s = new Double[2];
        s[0] = start_lat;
        s[1] = start_lng;
        int n=1;
        for(Direction d : steps){
            s[0] += d.latA;
            s[1] += d.latB;
            n++;
        }
        s[0]/=n;
        s[1]/=n;
        return s;
    }
    
    public double[] getMinMax(){
        // {minlat,minlng,maxlat,maxlng}
        double minlat = this.start_lat;
        double minlng = this.start_lng;
        double maxlat = minlat;
        double maxlng = minlng;
        for(Direction s: this.steps){
            if(s.latB<minlat) minlat = s.latB;
            if(s.lngB<minlng) minlng = s.lngB;
            if(s.latB>maxlat) maxlat = s.latB;
            if(s.lngB>maxlng) maxlng = s.lngB;
        }
        return new double[]{minlat,minlng,maxlat,maxlng};
    }
    
    public String toString(){
        String str = "<direction_unknown>";
        if(latA==0&&latB==0&&lngA==0&&lngB==0)
            str = "Go from \""+start_address+"\" to \""+end_address+"\" "+distance+" m and "+time+" sec:\n";
        else
            str = "Go from ("+latA+","+lngA+") to ("+latB+","+lngB+") "+distance+" m and "+time+" sec:\n";
        if(steps!=null)
            for(Direction dirstp : steps)
                str += "\t"+dirstp.toString();
        
        return str;
    }

}

class Path{
    int[] p;
    long len;
    boolean[] used;
    
    public Path(int[] p, long len){
        this.p = p;
        used = new boolean[p.length];
        Arrays.fill(used, false);
        used[0] = true;
        for(int i=1;i<p.length;i++)
            if(p[i]!=0){
                used[p[i]] = true;
            }
        this.len = len;
    }
    
    public Path(int nPoint){
        this.p = new int[nPoint];
        Arrays.fill(p, 0);
        this.len = 0;
        used = new boolean[p.length];
        Arrays.fill(used, false);
        used[0] = true;
    }
    
    public long getLen(){
        return len;
        
    }
    
    public ArrayList<Path> getSuccessors(long[][] distDir){
        ArrayList<Path> next = new ArrayList<Path>();
        int nextPos = 1;
        while(nextPos<p.length&&p[nextPos]!=0) nextPos++;
        if(nextPos>=p.length) return null;
        if(nextPos==p.length-1){
            int[] t = p.clone();
            t[nextPos] = nextPos;
            next.add(new Path(t,this.len+distDir[t[nextPos-1]][nextPos]));
        }
        for(int i=1;i<p.length-1;i++)
            if(!used[i]){
                int[] t = p.clone();
                t[nextPos] = i;
                next.add(new Path(t,this.len+distDir[t[nextPos-1]][i]));
            }
        if(next.size()==0) return null;
        else return next;
    }
    
    public boolean isFinalState(){
        return p[p.length-1]!=0;
    }
    
    public int[] toArray(){
        return this.p;
    }
    
    public String toString(){
        String str = "[ 0 ";
        int i=1;
        while(i<p.length&&p[i]!=0) str += "- "+Integer.toString(p[i++]);
        str +=  " ] : "+len+" meters";
        return str;
    }
}

class PriorityQueue{
    
    ArrayList<Path> values = null;
    
    public PriorityQueue(){
        values = new ArrayList<Path>();
    }
    
    public void push(Path path){
        if(values.size()==0){
            values.add(path);
            return;
        }
        int index = 0;
        double len = path.getLen();
        while(index<values.size()&&len>values.get(index).getLen()) index++;
        if(index==values.size())
            values.add(path);
        else
            values.add(index, path);
    }
    
    public Path pop(){
        if(values.size()==0) return null;
        return values.remove(0);
    }
    
    public boolean isEmpty(){
        return values.size()==0;
    }
}

class Util{
    static DecimalFormat df = new DecimalFormat("0.000");
    static Date lastcheck;
    static String getTimeString(long time){
        // time in sec
        long hr=time, mn=time, sc=time;
        if(time>3600){
            mn = time%3600;
            hr = (time-mn)/3600;
            time = mn;
        }
        else{
            hr = 0;
        }
        if(mn>60){
            sc = time%60;
            mn = (time-sc)/60;
            time = sc;
        }
        else{
            mn = 0;
        }
        String str = "";
        if(hr!=0) str += hr+" hr ";
        if(mn!=0) str += mn+" min ";
        str += sc+" sec";
        return str;
    }
    
    static String formatDouble(double d){
        return df.format(d);
    }
    
    static public boolean checkConnection() {
        lastcheck = new Date();
        boolean status = false;
        try {
            URLConnection con = (new URL("http://www.google.com/")).openConnection();
            con.setConnectTimeout(2000);
            con.getInputStream().close();
            status = true;
        } catch (Exception e) {
            status = false;
            e.printStackTrace();
            Util.debugAlert("Internet seems to be down");
            //JOptionPane.showMessageDialog(null, "It seems that your internet is down.\n"+
                    //"Please, make sure you are connected to the internet.", "Internet down", JOptionPane.WARNING_MESSAGE);
        }
        return status;
    }
    
    public static void debugInf(String s){
        GUI.labelDebug.setForeground(Color.BLACK);
        GUI.labelDebug.setText(s);
    }
    
    public static void debugAlert(String s){
        GUI.labelDebug.setForeground(Color.RED);
        GUI.labelDebug.setText(s);
    }
}

public class DataStructures {

}
