package MPC;

import Racos.Componet.Instance;
import Racos.Method.Continue;
import Racos.ObjectiveFunction.ObjectFunction;
import Racos.ObjectiveFunction.Task;
import Racos.Tools.ValueArc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

public class Combination {
    public String forbiddenConstraints;
    public ArrayList<Automata> automatas;
    public int automata_num;
    File output;
    BufferedWriter bufferedWriter;
    public ArrayList<ArrayList<Integer>> PathMap;
    public ArrayList<ArrayList<Integer>> combin;
    public String forbidden;
    public double delta ;
    public double cycle;
    public ValueArc minValueArc;

    public Combination(ArrayList<Automata> list) {
        automatas=list;
        automata_num=automatas.size();
    }


    public void print(String str) {
        try {
            bufferedWriter.write(str);
        } catch (IOException e) {
            System.out.println("write to file error!");
        }
    }

    public void println(String str) {
        try {
            bufferedWriter.write(str + "\n");
        } catch (IOException e) {
            System.out.println("write to file error!");
        }
    }
    double[] runRacos(Combination combination, int maxPathsize) {
        int samplesize = 10;       // parameter: the number of samples in each iteration
        int iteration = 5000;       // parameter: the number of iterations for batch racos
        int budget = 2000;         // parameter: the budget of sampling for sequential racos
        int positivenum = 2;       // parameter: the number of positive instances in each iteration
        double probability = 0.95; // parameter: the probability of sampling from the model
        int uncertainbit = 1;      // parameter: the number of sampled dimensions
        Instance ins = null;
        int repeat = 1;
        Task t = new ObjectFunction(combination,maxPathsize);
        double[] info = new double[0];
        for (int i = 0; i < repeat; i++) {
            double currentT = System.currentTimeMillis();
            Continue con = new Continue(t, combination);
            con.setMaxIteration(iteration);
            con.setSampleSize(samplesize);      // parameter: the number of samples in each iteration
            con.setBudget(budget);              // parameter: the budget of sampling
            con.setPositiveNum(positivenum);    // parameter: the number of positive instances in each iteration
            con.setRandProbability(probability);// parameter: the probability of sampling from the model
            con.setUncertainBits(uncertainbit); // parameter: the number of samplable dimensions
            con.setBound(maxPathsize);
            con.setPathMap(PathMap);
            ValueArc valueArc = con.run();                          // call sequential Racos              // call Racos
//            ValueArc valueArc = con.RRT();                          // call sequential Racos              // call Racos
//            ValueArc valueArc = con.monte();                          // call sequential Racos              // call Racos
//            ValueArc valueArc = con.run2();
            System.out.println("one component path choices: "+PathMap.size());
            System.out.println("total path choices: "+combin.size());

            double currentT2 = System.currentTimeMillis();
            ins = con.getOptimal();             // obtain optimal

            if (minValueArc == null || minValueArc.value >= valueArc.value) {
                minValueArc = valueArc;
//                int choice1=combin.get((int)ins.getFeature(0)).get(0);
//                int choice2=combin.get((int)ins.getFeature(0)).get(1);
//                ArrayList<Integer> path_tmp1 = PathMap.get(choice1);
//                ArrayList<Integer> path_tmp2 = PathMap.get(choice2);
//                minValueArc.path1=path_tmp1;
//                minValueArc.path2=path_tmp2;
//                System.out.print("best path1:");
//                for(int k=0;k<path_tmp1.size();k++){
//                    System.out.print(path_tmp1.get(k));
//                }
//                System.out.println(" ");
//                System.out.print("best path2:");
//                for(int k=0;k<path_tmp2.size();k++){
//                    System.out.print(path_tmp2.get(k));
//                }
//                System.out.println(" ");
            }
            info=new double[]{ins.getValue(),valueArc.cover,combin.size(),
                    valueArc.time_cost[0],valueArc.time_cost[1],valueArc.time_cost[2],valueArc.time_cost[3],
                    valueArc.time_cost[4],valueArc.time_cost[5],valueArc.time_cost[6],
                    //valueArc.time_cost[7],valueArc.time_cost[8],
//                    valueArc.accuracy[0],valueArc.accuracy[1],valueArc.accuracy[2],valueArc.accuracy[3]
                    };
            System.out.print("best function value:");
            System.out.println(ins.getValue() + "     ");



            print("best function value:");
            print(ins.getValue() + "     ");

            //System.out.print("[");
            System.out.print("[");
            for (int j = 0; j < ins.getFeature().length; ++j) {
                System.out.print(Double.toString(ins.getFeature(j)) + ",");
            }
            System.out.println("]");
        }
        return info;
    }




