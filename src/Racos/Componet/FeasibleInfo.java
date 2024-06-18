package Racos.Componet;

import java.util.ArrayList;
import java.util.List;

public class FeasibleInfo {
    public ArrayList<Integer> path;
    public List<List<Boolean>> infeasible;

    public FeasibleInfo(ArrayList<Integer> Path){
        path=Path;
        infeasible=new ArrayList<>();
    }

    public void setInfeasible(List<Boolean> addone){
        infeasible.add(addone);
    }

    public List<List<Boolean>> getInfeasible() {
        return infeasible;
    }

    public ArrayList<Integer> getPath() {
        return path;
    }

}
