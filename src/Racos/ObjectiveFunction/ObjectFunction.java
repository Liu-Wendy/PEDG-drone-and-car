package Racos.ObjectiveFunction;

import MPC.Automata;
import MPC.Combination;
import MPC.Transition;
import Racos.Componet.Dimension;
import Racos.Componet.Instance;
import MPC.Location;
import Racos.Tools.ValueArc;
import com.greenpineyu.fel.*;
import com.greenpineyu.fel.context.FelContext;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class ObjectFunction implements Task{
    private Dimension dim;
    private Combination combination;
    private int automata_num;
//    private int []path1;
//    private int []path2;
    private FelEngine fel;
    private FelContext ctx;
    private ArrayList<ArrayList<HashMap<String,Double>>> allParametersValuesList;
    public double delta;
    private double penalty = 0;
    private double globalPenalty = 0;
    private boolean sat = true;
    private double cerr = 0.01;
    public ArrayList<ArrayList<Boolean>> infeasiblelist;
    public ArrayList<ArrayList<Integer>> path;
    int rangeParaSize;
    int maxpathSize;
    int locsize;
    ArrayList<ArrayList<Integer>>PathMap;
//    ArrayList<Boolean> infeasible_loc1;
//    ArrayList<Boolean> infeasible_loc2;
    ArrayList<Integer> path_tmp1;
    ArrayList<Integer> path_tmp2;

    public ValueArc valueArc;
    public ArrayList<ArrayList<Integer>> combinPath;

    double[] time=new double[4];

    public ObjectFunction(Combination combination, int maxPathSize){

        this.combination = combination;
        automata_num=combination.automata_num;
        dim = new Dimension();
        delta = combination.automatas.get(0).delta;
        maxpathSize=maxPathSize;
        locsize=combination.automatas.get(0).locations.size();
        this.PathMap=combination.PathMap;
        this.combinPath=combination.combin;

        rangeParaSize = (combination.automatas.get(0).rangeParameters == null) ? 0 : combination.automatas.get(0).rangeParameters.size();
        int one_arm_size= rangeParaSize + maxpathSize;
        dim.setSize(1+one_arm_size*automata_num);

        dim.setDimension(0,0,combinPath.size()-1,false);
        int n=0;
        while(n<automata_num){
            for(int i = 0;i < rangeParaSize;++i)
                dim.setDimension(i+1+one_arm_size*n,combination.automatas.get(n).rangeParameters.get(i).lowerBound,combination.automatas.get(n).rangeParameters.get(i).upperBound,true);

            for(int i = 0;i < maxpathSize;++i)
                dim.setDimension(i+1+rangeParaSize+one_arm_size*n,1,combination.automatas.get(n).cycle / delta,false);
            n++;
        }

        fel = new FelEngineImpl();
        ctx = fel.getContext();
        valueArc = new ValueArc();
    }




    public int getPathLength(){
        return this.maxpathSize;
    }

    public boolean checkConstraints(Automata automata, HashMap<String,Double> parametersValues){
        for(Map.Entry<String,Double> entry : parametersValues.entrySet()){
            ctx.set(entry.getKey(),entry.getValue());
        }
        if(automata.forbiddenConstraints==null)
            return true;
        boolean result = (boolean)fel.eval(automata.forbiddenConstraints);
        if(!result) return true;
        sat = false;
        globalPenalty += computeConstraintValue(automata.forbiddenConstraints.trim());
        return false;
    }

    public double computeConstraintValue(String constraint){
        int firstRightBracket = constraint.trim().indexOf(")");
        if(firstRightBracket != -1 && constraint.indexOf('&') == -1 && constraint.indexOf('|') == -1)
            return computePenalty(constraint.substring(constraint.indexOf('(')+1,constraint.lastIndexOf(")")),false);
        if(firstRightBracket != -1 && firstRightBracket != constraint.length()-1){
            for(int i = firstRightBracket;i < constraint.length();++i){
                if(constraint.charAt(i) == '&'){
                    int index = 0;
                    int numOfBrackets = 0;
                    int partBegin = 0;
                    double pen = 0;
                    while(index < constraint.length()){
                        if(constraint.charAt(index) == '(')
                            ++numOfBrackets;
                        else if(constraint.charAt(index) == ')')
                            --numOfBrackets;
                        else if(constraint.charAt(index) == '&' && numOfBrackets==0){
                            String temp = constraint.substring(partBegin,index);
                            boolean result = (boolean)fel.eval(temp);
                            if(!result) return 0;
                            else pen+= computeConstraintValue(temp);
                            index = index + 2;
                            partBegin = index;
                            constraint = constraint.substring(index);
                            continue;
                        }
                        ++index;
                    }
                    return pen;
                }
                else if(constraint.charAt(i) == '|'){
                    int index = 0;
                    int numOfBrackets = 0;
                    int partBegin = 0;
                    double minPen = Double.MAX_VALUE;
                    while(index < constraint.length()){
                        if(constraint.charAt(index) == '(')
                            ++numOfBrackets;
                        else if(constraint.charAt(index) == ')')
                            --numOfBrackets;
                        else if(constraint.charAt(index) == '|' && numOfBrackets==0){
                            String temp = constraint.substring(partBegin,index);
                            boolean result = (boolean)fel.eval(temp);
                            if(result){
                                temp=temp.trim();
                                minPen = (computeConstraintValue(temp) < minPen) ? computeConstraintValue(temp) : minPen;
                            }
                            index = index+3;
                            partBegin = index;

                            continue;
                        }
                        ++index;
                    }
                    return minPen;
                }
            }
        }
        else{
            if(firstRightBracket != -1){
                constraint = constraint.substring(constraint.indexOf('(')+1,firstRightBracket);
            }
            if(constraint.indexOf('&') != -1){
                String []strings = constraint.split("&");
                double pen = 0;
                for(int i = 0;i < strings.length;++i){
                    if(strings[i].equals("")) continue;
                    boolean result = (boolean)fel.eval(strings[i]);
                    if(!result) return 0;
                    else pen += computeConstraintValue(strings[i]);
                }
                return pen;
            }
            else if(constraint.indexOf('|') != -1){
                String []strings = constraint.split("\\|");
                double minPen = Double.MAX_VALUE;
                for(int i = 0;i < strings.length;++i){
                    if(strings[i].equals("")) continue;
                    boolean result = (boolean) fel.eval(strings[i]);
                    if(!result) continue;
                    else minPen = (computeConstraintValue(strings[i]) < minPen) ? computeConstraintValue(strings[i]) : minPen;
                }
                return minPen;
            }
            else return computePenalty(constraint,false);
        }
        return 0;
    }

    public HashMap<String,Double> computeValuesByFlow(HashMap<String,Double> parametersValues,Location location,double arg){
        HashMap<String,Double> tempMap = new HashMap<>();
        for(HashMap.Entry<String,Double> entry : parametersValues.entrySet()){
            ctx.set(entry.getKey(),entry.getValue());
        }
        for(HashMap.Entry<String,Double> entry : parametersValues.entrySet()){
            if(location.flows.containsKey(entry.getKey())){
                String expression = location.flows.get(entry.getKey());
                if(expression.contains("phi(vx)")){
                    if(parametersValues.get("vx")<=2)
                        expression = expression.replace("phi(vx)","1");
                    else if(parametersValues.get("vx")<=5)
                        expression = expression.replace("phi(vx)","2");
                    else
                        expression = expression.replace("phi(vx)","3");
                }
                if(expression.contains("phi(vy)")){
                    if(parametersValues.get("vy")<=2)
                        expression = expression.replace("phi(vy)","1");
                    else if(parametersValues.get("vy")<=5)
                        expression = expression.replace("phi(vy)","2");
                    else
                        expression = expression.replace("phi(vy)","3");
                }
                double currentTime = System.currentTimeMillis();
                Object obj = fel.eval(expression);
                double endTime = System.currentTimeMillis();
                double temptime=(endTime - currentTime) / 1000;
                time[3] +=temptime;
                double result;
                if(obj instanceof Double)
                    result = (double)obj;
                else if(obj instanceof Integer) {
                    result = (int) obj;
                }
                else if(obj instanceof Long){
                    result = ((Long)obj).doubleValue();
                }
                else {
                    result = 0;
                    System.out.println("Not Double and Not Integer!");
                    System.out.println(obj.getClass().getName());
                    System.out.println(obj);
                    System.out.println(location.flows.get(entry.getKey()));
                    System.exit(0);
                }
                double delta = result * arg;
                tempMap.put(entry.getKey(),entry.getValue() + delta);
            }
            else {
                tempMap.put(entry.getKey(),entry.getValue());
            }
        }
        return tempMap;

    }

    public boolean checkGuards(Automata automata,int index){
        ArrayList<Integer> path_tmp=path.get(index);

        int[] path= new int[path_tmp.size()];
        for(int i=0;i<path_tmp.size();i++){
            path[i]=path_tmp.get(i);
        }

        ArrayList<HashMap<String, Double>> allParametersValues=allParametersValuesList.get(index);

        for(int i = 0;i < path.length;++i){
            Location location = automata.locations.get(path[i]);
            if(i>=allParametersValues.size()){
                int sad=1;
                sad++;
            }
            HashMap<String,Double> parameterValues = allParametersValues.get(i);
            int target;
            if(i + 1 < path.length){
                target = path[i + 1];
                int source = path[i];
                for(int k = 0;k < automata.transitions.size();++k){
                    Transition transition = automata.transitions.get(k);
                    if(transition.source == source && transition.target == target){
                        for(Map.Entry<String,Double> entry : parameterValues.entrySet()){
                            ctx.set(entry.getKey(),entry.getValue());
                        }
                        for(int guardIndex = 0;guardIndex < transition.guards.size();++guardIndex){
                            boolean result = (boolean)fel.eval(transition.guards.get(guardIndex));
                            if(!result) {
                                String guard = transition.guards.get(guardIndex);
                                if(Double.isNaN(computePenalty(guard,false))){
                                    sat = false;
                                    penalty += 100000;
                                }
                                else if(computePenalty(guard,false) > cerr){
                                    sat = false;
                                    penalty += computePenalty(guard, false);
                                }
                            }
                        }
                    }
                }
            }
        }

        return true;
    }

    public double checkCombine(ArrayList<HashMap<String,Double>> newMap){
        double res=0;
        for(int i=0;i< newMap.size();i++){
            for(int j=i+1;j< newMap.size();j++){
                HashMap<String,Double> newMap1=newMap.get(i);
                HashMap<String,Double> newMap2=newMap.get(j);
                res=Math.pow(newMap1.get("x")-newMap2.get("x"),2)+Math.pow(newMap1.get("y")-newMap2.get("y"),2);
                if(res<0.01) {
//                    System.out.print(i);
//                    System.out.println(j);
                    sat=false;
                    globalPenalty+=res;
                }
            }
        }
        return res;
    }
    public boolean checkInvarientsByODE(ArrayList<ArrayList<Double>> argsList){
        double step=0;
        ArrayList<HashMap<String,Double>> allstepMap=new ArrayList<>();
        ArrayList<Double> total_step=new ArrayList<>();
        ArrayList<Integer> locIndexList=new ArrayList<>();
        double max_total=0;
        for(int n=0;n<automata_num;n++){
            HashMap<String,Double> newMap = combination.automatas.get(n).duplicateInitParametersValues();
            allstepMap.add(newMap);
            total_step.add(argsList.get(n).get(argsList.get(n).size()-1));
            max_total=Math.max(max_total,argsList.get(n).get(argsList.get(n).size()-1));
            locIndexList.add(0);
        }
        while(step<max_total) {
            for (int n = 0; n < automata_num; n++) {
                int locIndex = locIndexList.get(n);
                HashMap<String, Double> newMap = allstepMap.get(n);
                Automata automata = combination.automatas.get(n);
                ArrayList<Double> args = argsList.get(n);
                if (step < args.get(locIndex)) {
                    newMap = computeValuesByFlow(newMap, automata.locations.get(path.get(n).get(locIndex)), delta);
                    checkConstraints(automata, newMap);
                    for (HashMap.Entry<String, Double> entry : newMap.entrySet()) {
                        ctx.set(entry.getKey(), entry.getValue());
                    }
                    //check invariants
                    for (int i = 0; i < automata.locations.get(path.get(n).get(locIndex)).invariants.size(); ++i) {
                        boolean result = (boolean) fel.eval(automata.locations.get(path.get(n).get(locIndex)).invariants.get((i)));
                        if (!result) {
                            String invariant = automata.locations.get(path.get(n).get(locIndex)).invariants.get(i);
                            if (computePenalty(invariant, false) < cerr)
                                continue;
                            if (Double.isNaN(computePenalty(invariant, false))) {
                                sat = false;
                                penalty += 100000;
                                infeasiblelist.get(n).set(locIndex, false);
                            } else {
                                sat = false;
                                //System.out.println(invariant);
                                penalty += computePenalty(invariant, false);
                                infeasiblelist.get(n).set(locIndex, false);
                            }

                        }
                    }
                    if(step==args.get(locIndex)-1){
                        allParametersValuesList.get(n).add(newMap);
                        if(locIndex!=path.get(n).size()-1) {
                            locIndex++;

                            Transition transition = automata.getTransitionBySourceAndTarget(path.get(n).get(locIndex-1), path.get(n).get(locIndex));
                            if (transition == null) {
                                System.out.println("Found no transition");
                                System.exit(-1);
                            }
                            for (HashMap.Entry<String, String> entry : transition.assignments.entrySet()) {
                                Object obj = fel.eval(entry.getValue());
                                double result = 0;
                                if (obj instanceof Integer) result = (int) obj;
                                else if (obj instanceof Double) result = (double) obj;
                                else {
                                    System.out.println("Not Double and Not Integer!");
                                    System.out.println(entry.getValue());
                                    System.exit(0);
                                }
                                newMap.put(entry.getKey(), result);
                            }
                        }
                    }
                    locIndexList.set(n,locIndex);
                    allstepMap.set(n,newMap);
                }
            }
            checkCombine(allstepMap);
            step++;
        }

        return true;
    }

//    public void updateInstanceRegion(Instance ins){
////        System.out.println("updateRegion");
//        ArrayList<Integer> choice=PathMap.get((int)ins.getFeature(0));
//        path1=new int[choice.size()];
//        for(int i=0;i<choice.size();i++){
//            path1[i]=choice.get(i);
//        }
//        double []args = new double[path1.length];
//        for(int i =0 ;i < rangeParaSize;++i){
//            automata.initParameterValues.put(automata.rangeParameters.get(i).name,ins.getFeature(i+1));
//        }
//        for(int i = 0;i < path1.length;++i){
//            args[i] = ins.getFeature(i+1+rangeParaSize);
//        }
//        HashMap<String,Double> initParameter = automata.duplicateInitParametersValues();
//        for(int i =0 ;i < rangeParaSize;++i){
//            initParameter.put(automata.rangeParameters.get(i).name,ins.getFeature(i+1));
//        }
//        double end = 0;
//        HashMap<String,Double> newMap = initParameter;
//        for(int locIndex = 0;locIndex < path1.length;++locIndex){
//            end = args[locIndex];
//            double step = 0;
//            if(locIndex != 0){
//                Transition transition = automata.getTransitionBySourceAndTarget(path1[locIndex - 1],path1[locIndex]);
//                if(transition == null){
//                    System.out.println("Found no transition");
//                    System.exit(-1);
//                }
//                for(HashMap.Entry<String,String> entry : transition.assignments.entrySet()){
//                    Object obj = fel.eval(entry.getValue());
//                    double result = 0;
//                    if(obj instanceof Integer)  result = (int)obj;
//                    else if(obj instanceof Double) result = (double)obj;
//                    else{
//                        System.out.println("Not Double and Not Integer!");
//                        System.out.println(entry.getValue());
//                        System.exit(0);
//                    }
//                    newMap.put(entry.getKey(),result);
//                    allParametersValues1.get(locIndex - 1).put(entry.getKey(),result);
//                }
//            }
//            if(end==0){
//                if(!checkConstraints(args,newMap)){
//                    System.out.println("end==0");
//                    ins.region[locIndex][1] = 1;
//                    return;
//                }
//            }
//            while(step < end){
//                newMap = computeValuesByFlow(newMap,automata.locations.get(path1[locIndex]),delta);
//                for(HashMap.Entry<String,Double> entry : newMap.entrySet()){
//                    ctx.set(entry.getKey(),entry.getValue());
//                }
//                if(!checkConstraints(args,newMap)) {
//                    if(step == 0) ++step;
//                    for(int i = 0;i < args.length;++i)
//                        System.out.print(args[i] + " ");
//                    System.out.println("locIndex " + locIndex);
//                    ins.region[locIndex][1] = step;
//                    return;
//                }
//                for(int i = 0;i < automata.locations.get(path1[locIndex]).invariants.size();++i){
//                    boolean result = (boolean)fel.eval(automata.locations.get(path1[locIndex]).invariants.get((i)));
//                    if(!result) {
//                        String invariant = automata.locations.get(path1[locIndex]).invariants.get(i);
//                        if(computePenalty(invariant,false) < cerr)
//                            continue;
//                        if(Double.isNaN(computePenalty(invariant,false))){
//                            System.out.println("NaN");
//                            System.exit(-1);
//                        }
//                        else {
//                            ins.region[locIndex][1] = step;
//                            return;
//                        }
//
//                    }
//                }
//                step += 1;
//            }
//        }
//    }

    private double computePenaltyOfConstraint(String expression){//just one level
        String []expressions = expression.split("\\|");
        double result = Double.MAX_VALUE;
        for(String string:expressions){
            if(string.length()<=0)  continue;
            double temp = computePenalty(string,false);
            result = (temp < result) ? temp : result;
        }
        return result;
    }

    private double computePenalty(String expression,boolean isConstraint){
        if(isConstraint && expression.indexOf("|") != -1)
            return computePenaltyOfConstraint(expression);

        String []strings;
        String bigPart = "",smallPart = "";
        strings = expression.split("<=|<|>=|>|==");
        Object obj1 = fel.eval(strings[0].trim());
        Object obj2 = fel.eval(strings[1].trim());
        double big = 0,small = 0;
        if(obj1 instanceof Double)
            big = (double)obj1;
        else if(obj1 instanceof Integer) {
            big = (int) obj1;
            //System.out.println(entry.getKey() + " " + entry.getValue());
        }
        else {
            System.out.println("Not Double and Not Integer!");
            System.out.println(expression);
            System.out.println(obj1);
            System.out.println(obj1.getClass().getName());
            System.out.println("here");
            System.exit(0);
        }
        if(obj2 instanceof Double)
            small = (double)obj2;
        else if(obj2 instanceof Integer) {
            small = (int) obj2;
        }
        else if(obj2 instanceof Long){
            small = ((Long)obj2).doubleValue();
        }
        else {
            small = 0;
            System.out.println("Not Double and Not Integer!");
            System.exit(0);
        }
        return Math.abs(big-small);
    }


    @Override
    public double getValue(Instance ins) {
        penalty = 0;
        globalPenalty = 0;
        sat = true;
        allParametersValuesList=new ArrayList<>();
        infeasiblelist =new ArrayList<>();
        path=new ArrayList<>();
        int one_arm_size = rangeParaSize + maxpathSize;
        ArrayList<ArrayList<Double>> args=new ArrayList<>();
        for(int n=0;n<automata_num;n++){
            ArrayList<HashMap<String,Double>> allParametersValues = new ArrayList<>();
            allParametersValuesList.add(allParametersValues);

            Automata automata=combination.automatas.get(n);
            int choice=combinPath.get((int)ins.getFeature(0)).get(n);
            ArrayList<Integer> path_tmp = PathMap.get(choice);
            path.add(path_tmp);

            ArrayList<Boolean> infeasible_loc =new ArrayList<>();
            for(int i = 0; i< path_tmp.size(); i++){
                infeasible_loc.add(true);
            }
            infeasiblelist.add(infeasible_loc);

            ArrayList<Double> args_tmp=new ArrayList<>();
            for(int i =0 ;i < rangeParaSize;++i){
                automata.initParameterValues.put(automata.rangeParameters.get(i).name,ins.getFeature(i+1+one_arm_size*n));
            }
            double sum=0;
            for(int i = 0;i < path_tmp.size();++i){
                double step=ins.getFeature(i+1+rangeParaSize+one_arm_size*n);
                if(step==0) continue;
                args_tmp.add(i,step+sum);
                sum=args_tmp.get(i);
            }
            args.add(args_tmp);
        }

        checkInvarientsByODE(args);
        for(int n=0;n<automata_num;n++)
            checkGuards(combination.automatas.get(n),n);
        if(!sat) {
            if(penalty + globalPenalty == 0){
                //todo cfg file should have brackets
                System.out.println("penalty = 0 when unsat");
                return Double.MAX_VALUE;
            }
            double penAll = penalty + globalPenalty;
            if(penAll < valueArc.penAll) {
                valueArc.penalty = penalty;
                valueArc.globalPenalty = globalPenalty;
                valueArc.penAll = penAll;
            }
            return penAll;
        }
        double value=0;
        double[] v=new double[automata_num];
        for(int n=0;n<automata_num;n++){
            v[n]=computeValue(n);
            value+=v[n];
        }

        if(value<-30000){
            int a=0;
        }
//        if(value1<0||value2<0){
//            System.out.println("YES");
//        }
//        if(value1*value2>0)
//            return value1+value2;
//        else return value1+value2+10000;
        return value;
    }


    public double computeValue(int index){
        Automata automata=combination.automatas.get(index);
        ArrayList<HashMap<String, Double>> allParametersValues = allParametersValuesList.get(index);
        if(allParametersValues.size()<1) {
            sat=false;
            valueArc.penalty=100;
            return valueArc.penalty;
        }
        HashMap<String,Double> map = allParametersValues.get(allParametersValues.size() - 1);
        if(Math.pow(automata.target_x - map.get("x"), 2) >1){
            sat=false;
            valueArc.penalty=Math.pow(automata.target_x - map.get("x"), 2)/10000;
            return valueArc.penalty;
        }
        for(HashMap.Entry<String,Double> entry : map.entrySet()){
            ctx.set(entry.getKey(),entry.getValue());
        }
        ctx.set("target_x",automata.target_x);
        ctx.set("target_y",automata.target_y);
        Object obj = fel.eval(automata.obj_function);
        double value = 0;
        if(obj instanceof Double)
            value = (double)obj - 10000;
        else if(obj instanceof Integer){
            value = (int) obj - 10000;
        }
        else {
            System.err.println("error: result not of double!");
            System.out.println(obj);
            return Double.MAX_VALUE;
        }
        if (value + 10000 < 0){
            System.err.println("error: objective function result is negtive!");
            System.out.println(map.get("x"));
            return Double.MAX_VALUE;
        }
        if(value < valueArc.value){
            valueArc.value = value;
            valueArc.allParametersValues = allParametersValues.get(allParametersValues.size()-1);
        }
        return value;
    }

    @Override
    public Dimension getDim() {
        return dim;
    }

    public double[] getTime(){
        return time;
    }

    @Override
    public ArrayList<Boolean> getInfeasibleLoc(int index) {
        return infeasiblelist.get(index);
    }
}