    public static void main(String[] args) {
        configUtil config = new configUtil();
        String prefix = new String("models/" + config.get("system") + "_" + config.get("mission"));
        String modelFile = prefix + ".xml";
        int mata_num=Integer.parseInt(config.get("number"));

        File result=new File("result1.txt");
        double value;
        double choices,total_choices;


        try {
            BufferedWriter buffer=new BufferedWriter(new FileWriter(result));
            int round=1;
            while(round>0){
                System.out.println("round:"+Integer.toString(round));
                System.out.println("free:" + Runtime.getRuntime().freeMemory() / 1024 / 1024);
                System.out.println("total:" + Runtime.getRuntime().totalMemory() / 1024 / 1024);
                System.out.println("max:" + Runtime.getRuntime().maxMemory() / 1024 / 1024);
                Runtime r=Runtime.getRuntime();
                r.gc();
                long startMem=r.freeMemory();

                double currentTime = System.currentTimeMillis();

                ArrayList<Automata> automataList=new ArrayList<>();
                for(int n=1;n<=mata_num;n++){
                    String cfgFile_tmp = prefix + Integer.toString(n)+ ".cfg";
                    Automata tmp=new Automata(modelFile, cfgFile_tmp);
                    automataList.add(tmp);
                }
                Combination combination=new Combination(automataList);

                combination.output = new File("logs.txt");
                combination.bufferedWriter = new BufferedWriter(new FileWriter(combination.output));
//            automata1.checkAutomata();
                int maxPathSize = Integer.parseInt(config.get("bound"));
                combination.automatas.get(0).createMap(maxPathSize);
                combination.PathMap=combination.automatas.get(0).PathMap;

                combination.setCombin();

                combination.cycle=combination.automatas.get(0).cycle;
                combination.delta=combination.automatas.get(0).delta;

                //{findFragTime,findPointTime,checkGoodOrNot,recordFragTime};
                double[] ans=combination.runRacos(combination,maxPathSize);


                System.out.println("free:" + Runtime.getRuntime().freeMemory() / 1024 / 1024);
                System.out.println("total:" + Runtime.getRuntime().totalMemory() / 1024 / 1024);
                System.out.println("max:" + Runtime.getRuntime().maxMemory() / 1024 / 1024);

                value=ans[0];
                choices=ans[1];
                total_choices=ans[2];
                double findFragTime=ans[3];
                double findPointTime=ans[4];
                double checkGoodOrNot=ans[5];
                double recordFragTime=ans[6];
                int stop_num=(int)ans[7];
                int total_sample=(int)ans[8];
//                int badpointsize=(int)ans[9];
//                int badfragment1=(int)ans[10];
//                int badfragment2=(int)ans[11];
//                int wrong_frag1= (int) ans[12];
//                int wrong_frag2= (int) ans[13];
//                int miss_frag1= (int) ans[14];
//                int miss_frag2=(int)ans[15];

                combination.bufferedWriter.close();
                double endTime = System.currentTimeMillis();
                double t=(endTime - currentTime) / 1000;


                long endMen=Math.abs(r.freeMemory()-startMem);
                endMen=endMen/1024/1024;
                System.out.println("Time cost :" + (endTime - currentTime) / 1000 + " seconds");
                System.out.println("Memory cost :" + endMen );

                String tmp=Double.toString(t)+" "+Double.toString(value)+" ";

//                ArrayList<Integer> path1=combination.minValueArc.path1;
//                tmp+=Integer.toString(path1.get(0));
//                for(int k=1;k<path1.size();k++){
//                    tmp+="->"+Integer.toString(path1.get(k));
//                }
//                tmp+=" ";
//                ArrayList<Integer> path2=combination.minValueArc.path2;
//                tmp+=Integer.toString(path2.get(0));
//                for(int k=1;k<path2.size();k++){
//                    tmp+="->"+Integer.toString(path2.get(k));
//                }
//                tmp+=" ";
//                tmp+=Integer.toString((int)choices)+"/"+Integer.toString((int)total_choices)+" ";
//                tmp+=Double.toString(recordFragTime)+" "+Double.toString(findFragTime)+" "+Double.toString(findPointTime)+" "+Double.toString(checkGoodOrNot)+" ";
//                tmp+=Integer.toString(badpointsize)+" "+Integer.toString(badfragment1)+" "+Integer.toString(badfragment2)+" ";
//                tmp+=Integer.toString(wrong_frag1)+" "+Integer.toString(wrong_frag2)+" "+Integer.toString(miss_frag1)+" "+Integer.toString(miss_frag2)+" ";
                //tmp+=Integer.toString(stop_num)+" ";
                tmp+=Integer.toString(total_sample)+" ";

                tmp+= Integer.toString((int) endMen)+" ";

                tmp+=Integer.toString((int)choices)+" "+Integer.toString((int)total_choices)+" ";


                tmp+="\n";

                try {
                    buffer.write(tmp);
                } catch (IOException e) {
                    System.out.println("write to file error!");
                }
                round--;
            }
            buffer.close();
        }catch (IOException e) {
            System.out.println("Open result.txt fail!");
        }


    }

    private void setCombin() {
        combin=new ArrayList<>();
        for(int i=0;i<PathMap.size();i++){
            ArrayList<Integer> tmp=new ArrayList<>();
            tmp.add(i);
            combin.add(tmp);
        }
        ArrayList<ArrayList<Integer>> pre=new ArrayList<>();
        int n=1;
        while (n<automata_num){
            pre.clear();
            for(int i=0;i<combin.size();i++){
                ArrayList<Integer> current= combin.get(i);
                for(int j=0;j<PathMap.size();j++){
                    ArrayList<Integer> tmp= (ArrayList<Integer>) current.clone();
                    tmp.add(j);
                    pre.add(tmp);
                }
            }
            combin= (ArrayList<ArrayList<Integer>>) pre.clone();
            n++;
        }
    }


}



