/**
 * Class continue
 * @author Yi-Qi Hu
 * @time 2015.11.14
 * @version 2.0
 * continue class is the method of Racos in continuous feasible region
 */

package Racos.Method;

import MPC.Combination;
import Racos.Tools.*;

import java.util.*;

import Racos.Componet.*;
import Racos.ObjectiveFunction.*;

public class Continue extends BaseParameters{
	
	private Task task;                   //the task that algorithm will optimize       
	private Dimension dimension;         //the task dimension message that algorithm optimize
	private Instance[] Pop;              //the instance set that algorithm will handle with
	private Instance[] NextPop;          //the instance set that algorithm will generate using instance set Pop
	private Instance[] PosPop;           //the instance set with best objective function value
	private Instance Optimal;            //an instance with the best objective function value
	private boolean on_off;				 //switch of sequential racos
	private int BudCount;
	private Model model;
	private RandomOperator ro;

	private Combination combination;
	public int auto_num;
	public int bound;
	public int pathChoice;
	public HashSet<Integer> badPoint;
	public ArrayList<HashMap<Double,Integer>> badFragment;
	public ArrayList<HashMap<Integer,FeasibleInfo>> feasib;
//	public HashMap<Integer, PathInfo> eachPath;
	public HashMap<Integer,Double> eachBest;
	public HashMap<Integer,Integer> occurtime;

	public boolean optimal=true;
	public ArrayList<ArrayList<Integer>> PathMap;
	public ArrayList<ArrayList<Integer>> combin;
	public double recordFragTime =0;
	
	private class Model{                 //the model of generating next instance
		
		public double[][] region;//shrinked region
		public boolean[] label;  //if label[i] is false, the corresponding dimension should be randomized from region
		
		public Model(int size){
			region = new double[size][2];
			label = new boolean[size];
			for(int i=0; i<size; i++){
				region[i][0] = 0;
				region[i][1]  =1;
				label[i] = false;
			}
		}
		
		public void PrintLabel(){
			for(int i=0; i<label.length; i++){
				if(!label[i]){
					System.out.print(i+" ");
				}
			}
			System.out.println();
		}
	}
	
	/**
	 * constructors function
	 * user must construct class Continue with a class which implements interface Task
	 * 
	 * @param ta  the class which implements interface Task
	 */
	public Continue(Task ta, Combination combination){
		task = ta;
		dimension = ta.getDim();
		ro = new RandomOperator();
		this.on_off = false; 		//set batch-racos
		this.combination = combination;
		badPoint=new HashSet<>();
		badFragment=new ArrayList<>();
		feasib=new ArrayList<>();
//		eachPath=new HashMap<>();
		auto_num=combination.automata_num;
		for(int i=0;i<auto_num;i++){
			HashMap<Double,Integer> tmpFrag=new HashMap<>();
			badFragment.add(tmpFrag);
			HashMap<Integer,FeasibleInfo> tmpFeas=new HashMap<>();
			feasib.add(tmpFeas);
		}
		this.combin=combination.combin;
		eachBest=new HashMap<>();
		occurtime=new HashMap<>();
//		occurBest=new HashMap<>();
//		eachBest=new HashMap<>();
//		occurtime=new HashMap<>();

//		infeasible =new HashMap<>();

//		for(int i=0;i<pathChoice;i++){
//			eachBest.put(i,Double.MAX_VALUE);
//		}
//		for(int i=0;i<pathChoice;i++){
//			occurtime.put(i,0);
//		}

	}
	public void setPathMap(ArrayList<ArrayList<Integer>> pathMap){
		PathMap=pathMap;
		pathChoice=pathMap.size();
	}

	public void setBound(int bound){
		this.bound=bound;
	}
	//the next several functions are prepared for testing
	public void PrintPop(){
		System.out.println("Pop set:");
		if(Pop.length==0){
			System.out.println("Pop set is null!");
		}
		for(int i=0; i<Pop.length; i++){
			Pop[i].PrintInstance();
		}
	}
	public void PrintPosPop(){
		System.out.println("PosPop set:");
		if(PosPop.length==0){
			System.out.println("PosPop set is null!");
		}
		for(int i=0; i<PosPop.length; i++){
			PosPop[i].PrintInstance();
		}
	}
	public void PrintNextPop(){
		System.out.println("NextPop set:");
		if(NextPop.length==0){
			System.out.println("NextPop set is null!");
		}
		for(int i=0; i<NextPop.length; i++){
			NextPop[i].PrintInstance();
		}
	}
	public void PrintOptimal(){
		System.out.println("Optimal:");
		Optimal.PrintInstance();
	}
	public void PrintRegion(){
		System.out.println("Region:");
		for(int i=0; i<model.region.length; i++){
			System.out.print("["+model.region[i][0]+","+model.region[i][1]+"] ");
		}
		System.out.println();
	}
	
	/**
	 * using sequential Racos
	 */
	public void TurnOnSequentialRacos(){
		this.on_off = true;
		return ;
	}
	
	/**
	 * using batch-Racos
	 */
	public void TurnOffSequentialRacos(){
		this.on_off = false;
		return ;
	}
	
	/**
	 * 
	 * @return the optimal that algorithm found
	 */
	public Instance getOptimal(){
		return Optimal;
	}
	
