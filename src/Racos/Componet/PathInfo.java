package Racos.Componet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PathInfo {
    private int []path1;
    private int []path2;
    private double best_value;
    private List<List<Boolean>> infeasible1;
    private List<List<Boolean>> infeasible2;

    public PathInfo(int occur,double best){
        best_value=best;
        infeasible1=new ArrayList<>();
        infeasible2=new ArrayList<>();
    }

    public void setPath1(int[] path1) {
        this.path1 = path1;
    }

    public void setPath2(int[] path2) {
        this.path2 = path2;
    }

    public int[] getPath1() {
        return path1;
    }

    public int[] getPath2() {
        return path2;
    }

    public void setBest_value(double best_value) {
        this.best_value = best_value;
    }

    public double getBest_value() {
        return best_value;
    }

    public void setInfeasible1(List<Boolean> addone){
        infeasible1.add(addone);
    }
    public void setInfeasible2(List<Boolean> addone){
        infeasible2.add(addone);
    }

    public List<List<Boolean>> getInfeasible1() {
        return infeasible1;
    }
    public List<List<Boolean>> getInfeasible2() {
        return infeasible2;
    }
}
