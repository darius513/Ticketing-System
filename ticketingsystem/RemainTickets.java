package ticketingsystem;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;

import static java.lang.Math.random;
import static java.lang.Thread.sleep;

public class RemainTickets {
    private AtomicStampedReference<int []> globalRemainTickets;
    private ThreadLocalRandom rand = ThreadLocalRandom.current();
    private final int seatNum;
    private final int stationNum;
    RemainTickets(int seatNum, int stationNum){
        this.seatNum = seatNum;
        this.stationNum = stationNum;
        int [] seats = new int[stationNum - 1];
        Arrays.fill(seats, seatNum * stationNum);
        this.globalRemainTickets = new AtomicStampedReference<>(seats, 0);
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
            int[] localRemain = (int[])Arrays.copyOf(globalRemainTickets_Old,this.stationNum - 1);
            if(increase){
                for(int i = departure; i < arrival; i++){
                    localRemain[i] += 1;
                }
            }else{
                for(int i = departure; i < arrival; i++){
                    localRemain[i] += (-1);
                }
            }
            if(globalRemainTickets.compareAndSet(globalRemainTickets_Old, localRemain, stamp, stamp + 1)){
                return;
            }

        }
    }



}