	/**
	 * RandomInstance without parameter
	 * 
	 * @return an Instance, each feature in this instance is a random number from original feasible region
	 */
	protected Instance RandomInstance(){
		Instance ins = new Instance(dimension);

		for (int i = 0; i < dimension.getSize(); i++) {

			if (dimension.getType(i)) {//if i-th dimension type is continue
				ins.setFeature(i, ro.getDouble(dimension.getRegion(i)[0], dimension.getRegion(i)[1]));
			} else {//if i-th dimension type is discrete
				ins.setFeature(i, ro.getInteger((int) dimension.getRegion(i)[0], (int) dimension.getRegion(i)[1]));
			}
		}
//			double sum = 0;
//			for(int j = 0;j < ((ObjectFunction)task).getPathLength();++j){
//				sum += ins.getFeature(j);
//			}
//			if(sum <= combination.cycle/combination.delta)
//				break;

		return ins;
	}
	
	/**
	 * Random instance with parameter
	 * 
	 * @param pos, the positive instance that generate the model
	 * @return an Instance, each feature in this instance is a random number from a feasible region named model
	 */
	protected Instance RandomInstance(Instance pos){
//		System.out.println("RandomInstance");
		Instance ins = new Instance(dimension);
//		model.PrintLabel();
		while(true) {
			for (int i = 0; i < dimension.getSize(); i++) {

				if (dimension.getType(i)) {//if i-th dimension type is continue
					if (model.label[i]) {//according to fixed dimension, valuate using corresponding value in pos
						ins.setFeature(i, pos.getFeature(i));
					} else {//according to not fixed dimension, random in region
//					System.out.println("["+model.region[i][0]+", "+model.region[i][1]+"]");
						ins.setFeature(i, ro.getDouble(Math.max(pos.region[i][0],model.region[i][0]),Math.min(pos.region[i][1],model.region[i][1])));
					}
				} else {//if i-th dimension type is discrete
					if (model.label[i]) {//according to fixed dimension, valuate using corresponding value in pos
						ins.setFeature(i, pos.getFeature(i));
					} else {//according to not fixed dimension, random in region
//						if(pos.region[i][1] < model.region[i][1]) {
//							System.out.println("optimization");
//						}
						int bound1 = Math.max((int) pos.region[i][0],(int) model.region[i][0]), bound2 = Math.min((int) pos.region[i][1],(int) model.region[i][1]);
						if(bound1 > bound2){
//							System.out.println("Dimsion is " + i);
//							System.out.println("pos region [" + pos.region[i][0] + "," + pos.region[i][1] + "]");
//							System.out.println("model region [" + model.region[i][0] + "," + model.region[i][1] + "]");
							ins.setFeature(i,ro.getInteger((int) model.region[i][0],(int) model.region[i][1]));
//							int tmp=ro.getInteger((int) model.region[i][0],(int) model.region[i][1]);
//							if(i==0){
//								String path=getpath(ins,i,tmp);
//								if(badPoint.contains(path)){
//									//System.out.println(Integer.toString(tmp)+" is a bad point!");
////									do{
////										tmp=ro.getInteger((int) model.region[i][0],(int) model.region[i][1]);
////										path=getpath(ins,i,tmp);
////									}while(badPoint.contains(path));
//								}
//							}
//							ins.setFeature(i, tmp);
						}
						else {
							ins.setFeature(i, ro.getInteger(bound1,bound2));
//							int tmp=ro.getInteger(bound1,bound2);
//							if(i==0){
//								String path=getpath(ins,i,tmp);
//								if(badPoint.contains(path)){
//									//System.out.println(Integer.toString(tmp)+" is a bad point!");
////									do{
////										tmp=ro.getInteger(bound1,bound2);
////										path=getpath(ins,i,tmp);
////									}while(badPoint.contains(path));
//								}
//							}
//							ins.setFeature(i, tmp);
						}
					}
				}

			}
			break;
//			double sum = 0;
//			for(int j = 0;j < ((ObjectFunction)task).getPathLength();++j){
//				sum += ins.getFeature(j);
//			}
//			if(sum <= combination.cycle/combination.delta)
//				break;
		}
		return ins;
	}

	private String getpath(Instance ins, int i, int newvalue) {
		double last=-1;
		String path_tmp="";
		int value;
		for(int j=0;j<bound;j++){
			if(j==i) value=newvalue;
			else value=(int)ins.getFeature(j);
			if(value==0) break;
			if(value==last) continue;
			path_tmp+=Integer.toString(value)+" ";
			last=ins.getFeature(j);
		}
		return path_tmp;
	}

