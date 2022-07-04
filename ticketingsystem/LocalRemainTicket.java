package ticketingsystem;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;

import static java.lang.Math.random;
import static java.lang.Thread.sleep;

public class LocalRemainTicket {
    private ThreadLocal <int[]> localRemainTickets1;
    private ThreadLocal <int[]> localRemainTickets2;
    private AtomicStampedReference<int []> globalRemainTickets;
    private final int seatNum;
    private final int stationNum;
    LocalRemainTicket(int seatNum, int stationNum){
        this.localRemainTickets1 = ThreadLocal.withInitial(() -> {
            int [] seats = new int[stationNum - 1];
            Arrays.fill(seats, seatNum * stationNum);
            return seats;
        });
        this.localRemainTickets2 = ThreadLocal.withInitial(() -> {
            int [] seats = new int[stationNum - 1];
            Arrays.fill(seats, seatNum * stationNum);
            return seats;
        });
        this.globalRemainTickets = new AtomicStampedReference<>(localRemainTickets1.get(), 0);
        this.seatNum = seatNum;
        this.stationNum = stationNum;
    }

    public int getRemainTickets(int departure, int arrival){
        int [] remainTickets = globalRemainTickets.getReference();
        int min = remainTickets[departure];
        for(int i = departure; i < arrival; i++){
            if(min > remainTickets[i]){
                min = remainTickets[i];
            }
        }
        return min;
    }


    public void OperateRemainTickets(int departure, int arrival, boolean increase) throws InterruptedException {
        while(true){
            int [] globalRemainTickets_Old = globalRemainTickets.getReference();
            int stamp = globalRemainTickets.getStamp();
            int [] localRemain1 = localRemainTickets1.get();
            int [] localRemain2 = localRemainTickets2.get();
            int []operator;
            if(globalRemainTickets_Old == localRemain1) { //判断全局的remainTicket是否为对本地remainTicket的引用
                operator = localRemain2;
            }else{
                operator = localRemain1;
            }
            //本地的table的一个复制，为了不要改原来的table（因为这个table是线程共享的）
            System.arraycopy(globalRemainTickets_Old, 0, operator, 0, operator.length);
            if(increase){
                for(int i = departure; i < arrival; i++){
                    operator[i] += 1;
                }
            }else{
                for(int i = departure; i < arrival; i++){
                    operator[i] += (-1);
                }
            }
            if(globalRemainTickets.compareAndSet(globalRemainTickets_Old, operator, stamp, stamp + 1)){
                return;
            }

        }
    }




}
