package mpath;

import mpath.Address;
import mpath.Path;
import mpath.Direction;
import mpath.PriorityQueue;
import mpath.Util;
import java.io.IOException;
import java.util.ArrayList;


public class GraphPath {
    ArrayList<Address> addr;
    Address begin, end;
    int nPoint = 0;
    Direction[][] dir = null;
    long[][] distDir = null;
    long[][] pathPrevious = null;
    int[] traject, fastestPath;
    
    public GraphPath(Address A, Address B, ArrayList<Address> addr) throws IOException{
        this.begin = A;
        this.end = B;
        this.addr = addr;
        this.nPoint = addr.size()+2;
        dir = new Direction[nPoint][nPoint]; // first and last set to A and B
        distDir = new long[nPoint][nPoint];
        pathPrevious = new long[nPoint][nPoint];
        traject = new int[nPoint];
        fastestPath = new int[nPoint];
        for(int i=0;i<nPoint;i++)
            System.out.println("["+i+"] - "+this.get(i));
    }
    
    public String formMatr() throws IOException{
        for(int i=0;i<this.nPoint;i++){
            Address A = this.get(i);
            this.dir[i][i] = new Direction(0,0,A.toString(),A.toString(),"DRIVING",A.lat,A.lng,A.lat,A.lng);
            this.distDir[i][i] = 0;
            for(int j=i+1;j<this.nPoint;j++){
                Address B = this.get(j);
                this.dir[i][j] = MapFunc.getDirection(A, B);
                if(this.dir[i][j]==null)
                    return "Error: Cannot get the direction from \""+A+"\" to \""+B+" from GoogleMap";
                this.dir[j][i] = this.dir[i][j].invert();
                this.distDir[i][j] = this.dir[i][j].distance;
                this.distDir[j][i] = this.dir[j][i].distance;
            }
        }
        return null;
    }
    
    public Path getMinPath(){
        PriorityQueue queue = new PriorityQueue();
        Path currentPath = new Path(this.nPoint);;
        System.out.println("Starting with : "+currentPath);
        queue.push(currentPath);
        while(!queue.isEmpty()){
            Util.debugAlert("Searching for shortest path ...");
            currentPath = queue.pop();
            if(currentPath.isFinalState())
                break;
            System.out.println("Current path : "+currentPath);
            ArrayList<Path> successors = currentPath.getSuccessors(distDir);
            if(successors==null) continue;
            System.out.println("Successors are:");
            for(Path succ: successors){
                queue.push(succ);
                System.out.println("\t"+succ);
            }
        }
        Util.debugInf("Shortest path found!");
        return currentPath;
    }
    
    public Address get(int index){
        // get(0) return A, get(nPoint) return B, get(n) return addr[n-1]
        if(index>=this.nPoint) return null;
        if(index==0) return begin;
        if(index==this.nPoint-1) return end;
        return addr.get(index-1);
    }
    
    public Direction[][] getDirectionMatrix(){
        return this.dir;
    }
}