	/**
	 * initialize Pop, NextPop, PosPop and Optimal
	 * 
	 */
	@SuppressWarnings("unchecked")
	protected void Initialize(){
		
		Instance[] temp = new Instance[SampleSize+PositiveNum];
		
		Pop = new Instance[SampleSize];
		
		//sample Sample+PositiveNum instances and add them into temp
		for(int i=0; i<SampleSize+PositiveNum; i++){
			temp[i] = RandomInstance();

			double val=task.getValue(temp[i]);
			temp[i].setValue(val);
			if(optimal) {
				int path_tmp= (int) temp[i].getFeature(0);

				if (eachBest.containsKey(path_tmp)){
					double last_value=eachBest.get(path_tmp);
					if(last_value>val){
						eachBest.put(path_tmp,val);
					}
					int occur=occurtime.get(path_tmp);
					occurtime.put(path_tmp,occur+1);
				}else{
					eachBest.put(path_tmp,val);
					occurtime.put(path_tmp,1);
				}
				double time1=System.currentTimeMillis();
				for(int l=0;l<auto_num;l++){
					int choice=combin.get(path_tmp).get(l);
					ArrayList<Integer> path=PathMap.get(choice);
					FeasibleInfo feasibleInfo;

					if(feasib.get(l).containsKey(choice)){
						feasibleInfo=feasib.get(l).get(choice);
					}else{
						feasibleInfo=new FeasibleInfo(path);
					}
					feasibleInfo.setInfeasible(task.getInfeasibleLoc(l));
					feasib.get(l).put(choice,feasibleInfo);

				}
				double time2=System.currentTimeMillis();
				recordFragTime+=(time2-time1)/1000;


//			if(occurBest.containsKey(path_tmp)){
//				if(occurBest.get(path_tmp)>val) occurBest.put(path_tmp,val);
//			}
//			else occurBest.put(path_tmp,val);

//				if (eachPath.containsKey(path_tmp)) {
//					PathInfo tmp = eachPath.get(path_tmp);
//					if (tmp.getBest_value() > val) tmp.setBest_value(val);
//					double time1=System.currentTimeMillis();
//					tmp.setInfeasible1(task.getInfeasibleLoc1());
//					tmp.setInfeasible2(task.getInfeasibleLoc2());
//					double time2=System.currentTimeMillis();
//					recordFragTime+=(time2-time1)/1000;
//					tmp.setPath1(task.getPath1());
//					tmp.setPath2(task.getPath2());
//				} else {
//					PathInfo tmp = new PathInfo(1, val);
//					double time1=System.currentTimeMillis();
//					tmp.setInfeasible1(task.getInfeasibleLoc1());
//					tmp.setInfeasible2(task.getInfeasibleLoc2());
//					double time2=System.currentTimeMillis();
//					recordFragTime+=(time2-time1)/1000;
//					tmp.setPath1(task.getPath1());
//					tmp.setPath2(task.getPath2());
//					eachPath.put(path_tmp, tmp);
//				}
			}
//			int ii=(int)temp[i].getFeature(0);
//			if(eachBest.get(ii)>val){
//				eachBest.put(ii,val);
//			}
//			int cur_times=occurtime.get(ii);
//			occurtime.put(ii,cur_times+1);


			//temp[i].setValue(task.getValue(temp[i]));
			if (this.on_off){
				this.BudCount++;
			}

//			if(infeasible.containsKey(path_tmp)){
//				List<List<Boolean>> t=infeasible.get(path_tmp);
//				t.add(task.getInfeasibleLoc());
//				infeasible.put(path_tmp,t);
//			}else{
//				List<List<Boolean>> t=new ArrayList<>();
//				t.add(task.getInfeasibleLoc());
//				infeasible.put(path_tmp,t);
//			}

		}	
				
		//sort Pop according to objective function value
		InstanceComparator comparator = new InstanceComparator();
		java.util.Arrays.sort(temp,comparator);
		
		//initialize Optimal
		Optimal = temp[0].CopyInstance();
		
		//after sorting, the beginning several instances in temp are used for initializing PosPop
		PosPop = new Instance[PositiveNum];
		for(int i=0; i<PositiveNum; i++){
			PosPop[i] = temp[i];
//			((ObjectFunction)task).updateInstanceRegion(PosPop[i]);
		}
		
		Pop = new Instance[SampleSize];
		for(int i=0; i<SampleSize; i++){
			Pop[i] = temp[i+PositiveNum];
		}
		
		//initialize NextPop
		NextPop = new Instance[SampleSize];
		
		model = new Model(dimension.getSize());
		
		return ;
		
	}
	
	/**
	 * reset sampling model
	 * @return the model with original feasible region and all label is true
	 */
	protected void ResetModel(){
		for(int i=0; i<dimension.getSize(); i++){
			model.region[i][0] = dimension.getRegion(i)[0];	
			model.region[i][1] = dimension.getRegion(i)[1];	
			model.label[i] = false;
		}
		return ;
	}
	
	/**
	 * shrink model for instance pos
	 * 
	 * @param pos
	 */
	protected void ShrinkModel(Instance pos){
//		System.out.println("ShrinkModel");
		int ChoosenDim;  
		int ChoosenNeg;  
		double tempBound;
		
		int ins_left = SampleSize;
		int c = 0;
		while (ins_left>0) {//generate the model
			if(c>1000){
				System.out.println("dead loop");
			}
			++c;
			ChoosenDim = ro.getInteger(0, dimension.getSize() - 1);//choose a dimension randomly
			ChoosenNeg = ro.getInteger(0, this.SampleSize - 1);    //choose a negative instance randomly
			// shrink model
			if (pos.getFeature(ChoosenDim) < Pop[ChoosenNeg].getFeature(ChoosenDim)) {
				tempBound = ro.getDouble(pos.getFeature(ChoosenDim), Pop[ChoosenNeg].getFeature(ChoosenDim));
				if (tempBound < model.region[ChoosenDim][1]) {
					model.region[ChoosenDim][1] = tempBound;
					int i=0;
					while(i<ins_left){
						if(Pop[i].getFeature(ChoosenDim)>=tempBound){
							ins_left--;
							Instance tempins = Pop[i];
							Pop[i] = Pop[ins_left];
							Pop[ins_left] = tempins;
						}else{
							i++;
						}
					}

				}
			} else {
				tempBound = ro.getDouble(Pop[ChoosenNeg].getFeature(ChoosenDim), pos.getFeature(ChoosenDim));
				if (tempBound > model.region[ChoosenDim][0]) {
					model.region[ChoosenDim][0] = tempBound;
					int i=0;
					while(i<ins_left){
						if(Pop[i].getFeature(ChoosenDim)<=tempBound){
							ins_left--;
							Instance tempins = Pop[i];
							Pop[i] = Pop[ins_left];
							Pop[ins_left] = tempins;
						}else{
							i++;
						}
					}
				}
			}
		}
		return ;
		
	}
	
