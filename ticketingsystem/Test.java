package ticketingsystem;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Test {
//	final static int [] threadnum = new int[]{1, 4, 7, 10, 13, 16, 19, 22, 25, 28, 31, 34, 37, 40, 43, 46, 49, 52, 55, 58, 61, 64};
//	final static int [] threadnum = new int[]{ 4,  10,  16,  22,  28,  34,  40,  46,  52,  58,  64};
//	final static int threadnum = 64;
	final static int routenum = 10; // route is designed from 1 to 3
	final static int coachnum = 8; // coach is arranged from 1 to 5
	final static int seatnum = 100; // seat is allocated from 1 to 20
	final static int stationnum = 10; // station is designed from 1 to 5

	final static int testnum = 100000;
//	final static int retpc = 10; // return ticket operation is 10% percent
//	final static int buypc = 40; // buy ticket operation is 30% percent
//	final static int inqpc = 100; //inquiry ticket operation is 60% percent
	final static int NUMBER_OF_RUN = 50;
	final static int retpc = 10; // return ticket operation is 10% percent
	final static int buypc = 40; // buy ticket operation is 30% percent
	final static int inqpc = 100; //inquiry ticket operation is 60% percent

	static String passengerName() {
		Random rand = new Random();
		long uid = rand.nextInt(testnum);
		return "passenger" + uid;
	}

	static AtomicLong totalTime = new AtomicLong(0);

	static AtomicInteger totalRetpcNum = new AtomicInteger(0);
	static AtomicInteger totalbuypcNum = new AtomicInteger(0);
	static AtomicInteger totalinqpcNum = new AtomicInteger(0);
	static AtomicLong totalretpcRuntime = new AtomicLong(0L);
	static AtomicLong totalbuypcRuntime = new AtomicLong(0L);
	static AtomicLong totalinqpcRuntime = new AtomicLong(0L);

	public static void threadWork(int threadnum) throws InterruptedException {
		System.out.println("====================================== threadNum " + threadnum + " start ======================================");
		//每个线程数执行10次，取平均值
		long TOTAL_TIME = 0;
		for(int j = 0; j < NUMBER_OF_RUN; j++){
			final TicketingDS tds = new TicketingDS(routenum, coachnum, seatnum, stationnum, threadnum);
			Thread[] threads = new Thread[threadnum];
			final long startTime = System.nanoTime();
			for (int i = 0; i< threadnum; i++) {
				threads[i] = new Thread(new Runnable() {
					public void run() {
						final long THREAD_INNER_START_TIME = System.nanoTime();
						//线程开始时间，存储在线程局部变量中
						Random rand = new Random();
						Ticket ticket = new Ticket();
						ArrayList<Ticket> soldTicket = new ArrayList<Ticket>();
						int retpcNum = 0;
						int buypcNum = 0;
						int inqpcNum = 0;
						long retpcRuntime = 0L;
						long buypcRuntime = 0L;
						long inqpcRuntime = 0L;
						for (int i = 0; i < testnum; i++) {
							int sel = rand.nextInt(inqpc);
							if (0 <= sel && sel < retpc && soldTicket.size() > 0) { // return ticket
								int select = rand.nextInt(soldTicket.size());
								long preTime = System.nanoTime();
								if ((ticket = soldTicket.remove(select)) != null) {

									if (tds.refundTicket(ticket)) {
										long postTime = System.nanoTime();
										retpcNum++;
										retpcRuntime += (postTime - preTime);
									} else {
										retpcNum++;
										retpcRuntime += (System.nanoTime() - preTime);
									}
								} else {
									retpcNum++;
									retpcRuntime += (System.nanoTime() - preTime);
								}
							} else if (retpc <= sel && sel < buypc) { // buy ticket
								String passenger = passengerName();
								int route = rand.nextInt(routenum) + 1;
								int departure = rand.nextInt(stationnum - 1) + 1;
								int arrival = departure + rand.nextInt(stationnum - departure) + 1; // arrival is always greater than departure
								long preTime = System.nanoTime();
								if ((ticket = tds.buyTicket(passenger, route, departure, arrival)) != null) {
									long postTime = System.nanoTime();
									soldTicket.add(ticket);
									buypcNum++;
									buypcRuntime += (postTime - preTime);
								} else {
									buypcNum++;
									buypcRuntime += (System.nanoTime()  - preTime);

								}
							} else if (buypc <= sel && sel < inqpc) { // inquiry ticket

								int route = rand.nextInt(routenum) + 1;
								int departure = rand.nextInt(stationnum - 1) + 1;
								int arrival = departure + rand.nextInt(stationnum - departure) + 1; // arrival is always greater than departure
								long preTime = System.nanoTime();
								int leftTicket = tds.inquiry(route, departure, arrival);
								long postTime = System.nanoTime();
								inqpcNum++;
								inqpcRuntime += (postTime - preTime);
							}
						}

						final long THREAD_INNER_END_TIME = System.nanoTime();
						totalTime.addAndGet(THREAD_INNER_END_TIME - THREAD_INNER_START_TIME);
						totalRetpcNum.addAndGet(retpcNum);
						totalbuypcNum.addAndGet(buypcNum);
						totalinqpcNum.addAndGet(inqpcNum);
						totalretpcRuntime.addAndGet(retpcRuntime);
						totalbuypcRuntime.addAndGet(buypcRuntime);
						totalinqpcRuntime.addAndGet(inqpcRuntime);

					}
				});
				threads[i].start();
			}
			for (int i = 0; i< threadnum; i++) {
				threads[i].join();
			}

			//一个线程完成所有任务的平均时间，由于是并行的，可以认为是所有线程完成任务的平均时间
			Long runTime = System.nanoTime() - startTime;
			TOTAL_TIME += runTime;
		}

		//一般的方法计算时间
//		double averageRuntime = (double)TOTAL_TIME / NUMBER_OF_RUN /1000000;
//		double Throughput = (double) (threadnum *testnum)/(averageRuntime);
//		System.out.println("TotalTime: " + averageRuntime + "ms    " + "Throughput: " + Throughput);


		//统计方法的执行时间，作为运行总时间
		double averageRuntime = (double) totalTime.get() / threadnum / NUMBER_OF_RUN /1000000;
		double Throughput = (double) (threadnum *testnum)/(averageRuntime);
		System.out.println("TotalTime: " + averageRuntime + "ms    " + "Throughput: " + Throughput);

		double retpc = (double) totalretpcRuntime.get() / totalRetpcNum.get() ;
		double inqpc = (double) totalinqpcRuntime.get() / totalinqpcNum.get() ;
		double buypc = (double) totalbuypcRuntime.get() / totalbuypcNum.get() ;
//		System.out.println("retpcRunTime: " + String.format("%.2f", (double)totalretpcRuntime.get()) + "ns");
//		System.out.println("inqpcRunTime: " + String.format("%.2f", (double)totalinqpcRuntime.get()) + "ns" );
//		System.out.println("buypcRunTime: " + String.format("%.2f", (double)totalbuypcRuntime.get()) + "ns");
		System.out.println("retpc: " + String.format("%.2f", retpc) + "ns/p");
		System.out.println("inqpc: " + String.format("%.2f", inqpc) + "ns/p" );
		System.out.println("buypc: " + String.format("%.2f", buypc) + "ns/p");
		System.out.println("====================================== threadNum " + threadnum + " end ======================================");
		System.out.println();
		try{
			File file = new File("/pub/home/user008/experiment/result.txt");
			if (!file.exists()) {
				file.createNewFile();
			}
			BufferedWriter writer  = new BufferedWriter(new FileWriter(file.getAbsoluteFile(),true));
			writer.write(averageRuntime + " " + Throughput +" " + retpc + " " + inqpc + " " + buypc + "\n");
			writer.flush();
			writer.close();
		}catch ( IOException e){

		}
	}

	public static void main(String[] args) throws InterruptedException {

//		for(int i = 0; i < threadnum.length; i++){
//			threadWork(threadnum[i]);
//		}
		threadWork(Integer.parseInt(args[0]));

	}
}
