package ticketingsystem;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 单座位状态位图实现
 */
public class TrainImpl1 {
    public final int coachnum;
    public final int seatNumOfEachCoach;
    public final int totalSeatNum;
    public final int stationnum;
    //存储每个座位的占座情况，每个Long用位图表示该座位在不同station的占座情况。
    public AtomicLong[] seatState;
    //存储每个车厢占座情况，越大表示该车厢人越多
    //不用Atomic，是想这个占座密度可以不用那么准确，用Atomic会降低性能
    public Integer [] coachState;
    public RemainTickets remainTickets;
    public LocalRemainTicket localRemainTicket;
    TrainImpl1(int coachnum, int seatnum, int stationnum){
        this.coachnum = coachnum;
        this.seatNumOfEachCoach = seatnum;
        this.totalSeatNum = coachnum * seatnum;
        this.stationnum = stationnum;
        this.seatState = new AtomicLong[this.totalSeatNum];
        this.coachState = new Integer[coachnum];
        for(int i=0; i<this.totalSeatNum; i++){
            this.seatState[i] = new AtomicLong(0);
        }
        for(int i=0;i<this.coachnum;i++){
            this.coachState[i] = 0;
        }

    }

    /**
     * 查找人最少的车厢
     * @return 车厢号
     */
    public int findColdCoach(){
        int min = 0;
        int ColdCoach = 0;
        for(int i = 0; i<this.coachState.length;i++){
            if(min > this.coachState[i]){
                min = this.coachState[i];
                ColdCoach = i;
            }
        }
        return ColdCoach;
    }

    /**
     * 创建掩码
     * @param departure 始发站
     * @param arrival 终点站
     * @return 掩码
     */
    public Long makeMask(int departure, int arrival){
        return ((0x01L << (arrival - departure)) -1) << departure;
    }

    /**
     * 占座
     * @param departure 始发站
     * @param arrival 终点站
     * @return 车票/空
     */
    public int reserveSeat(int departure, int arrival, int beginSeat) throws InterruptedException {
        long mask = makeMask(departure, arrival);
        beginSeat = beginSeat % this.totalSeatNum;
        long State;
        int index;
        for(int i = 0;i<this.totalSeatNum;i++){
            index = (i+beginSeat) % this.totalSeatNum;
            State = seatState[index].get();
            while((mask & State) == 0){
                if(seatState[index].compareAndSet(State, (State | mask))){
//                  remainTickets.OperateRemainTickets(departure, arrival, false);
//                  localRemainTicket.OperateRemainTickets(departure, arrival, false);
//                    return i;
                    return index;
                }
                State = seatState[index].get();
            }
        }
//            }
        return -1;
    }


    public boolean releaseSeat(int seat, int departure, int arrival) throws InterruptedException {
        long mask = makeMask(departure, arrival);
        long seatState = this.seatState[seat].get();
        while(true) {
            if (this.seatState[seat].compareAndSet(seatState, (seatState & ~mask))) {
                return true;
            }
            seatState = this.seatState[seat].get();
        }
    }

    public int remainSeat(int departure, int arrival, int beginSeat) {
        long mask = makeMask(departure, arrival);
        int remainSeat = 0;
        int index;
        for(int i=0;i<this.totalSeatNum;i++){
            if((this.seatState[i].get() & mask) == 0){
                remainSeat++;
            }
        }
        return remainSeat;
    }
}