	/**
	 * make sure that the number of random dimension is smaller than the threshold UncertainBits
	 * 
	 * @param
	 * @return
	 */
	protected void setRandomBits(){
//		System.out.println("setRandomBits");
		int labelMarkNum;
		int[] labelMark = new int[dimension.getSize()];
		int tempLab;
		labelMarkNum = dimension.getSize();
		for(int k=0; k<dimension.getSize(); k++){
			labelMark[k] = k;
		}
		for (int k = 0; k < dimension.getSize()-UncertainBits; k++) {						
			tempLab = ro.getInteger(0, labelMarkNum-1);
			model.label[labelMark[tempLab]] = true;
			labelMark[tempLab] = labelMark[labelMarkNum-1];
			labelMarkNum--;
		}
		return ;
		
	}
	
	/**
	 * if each instance in Pop is not in model, return true; if exist more than one instance in Pop is in the model, return false
	 * 
	 * @param
	 * @return true or false
	 */
	protected boolean Distinguish(){
		int j;
		for(int i=0;i<this.SampleSize; i++){
			for(j=0; j<dimension.getSize(); j++){
				if(Pop[i].getFeature(j)>model.region[j][0]&&Pop[i].getFeature(j)<model.region[j][1]){
					
				}else{
					break;
				}
			}
			if(j==dimension.getSize()){
				return false;
			}
		}
		return true;		
	}
	
	/**
	 * judge whether the instance is in the region of model, if instance is in the model, return true, else return true
	 * @param model, the model
	 * @param ins, the instance
	 * @return
	 */
	protected boolean InstanceIsInModel(Model model, Instance ins){
		for(int i=0; i<ins.getFeature().length; i++){
			if(ins.getFeature(i)>model.region[i][1]||ins.getFeature(i)<model.region[i][0]){
				return false;//if i-th dimension is not in the region, return false
			}
		}
		return true;
	}
	
	/**
	 * if ins exist in Pop, PosPop, NextPop, return true; else return false
	 * @param ins
	 * @return
	 */
	protected boolean notExistInstance(int end, Instance ins){
//		System.out.println("notExistInstance");
		int i,j;
		for(i=0; i<this.PositiveNum;i++){
			if(ins.Equal(PosPop[i])){
				return false;
			}
		}
		for(i=0;i<this.SampleSize;i++){
			if(ins.Equal(Pop[i])){
				return false;
			}
		}
		for(i=0;i<end;i++){
			if(ins.Equal(NextPop[i])){
				return false;
			}
		}
		return true;
		
	}
	
	/**
	 * update set PosPop using NextPop
	 * 
	 */
	protected void UpdatePosPop(){
//		System.out.println("UpdatePosPop");
		Instance TempIns = new Instance(dimension);
		int j;
		for(int i=0; i<this.SampleSize; i++){
			for(j=0; j<this.PositiveNum; j++){
				if(PosPop[j].getValue()>Pop[i].getValue()){
					break;
				}
			}
			if(j<this.PositiveNum){
//				((ObjectFunction)task).updateInstanceRegion(Pop[i]);
				TempIns=Pop[i];
				Pop[i]=PosPop[this.PositiveNum-1];
				for(int k=this.PositiveNum-1; k>j;k--){
					PosPop[k]=PosPop[k-1];
				}
				PosPop[j]=TempIns;
			}
		}
		return ;
	}
	
	/**
	 * update sample set for sequential racos
	 * 
	 */
	protected void UpdateSampleSet(Instance temp){
		Instance TempIns = new Instance(dimension);
		RandomOperator ro = new RandomOperator();
		int j;
		
		for(j=0; j<this.PositiveNum; j++){
			if(PosPop[j].getValue()>temp.getValue()){
				break;
			}
		}
		if(j<this.PositiveNum){
			TempIns=temp;
			temp=PosPop[this.PositiveNum-1];
			for(int k=this.PositiveNum-1; k>j;k--){
				PosPop[k]=PosPop[k-1];
			}
			PosPop[j]=TempIns;
		}

		for(j=0; j<this.SampleSize; j++){
			if(Pop[j].getValue()>temp.getValue())
				break;
		}
		if(j<this.SampleSize){
			for(int k=this.SampleSize-1; k>j;k--){
				Pop[k]=Pop[k-1];
			}
			Pop[j]=temp;
		}
		return ;
	}
	
	/**
	 * update optimal
	 */
	protected void UpdateOptimal(){
		if (Optimal.getValue() > PosPop[0].getValue()) {
			Optimal=PosPop[0];			
		}
		return ;
	}
	
	/**
	 * after setting parameters of Racos, user call this function can obtain optimal
	 * 
	 */
	public ValueArc run(){
		
		this.BudCount = 0;
		int ChoosenPos;
		double GlobalSample;
		boolean reSample;
		/////////////////////////////////////
		double[] result = new double[6];
		double findFragTime=0;
		double findPointTime=0;
		double checkGoodOrNot=0;
		
		model = new Model(dimension.getSize());
		
		ResetModel();
		Initialize();
//		HashSet<Double> pathchoice1=new HashSet<>();
//		for(int i=0; i<PositiveNum;i++){
//			pathchoice1.add(PosPop[i].getFeature(0));
//		}
//		for(int i=0; i<SampleSize;i++){
//			pathchoice1.add(Pop[i].getFeature(0));
//		}

		int bestValueCount = 0;
		int sumCount = 0;
		int stop_sum=0;
		int total_sample=SampleSize+PositiveNum;
		int iterativeNums = this.MaxIteration;
		ArrayList<Double> arrayListBestValues = new ArrayList<>();
		// batch Racos
		if (!this.on_off){
			double preBestValue = 0;
			// for each loop
			for(int i=1; i<this.MaxIteration; i++){
				double bestValue = getOptimal().getValue();
				System.out.println(i + " " + bestValue);
				if(bestValue < 0) {
					arrayListBestValues.add(bestValue + 100000);
//					break;
				}
				if(bestValue < -10000*(auto_num-1) && i != 1){
					if(Math.abs(bestValue-preBestValue) < 0.00001){
						bestValueCount++;
						if(bestValueCount>30) { // larger convergence ratio ?
							iterativeNums = i;
							break;
						}
					}
					else{
						bestValueCount = 0;
					}
				}

				//a choice is a bad point for a long time
				if(optimal&&bestValue < -10000 &&i%50==0){
					double time1 = System.currentTimeMillis();
					getFragment();
					double time2 = System.currentTimeMillis();
					findFragTime+=(time2 - time1) / 1000;

					time1 = System.currentTimeMillis();

					for (Map.Entry<Integer, Double> entry : eachBest.entrySet()) {
						if(badPoint.contains(entry.getKey())) continue;
						else if(occurtime.get(entry.getKey())>20&&entry.getValue()>0){
							badPoint.add(entry.getKey());
							System.out.println("******" +entry.getKey()+ " holds "+entry.getValue()+"******");
						}
					}
					time2 = System.currentTimeMillis();
					findPointTime+=(time2 - time1) / 1000;
				}

				preBestValue = bestValue;
				// for each sample in a loop
				for(int j=0; j<this.SampleSize; j++){	
					reSample = true;
					while (reSample) {
						ResetModel();
						ChoosenPos = ro.getInteger(0, this.PositiveNum - 1);
						GlobalSample = ro.getDouble(0, 1);
						if (GlobalSample >= this.RandProbability) {
						} else {	
							ShrinkModel(PosPop[ChoosenPos]);//shrinking model
							setRandomBits();//set uncertain bits							
						}
						NextPop[j] = RandomInstance(PosPop[ChoosenPos]);//sample
						//pathchoice1.add(NextPop[j].getFeature(0));
//						if (notExistInstance(j, NextPop[j]) ) {
						if (notExistInstance(j, NextPop[j]) ) {
							double start = System.currentTimeMillis();
							boolean isGood=not_a_bad_Instance(NextPop[j]);
							double end = System.currentTimeMillis();
							checkGoodOrNot+=(end-start)/1000;
							if(isGood) {

								double val = task.getValue(NextPop[j]);
								NextPop[j].setValue(val);//query
								total_sample++;
								reSample = false;
								int path_tmp = (int) NextPop[j].getFeature(0);

								if (optimal) {
									if (eachBest.containsKey(path_tmp)){
										double last_value=eachBest.get(path_tmp);
										if(last_value>val){
											eachBest.put(path_tmp,val);
										}
										int occur=occurtime.get(path_tmp);
										occurtime.put(path_tmp,occur+1);
									}else{
										eachBest.put(path_tmp,val);
										occurtime.put(path_tmp,1);
									}
									double time1=System.currentTimeMillis();
									for(int l=0;l<auto_num;l++){
										int choice=combin.get(path_tmp).get(l);
										ArrayList<Integer> path=PathMap.get(choice);
										FeasibleInfo feasibleInfo;

										if(feasib.get(l).containsKey(choice)){
											feasibleInfo=feasib.get(l).get(choice);
										}else{
											feasibleInfo=new FeasibleInfo(path);
										}
										feasibleInfo.setInfeasible(task.getInfeasibleLoc(l));
										feasib.get(l).put(choice,feasibleInfo);

									}
									double time2=System.currentTimeMillis();
									recordFragTime+=(time2-time1)/1000;

//									if (eachPath.containsKey(path_tmp)) {
//										PathInfo tmp = eachPath.get(path_tmp);
//										if (tmp.getBest_value() > val) tmp.setBest_value(val);
//
//										double time1 = System.currentTimeMillis();
//										tmp.setInfeasible1(task.getInfeasibleLoc1());
//										tmp.setInfeasible2(task.getInfeasibleLoc2());
//										double time2 = System.currentTimeMillis();
//										tmp.setPath1(task.getPath1());
//										tmp.setPath2(task.getPath2());
//									} else {
//										PathInfo tmp = new PathInfo(1, val);
//										double time1 = System.currentTimeMillis();
//										tmp.setInfeasible1(task.getInfeasibleLoc1());
//										tmp.setInfeasible2(task.getInfeasibleLoc2());
//										double time2 = System.currentTimeMillis();
//										tmp.setPath1(task.getPath1());
//										tmp.setPath2(task.getPath2());
//										eachPath.put(path_tmp, tmp);
//									}
								}

							}
							else{
								stop_sum++;
							}
//							NextPop[j].setValue(task.getValue(NextPop[j]));//query
//							reSample = false;
						}

					}
//					if(k == 100){
//						System.out.println("reset reSample");
//						NextPop[j].setValue(task.getValue(NextPop[j]));
//					}
				}				
				//copy NextPop
				for (int j = 0; j < this.SampleSize; j++) {
					Pop[j] = NextPop[j];
				}				
				UpdatePosPop();//update PosPop set				
				UpdateOptimal();//obtain optimal	
			}
		}else{
			// sequential Racos
			Instance new_sample = null;
			for(; BudCount<this.Budget; ){	
				reSample = true;
				while (reSample) {		
					ResetModel();
					ChoosenPos = ro.getInteger(0, this.PositiveNum - 1);
					GlobalSample = ro.getDouble(0, 1);
					if (GlobalSample >= this.RandProbability) {
					} else {
						//ShrinkModel(PosPop[ChoosenPos]);//shrinking model
						setRandomBits();//set uncertain bits						
					}
					new_sample = RandomInstance(PosPop[ChoosenPos]);//sample
					if (notExistInstance(0, new_sample)) {
						new_sample.setValue(task.getValue(new_sample));//query
						BudCount++;
						reSample = false;
					}
				}
				UpdateSampleSet(new_sample);//update PosPop set
				UpdateOptimal();//obtain optimal

			}
		}
//		double wrong_frag1,wrong_frag2,miss_frag1,miss_frag2;
//		wrong_frag1=badFragment1.size();
//		miss_frag1=0;
//		wrong_frag2=badFragment2.size();
//		miss_frag2=0;
//		if(badFragment1.containsKey(4.5)){
//			wrong_frag1--;
//		}else{
//			miss_frag1++;
//		}
//		if(badFragment1.containsKey(4.6)){
//			wrong_frag1--;
//		}else{
//			miss_frag1++;
//		}
//		if(badFragment2.containsKey(4.5)){
//			wrong_frag2--;
//		}else{
//			miss_frag2++;
//		}
//		if(badFragment2.containsKey(4.6)){
//			wrong_frag2--;
//		}else{
//			miss_frag2++;
//		}

		System.out.println("stop simulation "+Integer.toString(stop_sum)+" times");
		System.out.println("Totally sampling "+Integer.toString(total_sample)+" times");
		System.out.println("Bad points set contains "+ Integer.toString(badPoint.size())+" choices");
		for(int n=0;n<auto_num;n++){
			System.out.println("Bad fragment"+Integer.toString(n)+" set contains "+ Integer.toString(badFragment.get(n).size())+" fragments");
		}
		System.out.println("\nTotally try "+eachBest.size()+" different combined paths.");
		ObjectFunction of = (ObjectFunction)task;
		of.valueArc.iterativeNums = iterativeNums;
		of.valueArc.arrayListBestValues = arrayListBestValues;
		of.valueArc.cover=eachBest.size();
		of.valueArc.time_cost=new double[]{findFragTime,findPointTime,checkGoodOrNot,recordFragTime,stop_sum,total_sample,badPoint.size()};
		//of.valueArc.accuracy=new double[]{wrong_frag1,wrong_frag2,miss_frag1,miss_frag2};
		return of.valueArc;
	}

	private boolean not_a_bad_Instance(Instance instance) {
		if(badPoint.isEmpty()) return true;
		else{
			double last=-1;
			int path_tmp=(int)instance.getFeature(0);

			return !badPoint.contains(path_tmp);
//			else {
//
//				int choice1=combin.get(path_tmp).get(0);
//				int choice2=combin.get(path_tmp).get(1);
//				ArrayList<Integer> path1=PathMap.get(choice1);
//				ArrayList<Integer> path2=PathMap.get(choice2);
//				if(!badFragment1.isEmpty()){
//					for(int i=1;i<path1.size();i++){
//						String front=Integer.toString(path1.get(i-1));
//						String back=Integer.toString(path1.get(i));
//						int number_zero=0;
//						for(int m= back.length()-1;m>=0;m--){
//							if(back.charAt(m)=='0') number_zero++;
//							else break;
//						}
//						front+=".";
//						front+=back;
//						double tmp=Double.parseDouble(front);
//						if(badFragment1.containsKey(tmp)&&badFragment1.get(tmp)==number_zero) return false;
//					}
//				}
//				if(!badFragment2.isEmpty()){
//					for(int i=1;i<path2.size();i++){
//						String front=Integer.toString(path2.get(i-1));
//						String back=Integer.toString(path2.get(i));
//						int number_zero=0;
//						for(int m= back.length()-1;m>=0;m--){
//							if(back.charAt(m)=='0') number_zero++;
//							else break;
//						}
//						front+=".";
//						front+=back;
//						double tmp=Double.parseDouble(front);
//						if(badFragment2.containsKey(tmp)&&badFragment2.get(tmp)==number_zero) return false;
//					}
//				}
//
//			}
		}
	}

	public void getFragment(){
		int n=0;
		while(n<auto_num){
			HashSet<Integer> not_good=new HashSet<>();
			for(Map.Entry<Integer, FeasibleInfo> entry : feasib.get(n).entrySet()){
				ArrayList<Integer> path=entry.getValue().getPath();
				int pathlen=path.size();
				if(pathlen<2) continue;

				List<List<Boolean>> curlocList=entry.getValue().getInfeasible();
				int occuretime=curlocList.size();
				if(occuretime<20) continue;
				for(int i=1;i<pathlen;i++){
					int k;
					for(k=0;k<curlocList.size();k++){
						if(curlocList.get(k).get(i)&&curlocList.get(k).get(i-1)) break;
					}
					if(k==occuretime){
						//path[i-1].path[i] as key
						String front=Integer.toString(path.get(i-1));
						String back=Integer.toString(path.get(i));
						int number_zero=0;
						for(int m= back.length()-1;m>=0;m--){
							if(back.charAt(m)=='0') number_zero++;
							else break;
						}
						front+=".";
						front+=back;
						double tmp=Double.parseDouble(front);
						if(badFragment.get(n).containsKey(tmp)) continue;
						badFragment.get(n).put(tmp,number_zero);
						System.out.println("----Bad fragment "+front+" -----");


						for(int m=0;m<PathMap.size();m++){
							ArrayList<Integer> path_tmp=PathMap.get(m);
							for(int l=0;l<path_tmp.size();l++){
								if(Objects.equals(path_tmp.get(l), path.get(i - 1)) &&l!=path_tmp.size()-1&& Objects.equals(path_tmp.get(l + 1), path.get(i))){
									not_good.add(m);
									break;
								}
							}
						}


					}

				}
			}
			for(int i=0;i<combin.size();i++){
				if(not_good.contains(combin.get(i).get(n))){
					badPoint.add(i);
					System.out.println("******"+ i +"be found by badFrag ******");
				}
			}
			n++;
		}
	}


	public ValueArc CEM(){
		int max_iter = 100;
		int sample_size = 40;
		int elite_size = 10;
		double[] gaussion_mean = init_mean();
		double[] gaussion_variance = init_variance();

		for(int i = 0;i < max_iter;++i){
			Instance[] instances = gaussionSampling(sample_size,gaussion_mean,gaussion_variance);
			for(int j = 0;j < sample_size;++j){
				instances[j].setValue(task.getValue(instances[j]));
			}
			java.util.Arrays.sort(instances,new InstanceComparator());

			if(i == 0 || Optimal.getValue() > instances[0].getValue())
				Optimal = instances[0];
			System.out.println("iteration" + i + " best value :" + task.getValue(Optimal));

			//update mean and std
			for(int j = 0;j < dimension.getSize();++j){
				double mean_j = 0.0,variance_j = 0.0;
				for(int k = 0;k < elite_size;++k){
					mean_j += instances[k].getFeature(j);
				}
				mean_j /= elite_size;
				for(int k = 0;k < elite_size;++k){
					variance_j += Math.pow(instances[k].getFeature(j) - mean_j,2);
				}
				variance_j /= (elite_size - 1);
				gaussion_mean[j] = mean_j;
				gaussion_variance[j] = variance_j;
			}
		}
		ObjectFunction of = (ObjectFunction)task;
		return of.valueArc;
	}

	public Instance[] gaussionSampling(int sample_size,double[] mean,double[] variance){
		Instance[] instances = new Instance[sample_size];
		for(int i = 0;i < sample_size;++i){
			instances[i] = new Instance(dimension);
			for(int j = 0;j < dimension.getSize();++j){
				double t = ro.getGaussion(mean[j],variance[j]);
				while(t < dimension.getRegion(j)[0] || t > dimension.getRegion(j)[1]){
					t = ro.getGaussion(mean[j],variance[j]);
				}
				instances[i].setFeature(j,t);
			}
		}
		return instances;
	}

	public double[] init_mean(){
		double[] mean = new double[dimension.getSize()];
		for(int i = 0;i < dimension.getSize();++i){
			mean[i] = (dimension.getRegion(i)[0] + dimension.getRegion(i)[1]) / 2.0;
		}
		return mean;
	}

	public double[] init_variance(){
		double[] variance = new double[dimension.getSize()];
		for(int i = 0;i < dimension.getSize();++i)
			variance[i] = Math.pow((dimension.getRegion(i)[1] - dimension.getRegion(i)[0]),2); // may be too large for narrow range
		return variance;
	}

	public ValueArc GA(){
		double mutationProb = 0.3; // the probability of mutation
		int max_iter = 20;
		int sample_size = 150;
		int eliteNum = 20;
		Instance[] instances = new Instance[sample_size];
		for(int i = 0;i < sample_size;++i) instances[i] = RandomInstance();

		for(int i = 0;i < max_iter;++i){
			for(int j = 0;j < sample_size;++j) instances[j].setValue(task.getValue(instances[j]));

			java.util.Arrays.sort(instances,new InstanceComparator());

			if(i == 0 || Optimal.getValue() > instances[0].getValue())
				Optimal = instances[0];

			System.out.println("iteration" + i + " best value :" + Optimal.getValue());

			instances = crossover(instances,eliteNum,sample_size);

			mutation(instances,mutationProb);
		}
		ObjectFunction of = (ObjectFunction)task;
		return of.valueArc;
	}

	public Instance[] crossover(Instance[] instances,int eliteNum,int sample_size){
		int currentPos = 0;
		Instance[] newInstances = new Instance[sample_size];
		for(int i = 0;i < eliteNum;++i){
			for(int j = i + 1;j < eliteNum;++j){
				newInstances[currentPos] = crossoverBetweenTwoIns(instances[i],instances[j]);
				if(++currentPos >= sample_size) {
					i = eliteNum;
					break;
				}
			}
		}
		for(int i = 0;i < sample_size && currentPos < sample_size;++i,++currentPos)
			newInstances[currentPos] = instances[i];
		return newInstances;
	}

	public void mutation(Instance[] instances,double mutationProb){
		int instancePosition = (int)(Math.random() * instances.length);
		for(int i = 0;i < dimension.getSize();++i){
			if(Math.random() > mutationProb) continue;
			int mutationPos = (int)(Math.random() * 8);
			byte[] bytes = doubleToBytes(instances[instancePosition].getFeature(i));
			bytes[mutationPos] = (byte) (~(long) (bytes[mutationPos] & 0xff));
		}
	}

	public Instance crossoverBetweenTwoIns(Instance ins_i,Instance ins_j){
		Instance new_ins = new Instance(dimension.getSize());
		for(int i = 0;i < dimension.getSize();++i){
			byte[] arr_i = doubleToBytes(ins_i.getFeature(i));
			byte[] arr_j = doubleToBytes(ins_j.getFeature(i));
			int crossPos = (int)(Math.random() * 7);
			for(int j = crossPos + 1;j < 8;++j)
				arr_i[j] = arr_j[j];
			new_ins.setFeature(i,bytesToDouble(arr_i));
		}
		return new_ins;
	}

	public byte[] doubleToBytes(double d){
		long value = Double.doubleToRawLongBits(d);
		byte[] bytes = new byte[8];
		for(int i = 0;i < 8;++i){
			bytes[i] = (byte) ((value >> 8 * i) & 0xff);
		}
		return bytes;
	}

	public double bytesToDouble(byte[] arr){
		long value = 0;
		for(int i = 0;i < 8;++i){
			value |= ((long) (arr[i] & 0xff)) << (8 * i);
		}
		return Double.longBitsToDouble(value);
	}

	public ValueArc RRT(){
		int max_iter = 25;
		int sample_size = 100;
		int elite_size = 20;

		Instance[] instances = new Instance[sample_size];

		for(int i = 0;i < sample_size;++i) instances[i] = RandomInstance();

		for(int i = 0;i < max_iter;++i){
			for(int k = 0;k < sample_size;++k) instances[k].setValue(task.getValue(instances[k]));

			java.util.Arrays.sort(instances,new InstanceComparator());

			if(i == 0 || Optimal.getValue() > instances[0].getValue()) Optimal = instances[0];

			System.out.println("iteration" + i + " best value :" + Optimal.getValue());

			instances = extend(instances,elite_size,sample_size);

//			for(int j = elite_size;j < sample_size;++j) instances[j] = RandomInstance();

		}

		ObjectFunction of = (ObjectFunction)task;
		return of.valueArc;
	}

	public Instance[] extend(Instance[] instances,int elite_size,int sample_size){
        int extend_num = 50;
        for(int i = 0;i < extend_num;++i){
            int chosenPos = ro.getInteger(elite_size,sample_size - 1);
            int chosenDimension = ro.getInteger(0,dimension.getSize() - 1);
            double delta = ro.getDouble(-0.5,0.5);
            if(!dimension.getType(chosenDimension)){ // discrete
            	if(delta <= 0) delta = -1;
            	else delta = 1;

			}
			double newValue = instances[chosenPos].getFeature(chosenDimension) + delta;
			if(newValue >= dimension.getRegion(chosenDimension)[0] && newValue <= dimension.getRegion(chosenDimension)[1]) {
				instances[chosenPos].setFeature(chosenDimension, newValue);
//				System.out.println("success");
			}
        }
        return instances;
	}

	public ValueArc monte(){
		int max_iter = 50;
		int sample_size = 100;
		int elite_size = 10;

		Instance[] instances = new Instance[sample_size];

		for(int i = 0;i < sample_size;++i) instances[i] = RandomInstance();

		for(int i = 0;i < max_iter;++i){
			for(int k = 0;k < sample_size;++k) instances[k].setValue(task.getValue(instances[k]));

			java.util.Arrays.sort(instances,new InstanceComparator());

			if(i == 0 || Optimal.getValue() > instances[0].getValue()) Optimal = instances[0];

			System.out.println("iteration" + i + " best value :" + Optimal.getValue());

			instances = rejectSampling(instances,elite_size,sample_size);
		}

		ObjectFunction of = (ObjectFunction)task;
		return of.valueArc;
	}

	public Instance[] rejectSampling(Instance[] instances,int elite_size,int sample_size){
		int sampling_num = 50;
		for(int i = 0;i < sampling_num;++i){
			int chosenPos = ro.getInteger(elite_size,sample_size - 1);
			int chosenDimension = ro.getInteger(0,dimension.getSize() - 1);
			double max_v = dimension.getRegion(chosenDimension)[0],min_v = dimension.getRegion(chosenDimension)[1];
			for(int j = 0;j < elite_size;++j){
				max_v = Math.max(max_v,instances[j].getFeature(chosenDimension));
				min_v = Math.min(min_v,instances[j].getFeature(chosenDimension));
			}
			max_v = Math.min(max_v + 0.5,dimension.getRegion(chosenDimension)[1]);
			min_v = Math.max(min_v - 0.5,dimension.getRegion(chosenDimension)[0]);

			instances[chosenPos].setFeature(chosenDimension,ro.getDouble(min_v,max_v));
		}
		return instances;
	}
}
